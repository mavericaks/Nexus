package com.nexus.ai.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GeminiEmbeddingService} input validation.
 *
 * <p>These tests verify the service rejects invalid input before
 * making any API call. The actual API call is tested manually
 * (smoke test, non-gating) — per guardrails §9.4.
 */
@DisplayName("GeminiEmbeddingService")
class GeminiEmbeddingServiceTest {

    // Use a dummy API key — we're testing input validation, not the API
    private final GeminiEmbeddingService service = new GeminiEmbeddingService("test-key");

    @Test
    @DisplayName("embed rejects null text")
    void nullText() {
        assertThrows(EmbeddingException.class, () -> service.embed(null));
    }

    @Test
    @DisplayName("embed rejects blank text")
    void blankText() {
        assertThrows(EmbeddingException.class, () -> service.embed("   "));
    }

    @Test
    @DisplayName("embed rejects empty text")
    void emptyText() {
        assertThrows(EmbeddingException.class, () -> service.embed(""));
    }

    @Test
    @DisplayName("dimensions returns 768")
    void dimensionsIs768() {
        assertEquals(768, service.dimensions());
    }
}
