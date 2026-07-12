package com.nexus.ticket.domain;

/**
 * Urgency level assigned to a ticket — either by the AI triage
 * or manually by a human agent.
 *
 * <p>Used by the Strategy pattern to select escalation thresholds:
 * a CRITICAL ticket may have a lower confidence threshold for
 * auto-resolve than a LOW-priority one.
 */
public enum TicketPriority {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
