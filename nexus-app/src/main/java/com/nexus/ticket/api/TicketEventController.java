package com.nexus.ticket.api;

import com.nexus.ticket.application.TicketEventService;
import com.nexus.ticket.application.dto.TicketEventResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for ticket activity timeline / audit trail.
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets/{ticketId}/events")
public class TicketEventController {

    private final TicketEventService ticketEventService;

    public TicketEventController(TicketEventService ticketEventService) {
        this.ticketEventService = ticketEventService;
    }

    /**
     * Get the full event timeline for a ticket.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN', 'OWNER')")
    public ResponseEntity<List<TicketEventResponse>> getEvents(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        List<TicketEventResponse> events = ticketEventService.getEventsForTicket(ticketId);
        return ResponseEntity.ok(events);
    }
}
