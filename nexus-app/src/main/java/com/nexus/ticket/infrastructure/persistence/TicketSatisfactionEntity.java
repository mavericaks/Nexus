package com.nexus.ticket.infrastructure.persistence;

import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for the {@code ticket_satisfaction} table — CSAT ratings.
 */
@Entity
@Table(name = "ticket_satisfaction")
public class TicketSatisfactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private TicketEntity ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TicketSatisfactionEntity() {
    }

    public TicketSatisfactionEntity(TicketEntity ticket, TenantEntity tenant,
                                     int score, String feedback) {
        this.ticket = ticket;
        this.tenant = tenant;
        this.score = score;
        this.feedback = feedback;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public TicketEntity getTicket() { return ticket; }
    public UUID getTicketId() { return ticket != null ? ticket.getId() : null; }
    public TenantEntity getTenant() { return tenant; }
    public int getScore() { return score; }
    public String getFeedback() { return feedback; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
