package com.pomotodo.repository;

import com.pomotodo.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    
    List<PushSubscription> findByUserIdAndIsActiveTrue(Long userId);
    
    Optional<PushSubscription> findByEndpoint(String endpoint);
    
    Optional<PushSubscription> findByEndpointAndUserId(String endpoint, Long userId);
    
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.user.id = :userId AND ps.isActive = true")
    List<PushSubscription> findActiveSubscriptionsByUser(@Param("userId") Long userId);
    
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET ps.isActive = false WHERE ps.endpoint = :endpoint")
    void deactivateByEndpoint(@Param("endpoint") String endpoint);
    
    @Modifying
    @Transactional
    @Query("UPDATE PushSubscription ps SET ps.lastUsed = :lastUsed WHERE ps.id = :id")
    void updateLastUsed(@Param("id") Long id, @Param("lastUsed") LocalDateTime lastUsed);
    
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.isActive = true AND ps.user.id IN (SELECT t.user.id FROM Task t WHERE t.deadline <= :deadline AND t.completed = false)")
    List<PushSubscription> findSubscriptionsForTasksWithDeadline(@Param("deadline") LocalDateTime deadline);
}
