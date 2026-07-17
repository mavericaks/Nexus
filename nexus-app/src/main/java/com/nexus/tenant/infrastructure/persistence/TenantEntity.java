package com.nexus.tenant.infrastructure.persistence;

import com.nexus.tenant.domain.PlanTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code tenants} table.
 *
 * <p>Lives in {@code infrastructure}, not {@code domain}, because it
 * carries framework annotations ({@code @Entity}, {@code @Column}).
 * The domain layer knows nothing about JPA — this entity is the
 * persistence adapter.
 */
@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false, length = 50)
    private PlanTier planTier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ─── JPA requires a no-arg constructor ──────────────────────────
    protected TenantEntity() {
    }

    public TenantEntity(String name, String slug, PlanTier planTier) {
        this.name = name;
        this.slug = slug;
        this.planTier = planTier;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // ─── Getters ────────────────────────────────────────────────────

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public PlanTier getPlanTier() {
        return planTier;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ─── Setters (only for mutable fields) ──────────────────────────

    public void setName(String name) {
        this.name = name;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setPlanTier(PlanTier planTier) {
        this.planTier = planTier;
        this.updatedAt = OffsetDateTime.now();
    }
}
