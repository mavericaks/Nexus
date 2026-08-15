package com.nexus.ticket.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link TicketEventEntity}.
 *
 * <p>No tenant filter needed — RLS handles isolation.
 * Events are ordered by creation time (oldest first) to build a timeline.
 */
@Repository
public interface TicketEventRepository extends JpaRepository<TicketEventEntity, UUID> {

    List<TicketEventEntity> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
