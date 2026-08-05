package com.nexus.ticket.infrastructure.messaging;

import com.nexus.ai.triage.TriageService;
import com.nexus.common.multitenancy.TenantContext;
import com.nexus.ticket.domain.event.TicketCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that triggers async AI triage when a ticket is created.
 *
 * <p>Before Phase 5, triage was synchronous: the user called
 * {@code POST /triage} and waited for the LLM to respond. Now, the
 * {@link com.nexus.ticket.application.TicketService} publishes a
 * {@link TicketCreatedEvent} to Kafka, and this consumer picks it up
 * in a background thread to run triage automatically.
 *
 * <p><b>Tenant context:</b> Kafka consumer threads don't have an HTTP
 * request, so there's no JWT and no {@code TenantContextFilter}. We
 * manually set {@link TenantContext} from the event's {@code tenantId}
 * so that RLS-aware database queries still work correctly.
 *
 * <p><b>Idempotency:</b> The consumer checks that the ticket is still
 * in {@code NEW} status before triaging. If a duplicate event arrives
 * (Kafka at-least-once delivery), the ticket will already be in
 * {@code CLASSIFIED/AI_DRAFTED/ESCALATED} and triage will be skipped.
 */
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class TriageEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TriageEventConsumer.class);

    private final TriageService triageService;

    public TriageEventConsumer(TriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * Consumes TicketCreatedEvent from Kafka and triggers AI triage.
     *
     * <p>The consumer group "nexus-triage-group" ensures that if we scale
     * to multiple app instances, each ticket is triaged exactly once
     * (Kafka assigns partitions to consumers within the same group).
     */
    @KafkaListener(
            topics = TicketKafkaTopics.TICKET_CREATED,
            groupId = "nexus-triage-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("Received TicketCreatedEvent from Kafka: ticketId={}, tenant={}, subject='{}'",
                event.ticketId(), event.tenantId(), event.subject());

        try {
            // Set tenant context for RLS — no HTTP request on Kafka threads
            TenantContext.setTenantId(event.tenantId().toString());

            triageService.triageTicket(event.ticketId());

            log.info("Async triage completed for ticket {}", event.ticketId());
        } catch (Exception e) {
            // Log but don't rethrow — we don't want Kafka to retry endlessly
            // on a permanent failure (e.g., ticket not found, LLM timeout).
            // In production, this would push to a dead-letter topic (DLT).
            log.error("Async triage failed for ticket {}: {}", event.ticketId(), e.getMessage(), e);
        } finally {
            // Always clean up to prevent tenant leakage across Kafka consumer reuse
            TenantContext.clear();
        }
    }
}