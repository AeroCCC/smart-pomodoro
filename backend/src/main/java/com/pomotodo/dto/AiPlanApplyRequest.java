package com.pomotodo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanApplyRequest {
    private List<AiPlanApplyTaskRequest> tasks = new ArrayList<>();
}
