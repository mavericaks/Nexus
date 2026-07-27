package com.nexus.ai.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.ai.config.AiProperties;
import com.nexus.ai.embedding.EmbeddingService;
import com.nexus.ai.knowledge.KnowledgeArticleRepository;
import com.nexus.ai.rag.KnowledgeBaseSearchService;
import com.nexus.ai.rag.RetrievedArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TriageAgent} using a mock ChatModel.
 *
 * <p>These tests use Mockito to stub the LLM — NO real Groq calls.
 * This satisfies guardrails §9.4: "put a port/adapter around the
 * LLM client; test orchestration logic against a fake."
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Successful structured JSON parsing</li>
 *   <li>Graceful fallback on LLM failure</li>
 *   <li>Graceful handling of malformed LLM output</li>
 *   <li>Confidence score derivation (not raw self-report)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TriageAgent")
class TriageAgentTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private KnowledgeBaseSearchService kbSearchService;

    private TriageAgent triageAgent;

    private static final AiProperties AI_PROPS = new AiProperties(0.75, true, 5);

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        triageAgent = new TriageAgent(builder, kbSearchService, AI_PROPS, new ObjectMapper());
    }

    private void stubLlmResponse(String responseText) {
        AssistantMessage message = new AssistantMessage(responseText);
        Generation generation = new Generation(message);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
    }

    private List<RetrievedArticle> sampleArticles() {
        return List.of(
                new RetrievedArticle("Password Reset", "How to reset your password...",
                        "ACCOUNT", 0.92),
                new RetrievedArticle("2FA Setup", "Setting up two-factor auth...",
                        "ACCOUNT", 0.78)
        );
    }

    @Nested
    @DisplayName("Successful triage")
    class SuccessfulTriage {

        @Test
        @DisplayName("Valid JSON response → correct classification + high confidence")
        void validJsonResponse() {
            // Given: KB has relevant articles
            when(kbSearchService.search(any())).thenReturn(sampleArticles());

            // And: LLM returns valid structured JSON
            stubLlmResponse("""
                    {
                      "category": "ACCOUNT",
                      "priority": "MEDIUM",
                      "suggested_reply": "To reset your password, click Forgot Password on the login page.",
                      "reasoning": "Customer is asking about password reset, matches KB article directly."
                    }
                    """);

            // When
            TriageResult result = triageAgent.triage("Can't log in", "I forgot my password");

            // Then
            assertEquals("ACCOUNT", result.category().name());
            assertEquals("MEDIUM", result.priority().name());
            assertFalse(result.suggestedReply().isEmpty());
            assertFalse(result.reasoning().isEmpty());
            assertTrue(result.confidenceScore() > 0.5,
                    "Expected reasonable confidence, got " + result.confidenceScore());
        }

        @Test
        @DisplayName("JSON wrapped in markdown code fences → still parses correctly")
        void markdownWrappedJson() {
            when(kbSearchService.search(any())).thenReturn(sampleArticles());

            stubLlmResponse("""
                    ```json
                    {
                      "category": "BILLING",
                      "priority": "HIGH",
                      "suggested_reply": "I can help with your invoice question.",
                      "reasoning": "Billing-related inquiry."
                    }
                    ```
                    """);

            TriageResult result = triageAgent.triage("Invoice issue", "Wrong charge on invoice");

            assertEquals("BILLING", result.category().name());
            assertEquals("HIGH", result.priority().name());
        }
    }

    @Nested
    @DisplayName("Graceful degradation")
    class GracefulDegradation {

        @Test
        @DisplayName("LLM returns invalid JSON → fallback with low confidence")
        void invalidJson() {
            when(kbSearchService.search(any())).thenReturn(sampleArticles());
            stubLlmResponse("I'm not sure how to help with that.");

            TriageResult result = triageAgent.triage("Help", "Something is wrong");

            // Should still return a result, just with low confidence
            assertNotNull(result);
            assertTrue(result.confidenceScore() < 0.75,
                    "Expected low confidence for failed parse, got " + result.confidenceScore());
            assertFalse(result.autoResolvable(),
                    "Should NOT auto-resolve with failed parse");
        }

        @Test
        @DisplayName("LLM throws exception → fallback result")
        void llmException() {
            when(kbSearchService.search(any())).thenReturn(List.of());
            when(chatModel.call(any(Prompt.class)))
                    .thenThrow(new RuntimeException("Groq API is down"));

            TriageResult result = triageAgent.triage("Help", "Something broke");

            assertNotNull(result);
            assertEquals(0.0, result.confidenceScore());
            assertFalse(result.autoResolvable());
        }

        @Test
        @DisplayName("No KB articles found → lower confidence but still triages")
        void noKbArticles() {
            when(kbSearchService.search(any())).thenReturn(List.of());

            stubLlmResponse("""
                    {
                      "category": "TECHNICAL",
                      "priority": "LOW",
                      "suggested_reply": "Let me look into this.",
                      "reasoning": "No KB articles matched."
                    }
                    """);

            TriageResult result = triageAgent.triage("Random question", "Nothing specific");

            assertEquals("TECHNICAL", result.category().name());
            // With no KB articles, confidence should be lower
            assertTrue(result.confidenceScore() < 0.5,
                    "Expected low confidence with no KB articles, got " + result.confidenceScore());
        }
    }

    @Nested
    @DisplayName("Auto-resolve logic")
    class AutoResolve {

        @Test
        @DisplayName("High confidence + auto-resolve enabled → autoResolvable is true")
        void highConfidenceAutoResolves() {
            // High similarity articles that match the category
            var articles = List.of(
                    new RetrievedArticle("Password Reset", "content", "ACCOUNT", 0.95),
                    new RetrievedArticle("Login Help", "content", "ACCOUNT", 0.90)
            );
            when(kbSearchService.search(any())).thenReturn(articles);

            stubLlmResponse("""
                    {
                      "category": "ACCOUNT",
                      "priority": "MEDIUM",
                      "suggested_reply": "Click Forgot Password to reset.",
                      "reasoning": "Direct match to KB."
                    }
                    """);

            TriageResult result = triageAgent.triage("Password reset", "How do I reset?");

            // With high RAG similarity + parse success + category match → high confidence
            assertTrue(result.confidenceScore() >= 0.75,
                    "Expected high confidence, got " + result.confidenceScore());
            assertTrue(result.autoResolvable(),
                    "Should be auto-resolvable with high confidence");
        }
    }
}
