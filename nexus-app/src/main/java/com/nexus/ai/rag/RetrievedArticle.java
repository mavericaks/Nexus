package com.nexus.ai.rag;

import java.util.List;

/**
 * A single retrieved knowledge base article with its similarity score.
 *
 * <p>The similarity score (0.0–1.0) comes from pgvector's cosine
 * similarity calculation. Higher = more relevant. This score feeds
 * directly into the confidence derivation — it's a measurable signal,
 * not a raw LLM self-report (guardrails §9.3).
 */
public record RetrievedArticle(
        String title,
        String content,
        String category,
        double similarityScore
) {
    /**
     * Formats this article for inclusion in the LLM prompt context.
     */
    public String toPromptContext() {
        return """
                --- Article: %s (relevance: %.0f%%) ---
                %s
                """.formatted(title, similarityScore * 100, content);
    }
}
