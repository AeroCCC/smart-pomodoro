package com.pomotodo.controller;

import com.pomotodo.dto.TeamTaskRequest;
import com.pomotodo.dto.TeamTaskResponse;
import com.pomotodo.dto.TaskResponse;
import com.pomotodo.exception.ApiException;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.Team;
import com.pomotodo.entity.TeamMember;
import com.pomotodo.entity.User;
import com.pomotodo.repository.TaskRepository;
import com.pomotodo.repository.TeamMemberRepository;
import com.pomotodo.repository.TeamRepository;
import com.pomotodo.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Data
    public static class TaskRequest {
        private String text;
        private String priority = "medium";
        private String deadline;
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isEmpty()) {
            return null;
        }
        try {
            if (deadline.contains("T")) {
                if (deadline.length() <= 16) {
                    return LocalDateTime.parse(deadline + ":00");
                }
                return LocalDateTime.parse(deadline);
            }
            return LocalDateTime.parse(deadline.replace(" ", "T"));
        } catch (Exception e) {
            System.err.println("Failed to parse deadline: " + deadline + " - " + e.getMessage());
            return null;
        }
    }

    @Data
    public static class UpdateTaskRequest {
        private String text;
        private String priority;
        private String deadline;
        private Boolean completed;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

    @GetMapping
    public List<TaskResponse> getAllTasks(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return taskRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::convertToTaskResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getTaskStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        long total = taskRepository.countByUserId(user.getId());
        long completed = taskRepository.countByUserIdAndCompleted(user.getId(), true);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", total - completed);
        stats.put("completionRate", total > 0 ? Math.round((double) completed / total * 100) : 0);
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (request.getText() == null || request.getText().trim().isEmpty()) {
                throw ApiException.badRequest("TASK_TEXT_REQUIRED", "Task text is required");
            }
            
            User user = getCurrentUser(userDetails);
            
            Task task = new Task();
            task.setText(request.getText().trim());
            task.setPriority(request.getPriority() != null ? request.getPriority() : "medium");
            task.setDeadline(parseDeadline(request.getDeadline()));
            task.setUser(user);
            
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(convertToTaskResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TASK_CREATE_FAILED", "Failed to create task", e);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id,
                                       @RequestBody UpdateTaskRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            
            return taskRepository.findByIdAndUserId(id, user.getId())
                .map(task -> {
                    if (request.getText() != null) {
                        task.setText(request.getText());
                    }
                    if (request.getPriority() != null) {
                        task.setPriority(request.getPriority());
                    }
                    
                    if (request.getDeadline() != null) {
                        task.setDeadline(parseDeadline(request.getDeadline()));
                    }
                    
                    if (request.getCompleted() != null) {
                        task.setCompleted(request.getCompleted());
                        if (request.getCompleted() && task.getCompletedAt() == null) {
                            task.setCompletedAt(LocalDateTime.now());
                        } else if (!request.getCompleted()) {
                            task.setCompletedAt(null);
                        }
                    }
                    
                    return ResponseEntity.ok(convertToTaskResponse(taskRepository.save(task)));
                })
                .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TASK_UPDATE_FAILED", "Failed to update task", e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        
        return taskRepository.findByIdAndUserId(id, user.getId())
            .map(task -> {
                taskRepository.delete(task);
                return ResponseEntity.ok().<Void>build();
            })
            .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
    }

    // ==================== Team Task APIs ====================

    @GetMapping("/team/{teamId}")
    public ResponseEntity<?> getTeamTasks(@PathVariable Long teamId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            if (!isTeamMember(teamId, user.getId())) {
                throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
            }
            List<Task> tasks = taskRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
            List<TeamTaskResponse> response = tasks.stream()
                .map(this::convertToTeamTaskResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TEAM_TASKS_FETCH_FAILED", "Failed to fetch team tasks", e);
        }
    }

    @PostMapping("/team/{teamId}")
    @Transactional
    public ResponseEntity<?> createTeamTask(@PathVariable Long teamId,
                                           @RequestBody TeamTaskRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            if (!isTeamMember(teamId, user.getId())) {
                throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
            }
            Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ApiException.notFound("TEAM_NOT_FOUND", "Team not found"));
            Task task = new Task();
            task.setText(request.getText());
            task.setPriority(request.getPriority() != null ? request.getPriority() : "medium");
            task.setStatus(request.getStatus() != null ? request.getStatus() : "TODO");
            task.setDeadline(parseDeadline(request.getDeadline()));
            task.setUser(user);
            task.setTeam(team);
            if (request.getAssignedToId() != null) {
                if (!isTeamMember(teamId, request.getAssignedToId())) {
                    throw ApiException.badRequest("INVALID_ASSIGNEE", "Assignee must be a team member");
                }
                User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> ApiException.notFound("ASSIGNEE_NOT_FOUND", "Assignee not found"));
                task.setAssignedTo(assignee);
            }
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(convertToTeamTaskResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TEAM_TASK_CREATE_FAILED", "Failed to create team task", e);
        }
    }

    @PutMapping("/team/{teamId}/{taskId}")
    @Transactional
    public ResponseEntity<?> updateTeamTask(@PathVariable Long teamId,
                                           @PathVariable Long taskId,
                                           @RequestBody TeamTaskRequest request,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            if (!isTeamMember(teamId, user.getId())) {
                throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
            }
            Task task = taskRepository.findByIdAndTeamId(taskId, teamId)
                .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
            if (!canEditTask(task, user.getId(), teamId)) {
                throw ApiException.forbidden("NO_PERMISSION", "No permission to edit");
            }
            if (request.getText() != null) task.setText(request.getText());
            if (request.getPriority() != null) task.setPriority(request.getPriority());
            if (request.getStatus() != null) {
                task.setStatus(request.getStatus());
                if ("DONE".equals(request.getStatus())) {
                    task.setCompleted(true);
                    task.setCompletedAt(LocalDateTime.now());
                } else {
                    task.setCompleted(false);
                    task.setCompletedAt(null);
                }
            }
            if (request.getDeadline() != null) task.setDeadline(parseDeadline(request.getDeadline()));
            if (request.getAssignedToId() != null) {
                if (!isTeamMember(teamId, request.getAssignedToId())) {
                    throw ApiException.badRequest("INVALID_ASSIGNEE", "Assignee must be a team member");
                }
                User assignee = userRepository.findById(request.getAssignedToId())
                    .orElseThrow(() -> ApiException.notFound("ASSIGNEE_NOT_FOUND", "Assignee not found"));
                task.setAssignedTo(assignee);
            }
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(convertToTeamTaskResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TEAM_TASK_UPDATE_FAILED", "Failed to update team task", e);
        }
    }

    @DeleteMapping("/team/{teamId}/{taskId}")
    @Transactional
    public ResponseEntity<?> deleteTeamTask(@PathVariable Long teamId,
                                           @PathVariable Long taskId,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            Task task = taskRepository.findByIdAndTeamId(taskId, teamId)
                .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
            if (!canEditTask(task, user.getId(), teamId)) {
                throw ApiException.forbidden("NO_PERMISSION", "No permission");
            }
            taskRepository.delete(task);
            return ResponseEntity.ok(Map.of("message", "Task deleted"));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TEAM_TASK_DELETE_FAILED", "Failed to delete team task", e);
        }
    }

    @PutMapping("/team/{teamId}/{taskId}/status")
    @Transactional
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long teamId,
                                             @PathVariable Long taskId,
                                             @RequestBody Map<String, String> request,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            if (!isTeamMember(teamId, user.getId())) {
                throw ApiException.forbidden("NOT_TEAM_MEMBER", "Not a team member");
            }
            String status = request.get("status");
            if (status == null || !List.of("TODO", "IN_PROGRESS", "DONE").contains(status)) {
                throw ApiException.badRequest("INVALID_STATUS", "Invalid status");
            }
            Task task = taskRepository.findByIdAndTeamId(taskId, teamId)
                .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
            task.setStatus(status);
            if ("DONE".equals(status)) {
                task.setCompleted(true);
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setCompleted(false);
                task.setCompletedAt(null);
            }
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(convertToTeamTaskResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TASK_STATUS_UPDATE_FAILED", "Failed to update task status", e);
        }
    }

    @PutMapping("/team/{teamId}/{taskId}/assign")
    @Transactional
    public ResponseEntity<?> assignTask(@PathVariable Long teamId,
                                       @PathVariable Long taskId,
                                       @RequestBody Map<String, Long> request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            if (!hasManagePermission(teamId, user.getId())) {
                throw ApiException.forbidden("NO_PERMISSION", "No permission");
            }
            Long assigneeId = request.get("assignedToId");
            if (assigneeId == null) {
                throw ApiException.badRequest("ASSIGNEE_REQUIRED", "Assignee required");
            }
            if (!isTeamMember(teamId, assigneeId)) {
                throw ApiException.badRequest("INVALID_ASSIGNEE", "Not a team member");
            }
            Task task = taskRepository.findByIdAndTeamId(taskId, teamId)
                .orElseThrow(() -> ApiException.notFound("TASK_NOT_FOUND", "Task not found"));
            User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNEE_NOT_FOUND", "Assignee not found"));
            task.setAssignedTo(assignee);
            Task saved = taskRepository.save(task);
            return ResponseEntity.ok(convertToTeamTaskResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("TASK_ASSIGN_FAILED", "Failed to assign task", e);
        }
    }

    private boolean isTeamMember(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        if (team.getOwner().getId().equals(userId)) return true;
        return teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId);
    }

    private boolean hasManagePermission(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElse(null);
        if (team == null) return false;
        if (team.getOwner().getId().equals(userId)) return true;
        Optional<TeamMember> member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
        return member.isPresent() && 
               (member.get().getRole() == TeamMember.Role.ADMIN || member.get().getRole() == TeamMember.Role.OWNER);
    }

    private boolean canEditTask(Task task, Long userId, Long teamId) {
        if (task.getUser().getId().equals(userId)) return true;
        return hasManagePermission(teamId, userId);
    }

    private TeamTaskResponse convertToTeamTaskResponse(Task task) {
        TeamTaskResponse dto = new TeamTaskResponse();
        dto.setId(task.getId());
        dto.setText(task.getText());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setCompleted(task.isCompleted());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setDeadline(task.getDeadline());
        dto.setCreatorId(task.getUser().getId());
        dto.setCreatorName(task.getUser().getUsername());
        dto.setCreatorAvatar(task.getUser().getAvatar());
        if (task.getAssignedTo() != null) {
            dto.setAssignedToId(task.getAssignedTo().getId());
            dto.setAssignedToName(task.getAssignedTo().getUsername());
            dto.setAssignedToAvatar(task.getAssignedTo().getAvatar());
        }
        if (task.getTeam() != null) {
            dto.setTeamId(task.getTeam().getId());
            dto.setTeamName(task.getTeam().getName());
        }
        return dto;
    }

    private TaskResponse convertToTaskResponse(Task task) {
        TaskResponse dto = new TaskResponse();
        dto.setId(task.getId());
        dto.setText(task.getText());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setCompleted(task.isCompleted());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setCompletedAt(task.getCompletedAt());
        dto.setDeadline(task.getDeadline());
        return dto;
    }
}
