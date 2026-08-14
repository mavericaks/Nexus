package com.nexus.analytics.application;

import com.nexus.ticket.infrastructure.persistence.TicketRepository;
import com.nexus.ticket.infrastructure.persistence.TicketSatisfactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for computing dashboard analytics and KPIs.
 *
 * <p>All queries go through RLS — results are automatically scoped
 * to the current tenant context.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TicketRepository ticketRepository;
    private final TicketSatisfactionRepository satisfactionRepository;

    public AnalyticsService(TicketRepository ticketRepository,
                            TicketSatisfactionRepository satisfactionRepository) {
        this.ticketRepository = ticketRepository;
        this.satisfactionRepository = satisfactionRepository;
    }

    /**
     * Returns a map of dashboard KPIs.
     *
     * <p>All metrics are tenant-scoped via RLS automatically.
     */
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        long totalTickets = ticketRepository.count();
        metrics.put("totalTickets", totalTickets);

        // Average CSAT score (null if no ratings yet)
        Double avgSatisfaction = satisfactionRepository.findAverageScore();
        metrics.put("averageSatisfaction", avgSatisfaction != null ? avgSatisfaction : 0.0);

        long totalRatings = satisfactionRepository.count();
        metrics.put("totalRatings", totalRatings);

        return metrics;
    }
}
