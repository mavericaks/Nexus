package com.nexus.ai.api;

import com.nexus.ai.triage.TriageResult;
import com.nexus.ai.triage.TriageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for AI triage operations.
 *
 * <p>Exposes endpoints to trigger AI triage on tickets.
 * In production, triage would also be triggered automatically
 * via async event processing (Phase 5). This endpoint allows
 * manual/on-demand triage for testing and re-triage scenarios.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
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
