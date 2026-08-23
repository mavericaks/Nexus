package com.nexus.ai.triage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.ai.config.AiProperties;
import com.nexus.ai.rag.KnowledgeBaseSearchService;
import com.nexus.ai.rag.RetrievedArticle;
import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The core AI triage agent — classifies tickets, drafts replies,
 * and decides whether to auto-resolve or escalate.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Receive ticket (subject + description)</li>
 *   <li>Search knowledge base via RAG (embed query → pgvector similarity)</li>
 *   <li>Build prompt with ticket + KB context</li>
 *   <li>Call LLM (Groq / Llama 3.3 70B) via Spring AI ChatClient</li>
 *   <li>Parse structured output (category, priority, reply, reasoning)</li>
 *   <li>Derive confidence score from measurable signals (NOT LLM self-report)</li>
 *   <li>Return {@link TriageResult}</li>
 * </ol>
 *
 * <p>The LLM call uses Spring AI's {@link ChatClient}, which handles
 * the OpenAI-compatible protocol that Groq implements.
 */
@Service
public class TriageAgent {

    private static final Logger log = LoggerFactory.getLogger(TriageAgent.class);

    private final ChatClient chatClient;
    private final KnowledgeBaseSearchService kbSearchService;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public TriageAgent(ChatClient.Builder chatClientBuilder,
                       KnowledgeBaseSearchService kbSearchService,
                       AiProperties aiProperties,
                       ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.kbSearchService = kbSearchService;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Triages a ticket: classifies it, searches the KB, drafts a reply,
     * and derives a confidence score.
     *
     * @param subject     the ticket subject
     * @param description the ticket description
     * @return the triage result with classification, reply, and confidence
     */
    public TriageResult triage(String subject, String description) {
        log.info("Triaging ticket: '{}'", subject);

        // Step 1: RAG retrieval — find relevant KB articles
        String query = subject + " " + description;
        List<RetrievedArticle> articles = kbSearchService.search(query);

        // Step 2: Build the prompt with KB context
        String kbContext = articles.isEmpty()
                ? "No relevant knowledge base articles found."
                : articles.stream()
                    .map(RetrievedArticle::toPromptContext)
                    .collect(Collectors.joining("\n"));

        // Step 3: Call the LLM (protected by circuit breaker + retry)
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(subject, description, kbContext);

        String llmResponse;
        try {
            llmResponse = callLlm(systemPrompt, userPrompt);
        } catch (Exception e) {
            // Circuit breaker opened or all retries exhausted — graceful fallback
            log.error("LLM call failed (circuit breaker/retry exhausted): {}", e.getMessage());
            return fallbackResult(subject, articles);
        }

        // Step 4: Parse structured output
        return parseResponse(llmResponse, articles);
    }

    /**
     * Streaming variant of {@link #triage} — emits stage events as the pipeline
     * progresses. Used by the SSE endpoint to stream real-time updates to the UI.
     *
     * @param subject     the ticket subject
     * @param description the ticket description
     * @param onStage     callback invoked for each pipeline stage event
     * @return the triage result
     */
    public TriageResult triageWithStages(String subject, String description,
                                          java.util.function.Consumer<TriageStageEvent> onStage) {
        log.info("Triaging ticket (streaming): '{}'", subject);

        // Stage 1: KB Search
        onStage.accept(TriageStageEvent.progress("KB_SEARCH", "Searching knowledge base..."));
        String query = subject + " " + description;
        List<RetrievedArticle> articles = kbSearchService.search(query);
        onStage.accept(new TriageStageEvent("KB_RESULTS",
                "Found " + articles.size() + " relevant article" + (articles.size() != 1 ? "s" : ""),
                articles.stream().map(a -> java.util.Map.of(
                        "title", a.title(),
                        "similarity", String.format("%.0f%%", a.similarityScore() * 100)
                )).toList()));

        // Stage 2: Build prompt
        String kbContext = articles.isEmpty()
                ? "No relevant knowledge base articles found."
                : articles.stream()
                    .map(RetrievedArticle::toPromptContext)
                    .collect(Collectors.joining("\n"));
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(subject, description, kbContext);

        // Stage 3: LLM Call
        onStage.accept(TriageStageEvent.progress("LLM_CALL", "Analyzing with AI (Llama 3.3 70B)..."));
        String llmResponse;
        try {
            llmResponse = callLlm(systemPrompt, userPrompt);
            onStage.accept(TriageStageEvent.progress("LLM_RESPONSE", "AI response received"));
        } catch (Exception e) {
            log.error("LLM call failed during streaming triage: {}", e.getMessage());
            onStage.accept(TriageStageEvent.error("AI service unavailable — escalating to human agent"));
            return fallbackResult(subject, articles);
        }

        // Stage 4: Confidence scoring
        onStage.accept(TriageStageEvent.progress("CONFIDENCE", "Computing confidence score..."));
        TriageResult result = parseResponse(llmResponse, articles);

        // Stage 5: Complete
        onStage.accept(TriageStageEvent.complete(java.util.Map.of(
                "category", result.category().name(),
                "priority", result.priority().name(),
                "confidenceScore", result.confidenceScore(),
                "autoResolvable", result.autoResolvable(),
                "suggestedReply", result.suggestedReply(),
                "reasoning", result.reasoning()
        )));

        return result;
    }

    /**
     * The actual LLM network call, isolated in its own method so Resilience4j
     * can proxy it with circuit breaker and retry annotations.
     *
     * <p><b>Circuit breaker:</b> If Groq is down, this method will fail fast
     * after the failure threshold is crossed, without making network calls.
     * <p><b>Retry:</b> Transient failures (network hiccups, 503s) get retried
     * up to 3 times with exponential backoff before giving up.
     *
     * <p>This method is package-private (not private) because Spring AOP
     * proxies can only intercept non-private methods. Resilience4j annotations
     * on private methods are silently ignored.
     *
     * @param systemPrompt the system prompt
     * @param userPrompt   the user prompt with ticket + KB context
     * @return the raw LLM response string
     */
    @CircuitBreaker(name = "groq-llm", fallbackMethod = "llmFallback")
    @Retry(name = "groq-llm")
    String callLlm(String systemPrompt, String userPrompt) {
        log.debug("Calling LLM via Groq API...");
        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        log.debug("LLM response received ({} chars)", response != null ? response.length() : 0);
        return response;
    }

    /**
     * Resilience4j fallback — invoked when the circuit breaker is OPEN
     * or when all retry attempts are exhausted.
     *
     * <p>Returns null, which the caller (triage()) catches and converts
     * into a proper fallback TriageResult for manual escalation.
     */
    @SuppressWarnings("unused") // Called reflectively by Resilience4j
    String llmFallback(String systemPrompt, String userPrompt, Throwable t) {
        log.warn("LLM fallback triggered (circuit breaker or retry exhausted): {}", t.getMessage());
        throw new RuntimeException("LLM unavailable: " + t.getMessage(), t);
    }

    /**
     * Parses the LLM's JSON response into a TriageResult.
     * If parsing fails, returns a low-confidence fallback.
     */
    private TriageResult parseResponse(String llmResponse, List<RetrievedArticle> articles) {
        boolean parseSucceeded = false;
        TicketCategory category = TicketCategory.GENERAL;
        TicketPriority priority = TicketPriority.MEDIUM;
        String suggestedReply = "";
        String reasoning = "";

        try {
            // Strip markdown code fences if present
            String json = llmResponse.strip();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").strip();
            }

            JsonNode node = objectMapper.readTree(json);

            category = parseCategory(node.path("category").asText("GENERAL"));
            priority = parsePriority(node.path("priority").asText("MEDIUM"));
            suggestedReply = node.path("suggested_reply").asText("");
            reasoning = node.path("reasoning").asText("");
            parseSucceeded = !suggestedReply.isEmpty() && !reasoning.isEmpty();

        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON: {}", e.getMessage());
            reasoning = "Parse failed — raw LLM response: " + llmResponse;
        }

        // Step 5: Derive confidence score
        double confidence = ConfidenceScoreCalculator.calculate(
                articles, parseSucceeded, category.name());

        // Step 6: Auto-resolve decision
        boolean autoResolvable = aiProperties.autoResolveEnabled() &&
                                 confidence >= aiProperties.confidenceThreshold();

        log.info("Triage complete: category={}, priority={}, confidence={:.2f}, autoResolvable={}",
                category, priority, confidence, autoResolvable);

        return new TriageResult(category, priority, suggestedReply,
                reasoning, confidence, autoResolvable);
    }

