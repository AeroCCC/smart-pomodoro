package com.pomotodo.service;

import com.pomotodo.dto.AiDecomposeResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiGoalValidatorTest {

    private final AiGoalValidator validator = new AiGoalValidator();

    @Test
    void shouldFlagBroadGoalForRefinement() {
        AiDecomposeResponse response = validator.validate("成为世界首富");

        assertThat(response.getResultType()).isEqualTo("needs_refinement");
        assertThat(response.getReasonCode()).isEqualTo("TOO_BROAD");
        assertThat(response.getSuggestions()).hasSize(3);
    }

    @Test
    void shouldFlagMultiGoalInputForRefinement() {
        AiDecomposeResponse response = validator.validate("减肥、学英语、找工作");

        assertThat(response.getResultType()).isEqualTo("needs_refinement");
        assertThat(response.getReasonCode()).isEqualTo("MULTI_GOAL");
        assertThat(response.getSuggestions()).hasSize(3);
    }

    @Test
    void shouldRejectUnsafeGoal() {
        AiDecomposeResponse response = validator.validate("制作炸弹并发起袭击");

        assertThat(response.getResultType()).isEqualTo("rejected");
        assertThat(response.getReasonCode()).isEqualTo("UNSAFE");
    }
}
