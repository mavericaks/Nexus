package com.nexus.ticket.application;

import com.nexus.ticket.application.dto.TicketEventResponse;
import com.nexus.ticket.infrastructure.persistence.TicketEventEntity;
import com.nexus.ticket.infrastructure.persistence.TicketEventRepository;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for recording and querying ticket events (audit trail).
 *
 * <p>Events are append-only — once written, they are never modified or deleted.
 * This provides a complete, tamper-proof history of every ticket mutation.
 */
@Service
@Transactional
public class TicketEventService {

    private final TicketEventRepository ticketEventRepository;

    public TicketEventService(TicketEventRepository ticketEventRepository) {
        this.ticketEventRepository = ticketEventRepository;
    }

    /**
     * Record a new event in the ticket's timeline.
     */
    public void recordEvent(TicketEntity ticket, TenantEntity tenant, String eventType,
                            UUID actorId, String actorName, Map<String, Object> details) {
        TicketEventEntity event = new TicketEventEntity(
                ticket, tenant, eventType, actorId, actorName, details);
        ticketEventRepository.save(event);
    }

    /**
     * Get all events for a ticket, ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<TicketEventResponse> getEventsForTicket(UUID ticketId) {
        return ticketEventRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(e -> new TicketEventResponse(
                        e.getId(),
                        e.getTicketId(),
                        e.getEventType(),
                        e.getActorId(),
                        e.getActorName(),
                        e.getDetails(),
                        e.getCreatedAt()))
                .toList();
    }
}
