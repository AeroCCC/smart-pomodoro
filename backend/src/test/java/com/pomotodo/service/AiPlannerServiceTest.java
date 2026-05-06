package com.pomotodo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.AiPlanApplyRequest;
import com.pomotodo.dto.AiPlanApplyTaskRequest;
import com.pomotodo.dto.AiPlanResponse;
import com.pomotodo.dto.TaskResponse;
import com.pomotodo.entity.AiPlanDraft;
import com.pomotodo.entity.AiPlanMilestone;
import com.pomotodo.entity.AiPlanTask;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.AiPlanDraftRepository;
import com.pomotodo.repository.AiPlanMilestoneRepository;
import com.pomotodo.repository.AiPlanTaskRepository;
import com.pomotodo.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiPlannerServiceTest {

    @Mock
    private AiCompletionClient aiCompletionClient;

    @Mock
    private AiPlanDraftRepository aiPlanDraftRepository;

    @Mock
    private AiPlanMilestoneRepository aiPlanMilestoneRepository;

    @Mock
    private AiPlanTaskRepository aiPlanTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    private AiPlannerService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AiPlannerService(
                new AiGoalValidator(),
                aiCompletionClient,
                new ObjectMapper(),
                aiPlanDraftRepository,
                aiPlanMilestoneRepository,
                aiPlanTaskRepository,
                taskRepository
        );
        ReflectionTestUtils.setField(service, "apiKey", "server-key");

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
    void shouldReturnNeedsRefinementWithoutPersistingDraft() {
        ReflectionTestUtils.setField(service, "apiKey", "");

        AiPlanResponse response = service.createPlan("成为世界首富", user);

        assertThat(response.getResultType()).isEqualTo("needs_refinement");
        assertThat(response.getPlanId()).isNull();
        verifyNoInteractions(aiCompletionClient);
        verify(aiPlanDraftRepository, never()).save(any());
    }

    @Test
    void shouldCreatePlanDraftFromValidAiOutput() {
        when(aiCompletionClient.complete(anyString(), eq("server-key")))
                .thenReturn(
                        """
                        { "normalizedGoal": "Launch the graduation project backend upgrade" }
                        """,
                        """
                        {
                          "milestones": [
                            { "title": "Define data model changes", "summary": "Lock the storage changes for tasks and AI drafts." },
                            { "title": "Ship AI planning APIs", "summary": "Expose plan creation and apply endpoints." }
                          ]
                        }
                        """,
                        """
                        {
                          "tasks": [
                            {
                              "text": "Add task columns for AI metadata",
                              "priority": "high",
                              "completionDefinition": "Task entity and task response expose the new metadata fields.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-18T10:00:00"
                            },
                            {
                              "text": "Create draft entities and indexes",
                              "priority": "high",
                              "completionDefinition": "Three draft tables exist with mapped JPA entities and indexes.",
                              "estimatedPomodoros": 3,
                              "suggestedDeadline": "2026-04-18T16:00:00"
                            },
                            {
                              "text": "Update production SQL script",
                              "priority": "medium",
                              "completionDefinition": "db-update.sql includes the new columns and draft tables.",
                              "estimatedPomodoros": 1,
                              "suggestedDeadline": "2026-04-18T20:00:00"
                            }
                          ]
                        }
                        """,
                        """
                        {
                          "tasks": [
                            {
                              "text": "Add create plan endpoint",
                              "priority": "high",
                              "completionDefinition": "POST /api/ai/plans returns a persisted plan draft.",
                              "estimatedPomodoros": 3,
                              "suggestedDeadline": "2026-04-19T10:00:00"
                            },
                            {
                              "text": "Add get plan endpoint",
                              "priority": "medium",
                              "completionDefinition": "GET /api/ai/plans/{id} returns the current user's draft.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-19T15:00:00"
                            },
                            {
                              "text": "Add apply plan endpoint",
                              "priority": "high",
                              "completionDefinition": "Applying a draft creates only the selected personal tasks transactionally.",
                              "estimatedPomodoros": 3,
                              "suggestedDeadline": "2026-04-19T20:00:00"
                            }
                          ]
                        }
                        """
                );
        when(aiPlanDraftRepository.save(any(AiPlanDraft.class))).thenAnswer(invocation -> assignDraftIds(invocation.getArgument(0)));

        AiPlanResponse response = service.createPlan("Implement AI planning backend upgrade", user);

        assertThat(response.getResultType()).isEqualTo("tasks");
        assertThat(response.getPlanId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("GENERATED");
        assertThat(response.getMilestones()).hasSize(2);
        assertThat(response.getMilestones().get(0).getTasks()).hasSize(3);
        assertThat(response.getMilestones().get(1).getTasks()).hasSize(3);
        verify(aiPlanDraftRepository).save(any(AiPlanDraft.class));
    }

    @Test
    void shouldRetryWhenAiTaskPayloadIsInvalid() {
        when(aiCompletionClient.complete(anyString(), eq("server-key")))
                .thenReturn(
                        """
                        { "normalizedGoal": "Prepare AI planning backend release" }
                        """,
                        """
                        {
                          "milestones": [
                            { "title": "Finish schema work", "summary": "Prepare the database model." },
                            { "title": "Finish API work", "summary": "Prepare the controller and service flow." }
                          ]
                        }
                        """,
                        """
                        {
                          "tasks": [
                            {
                              "text": "Only one task",
                              "priority": "high",
                              "completionDefinition": "This output is intentionally invalid.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-18T10:00:00"
                            }
                          ]
                        }
                        """,
                        """
                        { "normalizedGoal": "Prepare AI planning backend release" }
                        """,
                        """
                        {
                          "milestones": [
                            { "title": "Finish schema work", "summary": "Prepare the database model." },
                            { "title": "Finish API work", "summary": "Prepare the controller and service flow." }
                          ]
                        }
                        """,
                        """
                        {
                          "tasks": [
                            {
                              "text": "Add draft tables",
                              "priority": "high",
                              "completionDefinition": "Draft tables and entities are in place.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-18T10:00:00"
                            },
                            {
                              "text": "Add task metadata columns",
                              "priority": "high",
                              "completionDefinition": "Task entity persists AI metadata fields.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-18T14:00:00"
                            },
                            {
                              "text": "Patch migration SQL",
                              "priority": "medium",
                              "completionDefinition": "The SQL patch contains the new schema changes.",
                              "estimatedPomodoros": 1,
                              "suggestedDeadline": "2026-04-18T18:00:00"
                            }
                          ]
                        }
                        """,
                        """
                        {
                          "tasks": [
                            {
                              "text": "Create plan endpoints",
                              "priority": "high",
                              "completionDefinition": "Plan creation and query endpoints are live.",
                              "estimatedPomodoros": 3,
                              "suggestedDeadline": "2026-04-19T10:00:00"
                            },
                            {
                              "text": "Create apply endpoint",
                              "priority": "high",
                              "completionDefinition": "Plan apply works transactionally for selected tasks only.",
                              "estimatedPomodoros": 3,
                              "suggestedDeadline": "2026-04-19T15:00:00"
                            },
                            {
                              "text": "Add regression tests",
                              "priority": "medium",
                              "completionDefinition": "Compatibility and AI planner tests cover the new flow.",
                              "estimatedPomodoros": 2,
                              "suggestedDeadline": "2026-04-19T20:00:00"
                            }
                          ]
                        }
                        """
                );
        when(aiPlanDraftRepository.save(any(AiPlanDraft.class))).thenAnswer(invocation -> assignDraftIds(invocation.getArgument(0)));

        AiPlanResponse response = service.createPlan("Prepare AI planning backend release", user);

        assertThat(response.getPlanId()).isEqualTo(10L);
        verify(aiCompletionClient, times(7)).complete(anyString(), eq("server-key"));
    }

    @Test
    void shouldApplyPlanOnlyOnceAndCreateSelectedTasks() {
        AiPlanDraft draft = buildDraftEntity();
        when(aiPlanDraftRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(draft));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(500L);
            task.setCreatedAt(LocalDateTime.of(2026, 4, 17, 12, 0));
            return task;
        });

        AiPlanApplyRequest request = new AiPlanApplyRequest();
        request.setTasks(List.of(
                buildApplyTask(1001L, "Persist task metadata", "high", "Task metadata fields are saved.", 2, "2026-04-20T10:00:00", true),
                buildApplyTask(1002L, "Persist draft entities", "medium", "Draft entities are committed.", 3, "2026-04-20T16:00:00", false)
        ));

        List<TaskResponse> createdTasks = service.applyPlan(10L, request, user);

        assertThat(createdTasks).hasSize(1);
        assertThat(createdTasks.get(0).getText()).isEqualTo("Persist task metadata");
        assertThat(draft.getStatus()).isEqualTo(AiPlanDraft.Status.APPLIED);
        assertThat(draft.getAppliedAt()).isNotNull();
        assertThat(draft.getMilestones().get(0).getTasks().get(1).isSelected()).isFalse();
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void shouldRejectRepeatedApply() {
        AiPlanDraft draft = buildDraftEntity();
        draft.setStatus(AiPlanDraft.Status.APPLIED);
        when(aiPlanDraftRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(draft));

        AiPlanApplyRequest request = new AiPlanApplyRequest();
        request.setTasks(List.of(
                buildApplyTask(1001L, "Persist task metadata", "high", "Task metadata fields are saved.", 2, "2026-04-20T10:00:00", true),
                buildApplyTask(1002L, "Persist draft entities", "medium", "Draft entities are committed.", 3, "2026-04-20T16:00:00", false)
        ));

        assertThatThrownBy(() -> service.applyPlan(10L, request, user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("AI_PLAN_ALREADY_APPLIED", "这个 AI 规划草稿已经应用过了。");
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectApplyWhenRequestTasksDoNotMatchDraft() {
        AiPlanDraft draft = buildDraftEntity();
        when(aiPlanDraftRepository.findByIdAndUserId(10L, user.getId())).thenReturn(Optional.of(draft));

        AiPlanApplyRequest request = new AiPlanApplyRequest();
        request.setTasks(List.of(
                buildApplyTask(1001L, "Persist task metadata", "high", "Task metadata fields are saved.", 2, "2026-04-20T10:00:00", true)
        ));

        assertThatThrownBy(() -> service.applyPlan(10L, request, user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("AI_PLAN_TASK_SET_MISMATCH", "提交的任务集合必须与草稿中的任务完全一致。");
        verify(taskRepository, never()).save(any(Task.class));
    }

    private AiPlanApplyTaskRequest buildApplyTask(Long draftTaskId,
                                                  String text,
                                                  String priority,
                                                  String completionDefinition,
                                                  Integer estimatedPomodoros,
                                                  String suggestedDeadline,
                                                  boolean selected) {
        AiPlanApplyTaskRequest request = new AiPlanApplyTaskRequest();
        request.setDraftTaskId(draftTaskId);
        request.setText(text);
        request.setPriority(priority);
        request.setCompletionDefinition(completionDefinition);
        request.setEstimatedPomodoros(estimatedPomodoros);
        request.setSuggestedDeadline(suggestedDeadline);
        request.setSelected(selected);
        return request;
    }

    private AiPlanDraft buildDraftEntity() {
        AiPlanDraft draft = AiPlanDraft.builder()
                .id(10L)
                .user(user)
                .goal("Implement AI planning backend")
                .normalizedGoal("Implement AI planning backend")
                .status(AiPlanDraft.Status.GENERATED)
                .milestones(new ArrayList<>())
                .build();

        AiPlanMilestone milestone = AiPlanMilestone.builder()
                .id(101L)
                .draft(draft)
                .title("Schema work")
                .summary("Prepare the persistence model.")
                .sortOrder(0)
                .tasks(new ArrayList<>())
                .build();

        milestone.getTasks().add(AiPlanTask.builder()
                .id(1001L)
                .milestone(milestone)
                .text("Original task one")
                .priority("high")
                .completionDefinition("Original completion one")
                .estimatedPomodoros(1)
                .suggestedDeadline(LocalDateTime.of(2026, 4, 18, 10, 0))
                .sortOrder(0)
                .selected(true)
                .build());
        milestone.getTasks().add(AiPlanTask.builder()
                .id(1002L)
                .milestone(milestone)
                .text("Original task two")
                .priority("medium")
                .completionDefinition("Original completion two")
                .estimatedPomodoros(2)
                .suggestedDeadline(LocalDateTime.of(2026, 4, 18, 16, 0))
                .sortOrder(1)
                .selected(true)
                .build());

        draft.getMilestones().add(milestone);
        return draft;
    }

    private AiPlanDraft assignDraftIds(AiPlanDraft draft) {
        draft.setId(10L);
        AtomicLong milestoneId = new AtomicLong(100L);
        AtomicLong taskId = new AtomicLong(1000L);
        draft.getMilestones().forEach(milestone -> {
            milestone.setId(milestoneId.incrementAndGet());
            milestone.getTasks().forEach(task -> task.setId(taskId.incrementAndGet()));
        });
        return draft;
    }
}
