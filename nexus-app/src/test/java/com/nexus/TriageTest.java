package com.nexus;

import com.nexus.ai.triage.TriageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class TriageTest {

    @Autowired
    private TriageService triageService;

    @Test
    public void testTriage() {
        UUID ticketId = UUID.fromString("8ccd0ed4-bf91-46ec-8f9d-37e9bbc65517");
        try {
            triageService.triageTicket(ticketId);
            System.out.println("TRIAGE SUCCESSFUL!");
        } catch (Exception e) {
            System.out.println("TRIAGE FAILED!");
            e.printStackTrace();
            throw e;
        }
    }
}
