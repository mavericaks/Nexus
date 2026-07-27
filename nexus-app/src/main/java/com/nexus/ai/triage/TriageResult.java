package com.nexus.ai.triage;

import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;

/**
 * Structured output from the AI triage agent.
 *
 * <p>Captures everything the triage agent decides about a ticket:
 * classification (category/priority), a suggested customer-facing
 * reply, the AI's reasoning, and a derived confidence score.
 *
 * <p>The confidence score is NOT raw LLM self-report — it's derived
 * from measurable signals (RAG similarity, parse success, etc.)
 * per guardrails §9.3.
 */
public record TriageResult(
        /** AI-classified ticket category */
        TicketCategory category,

        /** AI-classified ticket priority */
        TicketPriority priority,

        /** Customer-facing suggested reply */
        String suggestedReply,

        /** Internal reasoning (why the AI made this decision) */
        String reasoning,

        /**
         * Derived confidence score (0.0–1.0).
         * NOT raw LLM self-report — calculated from:
         * - RAG retrieval similarity score
         * - Structured output parse success
         * - Category/priority agreement with KB article categories
         */
        double confidenceScore,

        /** Whether the AI recommends auto-resolution */
        boolean autoResolvable
) {
}
