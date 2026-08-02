package com.nexus.ticket.infrastructure.persistence;

import com.nexus.ticket.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TicketEntity}.
 *
 * <p>Extends two interfaces:
 * <ul>
 *   <li>{@link JpaRepository} — standard CRUD + pagination ({@code findAll(Pageable)})</li>
 *   <li>{@link JpaSpecificationExecutor} — dynamic filtering with composable
 *       {@code Specification} predicates (filter by status AND/OR priority
 *       AND/OR category without a separate query method for each combination)</li>
 * </ul>
 *
 * <p><b>No {@code WHERE tenant_id = ?} needed.</b> Postgres RLS automatically
 * filters every query to the current tenant's rows. This repository writes
 * normal queries, and the database enforces isolation transparently.
 */
@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, UUID>,
        JpaSpecificationExecutor<TicketEntity> {

    /**
     * Finds tickets that have been stuck in early-lifecycle states
     * (NEW or CLASSIFIED) for longer than the SLA threshold.
     *
     * <p>Used by the SLA Sweep scheduled job to auto-escalate stale tickets.
     * RLS ensures this only returns tickets for the current tenant context.
     */
    @Query("SELECT t FROM TicketEntity t WHERE t.status IN :statuses AND t.createdAt < :cutoff")
    List<TicketEntity> findByStatusInAndCreatedAtBefore(
            @Param("statuses") List<TicketStatus> statuses,
            @Param("cutoff") OffsetDateTime cutoff);
}
