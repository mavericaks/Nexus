package com.nexus.ticket.domain;

/**
 * The category the AI triage assigns to a ticket based on its content.
 *
 * <p>These map to the knowledge-base partitions a tenant can upload —
 * the RAG pipeline searches the matching partition first,
 * improving retrieval relevance.
 */
public enum TicketCategory {

    /** Billing, invoices, payment issues. */
    BILLING,

    /** Technical problems, bugs, integrations. */
    TECHNICAL,

    /** Account access, permissions, profile changes. */
    ACCOUNT,

    /** Feature requests, product feedback. */
    FEATURE_REQUEST,

    /** Anything that doesn't fit the above categories. */
    GENERAL
}
