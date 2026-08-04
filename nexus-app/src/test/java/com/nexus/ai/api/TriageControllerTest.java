package com.nexus.ai.api;

import com.nexus.ai.rag.KnowledgeBaseSearchService;
import com.nexus.ai.triage.TriageResult;
import com.nexus.ai.triage.TriageService;
import com.nexus.common.exception.TicketNotFoundException;
import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link TriageController} using {@code @WebMvcTest}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>POST /triage returns 200 with structured triage result</li>
 *   <li>POST /triage returns 404 when ticket not found</li>
 *   <li>POST /backfill-kb returns 200 with count</li>
 * </ul>
 *
 * <p>Security filters are disabled ({@code addFilters = false}) because
 * security is tested separately in {@code TicketSecurityTest}.
 */
@WebMvcTest(TriageController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TriageController")
class TriageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageService triageService;

    @MockitoBean
    private KnowledgeBaseSearchService knowledgeBaseSearchService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TICKET_ID = UUID.randomUUID();
    private static final String TRIAGE_URL =
            "/api/v1/tenants/" + TENANT_ID + "/tickets/" + TICKET_ID + "/triage";
    private static final String BACKFILL_URL =
            "/api/v1/tenants/" + TENANT_ID + "/tickets/backfill-kb";

    private TriageResult sampleResult() {
        return new TriageResult(
                TicketCategory.ACCOUNT, TicketPriority.MEDIUM,
                "Please check your spam folder for the password reset email.",
                "Customer is asking about password reset — common ACCOUNT issue.",
                0.87, true
        );
    }

    @Nested
    @DisplayName("POST /triage")
    class Triage {

        @Test
        @DisplayName("returns 200 with triage result")
        void returns200WithTriageResult() throws Exception {
            when(triageService.triageTicket(TICKET_ID)).thenReturn(sampleResult());

            mockMvc.perform(post(TRIAGE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.category").value("ACCOUNT"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.suggestedReply").value("Please check your spam folder for the password reset email."))
                    .andExpect(jsonPath("$.reasoning").value("Customer is asking about password reset — common ACCOUNT issue."))
                    .andExpect(jsonPath("$.confidenceScore").value(0.87))
                    .andExpect(jsonPath("$.autoResolvable").value(true));
        }

        @Test
        @DisplayName("returns 404 when ticket not found")
        void returns404WhenTicketNotFound() throws Exception {
            when(triageService.triageTicket(TICKET_ID))
                    .thenThrow(new TicketNotFoundException(TICKET_ID));

            mockMvc.perform(post(TRIAGE_URL))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /backfill-kb")
    class BackfillKb {

        @Test
        @DisplayName("returns 200 with backfill count")
        void returns200WithBackfillCount() throws Exception {
            when(knowledgeBaseSearchService.backfillEmbeddings()).thenReturn(5);

            mockMvc.perform(post(BACKFILL_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().string("5"));
        }
    }
}
