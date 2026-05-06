package com.pomotodo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiTaskItem {
    private String text;

    @Builder.Default
    private String priority = "medium";
}
