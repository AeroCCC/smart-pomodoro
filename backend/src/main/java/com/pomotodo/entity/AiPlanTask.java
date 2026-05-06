package com.pomotodo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_plan_tasks", indexes = {
        @Index(name = "idx_ai_plan_tasks_milestone_sort", columnList = "milestone_id, sort_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPlanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "milestone_id", nullable = false)
    private AiPlanMilestone milestone;

    @Column(nullable = false, length = 300)
    private String text;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "completion_definition", length = 500)
    private String completionDefinition;

    @Column(name = "estimated_pomodoros")
    private Integer estimatedPomodoros;

    @Column(name = "suggested_deadline")
    private LocalDateTime suggestedDeadline;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    @Builder.Default
    private boolean selected = true;
}
