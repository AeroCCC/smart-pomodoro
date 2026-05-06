package com.pomotodo.repository;

import com.pomotodo.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCompletedOrderByCreatedAtDesc(boolean completed);
    
    List<Task> findAllByOrderByCreatedAtDesc();
    
    // User-specific queries
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<Task> findByUserIdAndCompletedOrderByCreatedAtDesc(Long userId, boolean completed);

    List<Task> findByUserIdAndCompletedFalseOrderByCreatedAtDesc(Long userId);
    
    Optional<Task> findByIdAndUserId(Long id, Long userId);
    
    long countByUserIdAndCompleted(Long userId, boolean completed);
    
    long countByUserId(Long userId);
    
    // Find pending tasks with deadlines between two dates
    List<Task> findByDeadlineBetweenAndCompletedFalse(LocalDateTime start, LocalDateTime end);
    
    // Team task queries
    List<Task> findByTeamIdOrderByCreatedAtDesc(Long teamId);
    
    List<Task> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, String status);
    
    Optional<Task> findByIdAndTeamId(Long id, Long teamId);
    
    long countByTeamIdAndStatus(Long teamId, String status);
    
    long countByTeamIdAndAssignedToId(Long teamId, Long assignedToId);
    
    // Update task status (for drag and drop)
    @Modifying
    @Query("UPDATE Task t SET t.status = :status WHERE t.id = :taskId AND t.team.id = :teamId")
    int updateTaskStatus(@Param("taskId") Long taskId, @Param("teamId") Long teamId, @Param("status") String status);
    
    // Update task assignee
    @Modifying
    @Query("UPDATE Task t SET t.assignedTo.id = :assignedToId WHERE t.id = :taskId AND t.team.id = :teamId")
    int updateTaskAssignee(@Param("taskId") Long taskId, @Param("teamId") Long teamId, @Param("assignedToId") Long assignedToId);
    
    // Find tasks assigned to a specific user in a team
    List<Task> findByTeamIdAndAssignedToIdOrderByCreatedAtDesc(Long teamId, Long assignedToId);
}
