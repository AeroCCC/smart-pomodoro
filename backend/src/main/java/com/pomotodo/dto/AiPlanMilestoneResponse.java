package com.pomotodo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPlanMilestoneResponse {
    private Long id;
    private String title;
    private String summary;

    @Builder.Default
    private List<AiPlanTaskResponse> tasks = new ArrayList<>();
}
