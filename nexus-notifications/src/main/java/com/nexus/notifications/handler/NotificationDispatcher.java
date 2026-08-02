package com.nexus.notifications.handler;

import com.nexus.notifications.consumer.TicketStatusChangedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Dispatches notifications based on ticket status changes.
 *
 * <p>In production, this would integrate with:
 * <ul>
 *   <li>SendGrid/SES for email</li>
 *   <li>Slack API for channel messages</li>
 *   <li>PagerDuty for on-call escalations</li>
 *   <li>Webhooks for customer-facing status pages</li>
 * </ul>
 *
 * <p>For now, it logs simulated notifications at INFO level so we can
 * verify the end-to-end pipeline works without external dependencies.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    /**
     * Routes a ticket status change to the appropriate notification channel.
     */
    public void dispatch(TicketStatusChangedMessage event) {
        switch (event.newStatus()) {
            case "ESCALATED" -> sendEscalationAlert(event);
            case "AUTO_RESOLVED" -> sendAutoResolvedNotification(event);
            case "IN_PROGRESS" -> sendAgentPickedUpNotification(event);
            case "RESOLVED" -> sendResolutionNotification(event);
            default -> log.debug("No notification configured for status: {}", event.newStatus());
        }
    }

    private void sendEscalationAlert(TicketStatusChangedMessage event) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("  To: support-team@tenant-{}.com", event.tenantId());
        log.info("  Subject: [URGENT] Ticket {} needs human attention", event.ticketId());
        log.info("  Body: Ticket transitioned from {} to ESCALATED. AI confidence was below threshold.",
                event.oldStatus());
        log.info("=======================");

        log.info("=== SIMULATED SLACK ===");
        log.info("  Channel: #escalations");
        log.info("  Message: :rotating_light: Ticket {} escalated (was {})",
                event.ticketId(), event.oldStatus());
        log.info("=======================");
    }

    private void sendAutoResolvedNotification(TicketStatusChangedMessage event) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("  To: customer@tenant-{}.com", event.tenantId());
        log.info("  Subject: Your support request {} has been resolved", event.ticketId());
        log.info("  Body: Our AI assistant resolved your issue. If you need further help, reply to reopen.");
        log.info("=======================");
    }

    private void sendAgentPickedUpNotification(TicketStatusChangedMessage event) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("  To: customer@tenant-{}.com", event.tenantId());
        log.info("  Subject: An agent is working on your request {}", event.ticketId());
        log.info("  Body: A support agent has picked up your ticket. We'll update you with a resolution soon.");
        log.info("=======================");
    }

    private void sendResolutionNotification(TicketStatusChangedMessage event) {
        log.info("=== SIMULATED EMAIL ===");
        log.info("  To: customer@tenant-{}.com", event.tenantId());
        log.info("  Subject: Your support request {} has been resolved", event.ticketId());
        log.info("  Body: Your ticket has been resolved by our support team.");
        log.info("=======================");
    }
}