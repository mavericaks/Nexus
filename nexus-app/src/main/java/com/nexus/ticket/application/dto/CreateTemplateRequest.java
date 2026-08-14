package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for creating/updating a response template.
 */
public record CreateTemplateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must be at most 10,000 characters")
        String content,

        String category
) {
}
