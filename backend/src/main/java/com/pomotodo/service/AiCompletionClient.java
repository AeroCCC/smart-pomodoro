package com.pomotodo.service;

public interface AiCompletionClient {
    String complete(String prompt, String apiKey);
}
