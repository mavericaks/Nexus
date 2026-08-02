package com.nexus.ticket.api;

import com.nexus.ticket.application.TicketService;
import com.nexus.ticket.application.dto.CreateTicketRequest;
import com.nexus.ticket.application.dto.TicketResponse;
import com.nexus.ticket.domain.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security tests for {@link TicketController} — validates RBAC, JWT auth,
 * and cross-tenant protection.
 *
 * <p>Unlike {@link TicketControllerTest} which uses {@code addFilters=false}
 * to test HTTP mapping in isolation, these tests <b>enable security filters</b>
 * so the full Spring Security filter chain is active.
 *
 * <p><b>How JWT is mocked:</b></p>
 * <p>Uses Spring Security Test's {@code SecurityMockMvcRequestPostProcessors.jwt()}
 * which injects a mock JWT authentication directly into the SecurityContext.
 * This avoids needing to generate real signed tokens, bypasses the decoder,
 * and lets us control exactly what claims and authorities are present.</p>
 *
 * <p><b>Why unauthenticated requests don't return 401:</b></p>
 * <p>Because {@code oauth2Login()} is configured, Spring Security's
 * {@code LoginUrlAuthenticationEntryPoint} intercepts unauthenticated GET
 * requests and redirects to Google (302). POST/DELETE without a session get
 * CSRF-rejected (403). This is correct production behavior — browsers get
 * redirected to Google login, API clients must send a Bearer token.</p>
 *
 * <p><b>Test categories:</b></p>
 * <ul>
 *   <li><b>Unauthenticated</b> — no token → rejected (302 redirect or 403 CSRF)</li>
 *   <li><b>RBAC</b> — AGENT can't delete (403), ADMIN/OWNER can</li>
 *   <li><b>Cross-tenant</b> — JWT tenant ≠ URL tenant → 403</li>
 *   <li><b>Happy path</b> — valid token, correct role → 200/201</li>
 * </ul>
 */
@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(com.nexus.common.security.SecurityConfig.class)
class TicketSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private com.nexus.common.security.oauth2.OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @MockitoBean
    private com.nexus.common.security.NexusUserDetailsService nexusUserDetailsService;

    @MockitoBean(name = "authDataSource")
    private javax.sql.DataSource authDataSource;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    // ─── Test data ──────────────────────────────────────────────────

    private static final UUID TENANT_ID = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("bbbb0000-0000-0000-0000-000000000002");
    private static final UUID TICKET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private static final String BASE_URL = "/api/v1/tenants/" + TENANT_ID + "/tickets";

    private TicketResponse sampleResponse() {
        return new TicketResponse(
                TICKET_ID, TENANT_ID, "Test ticket", "Description",
                TicketStatus.NEW.name(), "HIGH", "TECHNICAL",
                null, null, null, 0,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    // ═══════════════════════════════════════════════════════════════
    //  UNAUTHENTICATED — no token → 401
    //  Because we configured defaultAuthenticationEntryPointFor for
    //  /api/**, unauthenticated API requests correctly receive a 401
    //  Bearer challenge instead of the default oauth2Login 302 redirect.
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unauthenticated requests get 401")
    class Unauthenticated {

        @Test
        @DisplayName("GET without token → 401")
        void listWithoutToken_returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST without token → 401")
        void createWithoutToken_returns401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"subject\":\"test\",\"description\":\"d\",\"category\":\"BILLING\",\"priority\":\"LOW\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DELETE without token → 401")
        void deleteWithoutToken_returns401() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RBAC — role-based access control
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RBAC enforcement")
    class RbacEnforcement {

        @Test
        @DisplayName("AGENT can create tickets → 201")
        void agentCanCreate() throws Exception {
            when(ticketService.createTicket(eq(TENANT_ID), any(CreateTicketRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .claim("userId", USER_ID.toString())
                                    .subject("agent@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_AGENT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateTicketRequest("Bug report", "App crashes", "TECHNICAL", "HIGH"))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("AGENT can list tickets → 200")
        void agentCanList() throws Exception {
            when(ticketService.listTickets(any(), any(), any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            mockMvc.perform(get(BASE_URL)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("agent@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AGENT can get a ticket → 200")
        void agentCanGet() throws Exception {
            when(ticketService.getTicket(TICKET_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/" + TICKET_ID)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("agent@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AGENT cannot delete tickets → 403")
        void agentCannotDelete() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("agent@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_AGENT"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ADMIN can delete tickets → 204")
        void adminCanDelete() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("admin@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("OWNER can delete tickets → 204")
        void ownerCanDelete() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + TICKET_ID)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("owner@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_OWNER"))))
                    .andExpect(status().isNoContent());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CROSS-TENANT — JWT tenant ≠ URL tenant → 403
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cross-tenant protection")
    class CrossTenantProtection {

        @Test
        @DisplayName("JWT tenant ≠ URL tenant → 403")
        void crossTenantAccess_returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", OTHER_TENANT_ID.toString())
                                    .subject("admin@beta.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Same tenant access works → 200")
        void sameTenantAccess_works() throws Exception {
            when(ticketService.listTickets(any(), any(), any(), any()))
                    .thenReturn(org.springframework.data.domain.Page.empty());

            mockMvc.perform(get(BASE_URL)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("admin@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HAPPY PATH — valid token, correct role
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Authenticated happy paths")
    class AuthenticatedHappyPaths {

        @Test
        @DisplayName("Create ticket with valid token → 201 with response body")
        void createWithToken_returns201() throws Exception {
            when(ticketService.createTicket(eq(TENANT_ID), any(CreateTicketRequest.class)))
                    .thenReturn(sampleResponse());

            mockMvc.perform(post(BASE_URL)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("admin@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateTicketRequest("Bug report", "App crashes", "TECHNICAL", "HIGH"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()));
        }

        @Test
        @DisplayName("Get ticket with valid token → 200 with response body")
        void getWithToken_returns200() throws Exception {
            when(ticketService.getTicket(TICKET_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get(BASE_URL + "/" + TICKET_ID)
                            .with(jwt().jwt(j -> j
                                    .claim("tenantId", TENANT_ID.toString())
                                    .subject("admin@acme.com"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.subject").value("Test ticket"));
        }
    }
}
