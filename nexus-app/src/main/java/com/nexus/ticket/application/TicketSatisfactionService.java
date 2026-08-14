package com.nexus.ticket.application;

import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.application.dto.CreateSatisfactionRequest;
import com.nexus.ticket.application.dto.SatisfactionResponse;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import com.nexus.ticket.infrastructure.persistence.TicketSatisfactionEntity;
import com.nexus.ticket.infrastructure.persistence.TicketSatisfactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for customer satisfaction (CSAT) ratings.
 *
 * <p>Each ticket can have at most one satisfaction rating.
 * Ratings are submitted after a ticket is resolved.
 */
@Service
@Transactional
public class TicketSatisfactionService {

    private final TicketSatisfactionRepository satisfactionRepository;
    private final TicketRepository ticketRepository;

    public TicketSatisfactionService(TicketSatisfactionRepository satisfactionRepository,
                                      TicketRepository ticketRepository) {
        this.satisfactionRepository = satisfactionRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Submit a CSAT rating for a ticket.
     * Throws if the ticket doesn't exist or already has a rating.
     */
    public SatisfactionResponse rate(UUID ticketId, CreateSatisfactionRequest request) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Check for duplicate rating
        Optional<TicketSatisfactionEntity> existing = satisfactionRepository.findByTicketId(ticketId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Ticket already has a satisfaction rating.");
        }

        TicketSatisfactionEntity entity = new TicketSatisfactionEntity(
                ticket, ticket.getTenant(), request.score(), request.feedback());
        entity = satisfactionRepository.save(entity);

        return toResponse(entity);
    }

    /**
     * Get the CSAT rating for a ticket, if one exists.
     */
    @Transactional(readOnly = true)
    public Optional<SatisfactionResponse> getRating(UUID ticketId) {
        return satisfactionRepository.findByTicketId(ticketId).map(this::toResponse);
    }

    /**
     * Get the average CSAT score across all rated tickets in the current tenant.
     */
    @Transactional(readOnly = true)
    public Double getAverageScore() {
        return satisfactionRepository.findAverageScore();
    }

    private SatisfactionResponse toResponse(TicketSatisfactionEntity entity) {
        return new SatisfactionResponse(
                entity.getId(), entity.getTicketId(),
                entity.getScore(), entity.getFeedback(), entity.getCreatedAt());
    }
}
