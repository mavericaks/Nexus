package com.nexus.ticket.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO for ticket notes.
 */
public record NoteResponse(
        UUID id,
        UUID ticketId,
        UUID authorId,
        String authorName,
        String content,
        OffsetDateTime createdAt
) {
}
