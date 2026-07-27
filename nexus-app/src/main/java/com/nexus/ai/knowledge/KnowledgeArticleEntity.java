package com.nexus.ai.knowledge;

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
 * JPA entity mapping to the {@code knowledge_articles} table.
 *
 * <p>Each article belongs to a tenant (RLS-protected) and contains
 * a text embedding vector for similarity search via pgvector.
 *
 * <p>The {@code embedding} field is stored as a raw float array.
 * pgvector's JDBC driver handles the conversion to/from the
 * {@code vector(768)} column type.
 */
@Entity
@Table(name = "knowledge_articles")
public class KnowledgeArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private TenantEntity tenant;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "category", length = 50)
    private String category;

    /**
     * The embedding vector is not mapped via JPA — pgvector columns
     * are handled via native queries in the repository.
     * This avoids needing a custom Hibernate type for vector(768).
     */

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // ─── JPA requires a no-arg constructor ──────────────────────────
    protected KnowledgeArticleEntity() {
    }

    public KnowledgeArticleEntity(TenantEntity tenant, String title, String content, String category) {
        this.tenant = tenant;
        this.title = title;
        this.content = content;
        this.category = category;
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
        return tenant != null ? tenant.getId() : null;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
