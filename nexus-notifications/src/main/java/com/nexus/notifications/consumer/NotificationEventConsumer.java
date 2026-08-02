package com.nexus.notifications.consumer;

import com.nexus.notifications.dedup.InMemoryDedupStore;
import com.nexus.notifications.handler.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the notification microservice.
 *
 * <p>Listens to the {@code nexus.tickets.status-changed} topic and
 * dispatches notifications (email, Slack) for relevant status transitions.
 *
 * <p><b>Idempotent Consumer Pattern:</b> Before dispatching, each event
 * is checked against the {@link InMemoryDedupStore}. If the same
 * event has already been processed (e.g., Kafka redelivered it after
 * a rebalance), it is silently skipped.
 *
 * <p><b>Consumer group:</b> {@code nexus-notifications-group} — separate
 * from the core app's consumer groups, so notifications can be scaled
 * and deployed independently.
 */
@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final InMemoryDedupStore dedupStore;
    private final NotificationDispatcher dispatcher;

    public NotificationEventConsumer(InMemoryDedupStore dedupStore,
                                     NotificationDispatcher dispatcher) {
        this.dedupStore = dedupStore;
        this.dispatcher = dispatcher;
    }

    @KafkaListener(
            topics = "nexus.tickets.status-changed",
            groupId = "nexus-notifications-group"
    )
    public void onStatusChanged(TicketStatusChangedMessage event) {
        String dedupKey = event.ticketId() + ":" + event.newStatus();

        log.info("Received status change event: ticket={}, {} -> {}",
                event.ticketId(), event.oldStatus(), event.newStatus());

        if (!dedupStore.tryProcess(dedupKey)) {
            return;  // duplicate — already processed
        }

        try {
            dispatcher.dispatch(event);
        } catch (Exception e) {
            log.error("Failed to dispatch notification for ticket {}: {}",
                    event.ticketId(), e.getMessage(), e);
            // Don't rethrow — prevents infinite Kafka retries.
            // In production: push to a DLT or retry queue.
        }
    }
}