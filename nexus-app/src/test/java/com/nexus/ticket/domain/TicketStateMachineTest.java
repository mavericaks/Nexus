package com.nexus.ticket.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TicketStateMachine}.
 *
 * <p>No Spring context needed — pure Java, runs in milliseconds.
 * This is exactly why domain logic stays framework-free.
 */
class TicketStateMachineTest {

    // ─── Happy path: every legal transition ─────────────────────────
    @Nested
    @DisplayName("Legal transitions")
    class LegalTransitions {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "NEW,           CLASSIFIED",
                "CLASSIFIED,    AI_DRAFTED",
                "AI_DRAFTED,    AUTO_RESOLVED",
                "AI_DRAFTED,    ESCALATED",
                "ESCALATED,     IN_PROGRESS",
                "IN_PROGRESS,   RESOLVED",
                "AUTO_RESOLVED, CLOSED",
                "RESOLVED,      CLOSED"
        })
        void shouldAllowLegalTransition(TicketStatus from, TicketStatus to) {
            assertTrue(TicketStateMachine.canTransition(from, to),
                    from + " → " + to + " should be allowed");

            // transition() should return the target status without throwing
            assertEquals(to, TicketStateMachine.transition(from, to));
        }
    }

    // ─── Illegal transitions ────────────────────────────────────────
    @Nested
    @DisplayName("Illegal transitions")
    class IllegalTransitions {

        @ParameterizedTest(name = "{0} → {1} should be blocked")
        @CsvSource({
                "NEW,           RESOLVED",
                "NEW,           CLOSED",
                "CLASSIFIED,    NEW",
                "CLOSED,        NEW",
                "CLOSED,        CLASSIFIED",
                "AUTO_RESOLVED, NEW",
                "IN_PROGRESS,   CLASSIFIED",
                "ESCALATED,     AUTO_RESOLVED"
        })
        void shouldBlockIllegalTransition(TicketStatus from, TicketStatus to) {
            assertFalse(TicketStateMachine.canTransition(from, to),
                    from + " → " + to + " should be blocked");
        }

        @Test
        @DisplayName("transition() throws for illegal move")
        void transitionThrowsForIllegalMove() {
            var ex = assertThrows(IllegalStateException.class,
                    () -> TicketStateMachine.transition(TicketStatus.NEW, TicketStatus.CLOSED));

            assertTrue(ex.getMessage().contains("Illegal ticket transition"),
                    "Error message should describe the illegal transition");
            assertTrue(ex.getMessage().contains("NEW"),
                    "Error message should mention the source state");
        }
    }

    // ─── Terminal states ────────────────────────────────────────────
    @Nested
    @DisplayName("Terminal states")
    class TerminalStates {

        @Test
        @DisplayName("CLOSED is terminal")
        void closedIsTerminal() {
            assertTrue(TicketStateMachine.isTerminal(TicketStatus.CLOSED));
            assertTrue(TicketStateMachine.allowedTransitions(TicketStatus.CLOSED).isEmpty());
        }

        @Test
        @DisplayName("NEW is not terminal")
        void newIsNotTerminal() {
            assertFalse(TicketStateMachine.isTerminal(TicketStatus.NEW));
            assertFalse(TicketStateMachine.allowedTransitions(TicketStatus.NEW).isEmpty());
        }
    }

    // ─── Allowed transitions query ──────────────────────────────────
    @Nested
    @DisplayName("allowedTransitions()")
    class AllowedTransitions {

        @Test
        @DisplayName("AI_DRAFTED can go to AUTO_RESOLVED or ESCALATED")
        void aiDraftedHasTwoOptions() {
            var allowed = TicketStateMachine.allowedTransitions(TicketStatus.AI_DRAFTED);
            assertEquals(2, allowed.size());
            assertTrue(allowed.contains(TicketStatus.AUTO_RESOLVED));
            assertTrue(allowed.contains(TicketStatus.ESCALATED));
        }
    }
}
