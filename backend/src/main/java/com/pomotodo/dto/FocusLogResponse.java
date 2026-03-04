package com.pomotodo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FocusLogResponse {
    private Long id;
    private LocalDate date;
    private int duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

