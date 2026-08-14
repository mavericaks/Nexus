package com.nexus.ticket.application.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Outbound DTO for ticket events (activity timeline).
 */
public record TicketEventResponse(
        UUID id,
        UUID ticketId,
        String eventType,
        UUID actorId,
        String actorName,
        Map<String, Object> details,
        OffsetDateTime createdAt
) {
}
