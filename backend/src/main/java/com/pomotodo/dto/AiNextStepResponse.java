package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiNextStepResponse {
    private String source;
    private Long taskId;
    private Long draftTaskId;
    private String text;
    private String reason;
    private String priority;
    private Integer suggestedFocusMinutes;
    private String action;
}
