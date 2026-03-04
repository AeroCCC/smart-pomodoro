package com.pomotodo.controller;

import com.pomotodo.dto.DashboardDTO;
import com.pomotodo.exception.ApiException;
import com.pomotodo.entity.FocusLog;
import com.pomotodo.entity.Task;
import com.pomotodo.entity.User;
import com.pomotodo.repository.FocusLogRepository;
import com.pomotodo.repository.TaskRepository;
import com.pomotodo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final TaskRepository taskRepository;
    private final FocusLogRepository focusLogRepository;
    private final UserRepository userRepository;
    
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<DashboardDTO.DashboardStats> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getCurrentUser(userDetails);
        Long userId = user.getId();
        
        // Task statistics
        List<Task> userTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long totalTasks = userTasks.size();
        long completedTasks = userTasks.stream().filter(Task::isCompleted).count();
        long pendingTasks = totalTasks - completedTasks;
        int completionRate = totalTasks > 0 ? (int) Math.round((double) completedTasks / totalTasks * 100) : 0;
        
        // Focus statistics
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        
        Integer todayMinutes = focusLogRepository.getTotalDurationByUserAndDate(userId, today);
        todayMinutes = todayMinutes != null ? todayMinutes / 60 : 0;
        
        Integer weekMinutes = focusLogRepository.getTotalDurationSince(userId, weekStart);
        weekMinutes = weekMinutes != null ? weekMinutes / 60 : 0;
        
        Long todaySessions = focusLogRepository.getTodaySessionsByUser(userId, today);
        todaySessions = todaySessions != null ? todaySessions : 0;
        
        Long totalSessions = focusLogRepository.getTotalSessionsByUser(userId);
        totalSessions = totalSessions != null ? totalSessions : 0;
        
        // Calculate streak
        int currentStreak = calculateCurrentStreak(userId);
        
        // Weekly activity data
        List<DashboardDTO.DailyActivity> weeklyActivity = getWeeklyActivity(userId, weekStart, today);
        
        // Task categories (based on priority for now)
        List<DashboardDTO.TaskCategory> categories = getTaskCategories(userTasks);
        
        // Recent tasks (last 5)
        List<DashboardDTO.RecentTask> recentTasks = userTasks.stream()
                .limit(5)
                .map(task -> DashboardDTO.RecentTask.builder()
                        .id(task.getId())
                        .text(task.getText())
                        .priority(task.getPriority())
                        .completed(task.isCompleted())
                        .createdAt(task.getCreatedAt() != null ? 
                                task.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd")) : "-")
                        .build())
                .collect(Collectors.toList());
        
        DashboardDTO.DashboardStats stats = DashboardDTO.DashboardStats.builder()
                .completedTasks((int) completedTasks)
                .completedTasksChange(12) // Mock change percentage
                .focusHours(weekMinutes / 60.0)
                .focusHoursChange(-5)
                .bestStreak(currentStreak)
                .bestStreakChange(1)
                .score(calculateScore(completedTasks, totalSessions, currentStreak))
                .scoreChange(45)
                .focusActivity(weeklyActivity)
                .taskCategories(categories)
                .recentTasks(recentTasks)
                .build();
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/focus-stats")
    public ResponseEntity<DashboardDTO.FocusStats> getFocusStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = getCurrentUser(userDetails);
        Long userId = user.getId();
        
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        Integer todayMinutes = focusLogRepository.getTotalDurationByUserAndDate(userId, today);
        todayMinutes = todayMinutes != null ? todayMinutes / 60 : 0;
        
        Long todaySessions = focusLogRepository.getTodaySessionsByUser(userId, today);
        
        Integer weekMinutes = focusLogRepository.getTotalDurationSince(userId, weekStart);
        weekMinutes = weekMinutes != null ? weekMinutes / 60 : 0;
        
        Long totalSessions = focusLogRepository.getTotalSessionsByUser(userId);
        
        int currentStreak = calculateCurrentStreak(userId);
        int longestStreak = calculateLongestStreak(userId);
        
        List<DashboardDTO.DailyActivity> weeklyActivity = getWeeklyActivity(userId, weekStart, today);
        
        DashboardDTO.FocusStats stats = DashboardDTO.FocusStats.builder()
                .todayMinutes(todayMinutes)
                .todaySessions(todaySessions != null ? todaySessions.intValue() : 0)
                .weekMinutes(weekMinutes != null ? weekMinutes : 0)
                .weekSessions(weeklyActivity.stream().mapToInt(DashboardDTO.DailyActivity::getTasks).sum())
                .totalSessions(totalSessions != null ? totalSessions.intValue() : 0)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .weeklyActivity(weeklyActivity)
                .build();
        
        return ResponseEntity.ok(stats);
    }
    
    private int calculateCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        
        // Check today first
        Long todaySessions = focusLogRepository.getTodaySessionsByUser(userId, today);
        if (todaySessions != null && todaySessions > 0) {
            streak++;
        }
        
        // Check previous days
        LocalDate checkDate = today.minusDays(1);
        while (true) {
            Long sessions = focusLogRepository.getTodaySessionsByUser(userId, checkDate);
            if (sessions != null && sessions > 0) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private int calculateLongestStreak(Long userId) {
        // Simplified: get all dates with focus sessions and find longest consecutive sequence
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Object[]> dailyStats = focusLogRepository.getDailyStatsByUser(userId, thirtyDaysAgo, LocalDate.now());
        
        if (dailyStats.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> activeDates = dailyStats.stream()
                .map(row -> (LocalDate) row[0])
                .sorted()
                .collect(Collectors.toList());
        
        int maxStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < activeDates.size(); i++) {
            if (ChronoUnit.DAYS.between(activeDates.get(i - 1), activeDates.get(i)) == 1) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return maxStreak;
    }
    
    private List<DashboardDTO.DailyActivity> getWeeklyActivity(Long userId, LocalDate weekStart, LocalDate today) {
        List<Object[]> dailyStats = focusLogRepository.getDailyStatsByUser(userId, weekStart, today);
        Map<LocalDate, Object[]> statsMap = dailyStats.stream()
                .collect(Collectors.toMap(row -> (LocalDate) row[0], row -> row));
        
        List<DashboardDTO.DailyActivity> activities = new ArrayList<>();
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            Object[] stats = statsMap.get(date);
            
            int minutes = 0;
            int tasks = 0;
            
            if (stats != null) {
                minutes = stats[1] != null ? ((Number) stats[1]).intValue() / 60 : 0;
                tasks = stats[2] != null ? ((Number) stats[2]).intValue() : 0;
            }
            
            activities.add(DashboardDTO.DailyActivity.builder()
                    .day(dayNames[i])
                    .minutes(minutes)
                    .tasks(tasks)
                    .build());
        }
        
        return activities;
    }
    
    private List<DashboardDTO.TaskCategory> getTaskCategories(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return Arrays.asList(
                    DashboardDTO.TaskCategory.builder().name("Work").percentage(0).count(0).color("#FF6B35").build(),
                    DashboardDTO.TaskCategory.builder().name("Personal").percentage(0).count(0).color("#4299E1").build(),
                    DashboardDTO.TaskCategory.builder().name("Study").percentage(0).count(0).color("#9F7AEA").build(),
                    DashboardDTO.TaskCategory.builder().name("Health").percentage(0).count(0).color("#48BB78").build()
            );
        }
        
        // For now, categorize by priority as a proxy for categories
        long highCount = tasks.stream().filter(t -> "high".equals(t.getPriority())).count();
        long mediumCount = tasks.stream().filter(t -> "medium".equals(t.getPriority())).count();
        long lowCount = tasks.stream().filter(t -> "low".equals(t.getPriority())).count();
        long total = tasks.size();
        
        return Arrays.asList(
                DashboardDTO.TaskCategory.builder()
                        .name("High Priority")
                        .percentage((int) Math.round((double) highCount / total * 100))
                        .count((int) highCount)
                        .color("#F56565")
                        .build(),
                DashboardDTO.TaskCategory.builder()
                        .name("Medium Priority")
                        .percentage((int) Math.round((double) mediumCount / total * 100))
                        .count((int) mediumCount)
                        .color("#ED8936")
                        .build(),
                DashboardDTO.TaskCategory.builder()
                        .name("Low Priority")
                        .percentage((int) Math.round((double) lowCount / total * 100))
                        .count((int) lowCount)
                        .color("#48BB78")
                        .build()
        );
    }
    
    private int calculateScore(long completedTasks, long totalSessions, int streak) {
        // Simple scoring algorithm
        int taskScore = (int) completedTasks * 10;
        int sessionScore = (int) totalSessions * 5;
        int streakScore = streak * 20;
        return Math.min(1000, taskScore + sessionScore + streakScore);
    }
}
