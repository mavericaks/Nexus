package com.nexus.notifications.consumer;

import java.util.UUID;

/**
 * Mirrors the {@code TicketStatusChangedEvent} published by nexus-app.
 *
 * <p>This is an independent copy — the notification service has NO
 * compile-time dependency on nexus-app. The contract is the JSON schema
 * on the Kafka topic. If nexus-app adds a field, this record will
 * silently ignore it (Jackson default behaviour).
 */
public record TicketStatusChangedMessage(
        UUID tenantId,
        UUID ticketId,
        String oldStatus,
        String newStatus
) {}