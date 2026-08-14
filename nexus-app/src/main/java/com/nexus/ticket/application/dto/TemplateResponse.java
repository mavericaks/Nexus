package com.nexus.ticket.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO for response templates.
 */
public record TemplateResponse(
        UUID id,
        String title,
        String content,
        String category,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
