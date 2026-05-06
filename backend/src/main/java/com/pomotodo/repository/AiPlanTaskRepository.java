package com.pomotodo.repository;

import com.pomotodo.entity.AiPlanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiPlanTaskRepository extends JpaRepository<AiPlanTask, Long> {
}
