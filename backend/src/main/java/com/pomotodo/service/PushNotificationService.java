package com.pomotodo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Push Notification Service - DISABLED
 * WebPush functionality has been disabled for security and simplicity
 */
@Slf4j
@Service
public class PushNotificationService {
    
    public boolean isVapidConfigured() {
        return false;
    }
    
    public String getVapidPublicKey() {
        return "";
    }
    
    /**
     * Send push notification - DISABLED
     */
    public void sendNotification(Object subscription, String title, String body, Map<String, Object> data) {
        log.debug("Push notifications are disabled. Message would be: {} - {}", title, body);
    }
    
    /**
     * Send notification to user - DISABLED
     */
    public void sendNotificationToUser(Long userId, String title, String body, Map<String, Object> data) {
        log.debug("Push notifications are disabled for user: {}", userId);
    }
    
    /**
     * Send task deadline reminder - DISABLED
     */
    public void sendTaskDeadlineReminder(Long userId, String taskName, String deadline) {
        log.debug("Push notification disabled for task deadline: {}", taskName);
    }
    
    /**
     * Send Pomodoro completion notification - DISABLED
     */
    public void sendPomodoroCompleteNotification(Long userId, int sessionCount, int totalMinutes) {
        log.debug("Push notification disabled for pomodoro completion");
    }
    
    /**
     * Send break time notification - DISABLED
     */
    public void sendBreakTimeNotification(Long userId, String breakType) {
        log.debug("Push notification disabled for break time");
    }
}
