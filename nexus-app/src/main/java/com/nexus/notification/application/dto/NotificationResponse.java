package com.nexus.notification.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO for notifications.
 */
public record NotificationResponse(
        UUID id,
        String type,
        String title,
        String message,
        UUID referenceId,
        boolean read,
        OffsetDateTime createdAt
) {
}
