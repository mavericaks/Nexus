package com.nexus.ticket.infrastructure.persistence;

import com.nexus.ticket.domain.TicketCategory;
import com.nexus.ticket.domain.TicketPriority;
import com.nexus.ticket.domain.TicketStatus;
import com.nexus.tenant.infrastructure.persistence.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code tickets} table.
 *
 * <p>Lives in {@code infrastructure}, not {@code domain}, because it
 * imports JPA annotations. The domain enums ({@link TicketStatus},
 * {@link TicketPriority}, {@link TicketCategory}) are imported here
 * — the dependency direction is infrastructure → domain, never the
 * reverse.
 *
 * <p>Uses {@code @Version} for optimistic locking — two agents
 * editing the same ticket will get a clear conflict error instead
 * of silently overwriting each other.
 */
@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The tenant this ticket belongs to.
     * LAZY fetch — we don't load the full tenant object on every
     * ticket query. Access the tenant ID via {@link #getTenantId()}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 50)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private TicketCategory category;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    /**
     * Optimistic locking version — Hibernate increments this on every
     * update. If two concurrent writes target the same version,
     * the second one throws {@code OptimisticLockException}.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ─── JPA requires a no-arg constructor ──────────────────────────
    protected TicketEntity() {
    }

    public TicketEntity(TenantEntity tenant, String subject, String description) {
        this.tenant = tenant;
        this.subject = subject;
        this.description = description;
        this.status = TicketStatus.NEW;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // ─── Getters ────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public UUID getTenantId() {
        // Avoids loading the full tenant entity just to get the ID.
        // Hibernate extracts the FK value without a query.
        return tenant != null ? tenant.getId() : null;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public Integer getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ─── Setters (only for mutable fields) ──────────────────────────

    public void setSubject(String subject) {
        this.subject = subject;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
        this.updatedAt = OffsetDateTime.now();
    }
}
