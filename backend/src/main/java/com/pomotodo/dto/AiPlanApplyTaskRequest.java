package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanApplyTaskRequest {
    private Long draftTaskId;
    private String text;
    private String priority;
    private String completionDefinition;
    private Integer estimatedPomodoros;
    private String suggestedDeadline;
    private Boolean selected;
}
