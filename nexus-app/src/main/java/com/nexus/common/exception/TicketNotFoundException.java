package com.nexus.common.exception;

import java.util.UUID;

/**
 * Thrown when a ticket ID doesn't match any ticket visible to the current tenant.
 * Maps to HTTP 404.
 *
 * <p>Note: RLS might also cause this — a ticket exists but belongs to
 * a different tenant, so it's invisible. From the caller's perspective,
 * it simply doesn't exist (information hiding by design).
 */
public class TicketNotFoundException extends RuntimeException {

    private final UUID ticketId;

    public TicketNotFoundException(UUID ticketId) {
        super("Ticket not found: " + ticketId);
        this.ticketId = ticketId;
    }

    public UUID getTicketId() {
        return ticketId;
    }
}
