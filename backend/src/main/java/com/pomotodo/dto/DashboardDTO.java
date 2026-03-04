package com.pomotodo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class DashboardDTO {
    
    @Data
    @Builder
    public static class DashboardStats {
        private int completedTasks;
        private int completedTasksChange; // percentage change
        
        private double focusHours;
        private int focusHoursChange;
        
        private int bestStreak;
        private int bestStreakChange;
        
        private int score;
        private int scoreChange;
        
        private List<DailyActivity> focusActivity;
        private List<TaskCategory> taskCategories;
        private List<RecentTask> recentTasks;
    }
    
    @Data
    @Builder
    public static class DailyActivity {
        private String day;
        private int minutes;
        private int tasks;
    }
    
    @Data
    @Builder
    public static class TaskCategory {
        private String name;
        private int percentage;
        private int count;
        private String color;
    }
    
    @Data
    @Builder
    public static class RecentTask {
        private Long id;
        private String text;
        private String priority;
        private boolean completed;
        private String createdAt;
    }
    
    @Data
    @Builder
    public static class FocusStats {
        private int todayMinutes;
        private int todaySessions;
        private int weekMinutes;
        private int weekSessions;
        private int totalSessions;
        private int currentStreak;
        private int longestStreak;
        private List<DailyActivity> weeklyActivity;
    }
}
