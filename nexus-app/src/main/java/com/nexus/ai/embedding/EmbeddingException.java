package com.nexus.ai.embedding;

/**
 * Thrown when the embedding API call fails (network error, rate limit, bad response).
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
