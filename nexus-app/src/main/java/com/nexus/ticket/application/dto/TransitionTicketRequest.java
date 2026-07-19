package com.nexus.ticket.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO for transitioning a ticket's status.
 *
 * <p>Separated from {@link UpdateTicketRequest} because a state transition
 * is a distinct operation — it goes through the state machine validation,
 * not just a field update.
 */
public record TransitionTicketRequest(

        @NotBlank(message = "Target status is required")
        String targetStatus
) {
}
