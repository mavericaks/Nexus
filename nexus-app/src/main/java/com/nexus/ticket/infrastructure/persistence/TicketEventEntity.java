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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for the {@code ticket_events} table — immutable audit trail.
 *
 * <p>Each event records a single mutation in a ticket's lifecycle:
 * creation, status change, triage result, field update, note added, etc.
 *
 * <p>Events are append-only — never updated or deleted.
 */
@Entity
@Table(name = "ticket_events")
public class TicketEventEntity {

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

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name")
    private String actorName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ─── JPA requires a no-arg constructor ──────────────────────────
    protected TicketEventEntity() {
    }

    public TicketEventEntity(TicketEntity ticket, TenantEntity tenant, String eventType,
                             UUID actorId, String actorName, Map<String, Object> details) {
        this.ticket = ticket;
        this.tenant = tenant;
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorName = actorName;
        this.details = details;
        this.createdAt = OffsetDateTime.now();
    }

    // ─── Getters ────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public TicketEntity getTicket() { return ticket; }
    public UUID getTicketId() { return ticket != null ? ticket.getId() : null; }
    public TenantEntity getTenant() { return tenant; }
    public String getEventType() { return eventType; }
    public UUID getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public Map<String, Object> getDetails() { return details; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
