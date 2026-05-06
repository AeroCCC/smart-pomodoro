package com.pomotodo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.AiDecomposeRequest;
import com.pomotodo.dto.AiDecomposeResponse;
import com.pomotodo.dto.AiTaskItem;
import com.pomotodo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AiTaskPlanningService {
    private static final int MIN_TASK_COUNT = 3;
    private static final int MAX_TASK_COUNT = 6;
    private static final Set<String> ALLOWED_RESULT_TYPES = Set.of("tasks", "needs_refinement", "rejected");
    private static final Set<String> ALLOWED_PRIORITIES = Set.of("low", "medium", "high");
    private static final int MAX_ATTEMPTS = 2;

    private final AiGoalValidator aiGoalValidator;
    private final AiCompletionClient aiCompletionClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    public AiDecomposeResponse decomposeTask(AiDecomposeRequest request) {
        if (request == null) {
            throw ApiException.badRequest("INVALID_REQUEST", "请求体不能为空。");
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            throw ApiException.badRequest("API_KEY_IN_REQUEST_FORBIDDEN", "不允许在请求体中传入 apiKey。");
        }

        String normalizedGoal = normalizeText(request.getGoal());
        if (normalizedGoal.isBlank()) {
            throw ApiException.badRequest("GOAL_REQUIRED", "请输入需要拆解的目标。");
        }

        AiDecomposeResponse localDecision = aiGoalValidator.validate(normalizedGoal);
        if (localDecision != null) {
            return localDecision;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("AI_API_KEY_MISSING", "通义千问 API Key 尚未配置，请检查 DASHSCOPE_API_KEY。");
        }

        Exception lastException = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                String content = aiCompletionClient.complete(buildPrompt(normalizedGoal, attempt > 0), apiKey);
                AiDecomposeResponse parsed = objectMapper.readValue(stripCodeFences(content), AiDecomposeResponse.class);
                return normalizeResponse(parsed, normalizedGoal);
            } catch (Exception exception) {
                lastException = exception;
            }
        }

        throw toAiServiceException("AI_SERVICE_ERROR", "AI 拆解暂时不可用，请稍后重试。", lastException);
    }

    private AiDecomposeResponse normalizeResponse(AiDecomposeResponse response, String originalGoal) {
        String resultType = normalizeToken(response.getResultType());
        if (!ALLOWED_RESULT_TYPES.contains(resultType)) {
            throw new IllegalArgumentException("不支持的 resultType: " + response.getResultType());
        }

        String normalizedGoal = normalizeText(response.getNormalizedGoal());
        if (normalizedGoal.isBlank()) {
            normalizedGoal = originalGoal;
        }

        return switch (resultType) {
            case "tasks" -> normalizeTaskResult(response, normalizedGoal);
            case "needs_refinement" -> normalizeRefinementResult(response, normalizedGoal);
            case "rejected" -> normalizeRejectedResult(response, normalizedGoal);
            default -> throw new IllegalStateException("意外的 resultType: " + resultType);
        };
    }

    private AiDecomposeResponse normalizeTaskResult(AiDecomposeResponse response, String normalizedGoal) {
        List<AiTaskItem> tasks = response.getTasks();
        if (tasks == null) {
            throw new IllegalArgumentException("当 resultType=tasks 时，tasks 不能为空");
        }

        List<AiTaskItem> normalizedTasks = new ArrayList<>();
        Set<String> seenTexts = new LinkedHashSet<>();
        for (AiTaskItem task : tasks) {
            if (task == null) {
                continue;
            }
            String text = normalizeText(task.getText());
            if (text.isBlank() || !seenTexts.add(text.toLowerCase(Locale.ROOT))) {
                continue;
            }
            normalizedTasks.add(AiTaskItem.builder()
                    .text(text)
                    .priority(normalizePriority(task.getPriority()))
                    .build());
        }

        if (normalizedTasks.size() < MIN_TASK_COUNT || normalizedTasks.size() > MAX_TASK_COUNT) {
            throw new IllegalArgumentException("任务数量必须在 3 到 6 个之间");
        }

        return AiDecomposeResponse.tasks(normalizedTasks, normalizedGoal);
    }

    private AiDecomposeResponse normalizeRefinementResult(AiDecomposeResponse response, String normalizedGoal) {
        String message = normalizeText(response.getMessage());
        if (message.isBlank()) {
            message = "这个目标还需要进一步聚焦，才能拆解成可执行任务。";
        }

        String reasonCode = normalizeReasonCode(response.getReasonCode(), "NOT_ACTIONABLE");
        List<String> suggestions = normalizeSuggestions(response.getSuggestions(), normalizedGoal);
        return AiDecomposeResponse.needsRefinement(reasonCode, message, suggestions, normalizedGoal);
    }

    private AiDecomposeResponse normalizeRejectedResult(AiDecomposeResponse response, String normalizedGoal) {
        String message = normalizeText(response.getMessage());
        if (message.isBlank()) {
            message = "这个目标暂时无法处理，请改写成安全、具体且可执行的任务。";
        }
        return AiDecomposeResponse.rejected(normalizeReasonCode(response.getReasonCode(), "INVALID_INPUT"), message, normalizedGoal);
    }

    private List<String> normalizeSuggestions(List<String> suggestions, String goal) {
        List<String> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (suggestions != null) {
            for (String suggestion : suggestions) {
                String text = normalizeText(suggestion);
                if (!text.isBlank() && seen.add(text.toLowerCase(Locale.ROOT))) {
                    normalized.add(text);
                }
            }
        }

        for (String fallback : aiGoalValidator.buildFallbackSuggestions(goal)) {
            if (normalized.size() >= 3) {
                break;
            }
            String normalizedFallback = normalizeText(fallback);
            if (seen.add(normalizedFallback.toLowerCase(Locale.ROOT))) {
                normalized.add(normalizedFallback);
            }
        }

        if (normalized.size() < 3) {
            throw new IllegalArgumentException("至少需要返回 3 条改写建议");
        }

        return normalized.subList(0, 3);
    }

    private String normalizePriority(String priority) {
        String normalized = normalizeToken(priority);
        if (ALLOWED_PRIORITIES.contains(normalized)) {
            return normalized;
        }
        return "medium";
    }

    private String normalizeReasonCode(String reasonCode, String fallback) {
        String normalized = normalizeText(reasonCode).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeToken(String value) {
        return normalizeText(value).toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String stripCodeFences(String content) {
        return content
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    private String buildPrompt(String goal, boolean retry) {
        String retryInstruction = retry
                ? "上一次输出无效。请严格遵守 JSON 结构，只返回 JSON。"
                : "";

        return """
                你是一个中文效率应用中的任务拆解助手。
                请判断用户目标是否可以拆成 3 到 6 个简短、可执行的任务。

                要求：
                1. 目标必须由用户自己可控，且足够近期、具体，适合拆成 3 到 6 个任务。
                2. 对于不安全、违法或有害目标，必须拒绝。
                3. 如果目标过于宽泛、抽象，或混杂多个目标，请返回 needs_refinement，而不是 tasks。
                4. 任务必须是单一步骤、实际可做、表达具体。
                5. 所有面向用户的文本字段都必须使用简体中文。

                仅返回 JSON，结构必须完全如下：
                {
                  "resultType": "tasks" | "needs_refinement" | "rejected",
                  "reasonCode": "OK | TOO_BROAD | NOT_ACTIONABLE | MULTI_GOAL | NO_USER_AGENCY | INSUFFICIENT_CONTEXT | INVALID_INPUT | UNSAFE",
                  "message": "给用户的简短说明",
                  "normalizedGoal": "整理后的目标",
                  "suggestions": ["建议 1", "建议 2", "建议 3"],
                  "tasks": [
                    { "text": "任务内容", "priority": "low" | "medium" | "high" }
                  ]
                }

                规则：
                - 如果 resultType 是 "tasks"，请提供 3 到 6 个任务，suggestions 为空数组。
                - 如果 resultType 是 "needs_refinement"，请提供恰好 3 条建议，tasks 为空数组。
                - 如果 resultType 是 "rejected"，tasks 和 suggestions 都必须为空数组。
                - 不要输出 Markdown 代码块，也不要输出任何额外说明。
                %s

                用户目标：%s
                """.formatted(retryInstruction, goal);
    }

    private ApiException toAiServiceException(String code, String fallbackMessage, Exception exception) {
        if (exception instanceof AiProviderException providerException) {
            return new ApiException(
                    HttpStatus.BAD_REQUEST,
                    providerException.getCode(),
                    providerException.getUserMessage(),
                    providerException.getDetails(),
                    providerException
            );
        }
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                code,
                fallbackMessage,
                exception == null ? null : exception.getMessage(),
                exception
        );
    }
}
