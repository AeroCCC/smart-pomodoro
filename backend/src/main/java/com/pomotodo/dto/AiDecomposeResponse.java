package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AiDecomposeResponse {
    private String resultType;
    private String reasonCode;
    private String message;
    private String normalizedGoal;

    @Builder.Default
    private List<String> suggestions = List.of();

    @Builder.Default
    private List<AiTaskItem> tasks = List.of();

    public static AiDecomposeResponse tasks(List<AiTaskItem> tasks, String normalizedGoal) {
        return AiDecomposeResponse.builder()
                .resultType("tasks")
                .reasonCode("OK")
                .message("已生成可执行的任务拆解。")
                .normalizedGoal(normalizedGoal)
                .tasks(tasks)
                .build();
    }

    public static AiDecomposeResponse needsRefinement(String reasonCode,
                                                      String message,
                                                      List<String> suggestions,
                                                      String normalizedGoal) {
        return AiDecomposeResponse.builder()
                .resultType("needs_refinement")
                .reasonCode(reasonCode)
                .message(message)
                .normalizedGoal(normalizedGoal)
                .suggestions(suggestions)
                .build();
    }

    public static AiDecomposeResponse rejected(String reasonCode, String message, String normalizedGoal) {
        return AiDecomposeResponse.builder()
                .resultType("rejected")
                .reasonCode(reasonCode)
                .message(message)
                .normalizedGoal(normalizedGoal)
                .build();
    }
}
