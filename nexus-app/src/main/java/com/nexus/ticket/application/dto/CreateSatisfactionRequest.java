package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for submitting a CSAT satisfaction rating.
 */
public record CreateSatisfactionRequest(

        @Min(value = 1, message = "Score must be between 1 and 5")
        @Max(value = 5, message = "Score must be between 1 and 5")
        int score,

        @Size(max = 2000, message = "Feedback must be at most 2,000 characters")
        String feedback
) {
}
