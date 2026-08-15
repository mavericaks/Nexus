package com.nexus.ticket.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link TicketNoteEntity}.
 * RLS handles tenant isolation.
 */
@Repository
public interface TicketNoteRepository extends JpaRepository<TicketNoteEntity, UUID> {

    List<TicketNoteEntity> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
