package com.nexus.ai.triage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Structured audit logger for every AI triage decision.
 *
 * <p>This is a critical observability requirement: for every ticket the AI
 * triages, we log a structured record of exactly what happened. This serves
 * three purposes:
 * <ol>
 *   <li><b>Debugging:</b> When a ticket gets a wrong classification, we can
 *       see exactly what the model saw and decided.</li>
 *   <li><b>Compliance:</b> In regulated industries, you need an audit trail
 *       of AI decisions, including confidence scores and whether a human
 *       override occurred.</li>
 *   <li><b>Model improvement:</b> Over time, analyzing audit logs reveals
 *       patterns — which categories have low confidence, which queries
 *       consistently get escalated — that inform KB article improvements.</li>
 * </ol>
 *
 * <p>Uses a dedicated logger name ({@code nexus.audit.triage}) so audit
 * records can be routed to a separate log file, ELK index, or S3 bucket
 * without polluting the main application log.
 *
 * <p>The tenantId and traceId are already in MDC (set by the filters),
 * so they appear automatically in every audit log line.
 */
@Component
public class TriageAuditLogger {

    /**
     * Dedicated logger — separate from the class-level logger so it can
     * be routed independently in logback-spring.xml.
     */
    private static final Logger auditLog = LoggerFactory.getLogger("nexus.audit.triage");

    /**
     * Logs a structured audit record for an AI triage decision.
     *
     * @param ticketId    the ticket that was triaged
     * @param result      the triage result (category, priority, confidence, etc.)
     * @param durationMs  how long the triage took in milliseconds
     */
    public void logTriageDecision(UUID ticketId, TriageResult result, long durationMs) {
        auditLog.info(
                "AI_TRIAGE_DECISION | ticketId={} | category={} | priority={} | " +
                "confidence={:.4f} | autoResolvable={} | durationMs={} | " +
                "suggestedReplyLength={} | reasoning={}",
                ticketId,
                result.category(),
                result.priority(),
                result.confidenceScore(),
                result.autoResolvable(),
                durationMs,
                result.suggestedReply() != null ? result.suggestedReply().length() : 0,
                truncate(result.reasoning(), 200)
        );
    }

    /**
     * Logs when a triage is skipped (ticket not in NEW status).
     */
    public void logTriageSkipped(UUID ticketId, String currentStatus) {
        auditLog.info(
                "AI_TRIAGE_SKIPPED | ticketId={} | reason=ticket_not_new | currentStatus={}",
                ticketId, currentStatus
        );
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
