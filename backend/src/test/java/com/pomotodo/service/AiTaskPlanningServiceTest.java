package com.pomotodo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pomotodo.dto.AiDecomposeRequest;
import com.pomotodo.dto.AiDecomposeResponse;
import com.pomotodo.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiTaskPlanningServiceTest {

    @Test
    void shouldRejectClientProvidedApiKey() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        AiTaskPlanningService service = createService(client, "server-key");

        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("准备下周的产品演示");
        request.setApiKey("client-key");

        assertThatThrownBy(() -> service.decomposeTask(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("API_KEY_IN_REQUEST_FORBIDDEN", "不允许在请求体中传入 apiKey。");
    }

    @Test
    void shouldRejectMissingGoal() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        AiTaskPlanningService service = createService(client, "server-key");

        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("   ");

        assertThatThrownBy(() -> service.decomposeTask(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("GOAL_REQUIRED", "请输入需要拆解的目标。");
    }

    @Test
    void shouldRejectWhenServerApiKeyNotConfiguredForModelTask() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        AiTaskPlanningService service = createService(client, "");

        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("规划一次期末复习");

        assertThatThrownBy(() -> service.decomposeTask(request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode(), Throwable::getMessage)
                .containsExactly("AI_API_KEY_MISSING", "通义千问 API Key 尚未配置，请检查 DASHSCOPE_API_KEY。");
    }

    @Test
    void shouldReturnLocalRefinementWithoutCallingModel() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        AiTaskPlanningService service = createService(client, "");

        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("成为世界首富");

        AiDecomposeResponse response = service.decomposeTask(request);

        assertThat(response.getResultType()).isEqualTo("needs_refinement");
        assertThat(response.getReasonCode()).isEqualTo("TOO_BROAD");
        assertThat(response.getSuggestions()).hasSize(3);
        assertThat(client.callCount).isZero();
    }

    @Test
    void shouldReturnStructuredTasksFromModel() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        client.responses.add("""
                {
                  "resultType": "tasks",
                  "reasonCode": "OK",
                  "message": "ready",
                  "normalizedGoal": "准备下周的产品演示",
                  "tasks": [
                    { "text": "列出演示要展示的核心功能", "priority": "high" },
                    { "text": "编写 5 分钟演示脚本", "priority": "medium" },
                    { "text": "完整彩排一次演示流程", "priority": "low" }
                  ],
                  "suggestions": []
                }
                """);

        AiTaskPlanningService service = createService(client, "server-key");
        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("准备下周的产品演示");

        AiDecomposeResponse response = service.decomposeTask(request);

        assertThat(response.getResultType()).isEqualTo("tasks");
        assertThat(response.getTasks()).hasSize(3);
        assertThat(response.getTasks()).extracting("priority")
                .containsExactly("high", "medium", "low");
        assertThat(client.callCount).isEqualTo(1);
    }

    @Test
    void shouldRetryWhenModelReturnsInvalidTaskPayload() {
        StubAiCompletionClient client = new StubAiCompletionClient();
        client.responses.add("""
                {
                  "resultType": "tasks",
                  "reasonCode": "OK",
                  "message": "invalid",
                  "tasks": [
                    { "text": "只有一个任务", "priority": "high" }
                  ],
                  "suggestions": []
                }
                """);
        client.responses.add("""
                {
                  "resultType": "tasks",
                  "reasonCode": "OK",
                  "message": "valid",
                  "normalizedGoal": "规划一次高效复习",
                  "tasks": [
                    { "text": "确定本次复习主题", "priority": "high" },
                    { "text": "准备需要的资料和笔记", "priority": "medium" },
                    { "text": "预留一个 45 分钟的复习时段", "priority": "medium" }
                  ],
                  "suggestions": []
                }
                """);

        AiTaskPlanningService service = createService(client, "server-key");
        AiDecomposeRequest request = new AiDecomposeRequest();
        request.setGoal("规划一次高效复习");

        AiDecomposeResponse response = service.decomposeTask(request);

        assertThat(response.getResultType()).isEqualTo("tasks");
        assertThat(response.getTasks()).hasSize(3);
        assertThat(client.callCount).isEqualTo(2);
    }

    private AiTaskPlanningService createService(StubAiCompletionClient client, String apiKey) {
        AiTaskPlanningService service = new AiTaskPlanningService(
                new AiGoalValidator(),
                client,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        return service;
    }

    private static class StubAiCompletionClient implements AiCompletionClient {
        private final Queue<String> responses = new ArrayDeque<>();
        private int callCount = 0;

        @Override
        public String complete(String prompt, String apiKey) {
            callCount++;
            if (responses.isEmpty()) {
                throw new IllegalStateException("No stubbed AI response");
            }
            return responses.remove();
        }
    }
}
