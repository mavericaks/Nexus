package com.nexus.ticket.api;

import com.nexus.ticket.application.TicketSatisfactionService;
import com.nexus.ticket.application.dto.CreateSatisfactionRequest;
import com.nexus.ticket.application.dto.SatisfactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for ticket satisfaction (CSAT) ratings.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets/{ticketId}/satisfaction")
public class TicketSatisfactionController {

    private final TicketSatisfactionService satisfactionService;

    public TicketSatisfactionController(TicketSatisfactionService satisfactionService) {
        this.satisfactionService = satisfactionService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<SatisfactionResponse> rate(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateSatisfactionRequest request) {

        SatisfactionResponse response = satisfactionService.rate(ticketId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<SatisfactionResponse> getRating(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        return satisfactionService.getRating(ticketId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