    /**
     * Fallback result when the LLM call fails entirely.
     * Returns a low-confidence result that will always escalate.
     */
    private TriageResult fallbackResult(String subject, List<RetrievedArticle> articles) {
        return new TriageResult(
                TicketCategory.GENERAL,
                TicketPriority.MEDIUM,
                "",
                "AI triage failed — LLM call unsuccessful. Escalating for manual review.",
                0.0,
                false
        );
    }

    private String buildSystemPrompt() {
        return """
                You are an expert customer support triage agent for a SaaS product.
                Your job is to classify incoming support tickets and draft helpful replies.
                
                You MUST respond with valid JSON in exactly this format:
                {
                  "category": "ACCOUNT|BILLING|TECHNICAL|GENERAL",
                  "priority": "LOW|MEDIUM|HIGH|CRITICAL",
                  "suggested_reply": "A helpful, professional reply to the customer",
                  "reasoning": "Your internal reasoning about why you chose this classification"
                }
                
                Classification guidelines:
                - ACCOUNT: password resets, 2FA, login issues, account settings
                - BILLING: invoices, charges, refunds, payment methods, subscriptions
                - TECHNICAL: API issues, integrations, bugs, errors, performance
                - GENERAL: feature requests, feedback, general questions, other
                
                Priority guidelines:
                - CRITICAL: system down, data loss, security breach
                - HIGH: major functionality broken, billing errors, urgent deadline
                - MEDIUM: general issues, feature not working as expected
                - LOW: questions, feature requests, minor inconveniences
                
                Reply guidelines:
                - Be professional, empathetic, and helpful
                - Reference specific knowledge base content when available
                - Provide actionable steps the customer can take
                - Keep replies concise but thorough
                
                IMPORTANT: Respond ONLY with the JSON object. No additional text.
                """;
    }

    private String buildUserPrompt(String subject, String description, String kbContext) {
        return """
                === SUPPORT TICKET ===
                Subject: %s
                Description: %s
                
                === RELEVANT KNOWLEDGE BASE ARTICLES ===
                %s
                
                Please classify this ticket and draft a reply based on the knowledge base articles above.
                """.formatted(subject, description, kbContext);
    }

    private TicketCategory parseCategory(String value) {
        try {
            return TicketCategory.valueOf(value.toUpperCase().strip());
        } catch (IllegalArgumentException e) {
            return TicketCategory.GENERAL;
        }
    }

    private TicketPriority parsePriority(String value) {
        try {
            return TicketPriority.valueOf(value.toUpperCase().strip());
        } catch (IllegalArgumentException e) {
            return TicketPriority.MEDIUM;
        }
    }
}
