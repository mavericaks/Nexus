package com.nexus.common.exception;

import com.nexus.ticket.domain.TicketStatus;

/**
 * Thrown when a ticket state transition violates the state machine rules.
 * Maps to HTTP 409 (Conflict) — the request is well-formed but conflicts
 * with the ticket's current state.
 *
 * <p>Example: trying to transition a CLOSED ticket to IN_PROGRESS.
 */
public class IllegalTicketTransitionException extends RuntimeException {

    private final TicketStatus from;
    private final TicketStatus to;

    public IllegalTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot transition ticket from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public TicketStatus getFrom() {
        return from;
    }

    public TicketStatus getTo() {
        return to;
    }
}
