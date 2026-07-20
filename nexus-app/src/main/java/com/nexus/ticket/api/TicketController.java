package com.nexus.ticket.api;

import com.nexus.ticket.application.TicketService;
import com.nexus.ticket.application.dto.CreateTicketRequest;
import com.nexus.ticket.application.dto.TicketResponse;
import com.nexus.ticket.application.dto.TransitionTicketRequest;
import com.nexus.ticket.application.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for ticket CRUD and lifecycle transitions.
 *
 * <p>Resource-oriented endpoints under {@code /api/v1/tenants/{tenantId}/tickets}.
 * The tenantId in the URL is extracted by {@link com.nexus.common.multitenancy.TenantContextFilter}
 * and set on the database connection via RLS — so the controller doesn't
 * need to pass it to every service method (except create, which needs it
 * to look up the tenant entity).
 *
 * <p>This controller is intentionally thin — no business logic, no
 * repository calls. Just HTTP mapping + validation delegation.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /api/v1/tenants/{tenantId}/tickets}              — create</li>
 *   <li>{@code GET    /api/v1/tenants/{tenantId}/tickets}              — list (paginated, filterable)</li>
 *   <li>{@code GET    /api/v1/tenants/{tenantId}/tickets/{ticketId}}   — get one</li>
 *   <li>{@code PUT    /api/v1/tenants/{tenantId}/tickets/{ticketId}}   — update</li>
 *   <li>{@code PATCH  /api/v1/tenants/{tenantId}/tickets/{ticketId}/transition} — state transition</li>
 *   <li>{@code DELETE /api/v1/tenants/{tenantId}/tickets/{ticketId}}   — delete</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Create a new ticket for the given tenant.
     *
     * @return 201 Created with the ticket response body
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateTicketRequest request) {

        TicketResponse response = ticketService.createTicket(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List tickets with optional filters and pagination.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code status} — filter by status (e.g., NEW, ESCALATED)</li>
     *   <li>{@code priority} — filter by priority (e.g., HIGH, CRITICAL)</li>
     *   <li>{@code category} — filter by category (e.g., BILLING, TECHNICAL)</li>
     *   <li>{@code page} — page number (0-based, default 0)</li>
     *   <li>{@code size} — page size (default 20, max managed by Spring)</li>
     *   <li>{@code sort} — sort field + direction (e.g., createdAt,desc)</li>
     * </ul>
     *
     * @return 200 OK with a paginated list of tickets
     */
    @GetMapping
    public ResponseEntity<Page<TicketResponse>> listTickets(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<TicketResponse> page = ticketService.listTickets(status, priority, category, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Get a single ticket by ID.
     *
     * @return 200 OK with the ticket, or 404 if not found / hidden by RLS
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        TicketResponse response = ticketService.getTicket(ticketId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a ticket's mutable fields (partial update).
     *
     * @return 200 OK with the updated ticket
     */
    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> updateTicket(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {

        TicketResponse response = ticketService.updateTicket(ticketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Transition a ticket's status through the state machine.
     *
     * <p>Uses PATCH (not PUT) because this modifies one aspect of the resource
     * (its status), not the entire resource. The transition goes through
     * the state machine — illegal transitions return 409 Conflict.
     *
     * @return 200 OK with the transitioned ticket, or 409 if the transition is illegal
     */
    @PatchMapping("/{ticketId}/transition")
    public ResponseEntity<TicketResponse> transitionTicket(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody TransitionTicketRequest request) {

        TicketResponse response = ticketService.transitionTicket(ticketId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a ticket.
     *
     * @return 204 No Content on success, 404 if not found
     */
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable UUID tenantId,
            @PathVariable UUID ticketId) {

        ticketService.deleteTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}
