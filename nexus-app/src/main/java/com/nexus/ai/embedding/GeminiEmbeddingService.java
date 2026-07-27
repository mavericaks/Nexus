package com.nexus.ai.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Gemini Embedding API adapter — converts text to 768-dimensional vectors.
 *
 * <p>This is the adapter implementation of {@link EmbeddingService}.
 * It calls Google's Gemini {@code text-embedding-004} model via their
 * REST API. The Spring AI GenAI embedding starter is only available
 * in Spring AI 2.0+ (Boot 4.x), so we implement this ourselves
 * with a simple {@link RestClient}.
 *
 * <p>The Gemini embedding API is free (1500 RPM on AI Studio) and
 * produces high-quality 768-dimensional vectors suitable for
 * cosine similarity search via pgvector.
 */
@Service
public class GeminiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);

    private static final String GEMINI_EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent";

    private static final int DIMENSIONS = 768;

    private final RestClient restClient;
    private final String apiKey;

    public GeminiEmbeddingService(@Value("${spring.ai.google.genai.embedding.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(GEMINI_EMBED_URL)
                .build();
    }

    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Cannot embed null or blank text");
        }

        log.debug("Generating embedding for text ({} chars)", text.length());

        try {
            // Gemini embedding API request format
            var requestBody = Map.of(
                    "model", "models/text-embedding-004",
                    "content", Map.of(
                            "parts", List.of(Map.of("text", text))
                    )
            );

            @SuppressWarnings("unchecked")
            var response = restClient.post()
                    .uri("?key={key}", apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("embedding")) {
                throw new EmbeddingException("Gemini API returned unexpected response: " + response);
            }

            @SuppressWarnings("unchecked")
            var embedding = (Map<String, Object>) response.get("embedding");

            @SuppressWarnings("unchecked")
            var values = (List<Double>) embedding.get("values");

            if (values == null || values.size() != DIMENSIONS) {
                throw new EmbeddingException(
                        "Expected " + DIMENSIONS + " dimensions, got " +
                        (values == null ? "null" : values.size()));
            }

            // Convert Double to Float (pgvector uses float4)
            List<Float> result = values.stream()
                    .map(Double::floatValue)
                    .toList();

            log.debug("Embedding generated: {} dimensions", result.size());
            return result;

        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to call Gemini embedding API: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
