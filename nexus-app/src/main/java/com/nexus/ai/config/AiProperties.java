package com.nexus.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed, validated configuration for the AI triage pipeline.
 *
 * <p>Maps to {@code nexus.ai.*} in application YAML. Using
 * {@code @ConfigurationProperties} instead of scattered {@code @Value}
 * annotations means:
 * <ul>
 *   <li>IDE autocomplete for all AI config</li>
 *   <li>Startup failure if a required property is missing</li>
 *   <li>One class to find all AI-related config knobs</li>
 * </ul>
 *
 * <p>The {@code autoResolveEnabled} flag is the production kill switch
 * from architecture rationale §9 — disabling it falls back to
 * "AI drafts, human always approves" mode without a redeploy.
 */
@ConfigurationProperties(prefix = "nexus.ai")
public record AiProperties(

        /**
         * Confidence score threshold (0.0–1.0) for auto-resolution.
         * Below this → escalate to human agent.
         * Above this → auto-resolve with AI's suggested reply.
         */
        double confidenceThreshold,

        /**
         * Kill switch — set to false to disable AI auto-resolution.
         * When disabled, the AI still drafts a reply, but the ticket
         * is always escalated for human review.
         */
        boolean autoResolveEnabled,

        /**
         * Maximum number of knowledge base articles to retrieve
         * during RAG similarity search.
         */
        int maxRetrievalResults
) {
}
