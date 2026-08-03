package com.nexus.ticket.domain.event;

import com.nexus.ticket.domain.TicketStatus;
import java.util.UUID;

/**
 * Domain event emitted when a ticket's status changes.
 */
public record TicketStatusChangedEvent(
        UUID tenantId,
        UUID ticketId,
        TicketStatus oldStatus,
        TicketStatus newStatus
) {}
