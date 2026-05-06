package com.pomotodo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.AiNextStepResponse;
import com.pomotodo.entity.AiPlanDraft;
import com.pomotodo.entity.AiPlanMilestone;
import com.pomotodo.entity.AiPlanTask;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.User;
import com.pomotodo.repository.AiPlanDraftRepository;
import com.pomotodo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiNextStepServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AiPlanDraftRepository aiPlanDraftRepository;

    @Mock
    private AiCompletionClient aiCompletionClient;

    private AiNextStepService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AiNextStepService(
                taskRepository,
                aiPlanDraftRepository,
                aiCompletionClient,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "apiKey", "server-key");
        ReflectionTestUtils.setField(service, "useModel", true);

        user = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@test.com")
                .password("encoded")
                .enabled(true)
                .role("USER")
                .build();
    }

    @Test
    void shouldPreferRealTaskWhenAiReturnsValidDecision() {
        when(taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(
                buildTask(10L, "Ship next-step API", "high", LocalDateTime.of(2026, 4, 18, 9, 0), LocalDateTime.of(2026, 4, 17, 9, 0), 2)
        ));
        when(aiCompletionClient.complete(anyString(), eq("server-key"))).thenReturn("""
                {
                  "source": "task",
                  "taskId": 10,
                  "reason": "This task unlocks the recommendation flow first.",
                  "suggestedFocusMinutes": 50,
                  "action": "focus"
                }
                """);

        AiNextStepResponse response = service.getNextStep(user, null);

        assertThat(response.getSource()).isEqualTo("task");
        assertThat(response.getTaskId()).isEqualTo(10L);
        assertThat(response.getSuggestedFocusMinutes()).isEqualTo(50);
    }

    @Test
    void shouldFallbackToHighestPriorityRealTaskWhenAiFails() {
        when(taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(
                buildTask(10L, "Medium task", "medium", LocalDateTime.of(2026, 4, 20, 9, 0), LocalDateTime.of(2026, 4, 16, 9, 0), 1),
                buildTask(11L, "High urgent task", "high", LocalDateTime.of(2026, 4, 18, 9, 0), LocalDateTime.of(2026, 4, 17, 9, 0), 3)
        ));
        when(aiCompletionClient.complete(anyString(), eq("server-key"))).thenThrow(new IllegalStateException("AI offline"));

        AiNextStepResponse response = service.getNextStep(user, null);

        assertThat(response.getSource()).isEqualTo("task");
        assertThat(response.getTaskId()).isEqualTo(11L);
        assertThat(response.getPriority()).isEqualTo("high");
        assertThat(response.getReason()).contains("优先级最高");
    }

    @Test
    void shouldUseDraftTaskWhenNoRealTasksExist() {
        when(taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
        when(aiPlanDraftRepository.findByIdAndUserId(50L, user.getId())).thenReturn(Optional.of(buildDraft()));
        when(aiCompletionClient.complete(anyString(), eq("server-key"))).thenThrow(new IllegalStateException("AI offline"));

        AiNextStepResponse response = service.getNextStep(user, 50L);

        assertThat(response.getSource()).isEqualTo("draft");
        assertThat(response.getDraftTaskId()).isEqualTo(1001L);
        assertThat(response.getAction()).isEqualTo("review_plan");
    }

    @Test
    void shouldReturnNoneWhenNoCandidatesExist() {
        when(taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());

        AiNextStepResponse response = service.getNextStep(user, null);

        assertThat(response.getSource()).isEqualTo("none");
        assertThat(response.getAction()).isEqualTo("none");
    }

    @Test
    void shouldSkipAiAndUseFallbackWhenModelUsageDisabled() {
        ReflectionTestUtils.setField(service, "useModel", false);
        when(taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(
                buildTask(21L, "Write outline", "high", LocalDateTime.of(2026, 4, 18, 18, 0), LocalDateTime.of(2026, 4, 17, 9, 0), 2),
                buildTask(22L, "Collect references", "medium", LocalDateTime.of(2026, 4, 20, 18, 0), LocalDateTime.of(2026, 4, 17, 10, 0), 1)
        ));

        AiNextStepResponse response = service.getNextStep(user, null);

        assertThat(response.getSource()).isEqualTo("task");
        assertThat(response.getTaskId()).isEqualTo(21L);
        assertThat(response.getAction()).isEqualTo("focus");
        verify(aiCompletionClient, never()).complete(anyString(), anyString());
    }

    private Task buildTask(Long id,
                           String text,
                           String priority,
                           LocalDateTime deadline,
                           LocalDateTime createdAt,
                           Integer estimatedPomodoros) {
        Task task = new Task();
        task.setId(id);
        task.setText(text);
        task.setPriority(priority);
        task.setDeadline(deadline);
        task.setCreatedAt(createdAt);
        task.setEstimatedPomodoros(estimatedPomodoros);
        task.setUser(user);
        return task;
    }

    private AiPlanDraft buildDraft() {
        AiPlanDraft draft = AiPlanDraft.builder()
                .id(50L)
                .user(user)
                .goal("Draft goal")
                .normalizedGoal("Draft goal")
                .status(AiPlanDraft.Status.GENERATED)
                .build();

        AiPlanMilestone milestone = AiPlanMilestone.builder()
                .id(101L)
                .draft(draft)
                .title("Draft milestone")
                .summary("Draft summary")
                .sortOrder(0)
                .build();

        AiPlanTask task = AiPlanTask.builder()
                .id(1001L)
                .milestone(milestone)
                .text("Draft task one")
                .priority("high")
                .completionDefinition("Draft done definition")
                .estimatedPomodoros(2)
                .suggestedDeadline(LocalDateTime.of(2026, 4, 18, 10, 0))
                .sortOrder(0)
                .selected(true)
                .build();

        AiPlanTask taskTwo = AiPlanTask.builder()
                .id(1002L)
                .milestone(milestone)
                .text("Draft task two")
                .priority("medium")
                .completionDefinition("Draft done definition two")
                .estimatedPomodoros(1)
                .suggestedDeadline(LocalDateTime.of(2026, 4, 19, 10, 0))
                .sortOrder(1)
                .selected(true)
                .build();

        milestone.setTasks(List.of(task, taskTwo));
        draft.setMilestones(List.of(milestone));
        return draft;
    }
}
