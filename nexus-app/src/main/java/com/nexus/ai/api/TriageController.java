package com.nexus.ai.api;

import com.nexus.ai.triage.TriageResult;
import com.nexus.ai.triage.TriageService;
import com.nexus.ai.triage.TriageStageEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * REST controller for AI triage operations.
 *
 * <p>Exposes endpoints to trigger AI triage on tickets, including
 * a streaming SSE endpoint that emits pipeline stage updates in
 * real-time for the frontend.
 */
import com.nexus.ai.rag.KnowledgeBaseSearchService;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
public class TriageController {

    private static final Logger log = LoggerFactory.getLogger(TriageController.class);

    private final TriageService triageService;
    private final KnowledgeBaseSearchService knowledgeBaseSearchService;
    private final ObjectMapper objectMapper;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    public TriageController(TriageService triageService,
                            KnowledgeBaseSearchService knowledgeBaseSearchService,
                            ObjectMapper objectMapper) {
        this.triageService = triageService;
        this.knowledgeBaseSearchService = knowledgeBaseSearchService;
        this.objectMapper = objectMapper;
    }

    /**
     * Triggers AI triage on a ticket.
     *
     * <p>POST /api/v1/tenants/{tenantId}/tickets/{ticketId}/triage
     *
     * @param tenantId the tenant ID (validated by TenantContextFilter)
     * @param ticketId the ticket to triage
     * @return the triage result with classification, suggested reply, confidence
     */
    @PostMapping("/{ticketId}/triage")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'AGENT')")
    public ResponseEntity<TriageResultResponse> triageTicket(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        TriageResult result = triageService.triageTicket(ticketId);

        return ResponseEntity.ok(new TriageResultResponse(
                result.category().name(),
                result.priority().name(),
                result.suggestedReply(),
                result.reasoning(),
                result.confidenceScore(),
                result.autoResolvable()
        ));
    }

    /**
     * Streams AI triage pipeline stages via Server-Sent Events (SSE).
     *
     * <p>GET /api/v1/tenants/{tenantId}/tickets/{ticketId}/triage/stream
     *
     * <p>The client receives real-time updates as the pipeline progresses
     * through KB search, LLM call, confidence scoring, etc. Each event
     * is a JSON-serialized {@link TriageStageEvent}.
     *
     * <p>Uses Spring MVC's {@link SseEmitter} — works on the Servlet stack
     * (Tomcat) without needing WebFlux/Netty.
     */
    @GetMapping(value = "/{ticketId}/triage/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'AGENT')")
    public SseEmitter triageTicketStream(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        // 2 minute timeout — triage with retries can take a while
        SseEmitter emitter = new SseEmitter(120_000L);

        sseExecutor.execute(() -> {
            try {
                triageService.triageTicketStreaming(ticketId, stageEvent -> {
                    try {
                        String json = objectMapper.writeValueAsString(stageEvent);
                        emitter.send(SseEmitter.event()
                                .name(stageEvent.stage())
                                .data(json, MediaType.APPLICATION_JSON));
                    } catch (Exception e) {
                        log.warn("Failed to send SSE event: {}", e.getMessage());
                    }
                });
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE triage stream failed: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/backfill-kb")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public ResponseEntity<Integer> backfillKb(@PathVariable UUID tenantId) {
        int count = knowledgeBaseSearchService.backfillEmbeddings();
        return ResponseEntity.ok(count);
    }

    /**
     * Response DTO for triage results.
     */
    public record TriageResultResponse(
            String category,
            String priority,
            String suggestedReply,
            String reasoning,
            double confidenceScore,
            boolean autoResolvable
    ) {
    }
}

