package com.pomotodo.controller;

import com.pomotodo.dto.FocusLogResponse;
import com.pomotodo.exception.ApiException;
import com.pomotodo.entity.FocusLog;
import com.pomotodo.entity.User;
import com.pomotodo.repository.FocusLogRepository;
import com.pomotodo.repository.UserRepository;
import com.pomotodo.service.PushNotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/focus")
@RequiredArgsConstructor
public class FocusController {
    
    private final FocusLogRepository focusLogRepository;
    private final UserRepository userRepository;
    private final PushNotificationService notificationService;
    
    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }
    
    @PostMapping
    public ResponseEntity<?> saveFocusLog(@RequestBody FocusLogRequest request,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getCurrentUser(userDetails);
            
            FocusLog log = new FocusLog();
            log.setUser(user);
            log.setDate(LocalDate.now());
            log.setDuration(request.getDuration());
            log.setStartTime(request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now().minusSeconds(request.getDuration()));
            log.setEndTime(LocalDateTime.now());
            
            FocusLog saved = focusLogRepository.save(log);
            
            // Get today's stats for notification
            LocalDate today = LocalDate.now();
            Long todaySessions = focusLogRepository.getTodaySessionsByUser(user.getId(), today);
            Integer todayMinutes = focusLogRepository.getTotalDurationByUserAndDate(user.getId(), today);
            
            // Send notification
            if (todaySessions != null && todaySessions > 0) {
                notificationService.sendPomodoroCompleteNotification(
                        user.getId(), 
                        todaySessions.intValue(), 
                        todayMinutes != null ? todayMinutes / 60 : 0
                );
            }
            
            return ResponseEntity.ok(convertToFocusLogResponse(saved));
        } catch (Exception e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw ApiException.badRequest("FOCUS_LOG_SAVE_FAILED", "Failed to save focus log", e);
        }
    }
    
    @GetMapping("/today")
    public ResponseEntity<?> getTodayStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        LocalDate today = LocalDate.now();
        
        Integer totalSeconds = focusLogRepository.getTotalDurationByUserAndDate(user.getId(), today);
        Long sessions = focusLogRepository.getTodaySessionsByUser(user.getId(), today);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSeconds", totalSeconds != null ? totalSeconds : 0);
        stats.put("totalMinutes", totalSeconds != null ? totalSeconds / 60 : 0);
        stats.put("sessions", sessions != null ? sessions : 0);
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);
        
        List<Object[]> data = focusLogRepository.getDailyStatsByUser(user.getId(), start, end);
        
        List<Map<String, Object>> result = data.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", row[0].toString());
                    map.put("duration", row[1] != null ? ((Number) row[1]).intValue() / 60 : 0);
                    map.put("sessions", row[2] != null ? ((Number) row[2]).intValue() : 0);
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/logs")
    public ResponseEntity<?> getFocusLogs(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<FocusLog> logs = focusLogRepository.findByUserIdAndDateOrderByStartTimeDesc(user.getId(), LocalDate.now());
        return ResponseEntity.ok(logs.stream()
                .map(this::convertToFocusLogResponse)
                .collect(Collectors.toList()));
    }
    
    @GetMapping("/daily")
    public ResponseEntity<?> getDailyStats(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);
        
        List<Object[]> data = focusLogRepository.getDailyStatsByUser(user.getId(), start, end);
        
        List<Map<String, Object>> result = data.stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", row[0].toString());
                    map.put("duration", row[1] != null ? ((Number) row[1]).intValue() : 0);
                    map.put("sessions", row[2] != null ? ((Number) row[2]).intValue() : 0);
                    return map;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(Map.of("data", result));
    }
    
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        Integer totalSeconds = focusLogRepository.getTotalDurationSince(user.getId(), startOfWeek);
        Long sessions = focusLogRepository.getTotalSessionsByUser(user.getId());
        Long activeDays = focusLogRepository.getActiveDaysCount(user.getId(), startOfWeek);
        
        int totalMinutes = totalSeconds != null ? totalSeconds / 60 : 0;
        int daysInWeek = (int) java.time.temporal.ChronoUnit.DAYS.between(startOfWeek, today) + 1;
        int avgMinutes = activeDays != null && activeDays > 0 ? totalMinutes / activeDays.intValue() : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMinutes", totalMinutes);
        stats.put("totalHours", totalMinutes / 60);
        stats.put("sessions", sessions != null ? sessions : 0);
        stats.put("activeDays", activeDays != null ? activeDays : 0);
        stats.put("avgMinutesPerDay", avgMinutes);
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyStats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        
        Integer totalSeconds = focusLogRepository.getTotalDurationSince(user.getId(), startOfMonth);
        Long activeDays = focusLogRepository.getActiveDaysCount(user.getId(), startOfMonth);
        
        int totalMinutes = totalSeconds != null ? totalSeconds / 60 : 0;
        int avgMinutes = activeDays != null && activeDays > 0 ? totalMinutes / activeDays.intValue() : 0;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMinutes", totalMinutes);
        stats.put("totalHours", totalMinutes / 60);
        stats.put("activeDays", activeDays != null ? activeDays : 0);
        stats.put("avgMinutesPerDay", avgMinutes);
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/habits")
    public ResponseEntity<?> getHabits(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<FocusLog> logs = focusLogRepository.findByUserIdOrderByDateDesc(user.getId());
        
        String earliestTime = null;
        String latestTime = null;
        String peakHour = "09:00-11:00";
        
        if (!logs.isEmpty()) {
            LocalDateTime earliest = logs.stream()
                    .map(FocusLog::getStartTime)
                    .filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            LocalDateTime latest = logs.stream()
                    .map(FocusLog::getStartTime)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            
            if (earliest != null) {
                earliestTime = String.format("%02d:%02d", earliest.getHour(), earliest.getMinute());
            }
            if (latest != null) {
                latestTime = String.format("%02d:%02d", latest.getHour(), latest.getMinute());
            }
        }
        
        int currentStreak = calculateCurrentStreak(user.getId());
        int longestStreak = calculateLongestStreak(user.getId());
        
        Map<String, Object> habits = new HashMap<>();
        habits.put("earliestTime", earliestTime != null ? earliestTime : "N/A");
        habits.put("latestTime", latestTime != null ? latestTime : "N/A");
        habits.put("peakHours", peakHour);
        habits.put("currentStreak", currentStreak);
        habits.put("longestStreak", longestStreak);
        
        return ResponseEntity.ok(habits);
    }
    
    private int calculateCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate checkDate = today;
        
        while (true) {
            Integer duration = focusLogRepository.getTotalDurationByUserAndDate(userId, checkDate);
            if (duration != null && duration > 0) {
                streak++;
                checkDate = checkDate.minusDays(1);
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private int calculateLongestStreak(Long userId) {
        List<FocusLog> allLogs = focusLogRepository.findByUserIdOrderByDateDesc(userId);
        
        if (allLogs.isEmpty()) {
            return 0;
        }
        
        List<LocalDate> dates = allLogs.stream()
                .map(FocusLog::getDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        
        if (dates.isEmpty()) {
            return 0;
        }
        
        int maxStreak = 1;
        int currentStreak = 1;
        
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).equals(dates.get(i - 1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }
        
        return maxStreak;
    }
    
    @Data
    public static class FocusLogRequest {
        private int duration; // in seconds
        private LocalDateTime startTime;
    }

    private FocusLogResponse convertToFocusLogResponse(FocusLog log) {
        FocusLogResponse dto = new FocusLogResponse();
        dto.setId(log.getId());
        dto.setDate(log.getDate());
        dto.setDuration(log.getDuration());
        dto.setStartTime(log.getStartTime());
        dto.setEndTime(log.getEndTime());
        return dto;
    }
}
