package com.pomotodo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.*;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPlannerService {
    private static final int MIN_MILESTONE_COUNT = 2;
    private static final int MAX_MILESTONE_COUNT = 4;
    private static final int MIN_TASK_COUNT = 4;
    private static final int MAX_TASK_COUNT = 8;
    private static final int MIN_TASKS_PER_MILESTONE = 2;
    private static final int MAX_TASKS_PER_MILESTONE = 3;
    private static final int MAX_ESTIMATED_POMODOROS = 12;
    private static final int MAX_ATTEMPTS = 2;
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("low", "medium", "high");

    private final AiGoalValidator aiGoalValidator;
    private final AiCompletionClient aiCompletionClient;
    private final ObjectMapper objectMapper;
    private final AiPlanDraftRepository aiPlanDraftRepository;
    private final AiPlanMilestoneRepository aiPlanMilestoneRepository;
    private final AiPlanTaskRepository aiPlanTaskRepository;
    private final TaskRepository taskRepository;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Transactional
    public AiPlanResponse createPlan(String goal, User currentUser) {
        String normalizedGoal = normalizeText(goal);
        if (normalizedGoal.isBlank()) {
            throw ApiException.badRequest("GOAL_REQUIRED", "请输入需要规划的目标。");
        }

        AiDecomposeResponse localDecision = aiGoalValidator.validate(normalizedGoal);
        if (localDecision != null) {
            return AiPlanResponse.decisionOnly(
                    localDecision.getResultType(),
                    localDecision.getReasonCode(),
                    localDecision.getMessage(),
                    localDecision.getNormalizedGoal()
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("AI_API_KEY_MISSING", "通义千问 API Key 尚未配置，请检查 DASHSCOPE_API_KEY。");
        }

        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                String refinedGoal = normalizeGoalWithAi(normalizedGoal, attempt > 0);
                List<GeneratedMilestone> generatedMilestones = generateMilestones(refinedGoal, attempt > 0);
                List<GeneratedMilestone> milestonesWithTasks = populateTasks(refinedGoal, generatedMilestones, attempt > 0);

                AiPlanDraft draft = buildDraft(currentUser, goal, refinedGoal, milestonesWithTasks);
                AiPlanDraft saved = aiPlanDraftRepository.save(draft);
                return toPlanResponse(saved, "已生成规划草稿。");
            } catch (Exception exception) {
                lastException = exception;
            }
        }

        throw toAiServiceException("AI_SERVICE_ERROR", "AI 规划暂时不可用，请稍后重试。", lastException);
    }

    @Transactional(readOnly = true)
    public AiPlanResponse getPlan(Long planId, User currentUser) {
        AiPlanDraft draft = aiPlanDraftRepository.findByIdAndUserId(planId, currentUser.getId())
                .orElseThrow(() -> ApiException.notFound("AI_PLAN_NOT_FOUND", "未找到对应的 AI 规划草稿。"));
        return toPlanResponse(draft, "已加载规划草稿。");
    }

    @Transactional
    public List<TaskResponse> applyPlan(Long planId, AiPlanApplyRequest request, User currentUser) {
        if (request == null || request.getTasks() == null) {
            throw ApiException.badRequest("INVALID_REQUEST", "请求体不能为空。");
        }

        AiPlanDraft draft = aiPlanDraftRepository.findByIdAndUserId(planId, currentUser.getId())
                .orElseThrow(() -> ApiException.notFound("AI_PLAN_NOT_FOUND", "未找到对应的 AI 规划草稿。"));

        if (draft.getStatus() == AiPlanDraft.Status.APPLIED) {
            throw ApiException.conflict("AI_PLAN_ALREADY_APPLIED", "这个 AI 规划草稿已经应用过了。");
        }

        List<AiPlanTask> persistedTasks = flattenTasks(draft);
        Map<Long, AiPlanTask> persistedTaskMap = persistedTasks.stream()
                .collect(Collectors.toMap(AiPlanTask::getId, task -> task, (left, right) -> left, LinkedHashMap::new));

        Map<Long, AiPlanApplyTaskRequest> requestTaskMap = validateApplyRequest(request.getTasks(), persistedTaskMap.keySet());
        List<ValidatedApplyTask> validatedTasks = new ArrayList<>();

        for (AiPlanTask persistedTask : persistedTasks) {
            AiPlanApplyTaskRequest taskRequest = requestTaskMap.get(persistedTask.getId());
            validatedTasks.add(validateApplyTask(taskRequest, persistedTask.getId()));
        }

        for (ValidatedApplyTask validatedTask : validatedTasks) {
            AiPlanTask persistedTask = persistedTaskMap.get(validatedTask.getDraftTaskId());
            persistedTask.setText(validatedTask.getText());
            persistedTask.setPriority(validatedTask.getPriority());
            persistedTask.setCompletionDefinition(validatedTask.getCompletionDefinition());
            persistedTask.setEstimatedPomodoros(validatedTask.getEstimatedPomodoros());
            persistedTask.setSuggestedDeadline(validatedTask.getSuggestedDeadline());
            persistedTask.setSelected(validatedTask.isSelected());
        }

        List<TaskResponse> createdTasks = new ArrayList<>();
        for (ValidatedApplyTask validatedTask : validatedTasks) {
            if (!validatedTask.isSelected()) {
                continue;
            }
            Task task = new Task();
            task.setText(validatedTask.getText());
            task.setPriority(validatedTask.getPriority());
            task.setCompletionDefinition(validatedTask.getCompletionDefinition());
            task.setEstimatedPomodoros(validatedTask.getEstimatedPomodoros());
            task.setDeadline(validatedTask.getSuggestedDeadline());
            task.setUser(currentUser);

            createdTasks.add(toTaskResponse(taskRepository.save(task)));
        }

        draft.setStatus(AiPlanDraft.Status.APPLIED);
        draft.setAppliedAt(LocalDateTime.now());
        return createdTasks;
    }

    private String normalizeGoalWithAi(String normalizedGoal, boolean retry) throws Exception {
        String content = aiCompletionClient.complete(buildGoalNormalizationPrompt(normalizedGoal, retry), apiKey);
        GoalNormalizationResult parsed = objectMapper.readValue(stripCodeFences(content), GoalNormalizationResult.class);
        String normalized = normalizeText(parsed.getNormalizedGoal());
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("规划目标归一化结果不能为空");
        }
        return normalized;
    }

    private List<GeneratedMilestone> generateMilestones(String normalizedGoal, boolean retry) throws Exception {
        String content = aiCompletionClient.complete(buildMilestonePrompt(normalizedGoal, retry), apiKey);
        MilestoneGenerationResult parsed = objectMapper.readValue(stripCodeFences(content), MilestoneGenerationResult.class);

        List<GeneratedMilestone> milestones = new ArrayList<>();
        Set<String> seenTitles = new LinkedHashSet<>();
        if (parsed.getMilestones() != null) {
            int sortOrder = 0;
            for (MilestoneGenerationItem item : parsed.getMilestones()) {
                if (item == null) {
                    continue;
                }
                String title = normalizeText(item.getTitle());
                String summary = normalizeText(item.getSummary());
                if (title.isBlank() || !seenTitles.add(title.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                milestones.add(GeneratedMilestone.builder()
                        .title(title)
                        .summary(summary)
                        .sortOrder(sortOrder++)
                        .tasks(new ArrayList<>())
                        .build());
            }
        }

        if (milestones.size() < MIN_MILESTONE_COUNT || milestones.size() > MAX_MILESTONE_COUNT) {
            throw new IllegalArgumentException("里程碑数量必须在 2 到 4 个之间");
        }
        return milestones;
    }

    private List<GeneratedMilestone> populateTasks(String normalizedGoal,
                                                   List<GeneratedMilestone> milestones,
                                                   boolean retry) throws Exception {
        Set<String> globalSeenTasks = new LinkedHashSet<>();
        int totalTaskCount = 0;

        for (GeneratedMilestone milestone : milestones) {
            int taskTarget = milestones.size() == 2 ? MAX_TASKS_PER_MILESTONE : MIN_TASKS_PER_MILESTONE;
            String content = aiCompletionClient.complete(
                    buildMilestoneTaskPrompt(normalizedGoal, milestone, taskTarget, retry),
                    apiKey
            );
            TaskGenerationResult parsed = objectMapper.readValue(stripCodeFences(content), TaskGenerationResult.class);

            List<GeneratedTask> tasks = new ArrayList<>();
            if (parsed.getTasks() != null) {
                int sortOrder = 0;
                for (TaskGenerationItem item : parsed.getTasks()) {
                    if (item == null) {
                        continue;
                    }
                    String text = normalizeText(item.getText());
                    if (text.isBlank() || !globalSeenTasks.add(text.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    String completionDefinition = normalizeText(item.getCompletionDefinition());
                    if (completionDefinition.isBlank()) {
                        throw new IllegalArgumentException("任务完成定义不能为空");
                    }
                    Integer estimatedPomodoros = normalizeEstimatedPomodoros(item.getEstimatedPomodoros());
                    LocalDateTime suggestedDeadline = parseRequiredDateTime(item.getSuggestedDeadline());
                    tasks.add(GeneratedTask.builder()
                            .text(text)
                            .priority(normalizePriority(item.getPriority()))
                            .completionDefinition(completionDefinition)
                            .estimatedPomodoros(estimatedPomodoros)
                            .suggestedDeadline(suggestedDeadline)
                            .sortOrder(sortOrder++)
                            .selected(true)
                            .build());
                }
            }

            if (tasks.size() < MIN_TASKS_PER_MILESTONE || tasks.size() > MAX_TASKS_PER_MILESTONE) {
                throw new IllegalArgumentException("每个里程碑必须包含 2 到 3 个任务");
            }
            milestone.setTasks(tasks);
            totalTaskCount += tasks.size();
        }

        if (totalTaskCount < MIN_TASK_COUNT || totalTaskCount > MAX_TASK_COUNT) {
            throw new IllegalArgumentException("总任务数量必须在 4 到 8 个之间");
        }

        return milestones;
    }

    private AiPlanDraft buildDraft(User currentUser,
                                   String originalGoal,
                                   String normalizedGoal,
                                   List<GeneratedMilestone> generatedMilestones) {
        AiPlanDraft draft = AiPlanDraft.builder()
                .user(currentUser)
                .goal(normalizeText(originalGoal))
                .normalizedGoal(normalizedGoal)
                .status(AiPlanDraft.Status.GENERATED)
                .build();

        List<AiPlanMilestone> milestones = new ArrayList<>();
        for (GeneratedMilestone generatedMilestone : generatedMilestones) {
            AiPlanMilestone milestone = AiPlanMilestone.builder()
                    .draft(draft)
                    .title(generatedMilestone.getTitle())
                    .summary(generatedMilestone.getSummary())
                    .sortOrder(generatedMilestone.getSortOrder())
                    .build();

            List<AiPlanTask> tasks = new ArrayList<>();
            for (GeneratedTask generatedTask : generatedMilestone.getTasks()) {
                tasks.add(AiPlanTask.builder()
                        .milestone(milestone)
                        .text(generatedTask.getText())
                        .priority(generatedTask.getPriority())
                        .completionDefinition(generatedTask.getCompletionDefinition())
                        .estimatedPomodoros(generatedTask.getEstimatedPomodoros())
                        .suggestedDeadline(generatedTask.getSuggestedDeadline())
                        .sortOrder(generatedTask.getSortOrder())
                        .selected(generatedTask.isSelected())
                        .build());
            }
            milestone.setTasks(tasks);
            milestones.add(milestone);
        }
        draft.setMilestones(milestones);
        return draft;
    }

    private Map<Long, AiPlanApplyTaskRequest> validateApplyRequest(List<AiPlanApplyTaskRequest> requestTasks,
                                                                   Set<Long> expectedTaskIds) {
        if (requestTasks.isEmpty()) {
            throw ApiException.badRequest("AI_PLAN_TASKS_REQUIRED", "规划任务不能为空。");
        }

        Map<Long, AiPlanApplyTaskRequest> requestTaskMap = new LinkedHashMap<>();
        for (AiPlanApplyTaskRequest taskRequest : requestTasks) {
            if (taskRequest == null || taskRequest.getDraftTaskId() == null) {
                throw ApiException.badRequest("AI_PLAN_TASK_ID_REQUIRED", "每个规划任务都必须包含 draftTaskId。");
            }
            if (requestTaskMap.putIfAbsent(taskRequest.getDraftTaskId(), taskRequest) != null) {
                throw ApiException.badRequest("AI_PLAN_DUPLICATE_TASK", "不允许提交重复的草稿任务 ID。");
            }
        }

        if (!requestTaskMap.keySet().equals(expectedTaskIds)) {
            throw ApiException.badRequest("AI_PLAN_TASK_SET_MISMATCH", "提交的任务集合必须与草稿中的任务完全一致。");
        }
        return requestTaskMap;
    }

    private ValidatedApplyTask validateApplyTask(AiPlanApplyTaskRequest taskRequest, Long expectedTaskId) {
        String text = normalizeText(taskRequest.getText());
        if (text.isBlank()) {
            throw ApiException.badRequest("TASK_TEXT_REQUIRED", "任务内容不能为空。");
        }

        Boolean selected = taskRequest.getSelected();
        if (selected == null) {
            throw ApiException.badRequest("AI_PLAN_SELECTED_REQUIRED", "每个规划任务都必须明确 selected 字段。");
        }

        return ValidatedApplyTask.builder()
                .draftTaskId(expectedTaskId)
                .text(text)
                .priority(normalizePriority(taskRequest.getPriority()))
                .completionDefinition(normalizeOptionalText(taskRequest.getCompletionDefinition()))
                .estimatedPomodoros(normalizeOptionalEstimatedPomodoros(taskRequest.getEstimatedPomodoros()))
                .suggestedDeadline(parseOptionalDateTime(taskRequest.getSuggestedDeadline()))
                .selected(selected)
                .build();
    }

    private List<AiPlanTask> flattenTasks(AiPlanDraft draft) {
        return draft.getMilestones().stream()
                .sorted(Comparator.comparing(AiPlanMilestone::getSortOrder).thenComparing(AiPlanMilestone::getId, Comparator.nullsLast(Long::compareTo)))
                .flatMap(milestone -> milestone.getTasks().stream()
                        .sorted(Comparator.comparing(AiPlanTask::getSortOrder).thenComparing(AiPlanTask::getId, Comparator.nullsLast(Long::compareTo))))
                .collect(Collectors.toList());
    }

    private AiPlanResponse toPlanResponse(AiPlanDraft draft, String message) {
        return AiPlanResponse.builder()
                .resultType("tasks")
                .reasonCode("OK")
                .message(message)
                .normalizedGoal(draft.getNormalizedGoal())
                .planId(draft.getId())
                .status(draft.getStatus().name())
                .milestones(draft.getMilestones().stream()
                        .map(milestone -> AiPlanMilestoneResponse.builder()
                                .id(milestone.getId())
                                .title(milestone.getTitle())
                                .summary(milestone.getSummary())
                                .tasks(milestone.getTasks().stream()
                                        .map(task -> AiPlanTaskResponse.builder()
                                                .id(task.getId())
                                                .text(task.getText())
                                                .priority(task.getPriority())
                                                .completionDefinition(task.getCompletionDefinition())
                                                .estimatedPomodoros(task.getEstimatedPomodoros())
                                                .suggestedDeadline(task.getSuggestedDeadline())
                                                .selected(task.isSelected())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private TaskResponse toTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setText(task.getText());
        response.setPriority(task.getPriority());
        response.setCompletionDefinition(task.getCompletionDefinition());
        response.setEstimatedPomodoros(task.getEstimatedPomodoros());
        response.setStatus(task.getStatus());
        response.setCompleted(task.isCompleted());
        response.setCreatedAt(task.getCreatedAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setDeadline(task.getDeadline());
        return response;
    }

    private Integer normalizeEstimatedPomodoros(Integer value) {
        if (value == null || value < 0 || value > MAX_ESTIMATED_POMODOROS) {
            throw new IllegalArgumentException("预估番茄数必须在 0 到 " + MAX_ESTIMATED_POMODOROS + " 之间");
        }
        return value;
    }

    private Integer normalizeOptionalEstimatedPomodoros(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > MAX_ESTIMATED_POMODOROS) {
            throw ApiException.badRequest(
                    "INVALID_ESTIMATED_POMODOROS",
                    "预估番茄数必须在 0 到 " + MAX_ESTIMATED_POMODOROS + " 之间"
            );
        }
        return value;
    }

    private String normalizePriority(String priority) {
        String normalized = normalizeText(priority).toLowerCase(Locale.ROOT);
        if (ALLOWED_PRIORITIES.contains(normalized)) {
            return normalized;
        }
        return "medium";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String normalizeOptionalText(String value) {
        String normalized = normalizeText(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String stripCodeFences(String content) {
        return content
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private LocalDateTime parseRequiredDateTime(String value) {
        LocalDateTime parsed = parseOptionalDateTime(value);
        if (parsed == null) {
            throw new IllegalArgumentException("建议截止时间不能为空");
        }
        return parsed;
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        String normalized = normalizeText(value);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            if (!normalized.contains("T") && normalized.contains(" ")) {
                normalized = normalized.replace(" ", "T");
            }
            if (normalized.length() == 16) {
                normalized = normalized + ":00";
            }
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("建议截止时间格式不合法：" + value, exception);
        }
    }

    private String buildGoalNormalizationPrompt(String goal, boolean retry) {
        String retryInstruction = retry
                ? "上一次输出无效。请只返回 JSON，且 normalizedGoal 不能为空。"
                : "";
        return """
                你是一个中文效率应用中的目标规划助手。
                请把用户目标改写成简洁、明确、适合规划的短期目标，保留原意，去掉空话。

                仅返回 JSON：
                {
                  "normalizedGoal": "整理后的目标"
                }

                %s
                用户目标：%s
                """.formatted(retryInstruction, goal);
    }

    private String buildMilestonePrompt(String normalizedGoal, boolean retry) {
        String retryInstruction = retry
                ? "上一次输出无效。请只返回 JSON，并严格返回 2 到 4 个里程碑。"
                : "";
        return """
                你是一个中文效率应用中的规划助手。
                请为下面的目标拆出 2 到 4 个里程碑。
                每个里程碑都必须包含：
                - 一个简短标题
                - 一句简要说明
                所有标题和说明都必须使用简体中文。

                仅返回 JSON：
                {
                  "milestones": [
                    { "title": "里程碑标题", "summary": "里程碑说明" }
                  ]
                }

                %s
                目标：%s
                """.formatted(retryInstruction, normalizedGoal);
    }

    private String buildMilestoneTaskPrompt(String normalizedGoal,
                                            GeneratedMilestone milestone,
                                            int taskTarget,
                                            boolean retry) {
        String retryInstruction = retry
                ? "上一次输出无效。请只返回合法 JSON，并确保每个字段都存在。"
                : "";
        return """
                你是一个中文效率应用中的任务规划助手。
                请围绕一个里程碑生成可执行任务。
                目标：%s
                里程碑标题：%s
                里程碑说明：%s

                请严格返回 %d 个任务，并且只返回 JSON：
                {
                  "tasks": [
                    {
                      "text": "简短可执行的任务",
                      "priority": "low" | "medium" | "high",
                      "completionDefinition": "用户如何判断这个任务已完成",
                      "estimatedPomodoros": 0-12 integer,
                      "suggestedDeadline": "yyyy-MM-ddTHH:mm:ss"
                    }
                  ]
                }

                规则：
                - 所有面向用户的文本字段都必须使用简体中文。
                - 每个任务都必须由用户自己可执行，且内容具体、互不重复。
                - completionDefinition 必须具体，不能空泛。
                - suggestedDeadline 必须是一个合理的近期本地时间字符串。
                - 不要输出 Markdown 代码块，也不要输出额外说明。
                %s
                """.formatted(
                normalizedGoal,
                milestone.getTitle(),
                milestone.getSummary(),
                taskTarget,
                retryInstruction
        );
    }

    private ApiException toAiServiceException(String code, String fallbackMessage, Exception exception) {
        if (exception instanceof AiProviderException providerException) {
            return new ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    providerException.getCode(),
                    providerException.getUserMessage(),
                    providerException.getDetails(),
                    providerException
            );
        }
        return new ApiException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                code,
                fallbackMessage,
                exception == null ? null : exception.getMessage(),
                exception
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoalNormalizationResult {
        private String normalizedGoal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MilestoneGenerationResult {
        private List<MilestoneGenerationItem> milestones = List.of();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class MilestoneGenerationItem {
        private String title;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TaskGenerationResult {
        private List<TaskGenerationItem> tasks = List.of();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TaskGenerationItem {
        private String text;
        private String priority;
        private String completionDefinition;
        private Integer estimatedPomodoros;
        private String suggestedDeadline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class GeneratedMilestone {
        private String title;
        private String summary;
        private Integer sortOrder;
        @Builder.Default
        private List<GeneratedTask> tasks = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class GeneratedTask {
        private String text;
        private String priority;
        private String completionDefinition;
        private Integer estimatedPomodoros;
        private LocalDateTime suggestedDeadline;
        private Integer sortOrder;
        private boolean selected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class ValidatedApplyTask {
        private Long draftTaskId;
        private String text;
        private String priority;
        private String completionDefinition;
        private Integer estimatedPomodoros;
        private LocalDateTime suggestedDeadline;
        private boolean selected;
    }
}
