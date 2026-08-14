package com.nexus.ticket.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO for CSAT satisfaction ratings.
 */
public record SatisfactionResponse(
        UUID id,
        UUID ticketId,
        int score,
        String feedback,
        OffsetDateTime createdAt
) {
}
