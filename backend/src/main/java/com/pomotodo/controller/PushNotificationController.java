package com.pomotodo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Push Notification Controller - DISABLED
 * WebPush functionality has been disabled for security and simplicity
 */
@RestController
@RequestMapping("/api/notifications")
public class PushNotificationController {
    
    /**
     * All notification endpoints return disabled status
     */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<?> getVapidPublicKey() {
        return ResponseEntity.ok(Map.of(
            "enabled", "false",
            "message", "Push notifications are disabled"
        ));
    }
    
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe() {
        return ResponseEntity.ok(Map.of(
            "message", "Push notifications are disabled"
        ));
    }
    
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe() {
        return ResponseEntity.ok(Map.of(
            "message", "Push notifications are disabled"
        ));
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> sendTestNotification() {
        return ResponseEntity.ok(Map.of(
            "message", "Push notifications are disabled"
        ));
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getSubscriptionStatus() {
        return ResponseEntity.ok(Map.of(
            "subscribed", false,
            "subscriptionCount", 0,
            "vapidEnabled", false,
            "message", "Push notifications are disabled"
        ));
    }
}
