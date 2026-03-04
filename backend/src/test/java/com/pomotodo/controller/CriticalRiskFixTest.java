package com.pomotodo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pomotodo.dto.TeamTaskRequest;
import com.pomotodo.dto.TaskResponse;
import com.pomotodo.entity.FocusLog;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.Team;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.FocusLogRepository;
import com.pomotodo.repository.TaskRepository;
import com.pomotodo.repository.TeamMemberRepository;
import com.pomotodo.repository.TeamRepository;
import com.pomotodo.repository.UserRepository;
import com.pomotodo.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriticalRiskFixTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private FocusLogRepository focusLogRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @Mock
    private UserDetails userDetails;

    private ObjectMapper objectMapper;
    private TaskController taskController;
    private FocusController focusController;
    private User owner;
    private User outsider;
    private Team team;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        taskController = new TaskController(taskRepository, userRepository, teamRepository, teamMemberRepository);
        focusController = new FocusController(focusLogRepository, userRepository, pushNotificationService);

        owner = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@test.com")
                .password("encoded-password")
                .enabled(true)
                .role("USER")
                .build();

        outsider = User.builder()
                .id(2L)
                .username("outsider")
                .email("outsider@test.com")
                .password("encoded-password")
                .enabled(true)
                .role("USER")
                .build();

        team = Team.builder()
                .id(10L)
                .name("alpha")
                .owner(owner)
                .isActive(true)
                .build();

        when(userDetails.getUsername()).thenReturn("owner");
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
    }

    @Test
    void taskApiShouldNotExposeUserOrPasswordFields() throws Exception {
        Task task = new Task();
        task.setId(100L);
        task.setText("secure task");
        task.setPriority("high");
        task.setStatus("TODO");
        task.setUser(owner);
        task.setCreatedAt(LocalDateTime.now());

        when(taskRepository.findByUserIdOrderByCreatedAtDesc(owner.getId())).thenReturn(List.of(task));

        List<TaskResponse> result = taskController.getAllTasks(userDetails);
        String json = objectMapper.writeValueAsString(result);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("secure task");
        assertThat(json).doesNotContain("\"user\":");
        assertThat(json).doesNotContain("\"password\"");
    }

    @Test
    void focusLogsApiShouldNotExposeUserOrPasswordFields() throws Exception {
        FocusLog log = new FocusLog();
        log.setId(200L);
        log.setDate(LocalDate.now());
        log.setDuration(1500);
        log.setStartTime(LocalDateTime.now().minusMinutes(25));
        log.setEndTime(LocalDateTime.now());
        log.setUser(owner);

        when(focusLogRepository.findByUserIdAndDateOrderByStartTimeDesc(owner.getId(), LocalDate.now()))
                .thenReturn(List.of(log));

        ResponseEntity<?> response = focusController.getFocusLogs(userDetails);
        String json = objectMapper.writeValueAsString(response.getBody());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(json).doesNotContain("\"user\":");
        assertThat(json).doesNotContain("\"password\"");
    }

    @Test
    void createTeamTaskShouldRejectAssigneeOutsideTeam() {
        TeamTaskRequest request = new TeamTaskRequest();
        request.setText("team task");
        request.setAssignedToId(outsider.getId());

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), outsider.getId())).thenReturn(false);

        assertThatThrownBy(() -> taskController.createTeamTask(team.getId(), request, userDetails))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("INVALID_ASSIGNEE", "Assignee must be a team member");
        verify(userRepository, never()).findById(outsider.getId());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTeamTaskShouldRejectAssigneeOutsideTeam() {
        Task task = new Task();
        task.setId(300L);
        task.setText("existing task");
        task.setPriority("medium");
        task.setStatus("TODO");
        task.setUser(owner);
        task.setTeam(team);

        TeamTaskRequest request = new TeamTaskRequest();
        request.setAssignedToId(outsider.getId());

        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        when(taskRepository.findByIdAndTeamId(task.getId(), team.getId())).thenReturn(Optional.of(task));
        when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), outsider.getId())).thenReturn(false);

        assertThatThrownBy(() -> taskController.updateTeamTask(team.getId(), task.getId(), request, userDetails))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("INVALID_ASSIGNEE", "Assignee must be a team member");
        verify(userRepository, never()).findById(outsider.getId());
        verify(taskRepository, never()).save(any(Task.class));
    }
}
