package com.pomotodo.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiError {
    private String code;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
}

