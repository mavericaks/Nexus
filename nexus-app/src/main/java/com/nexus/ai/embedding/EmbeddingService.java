package com.nexus.ai.embedding;

import java.util.List;

/**
 * Port for text → vector embedding conversion.
 *
 * <p>This interface is the port in our port/adapter pattern (guardrails §9.4).
 * Production uses Gemini's API; tests use a fake implementation that
 * returns deterministic vectors. This ensures CI never calls a real
 * external API, keeping the build trustworthy.
 */
public interface EmbeddingService {

    /**
     * Converts a text string into a dense vector embedding.
     *
     * @param text the text to embed (e.g., ticket subject + description)
     * @return a list of floats representing the embedding vector
     * @throws EmbeddingException if the embedding API call fails
     */
    List<Float> embed(String text);

    /**
     * Returns the dimensionality of embeddings produced by this service.
     * Must match the pgvector column dimension (768 for Gemini text-embedding-004).
     */
    int dimensions();
}
