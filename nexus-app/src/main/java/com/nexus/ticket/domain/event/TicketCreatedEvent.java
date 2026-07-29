package com.nexus.ticket.domain.event;

import java.util.UUID;

/**
 * Domain event emitted when a new ticket is created.
 */
public record TicketCreatedEvent(
        UUID tenantId,
        UUID ticketId,
        String subject
) {}
