package com.nexus.common.scheduling;

import com.nexus.ai.rag.KnowledgeBaseSearchService;
import com.nexus.common.multitenancy.TenantContext;
import com.nexus.ticket.domain.TicketStateMachine;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.ticket.domain.event.TicketStatusChangedEvent;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import com.nexus.tenant.infrastructure.persistence.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled background jobs for the Nexus platform.
 *
 * <p><b>SLA Sweep:</b> Runs every 15 minutes in dev. Finds tickets stuck
 * in {@code NEW} or {@code CLASSIFIED} for longer than the SLA threshold
 * (default: 4 hours) and auto-escalates them to {@code ESCALATED}.
 *
 * <p><b>KB Backfill:</b> Runs once daily at 2 AM. Generates vector
 * embeddings for any knowledge base articles that don't have one yet.
 *
 * <p>Both jobs iterate per-tenant and manually set {@link TenantContext}
 * because scheduled threads have no HTTP request and no JWT — just like
 * the Kafka consumer in Unit 5.
 */
@Component
@EnableScheduling
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    /** Tickets stuck in these states for too long get escalated. */
    private static final List<TicketStatus> SLA_BREACH_STATUSES =
            List.of(TicketStatus.NEW, TicketStatus.CLASSIFIED);

    /** Default SLA threshold: 4 hours. */
    private static final int SLA_HOURS = 4;

    private final TicketRepository ticketRepository;
    private final TenantRepository tenantRepository;
    private final KnowledgeBaseSearchService kbSearchService;
    private final ApplicationEventPublisher eventPublisher;

    public ScheduledJobs(TicketRepository ticketRepository,
                         TenantRepository tenantRepository,
                         KnowledgeBaseSearchService kbSearchService,
                         ApplicationEventPublisher eventPublisher) {
        this.ticketRepository = ticketRepository;
        this.tenantRepository = tenantRepository;
        this.kbSearchService = kbSearchService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * SLA Sweep — escalates stale tickets.
     *
     * <p>Runs every 15 minutes. Iterates over all tenants, sets the tenant
     * context, finds tickets in breach, and transitions them to ESCALATED.
     */
    @Scheduled(fixedRateString = "${nexus.scheduling.sla-sweep-ms:900000}")
    @Transactional
    public void slaSweep() {
        log.info("SLA Sweep started");
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(SLA_HOURS);
        int totalEscalated = 0;

        List<TenantEntity> tenants = tenantRepository.findAll();
        for (TenantEntity tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getId().toString());

                List<TicketEntity> staleTickets = ticketRepository
                        .findByStatusInAndCreatedAtBefore(SLA_BREACH_STATUSES, cutoff);

                for (TicketEntity ticket : staleTickets) {
                    if (TicketStateMachine.canTransition(ticket.getStatus(), TicketStatus.ESCALATED)) {
                        TicketStatus oldStatus = ticket.getStatus();
                        ticket.setStatus(TicketStatus.ESCALATED);
                        ticketRepository.save(ticket);
                        totalEscalated++;

                        eventPublisher.publishEvent(new TicketStatusChangedEvent(
                                tenant.getId(), ticket.getId(), oldStatus, TicketStatus.ESCALATED));

                        log.info("SLA breach: escalated ticket {} (was {} for >{} hours)",
                                ticket.getId(), oldStatus, SLA_HOURS);
                    }
                }
            } catch (Exception e) {
                log.error("SLA sweep failed for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("SLA Sweep completed: {} tickets escalated across {} tenants",
                totalEscalated, tenants.size());
    }

    /**
     * KB Backfill — generates embeddings for articles missing them.
     *
     * <p>Runs daily at 2:00 AM. Iterates over all tenants and calls
     * {@link KnowledgeBaseSearchService#backfillEmbeddings()} for each.
     */
    @Scheduled(cron = "${nexus.scheduling.kb-backfill-cron:0 0 2 * * *}")
    @Transactional
    public void kbBackfill() {
        log.info("KB Backfill started");
        int totalBackfilled = 0;

        List<TenantEntity> tenants = tenantRepository.findAll();
        for (TenantEntity tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getId().toString());
                int count = kbSearchService.backfillEmbeddings();
                totalBackfilled += count;
            } catch (Exception e) {
                log.error("KB backfill failed for tenant {}: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }

        log.info("KB Backfill completed: {} embeddings across {} tenants",
                totalBackfilled, tenants.size());
    }
}