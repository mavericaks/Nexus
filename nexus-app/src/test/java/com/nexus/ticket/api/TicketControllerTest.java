package com.nexus.ticket.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.common.exception.IllegalTicketTransitionException;
import com.nexus.common.exception.TenantNotFoundException;
import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.application.TicketService;
import com.nexus.ticket.application.dto.CreateTicketRequest;
import com.nexus.ticket.application.dto.TicketResponse;
import com.nexus.ticket.application.dto.TransitionTicketRequest;
import com.nexus.ticket.application.dto.UpdateTicketRequest;
import com.nexus.ticket.domain.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests using {@code @WebMvcTest} — loads only the web layer
 * (controller + exception handler + filters), not the full application.
 *
 * <p>The service is mocked via {@code @MockBean}. These tests verify:
 * <ul>
 *   <li>Correct HTTP status codes (201, 200, 204, 400, 404, 409)</li>
 *   <li>Bean Validation is triggered by {@code @Valid}</li>
 *   <li>Global exception handler maps exceptions correctly</li>
 *   <li>JSON request/response serialization works</li>
 * </ul>
 */
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TICKET_ID = UUID.randomUUID();
    private static final String BASE_URL = "/api/v1/tenants/" + TENANT_ID + "/tickets";

    private TicketResponse sampleResponse() {
        return new TicketResponse(
                TICKET_ID, TENANT_ID, "Login broken", "Can't log in",
                "NEW", null, null, null, null, null, 0,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /tickets — create")
    class Create {

        @Test
        @DisplayName("201 Created with valid request")
        void happyPath() throws Exception {
            var request = new CreateTicketRequest("Login broken", "Details", null, null);
            when(ticketService.createTicket(eq(TENANT_ID), any())).thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.subject").value("Login broken"))
                    .andExpect(jsonPath("$.status").value("NEW"));
        }

        @Test
        @DisplayName("400 Bad Request when subject is blank")
        void validationFails() throws Exception {
            var request = new CreateTicketRequest("", null, null, null);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        @DisplayName("404 when tenant doesn't exist")
        void tenantNotFound() throws Exception {
            var request = new CreateTicketRequest("Test ticket", null, null, null);
            when(ticketService.createTicket(eq(TENANT_ID), any()))
                    .thenThrow(new TenantNotFoundException(TENANT_ID));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Tenant not found: " + TENANT_ID));
        }
    }

    @Nested
    @DisplayName("GET /tickets/{id} — get one")
    class GetOne {

        @Test
        @DisplayName("200 OK when ticket exists")
        void happyPath() throws Exception {
            when(ticketService.getTicket(TICKET_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/" + TICKET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TICKET_ID.toString()))
                    .andExpect(jsonPath("$.subject").value("Login broken"));
        }

        @Test
        @DisplayName("404 when ticket not found")
        void notFound() throws Exception {
            when(ticketService.getTicket(TICKET_ID))
                    .thenThrow(new TicketNotFoundException(TICKET_ID));

            mockMvc.perform(get(BASE_URL + "/" + TICKET_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Ticket not found: " + TICKET_ID));
        }
    }

    @Nested
    @DisplayName("PUT /tickets/{id} — update")
    class Update {

        @Test
        @DisplayName("200 OK with updated fields")
        void happyPath() throws Exception {
            var request = new UpdateTicketRequest("Updated subject", null, null, null);
            var updated = new TicketResponse(
                    TICKET_ID, TENANT_ID, "Updated subject", "Can't log in",
                    "NEW", null, null, null, null, null, 1,
                    OffsetDateTime.now(), OffsetDateTime.now()
            );
            when(ticketService.updateTicket(eq(TICKET_ID), any())).thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/" + TICKET_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject").value("Updated subject"));
        }
    }

    @Nested
    @DisplayName("PATCH /tickets/{id}/transition — state transition")
    class Transition {

        @Test
        @DisplayName("200 OK with legal transition")
        void happyPath() throws Exception {
            var request = new TransitionTicketRequest("CLASSIFIED");
            var transitioned = new TicketResponse(
                    TICKET_ID, TENANT_ID, "Login broken", null,
                    "CLASSIFIED", null, null, null, null, null, 1,
                    OffsetDateTime.now(), OffsetDateTime.now()
            );
            when(ticketService.transitionTicket(eq(TICKET_ID), any())).thenReturn(transitioned);

            mockMvc.perform(patch(BASE_URL + "/" + TICKET_ID + "/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLASSIFIED"));
        }

        @Test
        @DisplayName("409 Conflict for illegal transition")
        void illegalTransition() throws Exception {
            var request = new TransitionTicketRequest("CLOSED");
            when(ticketService.transitionTicket(eq(TICKET_ID), any()))
                    .thenThrow(new IllegalTicketTransitionException(
                            TicketStatus.NEW, TicketStatus.CLOSED));

            mockMvc.perform(patch(BASE_URL + "/" + TICKET_ID + "/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Conflict"));
        }
    }

    @Nested
    @DisplayName("DELETE /tickets/{id} — delete")
    class Delete {

        @Test
        @DisplayName("204 No Content on success")
        void happyPath() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 when ticket not found")
        void notFound() throws Exception {
            doThrow(new TicketNotFoundException(TICKET_ID))
                    .when(ticketService).deleteTicket(TICKET_ID);

            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
