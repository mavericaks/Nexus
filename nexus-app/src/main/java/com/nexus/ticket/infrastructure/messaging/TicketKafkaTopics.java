package com.nexus.ticket.infrastructure.messaging;

/**
 * Kafka topic name constants for ticket lifecycle events.
 *
 * <p>Centralised here so producers and consumers reference the same
 * string literal — a typo in a topic name is a silent black hole.
 */
public final class TicketKafkaTopics {

    /** Published when a new ticket is created. */
    public static final String TICKET_CREATED = "nexus.tickets.created";

    /** Published when a ticket's status changes (any transition). */
    public static final String TICKET_STATUS_CHANGED = "nexus.tickets.status-changed";

    private TicketKafkaTopics() {
        // Utility class — no instances
    }
}
