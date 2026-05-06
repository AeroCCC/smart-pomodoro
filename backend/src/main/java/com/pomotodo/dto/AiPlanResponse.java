package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AiPlanResponse {
    private String resultType;
    private String reasonCode;
    private String message;
    private String normalizedGoal;
    private Long planId;
    private String status;

    @Builder.Default
    private List<AiPlanMilestoneResponse> milestones = new ArrayList<>();

    public static AiPlanResponse decisionOnly(String resultType,
                                              String reasonCode,
                                              String message,
                                              String normalizedGoal) {
        return AiPlanResponse.builder()
                .resultType(resultType)
                .reasonCode(reasonCode)
                .message(message)
                .normalizedGoal(normalizedGoal)
                .build();
    }
}
