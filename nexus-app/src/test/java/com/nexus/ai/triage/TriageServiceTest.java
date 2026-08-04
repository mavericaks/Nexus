package com.nexus.ai.triage;

import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.ticket.domain.event.TicketStatusChangedEvent;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TriageService} — the orchestration service
 * that ties together the TriageAgent, state machine transitions,
 * event publishing, audit logging, and Micrometer metrics.
 *
 * <p>Uses Mockito mocks for all dependencies. A real
 * {@link SimpleMeterRegistry} is used for Micrometer (it's an
 * in-memory implementation designed for testing).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TriageService")
class TriageServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TriageAgent triageAgent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TriageAuditLogger auditLogger;

    @Captor
    private ArgumentCaptor<TicketStatusChangedEvent> eventCaptor;

    private SimpleMeterRegistry meterRegistry;
    private TriageService triageService;

    private static final UUID TICKET_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        triageService = new TriageService(
                ticketRepository, triageAgent, eventPublisher,
                auditLogger, meterRegistry
        );
    }

    /** Helper to create a stub TicketEntity in NEW status. */
    private TicketEntity newTicket() {
        TicketEntity ticket = mock(TicketEntity.class);
        TenantEntity tenant = mock(TenantEntity.class);
        when(tenant.getId()).thenReturn(TENANT_ID);
        when(ticket.getTenant()).thenReturn(tenant);
        when(ticket.getSubject()).thenReturn("Cannot reset password");
        when(ticket.getDescription()).thenReturn("I tried to reset my password but the email never arrived.");
        // Start in NEW status, but allow status changes
        final TicketStatus[] currentStatus = {TicketStatus.NEW};
        when(ticket.getStatus()).thenAnswer(inv -> currentStatus[0]);
        doAnswer(inv -> { currentStatus[0] = inv.getArgument(0); return null; })
                .when(ticket).setStatus(any(TicketStatus.class));
        return ticket;
    }

    /** Helper to create a high-confidence auto-resolvable triage result. */
    private TriageResult autoResolvableResult() {
        return new TriageResult(
                TicketCategory.ACCOUNT, TicketPriority.MEDIUM,
                "Please check your spam folder.", "Password reset issue",
                0.92, true
        );
    }

    /** Helper to create a low-confidence escalation triage result. */
    private TriageResult escalationResult() {
        return new TriageResult(
                TicketCategory.BILLING, TicketPriority.HIGH,
                "I'll escalate this to our billing team.", "Complex billing dispute",
                0.45, false
        );
    }

    @Nested
    @DisplayName("Successful Triage — Auto-Resolve")
    class AutoResolve {

        @Test
        @DisplayName("triages NEW ticket → CLASSIFIED → AI_DRAFTED → AUTO_RESOLVED")
        void triagesNewTicketToAutoResolved() {
            TicketEntity ticket = newTicket();
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
            when(triageAgent.triage(any(), any())).thenReturn(autoResolvableResult());
            when(ticketRepository.save(any())).thenReturn(ticket);

            TriageResult result = triageService.triageTicket(TICKET_ID);

            // Verify result
            assertEquals(TicketCategory.ACCOUNT, result.category());
            assertEquals(TicketPriority.MEDIUM, result.priority());
            assertTrue(result.autoResolvable());
            assertEquals(0.92, result.confidenceScore(), 0.001);

            // Verify ticket ended up at AUTO_RESOLVED
            verify(ticket).setStatus(TicketStatus.CLASSIFIED);
            verify(ticket).setStatus(TicketStatus.AI_DRAFTED);
            verify(ticket).setStatus(TicketStatus.AUTO_RESOLVED);

            // Verify AI results were set on the ticket
            verify(ticket).setCategory(TicketCategory.ACCOUNT);
            verify(ticket).setPriority(TicketPriority.MEDIUM);
            verify(ticket).setAiResponse("Please check your spam folder.");
            verify(ticket).setConfidenceScore(0.92);

            // Verify ticket was saved
            verify(ticketRepository).save(ticket);

            // Verify 3 status change events were published
            verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());
        }

        @Test
        @DisplayName("records Micrometer metrics after triage")
        void recordsMicrometerMetrics() {
            TicketEntity ticket = newTicket();
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
            when(triageAgent.triage(any(), any())).thenReturn(autoResolvableResult());
            when(ticketRepository.save(any())).thenReturn(ticket);

            triageService.triageTicket(TICKET_ID);

            // Verify triage count metric was recorded
            double count = meterRegistry.get("ticket.triage.count")
                    .tag("category", "ACCOUNT")
                    .tag("autoResolved", "true")
                    .counter().count();
            assertEquals(1.0, count);

            // Verify confidence metric was recorded
            double confidenceCount = meterRegistry.get("ticket.triage.confidence")
                    .tag("category", "ACCOUNT")
                    .summary().count();
            assertEquals(1, confidenceCount);
        }

        @Test
        @DisplayName("calls audit logger after triage")
        void callsAuditLogger() {
            TicketEntity ticket = newTicket();
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
            when(triageAgent.triage(any(), any())).thenReturn(autoResolvableResult());
            when(ticketRepository.save(any())).thenReturn(ticket);

            triageService.triageTicket(TICKET_ID);

            verify(auditLogger).logTriageDecision(eq(TICKET_ID), any(TriageResult.class), anyLong());
        }
    }

    @Nested
    @DisplayName("Successful Triage — Escalation")
    class Escalation {

        @Test
        @DisplayName("triages low-confidence ticket → CLASSIFIED → AI_DRAFTED → ESCALATED")
        void escalatesLowConfidenceTicket() {
            TicketEntity ticket = newTicket();
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
            when(triageAgent.triage(any(), any())).thenReturn(escalationResult());
            when(ticketRepository.save(any())).thenReturn(ticket);

            TriageResult result = triageService.triageTicket(TICKET_ID);

            // Verify escalation
            assertFalse(result.autoResolvable());
            assertEquals(TicketCategory.BILLING, result.category());

            // Verify ticket reached ESCALATED
            verify(ticket).setStatus(TicketStatus.CLASSIFIED);
            verify(ticket).setStatus(TicketStatus.AI_DRAFTED);
            verify(ticket).setStatus(TicketStatus.ESCALATED);

            // Verify 3 events (CLASSIFIED, AI_DRAFTED, ESCALATED)
            verify(eventPublisher, times(3)).publishEvent(eventCaptor.capture());
            verify(ticketRepository).save(ticket);
        }
    }

    @Nested
    @DisplayName("Skip Triage")
    class SkipTriage {

        @Test
        @DisplayName("skips triage when ticket is not in NEW status")
        void skipsTriageForNonNewTicket() {
            TicketEntity ticket = mock(TicketEntity.class);
            when(ticket.getStatus()).thenReturn(TicketStatus.CLASSIFIED);
            when(ticket.getCategory()).thenReturn(TicketCategory.ACCOUNT);
            when(ticket.getPriority()).thenReturn(TicketPriority.LOW);
            when(ticket.getAiResponse()).thenReturn("Previous response");
            when(ticket.getConfidenceScore()).thenReturn(0.8);
            when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

            TriageResult result = triageService.triageTicket(TICKET_ID);

            // Should return existing data, not run triage
            assertEquals(TicketCategory.ACCOUNT, result.category());
            assertFalse(result.autoResolvable());

            // Triage agent should NOT have been called
            verifyNoInteractions(triageAgent);

            // Audit logger should log the skip
            verify(auditLogger).logTriageSkipped(TICKET_ID, "CLASSIFIED");

            // No ticket save should happen
            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws TicketNotFoundException for unknown ticket ID")
        void throwsForUnknownTicket() {
            UUID unknownId = UUID.randomUUID();
            when(ticketRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> triageService.triageTicket(unknownId));

            // Nothing else should happen
            verifyNoInteractions(triageAgent);
            verifyNoInteractions(auditLogger);
        }
    }
}
