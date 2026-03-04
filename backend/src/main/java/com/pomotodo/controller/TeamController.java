package com.pomotodo.controller;

import com.pomotodo.entity.Team;
import com.pomotodo.entity.TeamMember;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.TeamMemberRepository;
import com.pomotodo.repository.TeamRepository;
import com.pomotodo.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private static final String INVITE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CHARS.charAt(secureRandom.nextInt(INVITE_CHARS.length())));
        }
        return code.toString();
    }

    @PostMapping
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);

        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (teamRepository.existsByInviteCode(inviteCode));

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .inviteCode(inviteCode)
                .owner(user)
                .isActive(true)
                .build();
        Team savedTeam = teamRepository.save(team);

        TeamMember ownerMember = TeamMember.builder()
                .team(savedTeam)
                .user(user)
                .role(TeamMember.Role.OWNER)
                .isActive(true)
                .build();
        teamMemberRepository.save(ownerMember);

        return ResponseEntity.ok(convertToDTO(savedTeam));
    }

    @GetMapping
    public ResponseEntity<?> getMyTeams(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);

        List<Team> ownedTeams = teamRepository.findByOwnerIdAndIsActiveTrueOrderByCreatedAtDesc(user.getId());
        List<TeamMember> memberTeams = teamMemberRepository.findAllByUserId(user.getId());

        Set<Long> ownedTeamIds = ownedTeams.stream().map(Team::getId).collect(Collectors.toSet());
        List<Team> joinedTeams = memberTeams.stream()
                .map(TeamMember::getTeam)
                .filter(team -> team != null && !ownedTeamIds.contains(team.getId()) && Boolean.TRUE.equals(team.getIsActive()))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("owned", ownedTeams.stream().map(this::convertToDTO).filter(Objects::nonNull).collect(Collectors.toList()));
        result.put("joined", joinedTeams.stream().map(this::convertToDTO).filter(Objects::nonNull).collect(Collectors.toList()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<?> getTeamDetail(@PathVariable Long teamId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));

        if (!isTeamMember(teamId, user.getId())) {
            throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
        }
        return ResponseEntity.ok(convertToDetailDTO(team, user.getId()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinTeam(@RequestBody JoinTeamRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Team team = teamRepository.findByInviteCode(request.getInviteCode().toUpperCase())
                .orElseThrow(() -> ApiException.badRequest("INVALID_INVITE_CODE", "Invalid invite code"));

        if (!Boolean.TRUE.equals(team.getIsActive())) {
            throw ApiException.badRequest("TEAM_INACTIVE", "Team is inactive");
        }
        if (teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), user.getId())) {
            throw ApiException.badRequest("ALREADY_MEMBER", "Already a member");
        }
        if (team.getOwner().getId().equals(user.getId())) {
            throw ApiException.badRequest("OWNER_CANNOT_JOIN", "You are the owner");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMember.Role.MEMBER)
                .isActive(true)
                .build();
        teamMemberRepository.save(member);

        return ResponseEntity.ok(convertToDTO(team));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long teamId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (!isTeamMember(teamId, user.getId())) {
            throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
        }

        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));

        List<MemberDTO> memberDTOs = members.stream()
                .map(this::convertToMemberDTO)
                .collect(Collectors.toList());

        boolean ownerInList = members.stream()
                .anyMatch(m -> m.getUser().getId().equals(team.getOwner().getId()));
        if (!ownerInList) {
            MemberDTO ownerDTO = new MemberDTO();
            ownerDTO.setId(team.getOwner().getId());
            ownerDTO.setUsername(team.getOwner().getUsername());
            ownerDTO.setEmail(team.getOwner().getEmail());
            ownerDTO.setAvatar(team.getOwner().getAvatar());
            ownerDTO.setRole("OWNER");
            ownerDTO.setJoinedAt(team.getCreatedAt());
            memberDTOs.add(0, ownerDTO);
        }
        return ResponseEntity.ok(memberDTOs);
    }

    @PutMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long teamId,
                                              @PathVariable Long userId,
                                              @RequestBody UpdateRoleRequest request,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        if (!hasManagePermission(teamId, currentUser.getId())) {
            throw ApiException.forbidden("NO_PERMISSION", "No permission");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));
        if (team.getOwner().getId().equals(userId)) {
            throw ApiException.badRequest("CANNOT_MODIFY_OWNER", "Cannot modify owner");
        }

        TeamMember.Role newRole = TeamMember.Role.valueOf(request.getRole());
        teamMemberRepository.updateRole(teamId, userId, newRole);
        return ResponseEntity.ok(Map.of("message", "Role updated"));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<?> removeMember(@PathVariable Long teamId,
                                          @PathVariable Long userId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));

        boolean isOwner = team.getOwner().getId().equals(currentUser.getId());
        boolean isSelf = currentUser.getId().equals(userId);
        if (!isOwner && !isSelf) {
            Optional<TeamMember> currentMember = teamMemberRepository.findByTeamIdAndUserId(teamId, currentUser.getId());
            if (currentMember.isEmpty() || currentMember.get().getRole() != TeamMember.Role.ADMIN) {
                throw ApiException.forbidden("NO_PERMISSION", "No permission");
            }
        }
        if (team.getOwner().getId().equals(userId)) {
            throw ApiException.badRequest("CANNOT_REMOVE_OWNER", "Cannot remove owner");
        }

        teamMemberRepository.deactivateMember(teamId, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> dissolveTeam(@PathVariable Long teamId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));
        if (!team.getOwner().getId().equals(currentUser.getId())) {
            throw ApiException.forbidden("ONLY_OWNER_CAN_DISSOLVE", "Only team owner can dissolve the team");
        }

        team.setIsActive(false);
        teamRepository.save(team);
        return ResponseEntity.ok(Map.of("message", "Team dissolved successfully"));
    }

    @GetMapping("/{teamId}/invite-link")
    public ResponseEntity<?> getInviteLink(@PathVariable Long teamId,
                                           @RequestParam(required = false, defaultValue = "http://localhost:3000") String baseUrl,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));

        if (!isTeamMember(teamId, user.getId())) {
            throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
        }

        Map<String, String> result = new HashMap<>();
        result.put("inviteCode", team.getInviteCode());
        result.put("inviteLink", baseUrl + "/team/join?code=" + team.getInviteCode());
        return ResponseEntity.ok(result);
    }

    private boolean isTeamMember(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return false;
        }
        if (team.getOwner().getId().equals(userId)) {
            return true;
        }
        return teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId);
    }

    private boolean hasManagePermission(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) {
            return false;
        }
        if (team.getOwner().getId().equals(userId)) {
            return true;
        }
        Optional<TeamMember> member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        return member.isPresent()
                && (member.get().getRole() == TeamMember.Role.ADMIN || member.get().getRole() == TeamMember.Role.OWNER);
    }

    private TeamDTO convertToDTO(Team team) {
        if (team == null) {
            return null;
        }
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setInviteCode(team.getInviteCode());
        if (team.getOwner() != null) {
            dto.setOwnerId(team.getOwner().getId());
            dto.setOwnerName(team.getOwner().getUsername());
        }
        dto.setCreatedAt(team.getCreatedAt());
        dto.setMemberCount(teamMemberRepository.countByTeamIdAndIsActiveTrue(team.getId()) + 1);
        return dto;
    }

    private TeamDetailDTO convertToDetailDTO(Team team, Long currentUserId) {
        TeamDetailDTO dto = new TeamDetailDTO();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setInviteCode(team.getInviteCode());
        dto.setOwnerId(team.getOwner().getId());
        dto.setOwnerName(team.getOwner().getUsername());
        dto.setCreatedAt(team.getCreatedAt());
        dto.setMemberCount(teamMemberRepository.countByTeamIdAndIsActiveTrue(team.getId()) + 1);
        if (team.getOwner().getId().equals(currentUserId)) {
            dto.setMyRole("OWNER");
        } else {
            teamMemberRepository.findByTeamIdAndUserId(team.getId(), currentUserId)
                    .ifPresent(member -> dto.setMyRole(member.getRole().name()));
        }
        return dto;
    }

    private MemberDTO convertToMemberDTO(TeamMember member) {
        MemberDTO dto = new MemberDTO();
        dto.setId(member.getUser().getId());
        dto.setUsername(member.getUser().getUsername());
        dto.setEmail(member.getUser().getEmail());
        dto.setAvatar(member.getUser().getAvatar());
        dto.setRole(member.getRole().name());
        dto.setJoinedAt(member.getJoinedAt());
        return dto;
    }

    @Data
    public static class CreateTeamRequest {
        private String name;
        private String description;
    }

    @Data
    public static class JoinTeamRequest {
        private String inviteCode;
    }

    @Data
    public static class UpdateRoleRequest {
        private String role;
    }

    @Data
    public static class TeamDTO {
        private Long id;
        private String name;
        private String description;
        private String inviteCode;
        private Long ownerId;
        private String ownerName;
        private LocalDateTime createdAt;
        private long memberCount;
    }

    @Data
    public static class TeamDetailDTO extends TeamDTO {
        private String myRole;
    }

    @Data
    public static class MemberDTO {
        private Long id;
        private String username;
        private String email;
        private String avatar;
        private String role;
        private LocalDateTime joinedAt;
    }
}

