package com.nexus.ticket.application;

import com.nexus.common.exception.IllegalTicketTransitionException;
import com.nexus.common.exception.TenantNotFoundException;
import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import com.nexus.tenant.infrastructure.persistence.TenantRepository;
import com.nexus.ticket.application.dto.CreateTicketRequest;
import com.nexus.ticket.application.dto.TicketMapper;
import com.nexus.ticket.application.dto.TicketResponse;
import com.nexus.ticket.application.dto.TransitionTicketRequest;
import com.nexus.ticket.application.dto.UpdateTicketRequest;
import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import com.nexus.ticket.domain.TicketStateMachine;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import com.nexus.ticket.infrastructure.persistence.TicketSpecifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for ticket CRUD and lifecycle transitions.
 *
 * <p>Orchestrates: validate tenant → create/update/transition entity
 * → enforce state machine → persist → return DTO.
 *
 * <p>All public methods are {@code @Transactional}, which means:
 * <ol>
 *   <li>Spring calls {@code setAutoCommit(false)} — our {@code TenantAwareDataSource}
 *       intercepts this and runs {@code SET LOCAL app.tenant_id}</li>
 *   <li>RLS filters every query to the current tenant's rows</li>
 *   <li>On success, Spring commits; on exception, Spring rolls back</li>
 * </ol>
 */
@Service
@Transactional
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final TenantRepository tenantRepository;

    public TicketService(TicketRepository ticketRepository, TenantRepository tenantRepository) {
        this.ticketRepository = ticketRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Create a new ticket for the given tenant.
     *
     * @param tenantId the tenant's UUID (from the URL path)
     * @param request  the validated DTO
     * @return the created ticket as a response DTO
     * @throws TenantNotFoundException if the tenant doesn't exist
     */
    public TicketResponse createTicket(UUID tenantId, CreateTicketRequest request) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        TicketEntity ticket = new TicketEntity(tenant, request.subject(), request.description());

        // Apply optional fields if provided
        if (request.priority() != null) {
            ticket.setPriority(TicketMapper.parsePriority(request.priority()));
        }
        if (request.category() != null) {
            ticket.setCategory(TicketMapper.parseCategory(request.category()));
        }

        TicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket created: id={}, tenant={}", saved.getId(), tenantId);

        return TicketMapper.toResponse(saved);
    }

    /**
     * Get a single ticket by ID.
     *
     * <p>RLS ensures only tickets belonging to the current tenant are visible.
     * If the ticket belongs to a different tenant, {@code findById} returns
     * empty — which we report as "not found" (information hiding).
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicket(UUID ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return TicketMapper.toResponse(ticket);
    }

    /**
     * List tickets for the current tenant with optional filters and pagination.
     *
     * <p>Specifications compose dynamically — only the filters the client
     * actually sends get added to the WHERE clause.
     *
     * @param status   optional status filter (e.g., "NEW", "ESCALATED")
     * @param priority optional priority filter
     * @param category optional category filter
     * @param pageable pagination + sorting (page, size, sort)
     */
    @Transactional(readOnly = true)
    public Page<TicketResponse> listTickets(String status, String priority,
                                            String category, Pageable pageable) {
        Specification<TicketEntity> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            spec = spec.and(TicketSpecifications.hasStatus(
                    TicketStatus.valueOf(status.toUpperCase())));
        }
        if (priority != null && !priority.isBlank()) {
            spec = spec.and(TicketSpecifications.hasPriority(
                    TicketPriority.valueOf(priority.toUpperCase())));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(TicketSpecifications.hasCategory(
                    TicketCategory.valueOf(category.toUpperCase())));
        }

        return ticketRepository.findAll(spec, pageable)
                .map(TicketMapper::toResponse);
    }

    /**
     * Update a ticket's mutable fields (subject, description, priority, category).
     *
     * <p>Only fields present in the request are applied (partial update).
     */
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (request.subject() != null) {
            ticket.setSubject(request.subject());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(TicketMapper.parsePriority(request.priority()));
        }
        if (request.category() != null) {
            ticket.setCategory(TicketMapper.parseCategory(request.category()));
        }

        TicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket updated: id={}", saved.getId());

        return TicketMapper.toResponse(saved);
    }

    /**
     * Transition a ticket's status through the state machine.
     *
     * @throws TicketNotFoundException         if the ticket doesn't exist (or is hidden by RLS)
     * @throws IllegalTicketTransitionException if the state machine rejects the transition
     */
    public TicketResponse transitionTicket(UUID ticketId, TransitionTicketRequest request) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus currentStatus = ticket.getStatus();
        TicketStatus targetStatus = TicketStatus.valueOf(request.targetStatus().toUpperCase());

        // Use the state machine from Phase 1 to validate
        try {
            TicketStateMachine.transition(currentStatus, targetStatus);
        } catch (IllegalStateException e) {
            throw new IllegalTicketTransitionException(currentStatus, targetStatus);
        }

        ticket.setStatus(targetStatus);
        TicketEntity saved = ticketRepository.save(ticket);
        log.info("Ticket transitioned: id={}, {} → {}", saved.getId(), currentStatus, targetStatus);

        return TicketMapper.toResponse(saved);
    }

    /**
     * Delete a ticket by ID.
     *
     * @throws TicketNotFoundException if the ticket doesn't exist (or is hidden by RLS)
     */
    public void deleteTicket(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        ticketRepository.deleteById(ticketId);
        log.info("Ticket deleted: id={}", ticketId);
    }
}
