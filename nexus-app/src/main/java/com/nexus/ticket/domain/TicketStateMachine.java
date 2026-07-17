package com.nexus.ticket.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ticket lifecycle state machine — defines which transitions are legal.
 *
 * <p>This is the <strong>State pattern</strong>: {@link TicketStatus} names
 * the states (vocabulary), this class enforces which transitions are
 * allowed (grammar).
 *
 * <p>Pure Java — zero framework imports. Testable in milliseconds.
 *
 * <pre>
 * NEW → CLASSIFIED → AI_DRAFTED ─→ AUTO_RESOLVED → CLOSED
 *                                └→ ESCALATED → IN_PROGRESS → RESOLVED → CLOSED
 * </pre>
 */
public final class TicketStateMachine {

    /**
     * The full transition table. For each status, the set of statuses
     * it's allowed to move to. If a status isn't a key, it's terminal
     * (no outgoing transitions).
     */
    private static final Map<TicketStatus, Set<TicketStatus>> TRANSITIONS;

    static {
        var map = new EnumMap<TicketStatus, Set<TicketStatus>>(TicketStatus.class);

        // ── AI triage path ──────────────────────────────────────────
        map.put(TicketStatus.NEW,
                EnumSet.of(TicketStatus.CLASSIFIED));

        map.put(TicketStatus.CLASSIFIED,
                EnumSet.of(TicketStatus.AI_DRAFTED));

        map.put(TicketStatus.AI_DRAFTED,
                EnumSet.of(TicketStatus.AUTO_RESOLVED,
                           TicketStatus.ESCALATED));

        // ── Human agent path ────────────────────────────────────────
        map.put(TicketStatus.ESCALATED,
                EnumSet.of(TicketStatus.IN_PROGRESS));

        map.put(TicketStatus.IN_PROGRESS,
                EnumSet.of(TicketStatus.RESOLVED));

        // ── Closure ─────────────────────────────────────────────────
        map.put(TicketStatus.AUTO_RESOLVED,
                EnumSet.of(TicketStatus.CLOSED));

        map.put(TicketStatus.RESOLVED,
                EnumSet.of(TicketStatus.CLOSED));

        // CLOSED has no outgoing transitions — it's terminal.

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private TicketStateMachine() {
        // Utility class — no instances
    }

    /**
     * Check whether a transition from {@code from} to {@code to} is legal.
     *
     * @return true if the transition is allowed
     */
    public static boolean canTransition(TicketStatus from, TicketStatus to) {
        Set<TicketStatus> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    /**
     * Validate and perform a transition. Returns the target status
     * if the transition is legal, throws if not.
     *
     * @param from current status
     * @param to   desired status
     * @return the validated target status
     * @throws IllegalStateException if the transition is not allowed
     */
    public static TicketStatus transition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException(
                    "Illegal ticket transition: " + from + " → " + to
                    + ". Allowed from " + from + ": "
                    + TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TicketStatus.class)));
        }
        return to;
    }

    /**
     * Returns the set of statuses reachable from the given status.
     * Returns an empty set for terminal states.
     *
     * @param from current status
     * @return unmodifiable set of reachable statuses
     */
    public static Set<TicketStatus> allowedTransitions(TicketStatus from) {
        return TRANSITIONS.getOrDefault(from,
                Collections.unmodifiableSet(EnumSet.noneOf(TicketStatus.class)));
    }

    /**
     * Returns true if the given status is terminal (no outgoing transitions).
     */
    public static boolean isTerminal(TicketStatus status) {
        return !TRANSITIONS.containsKey(status);
    }
}
