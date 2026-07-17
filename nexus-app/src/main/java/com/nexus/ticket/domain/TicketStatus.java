package com.nexus.ticket.domain;

/**
 * Every possible state in a ticket's lifecycle.
 *
 * <p>The allowed transitions are enforced by the state machine (separate class),
 * not by this enum — this enum only names the states.
 *
 * <pre>
 * NEW → CLASSIFIED → AI_DRAFTED → AUTO_RESOLVED (terminal for AI path)
 *                                → ESCALATED → IN_PROGRESS → RESOLVED → CLOSED
 * </pre>
 */
public enum TicketStatus {

    /** Just created, not yet seen by the AI triage pipeline. */
    NEW,

    /** AI has assigned a category and priority. */
    CLASSIFIED,

    /** AI has drafted a response, pending confidence check. */
    AI_DRAFTED,

    /** AI auto-resolved — confidence was above the tenant's threshold. */
    AUTO_RESOLVED,

    /** AI confidence was below threshold; handed to a human agent. */
    ESCALATED,

    /** A human agent has picked up the escalated ticket. */
    IN_PROGRESS,

    /** The issue is resolved (by human or confirmed auto-resolve). */
    RESOLVED,

    /** Closed — no further action. Terminal state. */
    CLOSED
}
