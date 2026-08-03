package com.nexus.ai.triage;

import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.domain.TicketStateMachine;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import com.nexus.ticket.domain.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service that orchestrates the AI triage flow for a ticket.
 *
 * <p>Flow:
 * <ol>
 *   <li>Load the ticket from the database</li>
 *   <li>Run the {@link TriageAgent} to classify and draft a reply</li>
 *   <li>Update the ticket with AI results (category, priority, response, confidence)</li>
 *   <li>Transition the ticket through the state machine:
 *       NEW → CLASSIFIED → AI_DRAFTED</li>
 *   <li>If auto-resolvable and kill switch is on → AI_DRAFTED → AUTO_RESOLVED</li>
 * </ol>
 */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final TicketRepository ticketRepository;
    private final TriageAgent triageAgent;
    private final ApplicationEventPublisher eventPublisher;

    public TriageService(TicketRepository ticketRepository, TriageAgent triageAgent,
                         ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.triageAgent = triageAgent;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs AI triage on a ticket: classify, draft reply, update, transition state.
     *
     * @param ticketId the ticket to triage
     * @return the triage result
     */
    @Transactional
    public TriageResult triageTicket(UUID ticketId) {
        // Load ticket
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        log.info("Starting triage for ticket {} (status: {})", ticketId, ticket.getStatus());

        // Only triage tickets in NEW status
        if (ticket.getStatus() != TicketStatus.NEW) {
            log.warn("Ticket {} is in status {}, not NEW — skipping triage", ticketId, ticket.getStatus());
            return new TriageResult(
                    ticket.getCategory(), ticket.getPriority(),
                    ticket.getAiResponse(), "Ticket already triaged",
                    ticket.getConfidenceScore() != null ? ticket.getConfidenceScore() : 0.0,
                    false
            );
        }

        // Run triage agent
        TriageResult result = triageAgent.triage(ticket.getSubject(), ticket.getDescription());

        // Update ticket with AI results
        ticket.setCategory(result.category());
        ticket.setPriority(result.priority());
        ticket.setAiResponse(result.suggestedReply());
        ticket.setConfidenceScore(result.confidenceScore());

        // Transition state: NEW → CLASSIFIED
        if (TicketStateMachine.canTransition(ticket.getStatus(), TicketStatus.CLASSIFIED)) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.CLASSIFIED);
            log.info("Ticket {} transitioned to CLASSIFIED", ticketId);
            eventPublisher.publishEvent(new TicketStatusChangedEvent(
                    ticket.getTenant().getId(), ticketId, oldStatus, TicketStatus.CLASSIFIED));
        }

        // Transition state: CLASSIFIED → AI_DRAFTED
        if (TicketStateMachine.canTransition(ticket.getStatus(), TicketStatus.AI_DRAFTED)) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.AI_DRAFTED);
            log.info("Ticket {} transitioned to AI_DRAFTED", ticketId);
            eventPublisher.publishEvent(new TicketStatusChangedEvent(
                    ticket.getTenant().getId(), ticketId, oldStatus, TicketStatus.AI_DRAFTED));
        }

        // If auto-resolvable, transition: AI_DRAFTED → AUTO_RESOLVED
        if (result.autoResolvable() &&
            TicketStateMachine.canTransition(ticket.getStatus(), TicketStatus.AUTO_RESOLVED)) {
            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(TicketStatus.AUTO_RESOLVED);
            log.info("Ticket {} AUTO-RESOLVED (confidence: {:.2f})", ticketId, result.confidenceScore());
            eventPublisher.publishEvent(new TicketStatusChangedEvent(
                    ticket.getTenant().getId(), ticketId, oldStatus, TicketStatus.AUTO_RESOLVED));
        } else if (!result.autoResolvable()) {
            // Escalate: AI_DRAFTED → ESCALATED
            if (TicketStateMachine.canTransition(ticket.getStatus(), TicketStatus.ESCALATED)) {
                TicketStatus oldStatus = ticket.getStatus();
                ticket.setStatus(TicketStatus.ESCALATED);
                log.info("Ticket {} ESCALATED (confidence: {:.2f} < threshold)", ticketId, result.confidenceScore());
                eventPublisher.publishEvent(new TicketStatusChangedEvent(
                        ticket.getTenant().getId(), ticketId, oldStatus, TicketStatus.ESCALATED));
            }
        }

        ticketRepository.save(ticket);
        log.info("Triage complete for ticket {}: category={}, confidence={:.2f}, status={}",
                ticketId, result.category(), result.confidenceScore(), ticket.getStatus());

        return result;
    }
}
