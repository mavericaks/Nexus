package com.nexus.ticket.application;

import com.nexus.common.exception.IllegalTicketTransitionException;
import com.nexus.common.exception.TenantNotFoundException;
import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.tenant.domain.PlanTier;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import com.nexus.tenant.infrastructure.persistence.TenantRepository;
import com.nexus.ticket.application.dto.CreateTicketRequest;
import com.nexus.ticket.application.dto.TicketResponse;
import com.nexus.ticket.application.dto.TransitionTicketRequest;
import com.nexus.ticket.application.dto.UpdateTicketRequest;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.ticket.infrastructure.persistence.TicketEntity;
import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TicketService}.
 *
 * <p>Uses Mockito to mock repositories — these are fast, in-memory tests
 * that verify business logic without hitting a database. Integration tests
 * with Testcontainers will cover the full stack including RLS.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TicketService ticketService;

    private UUID tenantId;
    private UUID ticketId;
    private TenantEntity tenant;
    private TicketEntity ticket;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        tenant = new TenantEntity("Acme Corp", "acme", PlanTier.PROFESSIONAL);
        ticket = new TicketEntity(tenant, "Login broken", "Can't log in since yesterday");
    }

    @Nested
    @DisplayName("createTicket")
    class CreateTicket {

        @Test
        @DisplayName("creates ticket with valid tenant and required fields")
        void happyPath() {
            var request = new CreateTicketRequest("Login broken", "Details here", null, null);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketResponse response = ticketService.createTicket(tenantId, request);

            assertNotNull(response);
            assertEquals("Login broken", response.subject());
            assertEquals("NEW", response.status());
            verify(ticketRepository).save(any(TicketEntity.class));
        }

        @Test
        @DisplayName("creates ticket with optional priority and category")
        void withOptionalFields() {
            var request = new CreateTicketRequest("Billing issue", "Wrong charge", "BILLING", "HIGH");
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
            when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketResponse response = ticketService.createTicket(tenantId, request);

            assertEquals("HIGH", response.priority());
            assertEquals("BILLING", response.category());
        }

        @Test
        @DisplayName("throws TenantNotFoundException for unknown tenant")
        void unknownTenant() {
            var request = new CreateTicketRequest("Test", null, null, null);
            when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

            assertThrows(TenantNotFoundException.class,
                    () -> ticketService.createTicket(tenantId, request));

            verify(ticketRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getTicket")
    class GetTicket {

        @Test
        @DisplayName("returns ticket when found")
        void happyPath() {
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            TicketResponse response = ticketService.getTicket(ticketId);

            assertEquals("Login broken", response.subject());
        }

        @Test
        @DisplayName("throws TicketNotFoundException when not found")
        void notFound() {
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.getTicket(ticketId));
        }
    }

    @Nested
    @DisplayName("updateTicket")
    class UpdateTicket {

        @Test
        @DisplayName("updates only provided fields (partial update)")
        void partialUpdate() {
            var request = new UpdateTicketRequest("Updated subject", null, null, null);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketResponse response = ticketService.updateTicket(ticketId, request);

            assertEquals("Updated subject", response.subject());
            // Description unchanged
            assertEquals("Can't log in since yesterday", response.description());
        }

        @Test
        @DisplayName("throws TicketNotFoundException for missing ticket")
        void notFound() {
            var request = new UpdateTicketRequest("New title", null, null, null);
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.updateTicket(ticketId, request));
        }
    }

    @Nested
    @DisplayName("transitionTicket")
    class TransitionTicket {

        @Test
        @DisplayName("transitions ticket when state machine allows it")
        void legalTransition() {
            // NEW → CLASSIFIED is legal
            var request = new TransitionTicketRequest("CLASSIFIED");
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(TicketEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            TicketResponse response = ticketService.transitionTicket(ticketId, request);

            assertEquals("CLASSIFIED", response.status());
        }

        @Test
        @DisplayName("throws IllegalTicketTransitionException for illegal transition")
        void illegalTransition() {
            // NEW → CLOSED is illegal
            var request = new TransitionTicketRequest("CLOSED");
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            assertThrows(IllegalTicketTransitionException.class,
                    () -> ticketService.transitionTicket(ticketId, request));

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TicketNotFoundException for missing ticket")
        void notFound() {
            var request = new TransitionTicketRequest("CLASSIFIED");
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.transitionTicket(ticketId, request));
        }
    }

    @Nested
    @DisplayName("deleteTicket")
    class DeleteTicket {

        @Test
        @DisplayName("deletes ticket when it exists")
        void happyPath() {
            when(ticketRepository.existsById(ticketId)).thenReturn(true);

            ticketService.deleteTicket(ticketId);

            verify(ticketRepository).deleteById(ticketId);
        }

        @Test
        @DisplayName("throws TicketNotFoundException for missing ticket")
        void notFound() {
            when(ticketRepository.existsById(ticketId)).thenReturn(false);

            assertThrows(TicketNotFoundException.class,
                    () -> ticketService.deleteTicket(ticketId));

            verify(ticketRepository, never()).deleteById(any());
        }
    }
}
