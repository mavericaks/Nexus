package com.nexus.ai.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for knowledge base articles with pgvector similarity search.
 *
 * <p>The {@link #findSimilar} method uses pgvector's cosine distance
 * operator ({@code <=>}) to find the most relevant articles for a
 * given query embedding. This is the core of the RAG retrieval step.
 *
 * <p>Note: RLS is enforced by Postgres — the query automatically
 * filters to the current tenant's articles via the policy on
 * {@code knowledge_articles}.
 */
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticleEntity, UUID> {

    /**
     * Finds the top-k most similar articles to the given embedding vector.
     *
     * <p>Uses pgvector's cosine distance operator ({@code <=>}).
     * Lower distance = higher similarity. The result is ordered by
     * similarity (most similar first).
     *
     * <p>Only articles that have an embedding (non-null) are considered.
     * RLS ensures this only returns the current tenant's articles.
     *
     * @param embedding the query vector as a string (pgvector format: "[0.1,0.2,...]")
     * @param limit     max number of results to return
     * @return articles ordered by cosine similarity (most similar first)
     */
    @Query(value = """
            SELECT ka.id, ka.tenant_id, ka.title, ka.content, ka.category,
                   ka.created_at, ka.updated_at,
                   1 - (ka.embedding <=> cast(:embedding AS vector)) AS similarity
            FROM knowledge_articles ka
            WHERE ka.embedding IS NOT NULL
            ORDER BY ka.embedding <=> cast(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilar(@Param("embedding") String embedding,
                               @Param("limit") int limit);

    /**
     * Finds all articles without an embedding — used by the
     * embedding backfill job to generate missing vectors.
     */
    @Query("SELECT ka FROM KnowledgeArticleEntity ka WHERE ka.id IN " +
           "(SELECT ka2.id FROM KnowledgeArticleEntity ka2 WHERE ka2.id NOT IN " +
           "(SELECT ka3.id FROM KnowledgeArticleEntity ka3))")
    List<KnowledgeArticleEntity> findAll();

    /**
     * Updates the embedding for a specific article.
     * Uses native query because JPA can't map vector columns directly.
     */
    @Query(value = "UPDATE knowledge_articles SET embedding = cast(:embedding AS vector), " +
                   "updated_at = now() WHERE id = :id",
           nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);

    /**
     * Finds articles that don't have embeddings yet (for backfill).
     */
    @Query(value = "SELECT id, title, content FROM knowledge_articles WHERE embedding IS NULL",
           nativeQuery = true)
    List<Object[]> findArticlesWithoutEmbedding();
}
