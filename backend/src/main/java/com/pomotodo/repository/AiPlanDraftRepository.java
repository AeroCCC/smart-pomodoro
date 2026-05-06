package com.pomotodo.repository;

import com.pomotodo.entity.AiPlanDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPlanDraftRepository extends JpaRepository<AiPlanDraft, Long> {
    Optional<AiPlanDraft> findByIdAndUserId(Long id, Long userId);
}
