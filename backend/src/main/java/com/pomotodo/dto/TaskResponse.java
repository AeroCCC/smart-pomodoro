package com.pomotodo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String text;
    private String priority;
    private String completionDefinition;
    private Integer estimatedPomodoros;
    private String status;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime deadline;
}
