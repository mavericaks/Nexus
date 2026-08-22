package com.nexus.ticket.infrastructure.messaging;

import com.nexus.ticket.domain.event.TicketCreatedEvent;
import com.nexus.ticket.domain.event.TicketStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Bridges Spring {@code ApplicationEvent}s to Kafka topics.
 *
 * <p>This is the outbound adapter in the Hexagonal Architecture sense:
 * the domain publishes events via Spring's {@code ApplicationEventPublisher}
 * (port), and this class converts them into Kafka messages (adapter).
 *
 * <p>The Kafka message key is the {@code ticketId} — this ensures all events
 * for the same ticket land on the same partition, preserving ordering
 * (e.g., CREATED always arrives before STATUS_CHANGED for that ticket).
 *
 * <p><b>Transactional safety:</b> Uses {@code @TransactionalEventListener(phase = AFTER_COMMIT)}
 * so that Kafka messages are only sent after the database transaction commits
 * successfully. If the transaction rolls back, no event is published — preventing
 * phantom events in Kafka that reference data that doesn't exist in the database.
 *
 * <p><b>Known limitation:</b> {@code AFTER_COMMIT} does NOT guarantee delivery.
 * If the application crashes after DB commit but before the Kafka send completes,
 * the event is lost. For guaranteed exactly-once delivery, an Outbox pattern
 * would be required — that is intentionally deferred to a future phase.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class TicketEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(TicketEventKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TicketEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        log.info("Publishing TicketCreatedEvent to Kafka: ticketId={}, tenant={}",
                event.ticketId(), event.tenantId());

        kafkaTemplate.send(
                TicketKafkaTopics.TICKET_CREATED,
                event.ticketId().toString(),   // key — partition affinity
                event                          // value — serialised as JSON
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("Publishing TicketStatusChangedEvent to Kafka: ticketId={}, {} -> {}",
                event.ticketId(), event.oldStatus(), event.newStatus());

        kafkaTemplate.send(
                TicketKafkaTopics.TICKET_STATUS_CHANGED,
                event.ticketId().toString(),
                event
        );
    }
}
