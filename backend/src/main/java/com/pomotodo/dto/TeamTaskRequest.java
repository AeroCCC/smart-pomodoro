package com.pomotodo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamTaskRequest {
    private String text;
    private String description;
    private String priority = "medium"; // low, medium, high
    private String deadline; // ISO format string
    private String status = "TODO"; // TODO, IN_PROGRESS, DONE
    private Long assignedToId; // User ID to assign task to
}
