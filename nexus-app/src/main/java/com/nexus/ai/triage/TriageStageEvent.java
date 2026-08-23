package com.nexus.ai.triage;

/**
 * Represents a single stage update during the AI triage pipeline.
 *
 * <p>Emitted as Server-Sent Events (SSE) to stream real-time progress
 * to the frontend. Each stage corresponds to a distinct step in the
 * triage pipeline.
 *
 * <p><b>Stages (in order):</b></p>
 * <ol>
 *   <li>{@code KB_SEARCH} — Searching the knowledge base</li>
 *   <li>{@code KB_RESULTS} — KB search complete, articles found</li>
 *   <li>{@code LLM_CALL} — Calling the LLM for classification</li>
 *   <li>{@code LLM_RESPONSE} — LLM response received</li>
 *   <li>{@code CONFIDENCE} — Computing confidence score</li>
 *   <li>{@code COMPLETE} — Triage pipeline finished</li>
 *   <li>{@code ERROR} — Pipeline failed</li>
 * </ol>
 *
 * @param stage   the pipeline stage identifier
 * @param message human-readable status message
 * @param data    optional stage-specific payload (JSON-serializable)
 */
public record TriageStageEvent(
        String stage,
        String message,
        Object data
) {
    /** Factory for progress stages (no data payload) */
    public static TriageStageEvent progress(String stage, String message) {
        return new TriageStageEvent(stage, message, null);
    }

    /** Factory for completion with result data */
    public static TriageStageEvent complete(Object result) {
        return new TriageStageEvent("COMPLETE", "Triage complete", result);
    }

    /** Factory for error events */
    public static TriageStageEvent error(String message) {
        return new TriageStageEvent("ERROR", message, null);
    }
}
