package com.pomotodo.controller;

import com.pomotodo.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    @PostMapping("/decompose")
    public Map<String, Object> decomposeTask(@RequestBody Map<String, String> request) {
        String goal = request.get("goal");
        String serverApiKey = apiKey;

        if (serverApiKey == null || serverApiKey.isEmpty()) {
            throw ApiException.badRequest("AI_API_KEY_MISSING", "AI service is not configured");
        }
        if (goal == null || goal.isBlank()) {
            throw ApiException.badRequest("GOAL_REQUIRED", "Goal is required");
        }

        String prompt = """
            You are a task planning assistant.
            Break the user's goal into 3-6 actionable tasks.
            Return a JSON array only.
            Each task object must include:
            - text
            - priority (low/medium/high)

            User goal: %s
            """.formatted(goal);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + serverApiKey);

            Map<String, Object> body = Map.of(
                    "model", "qwen-plus",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", entity, Map.class
            );

            Map responseBody = response.getBody();
            List choices = (List) responseBody.get("choices");
            Map choice = (Map) choices.get(0);
            Map message = (Map) choice.get("message");
            String content = (String) message.get("content");
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            return Map.of("tasks", content);
        } catch (Exception e) {
            throw ApiException.badRequest("AI_SERVICE_ERROR", "Failed to process AI request", e);
        }
    }
}

