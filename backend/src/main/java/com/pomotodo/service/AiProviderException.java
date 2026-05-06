package com.pomotodo.service;

import lombok.Getter;

@Getter
public class AiProviderException extends RuntimeException {
    private final String code;
    private final String userMessage;
    private final String details;

    public AiProviderException(String code, String userMessage, String details, Throwable cause) {
        super(userMessage, cause);
        this.code = code;
        this.userMessage = userMessage;
        this.details = details;
    }
}
