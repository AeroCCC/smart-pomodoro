package com.pomotodo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeamTaskResponse {
    private Long id;
    private String text;
    private String description;
    private String priority;
    private String status;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime deadline;
    
    // Creator info
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;
    
    // Assignee info
    private Long assignedToId;
    private String assignedToName;
    private String assignedToAvatar;
    
    // Team info
    private Long teamId;
    private String teamName;
}
