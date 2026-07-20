package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for updating an existing ticket.
 *
 * <p>All fields are optional — only the fields present in the request
 * body will be applied to the entity. This supports partial updates
 * without requiring the client to send the entire ticket.
 */
public record UpdateTicketRequest(

        @Size(max = 500, message = "Subject must be at most 500 characters")
        String subject,

        @Size(max = 10000, message = "Description must be at most 10,000 characters")
        String description,

        String category,

        String priority
) {
}
