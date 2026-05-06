package com.pomotodo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DashScopeAiCompletionClient implements AiCompletionClient {

    @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @Value("${ai.model:qwen-max}")
    private String model;

    @Value("${ai.temperature:0.2}")
    private double temperature;

    @Override
    public String complete(String prompt, String apiKey) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", temperature
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl + "/chat/completions", entity, Map.class);
            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null) {
                throw new AiProviderException(
                        "DASHSCOPE_EMPTY_RESPONSE",
                        "通义千问返回了空响应，请稍后重试。",
                        "DashScope response body is empty",
                        null
                );
            }

            List<?> choices = (List<?>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiProviderException(
                        "DASHSCOPE_EMPTY_CHOICES",
                        "通义千问没有返回可用结果，请稍后重试。",
                        "DashScope response does not contain choices",
                        null
                );
            }

            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message = (Map<?, ?>) choice.get("message");
            if (message == null || message.get("content") == null) {
                throw new AiProviderException(
                        "DASHSCOPE_EMPTY_CONTENT",
                        "通义千问没有返回可解析的内容，请稍后重试。",
                        "DashScope response does not contain message content",
                        null
                );
            }

            return message.get("content").toString();
        } catch (HttpStatusCodeException exception) {
            throw mapHttpStatusException(exception);
        } catch (ResourceAccessException exception) {
            throw new AiProviderException(
                    "DASHSCOPE_NETWORK_ERROR",
                    "无法连接通义千问服务，请检查网络连接后重试。",
                    exception.getMessage(),
                    exception
            );
        }
    }

    private AiProviderException mapHttpStatusException(HttpStatusCodeException exception) {
        int statusCode = exception.getStatusCode().value();
        String responseBody = exception.getResponseBodyAsString();
        String details = responseBody == null || responseBody.isBlank()
                ? "HTTP " + statusCode
                : "HTTP " + statusCode + ": " + responseBody;

        if (statusCode == 401 || statusCode == 403) {
            return new AiProviderException(
                    "DASHSCOPE_AUTH_ERROR",
                    "通义千问 API Key 无效、没有权限，或当前账号未开通对应模型能力。",
                    details,
                    exception
            );
        }

        if (statusCode == 429) {
            return new AiProviderException(
                    "DASHSCOPE_RATE_LIMIT",
                    "通义千问请求过于频繁，或当前账号额度不足，请稍后重试。",
                    details,
                    exception
            );
        }

        if (statusCode >= 500) {
            return new AiProviderException(
                    "DASHSCOPE_SERVER_ERROR",
                    "通义千问服务暂时不可用，请稍后重试。",
                    details,
                    exception
            );
        }

        return new AiProviderException(
                "DASHSCOPE_REQUEST_ERROR",
                "通义千问请求失败，请检查模型、接口地址或请求参数配置。",
                details,
                exception
        );
    }
}
