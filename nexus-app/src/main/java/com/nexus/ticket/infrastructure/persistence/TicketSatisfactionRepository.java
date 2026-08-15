package com.nexus.ticket.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link TicketSatisfactionEntity}.
 */
@Repository
public interface TicketSatisfactionRepository extends JpaRepository<TicketSatisfactionEntity, UUID> {

    Optional<TicketSatisfactionEntity> findByTicket_Id(UUID ticketId);

    @Query("SELECT AVG(s.score) FROM TicketSatisfactionEntity s")
    Double findAverageScore();

    long count();
}
