package com.pomotodo.repository;

import com.pomotodo.entity.FocusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FocusLogRepository extends JpaRepository<FocusLog, Long> {
    
    List<FocusLog> findByDate(LocalDate date);
    
    List<FocusLog> findByUserIdAndDateOrderByStartTimeDesc(Long userId, LocalDate date);
    
    Optional<FocusLog> findByIdAndUserId(Long id, Long userId);
    
    @Query("SELECT SUM(f.duration) FROM FocusLog f WHERE f.date = ?1")
    Integer getTotalDurationByDate(LocalDate date);
    
    @Query("SELECT SUM(f.duration) FROM FocusLog f WHERE f.user.id = :userId AND f.date = :date")
    Integer getTotalDurationByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    @Query("SELECT f.date, SUM(f.duration), COUNT(f) FROM FocusLog f WHERE f.date BETWEEN ?1 AND ?2 GROUP BY f.date ORDER BY f.date")
    List<Object[]> getDailyStats(LocalDate start, LocalDate end);
    
    @Query("SELECT f.date, SUM(f.duration), COUNT(f) FROM FocusLog f WHERE f.user.id = :userId AND f.date BETWEEN :start AND :end GROUP BY f.date ORDER BY f.date")
    List<Object[]> getDailyStatsByUser(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT SUM(f.duration) FROM FocusLog f WHERE f.user.id = :userId AND f.date >= :startDate")
    Integer getTotalDurationSince(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    @Query("SELECT COUNT(DISTINCT f.date) FROM FocusLog f WHERE f.user.id = :userId AND f.date >= :startDate")
    Long getActiveDaysCount(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    @Query("SELECT COUNT(f) FROM FocusLog f WHERE f.user.id = :userId")
    Long getTotalSessionsByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM FocusLog f WHERE f.user.id = :userId AND f.date = :date")
    Long getTodaySessionsByUser(@Param("userId") Long userId, @Param("date") LocalDate date);
    
    // Get longest streak of consecutive days with focus sessions
    @Query(value = "SELECT COUNT(DISTINCT date) FROM focus_logs WHERE user_id = :userId AND date >= :startDate", nativeQuery = true)
    Long countDistinctActiveDays(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    List<FocusLog> findByUserIdOrderByDateDesc(Long userId);
}
