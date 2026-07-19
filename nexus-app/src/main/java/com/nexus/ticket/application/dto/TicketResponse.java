package com.nexus.ticket.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO — the shape the API returns to clients.
 *
 * <p>This is the tenant-safety boundary: JPA entities (with lazy-loaded
 * relations, internal version fields, and tenant FK references) never
 * leave the service layer. Only this record is serialized to JSON.
 *
 * <p>Includes {@code version} so clients can detect optimistic locking
 * conflicts (send the version back on update — if it doesn't match,
 * someone else modified the ticket).
 */
public record TicketResponse(
        UUID id,
        UUID tenantId,
        String subject,
        String description,
        String status,
        String priority,
        String category,
        Double confidenceScore,
        String aiResponse,
        UUID assigneeId,
        Integer version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
