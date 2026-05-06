package com.pomotodo.controller;

import com.pomotodo.dto.TaskResponse;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.TaskRepository;
import com.pomotodo.repository.TeamMemberRepository;
import com.pomotodo.repository.TeamRepository;
import com.pomotodo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskControllerAiEnhancementTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserDetails userDetails;

    private TaskController taskController;
    private User user;

    @BeforeEach
    void setUp() {
        taskController = new TaskController(taskRepository, userRepository, teamRepository, teamMemberRepository);
        user = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@test.com")
                .password("encoded")
                .enabled(true)
                .role("USER")
                .build();

        when(userDetails.getUsername()).thenReturn("owner");
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(user));
    }

    @Test
    void createTaskShouldKeepLegacyRequestCompatible() {
        TaskController.TaskRequest request = new TaskController.TaskRequest();
        request.setText("Legacy task");
        request.setPriority("medium");

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            task.setCreatedAt(LocalDateTime.of(2026, 4, 17, 10, 0));
            return task;
        });

        ResponseEntity<?> response = taskController.createTask(request, userDetails);
        TaskResponse body = (TaskResponse) response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getText()).isEqualTo("Legacy task");
        assertThat(body.getCompletionDefinition()).isNull();
        assertThat(body.getEstimatedPomodoros()).isNull();
    }

    @Test
    void createTaskShouldPersistAiEnhancementFields() {
        TaskController.TaskRequest request = new TaskController.TaskRequest();
        request.setText("AI enhanced task");
        request.setPriority("high");
        request.setCompletionDefinition("The API returns the new fields.");
        request.setEstimatedPomodoros(3);
        request.setDeadline("2026-04-20T09:30");

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(11L);
            task.setCreatedAt(LocalDateTime.of(2026, 4, 17, 11, 0));
            return task;
        });

        ResponseEntity<?> response = taskController.createTask(request, userDetails);
        TaskResponse body = (TaskResponse) response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getCompletionDefinition()).isEqualTo("The API returns the new fields.");
        assertThat(body.getEstimatedPomodoros()).isEqualTo(3);
        assertThat(body.getDeadline()).isEqualTo(LocalDateTime.of(2026, 4, 20, 9, 30));
    }

    @Test
    void updateTaskShouldReturnUpdatedAiEnhancementFields() {
        Task existing = new Task();
        existing.setId(12L);
        existing.setUser(user);
        existing.setText("Original task");
        existing.setPriority("medium");
        existing.setStatus("TODO");

        TaskController.UpdateTaskRequest request = new TaskController.UpdateTaskRequest();
        request.setCompletionDefinition("Marked done when apply endpoint returns selected tasks.");
        request.setEstimatedPomodoros(4);

        when(taskRepository.findByIdAndUserId(existing.getId(), user.getId())).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        ResponseEntity<?> response = taskController.updateTask(existing.getId(), request, userDetails);
        TaskResponse body = (TaskResponse) response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.getCompletionDefinition()).isEqualTo("Marked done when apply endpoint returns selected tasks.");
        assertThat(body.getEstimatedPomodoros()).isEqualTo(4);
    }

    @Test
    void createTaskShouldRejectInvalidEstimatedPomodoros() {
        TaskController.TaskRequest request = new TaskController.TaskRequest();
        request.setText("Invalid estimate");
        request.setEstimatedPomodoros(13);

        assertThatThrownBy(() -> taskController.createTask(request, userDetails))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("INVALID_ESTIMATED_POMODOROS", "Estimated pomodoros must be between 0 and 12");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateTaskShouldSyncStatusWhenMarkedCompleted() {
        Task existing = new Task();
        existing.setId(13L);
        existing.setUser(user);
        existing.setText("Sync status");
        existing.setPriority("medium");
        existing.setStatus("TODO");
        existing.setCompleted(false);

        TaskController.UpdateTaskRequest request = new TaskController.UpdateTaskRequest();
        request.setCompleted(true);

        when(taskRepository.findByIdAndUserId(existing.getId(), user.getId())).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        ResponseEntity<?> response = taskController.updateTask(existing.getId(), request, userDetails);
        TaskResponse body = (TaskResponse) response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isCompleted()).isTrue();
        assertThat(body.getStatus()).isEqualTo("DONE");
        assertThat(body.getCompletedAt()).isNotNull();
    }

    @Test
    void updateTaskShouldResetStatusWhenMarkedIncomplete() {
        Task existing = new Task();
        existing.setId(14L);
        existing.setUser(user);
        existing.setText("Reset status");
        existing.setPriority("medium");
        existing.setStatus("DONE");
        existing.setCompleted(true);
        existing.setCompletedAt(LocalDateTime.of(2026, 4, 19, 9, 0));

        TaskController.UpdateTaskRequest request = new TaskController.UpdateTaskRequest();
        request.setCompleted(false);

        when(taskRepository.findByIdAndUserId(existing.getId(), user.getId())).thenReturn(Optional.of(existing));
        when(taskRepository.save(existing)).thenReturn(existing);

        ResponseEntity<?> response = taskController.updateTask(existing.getId(), request, userDetails);
        TaskResponse body = (TaskResponse) response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isCompleted()).isFalse();
        assertThat(body.getStatus()).isEqualTo("TODO");
        assertThat(body.getCompletedAt()).isNull();
    }
}
