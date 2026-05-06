package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiDecomposeRequest {
    private String goal;
    private String apiKey;
}
