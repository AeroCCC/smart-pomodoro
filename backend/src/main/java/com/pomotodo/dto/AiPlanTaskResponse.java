package com.pomotodo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPlanTaskResponse {
    private Long id;
    private String text;
    private String priority;
    private String completionDefinition;
    private Integer estimatedPomodoros;
    private LocalDateTime suggestedDeadline;
    private boolean selected;
}
