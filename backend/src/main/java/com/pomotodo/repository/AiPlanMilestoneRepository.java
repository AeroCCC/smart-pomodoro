package com.pomotodo.repository;

import com.pomotodo.entity.AiPlanMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPlanMilestoneRepository extends JpaRepository<AiPlanMilestone, Long> {
}
