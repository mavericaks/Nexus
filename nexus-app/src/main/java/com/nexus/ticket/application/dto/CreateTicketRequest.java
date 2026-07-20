package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for creating a new ticket.
 *
 * <p>Validated with Bean Validation before reaching the service layer.
 * A ticket must have a subject; description and category are optional
 * at creation time (the AI classifier will assign them later).
 */
public record CreateTicketRequest(

        @NotBlank(message = "Subject is required")
        @Size(max = 500, message = "Subject must be at most 500 characters")
        String subject,

        @Size(max = 10000, message = "Description must be at most 10,000 characters")
        String description,

        String category,

        String priority
) {
}
