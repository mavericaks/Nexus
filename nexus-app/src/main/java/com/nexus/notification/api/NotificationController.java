package com.nexus.notification.api;

import com.nexus.notification.application.NotificationService;
import com.nexus.notification.application.dto.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for in-app notifications.
 * Endpoints are user-scoped (no tenantId in URL — derived from JWT).
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("userId"));
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
