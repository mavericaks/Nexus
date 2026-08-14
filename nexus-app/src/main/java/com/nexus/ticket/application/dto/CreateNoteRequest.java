package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for creating an internal note on a ticket.
 */
public record CreateNoteRequest(

        @NotBlank(message = "Note content is required")
        @Size(max = 10000, message = "Note must be at most 10,000 characters")
        String content
) {
}
