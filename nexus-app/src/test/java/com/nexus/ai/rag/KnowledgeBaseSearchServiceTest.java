package com.nexus.ai.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link KnowledgeBaseSearchService} utility methods.
 *
 * <p>Tests the pgvector string formatting — this is critical because
 * a malformed vector string will cause a Postgres cast error at runtime.
 */
@DisplayName("KnowledgeBaseSearchService")
class KnowledgeBaseSearchServiceTest {

    @Test
    @DisplayName("toPgVectorString formats vector as pgvector-compatible string")
    void toPgVectorString() {
        var vector = List.of(0.1f, 0.2f, 0.3f);
        String result = KnowledgeBaseSearchService.toPgVectorString(vector);
        assertEquals("[0.1,0.2,0.3]", result);
    }

    @Test
    @DisplayName("toPgVectorString handles empty vector")
    void emptyVector() {
        String result = KnowledgeBaseSearchService.toPgVectorString(List.of());
        assertEquals("[]", result);
    }

    @Test
    @DisplayName("toPgVectorString handles single element")
    void singleElement() {
        var vector = List.of(0.5f);
        String result = KnowledgeBaseSearchService.toPgVectorString(vector);
        assertEquals("[0.5]", result);
    }
}
