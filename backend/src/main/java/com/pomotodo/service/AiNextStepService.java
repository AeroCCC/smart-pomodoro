package com.pomotodo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.AiNextStepResponse;
import com.pomotodo.entity.AiPlanDraft;
import com.pomotodo.entity.AiPlanMilestone;
import com.pomotodo.entity.AiPlanTask;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.User;
import com.pomotodo.exception.ApiException;
import com.pomotodo.repository.AiPlanDraftRepository;
import com.pomotodo.repository.TaskRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiNextStepService {
    private static final int DEFAULT_FOCUS_MINUTES = 25;
    private static final int MAX_ATTEMPTS = 2;
    private static final Set<String> ALLOWED_SOURCES = Set.of("task", "draft");

    private final TaskRepository taskRepository;
    private final AiPlanDraftRepository aiPlanDraftRepository;
    private final AiCompletionClient aiCompletionClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.next-step.use-model:false}")
    private boolean useModel;

    public AiNextStepResponse getNextStep(User currentUser, Long planId) {
        List<TaskCandidate> realTasks = taskRepository.findByUserIdAndCompletedFalseOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::toTaskCandidate)
                .collect(Collectors.toList());
        List<TaskCandidate> draftTasks = loadDraftCandidates(currentUser, planId);

        if (realTasks.isEmpty() && draftTasks.isEmpty()) {
            return AiNextStepResponse.builder()
                    .source("none")
                    .text("当前还没有待处理任务。")
                    .reason("你可以先手动创建任务，或生成一份 AI 规划来获取推荐。")
                    .priority("medium")
                    .suggestedFocusMinutes(DEFAULT_FOCUS_MINUTES)
                    .action("none")
                    .build();
        }

        if (useModel && apiKey != null && !apiKey.isBlank()) {
            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    String content = aiCompletionClient.complete(buildPrompt(realTasks, draftTasks, attempt > 0), apiKey);
                    NextStepDecision decision = objectMapper.readValue(stripCodeFences(content), NextStepDecision.class);
                    AiNextStepResponse response = validateAiDecision(decision, realTasks, draftTasks);
                    if (response != null) {
                        return response;
                    }
                } catch (Exception ignored) {
                    // Fallback is deterministic and stable; swallow provider errors here.
                }
            }
        }

        return buildFallback(realTasks, draftTasks);
    }

    private List<TaskCandidate> loadDraftCandidates(User currentUser, Long planId) {
        if (planId == null) {
            return List.of();
        }

        AiPlanDraft draft = aiPlanDraftRepository.findByIdAndUserId(planId, currentUser.getId())
                .orElseThrow(() -> ApiException.notFound("AI_PLAN_NOT_FOUND", "未找到对应的 AI 规划草稿。"));

        if (draft.getStatus() == AiPlanDraft.Status.APPLIED) {
            return List.of();
        }

        return draft.getMilestones().stream()
                .sorted(Comparator.comparing(AiPlanMilestone::getSortOrder).thenComparing(AiPlanMilestone::getId))
                .flatMap(milestone -> milestone.getTasks().stream()
                        .sorted(Comparator.comparing(AiPlanTask::getSortOrder).thenComparing(AiPlanTask::getId)))
                .filter(AiPlanTask::isSelected)
                .map(this::toDraftCandidate)
                .collect(Collectors.toList());
    }

    private TaskCandidate toTaskCandidate(Task task) {
        return TaskCandidate.builder()
                .source("task")
                .id(task.getId())
                .text(task.getText())
                .priority(normalizePriority(task.getPriority()))
                .completionDefinition(normalizeText(task.getCompletionDefinition()))
                .estimatedPomodoros(task.getEstimatedPomodoros())
                .deadline(task.getDeadline())
                .createdAt(task.getCreatedAt())
                .build();
    }

    private TaskCandidate toDraftCandidate(AiPlanTask task) {
        return TaskCandidate.builder()
                .source("draft")
                .id(task.getId())
                .text(task.getText())
                .priority(normalizePriority(task.getPriority()))
                .completionDefinition(normalizeText(task.getCompletionDefinition()))
                .estimatedPomodoros(task.getEstimatedPomodoros())
                .deadline(task.getSuggestedDeadline())
                .createdAt(null)
                .build();
    }

    private AiNextStepResponse validateAiDecision(NextStepDecision decision,
                                                  List<TaskCandidate> realTasks,
                                                  List<TaskCandidate> draftTasks) {
        if (decision == null) {
            return null;
        }

        String source = normalizeText(decision.getSource()).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SOURCES.contains(source)) {
            return null;
        }

        TaskCandidate candidate = null;
        if ("task".equals(source) && decision.getTaskId() != null) {
            candidate = findCandidate(realTasks, decision.getTaskId());
        } else if ("draft".equals(source) && decision.getDraftTaskId() != null) {
            candidate = findCandidate(draftTasks, decision.getDraftTaskId());
        }

        if (candidate == null) {
            return null;
        }

        return AiNextStepResponse.builder()
                .source(source)
                .taskId("task".equals(source) ? candidate.getId() : null)
                .draftTaskId("draft".equals(source) ? candidate.getId() : null)
                .text(candidate.getText())
                .reason(defaultIfBlank(normalizeText(decision.getReason()), "这是当前最清晰、最值得马上开始的下一步。"))
                .priority(candidate.getPriority())
                .suggestedFocusMinutes(normalizeFocusMinutes(decision.getSuggestedFocusMinutes(), candidate))
                .action(defaultIfBlank(normalizeText(decision.getAction()), defaultActionForSource(source)))
                .build();
    }

    private TaskCandidate findCandidate(List<TaskCandidate> candidates, Long id) {
        return candidates.stream()
                .filter(candidate -> Objects.equals(candidate.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private AiNextStepResponse buildFallback(List<TaskCandidate> realTasks, List<TaskCandidate> draftTasks) {
        TaskCandidate candidate;
        String reason;

        if (!realTasks.isEmpty()) {
            candidate = realTasks.stream()
                    .sorted(candidateComparator())
                    .findFirst()
                    .orElseThrow();
            reason = "这是当前优先级最高、且最值得立即推进的真实任务。";
        } else {
            candidate = draftTasks.stream()
                    .sorted(candidateComparator())
                    .findFirst()
                    .orElseThrow();
            reason = "在正式应用草稿之前，这一步是最适合马上推进的建议动作。";
        }

        return AiNextStepResponse.builder()
                .source(candidate.getSource())
                .taskId("task".equals(candidate.getSource()) ? candidate.getId() : null)
                .draftTaskId("draft".equals(candidate.getSource()) ? candidate.getId() : null)
                .text(candidate.getText())
                .reason(reason)
                .priority(candidate.getPriority())
                .suggestedFocusMinutes(defaultFocusMinutes(candidate))
                .action(defaultActionForSource(candidate.getSource()))
                .build();
    }

    private Comparator<TaskCandidate> candidateComparator() {
        return Comparator
                .comparingInt((TaskCandidate candidate) -> priorityRank(candidate.getPriority()))
                .thenComparing(candidate -> candidate.getDeadline() == null ? LocalDateTime.MAX : candidate.getDeadline())
                .thenComparing(candidate -> candidate.getCreatedAt() == null ? LocalDateTime.MIN : candidate.getCreatedAt());
    }

    private int priorityRank(String priority) {
        return switch (normalizePriority(priority)) {
            case "high" -> 0;
            case "medium" -> 1;
            default -> 2;
        };
    }

    private int defaultFocusMinutes(TaskCandidate candidate) {
        if (candidate.getEstimatedPomodoros() == null || candidate.getEstimatedPomodoros() <= 0) {
            return DEFAULT_FOCUS_MINUTES;
        }
        return Math.max(15, Math.min(candidate.getEstimatedPomodoros() * 25, 120));
    }

    private int normalizeFocusMinutes(Integer suggestedFocusMinutes, TaskCandidate candidate) {
        if (suggestedFocusMinutes == null || suggestedFocusMinutes <= 0) {
            return defaultFocusMinutes(candidate);
        }
        return Math.min(suggestedFocusMinutes, 120);
    }

    private String normalizePriority(String priority) {
        String normalized = normalizeText(priority).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "high", "medium", "low" -> normalized;
            default -> "medium";
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String defaultActionForSource(String source) {
        return "draft".equals(source) ? "review_plan" : "focus";
    }

    private String stripCodeFences(String content) {
        return content
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private String buildPrompt(List<TaskCandidate> realTasks, List<TaskCandidate> draftTasks, boolean retry) {
        String retryInstruction = retry
                ? "上一次输出无效。请只返回 JSON，并且必须引用现有任务 ID。"
                : "";
        return """
                你是一个中文效率应用中的下一步推荐助手。
                请从候选任务中选出最适合用户立刻开始的一步。
                只要真实待办任务足够合适，就优先推荐真实任务，而不是草稿任务。

                仅返回 JSON：
                {
                  "source": "task" | "draft",
                  "taskId": 123,
                  "draftTaskId": 456,
                  "reason": "简短中文说明",
                  "suggestedFocusMinutes": 25,
                  "action": "focus" | "review_plan"
                }

                真实任务：
                %s

                草稿任务：
                %s

                规则：
                - 如果 source 是 "task"，请设置 taskId，并将 draftTaskId 置为 null。
                - 如果 source 是 "draft"，请设置 draftTaskId，并将 taskId 置为 null。
                - reason 必须是简短、具体的简体中文。
                %s
                """.formatted(
                formatCandidates(realTasks),
                formatCandidates(draftTasks),
                retryInstruction
        );
    }

    private String formatCandidates(List<TaskCandidate> candidates) {
        if (candidates.isEmpty()) {
            return "- 无";
        }
        return candidates.stream()
                .map(candidate -> "- id=%d text=%s priority=%s deadline=%s estimatedPomodoros=%s completionDefinition=%s".formatted(
                        candidate.getId(),
                        candidate.getText(),
                        candidate.getPriority(),
                        candidate.getDeadline(),
                        candidate.getEstimatedPomodoros(),
                        candidate.getCompletionDefinition()
                ))
                .collect(Collectors.joining("\n"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NextStepDecision {
        private String source;
        private Long taskId;
        private Long draftTaskId;
        private String reason;
        private Integer suggestedFocusMinutes;
        private String action;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class TaskCandidate {
        private String source;
        private Long id;
        private String text;
        private String priority;
        private String completionDefinition;
        private Integer estimatedPomodoros;
        private LocalDateTime deadline;
        private LocalDateTime createdAt;
    }
}
