package com.pomotodo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification Scheduler - DISABLED
 * Scheduled notifications have been disabled
 */
@Slf4j
@Service
public class NotificationScheduler {
    
    /**
     * Check for tasks with approaching deadlines - DISABLED
     */
    public void checkTaskDeadlines() {
        // Disabled - no push notifications
        log.debug("Task deadline checking is disabled");
    }
    
    /**
     * Send daily summary notification - DISABLED
     */
    public void sendDailySummary() {
        // Disabled - no push notifications
        log.debug("Daily summary is disabled");
    }
}
