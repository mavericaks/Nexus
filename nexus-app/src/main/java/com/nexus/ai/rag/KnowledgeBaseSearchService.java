package com.nexus.ai.rag;

import com.nexus.ai.config.AiProperties;
import com.nexus.ai.embedding.EmbeddingService;
import com.nexus.ai.knowledge.KnowledgeArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG retrieval service — searches the knowledge base for articles
 * relevant to a given query using vector similarity search.
 *
 * <p>Flow: query text → embedding → pgvector cosine search → ranked articles.
 *
 * <p>The similarity scores from this service are a key input to the
 * confidence score derivation. High similarity scores (above 0.7)
 * mean the knowledge base has relevant content, which increases
 * confidence in the AI's response.
 */
@Service
public class KnowledgeBaseSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseSearchService.class);

    private final EmbeddingService embeddingService;
    private final KnowledgeArticleRepository articleRepository;
    private final AiProperties aiProperties;

    public KnowledgeBaseSearchService(EmbeddingService embeddingService,
                                      KnowledgeArticleRepository articleRepository,
                                      AiProperties aiProperties) {
        this.embeddingService = embeddingService;
        this.articleRepository = articleRepository;
        this.aiProperties = aiProperties;
    }

    /**
     * Searches the knowledge base for articles similar to the given query.
     *
     * @param query the search query (typically ticket subject + description)
     * @return list of retrieved articles, ordered by similarity (best first)
     */
    public List<RetrievedArticle> search(String query) {
        log.info("Searching knowledge base for: '{}'", truncate(query, 100));

        // Step 1: Convert query to embedding vector
        List<Float> queryEmbedding = embeddingService.embed(query);

        // Step 2: Format as pgvector string (e.g., "[0.1,0.2,0.3,...]")
        String vectorString = toPgVectorString(queryEmbedding);

        // Step 3: Run similarity search via pgvector
        List<Object[]> results = articleRepository.findSimilar(
                vectorString, aiProperties.maxRetrievalResults());

        // Step 4: Map to RetrievedArticle value objects
        List<RetrievedArticle> articles = results.stream()
                .map(row -> new RetrievedArticle(
                        (String) row[2],   // title
                        (String) row[3],   // content
                        (String) row[4],   // category
                        ((Number) row[7]).doubleValue()  // similarity score
                ))
                .toList();

        log.info("Found {} relevant articles (top similarity: {})",
                articles.size(),
                articles.isEmpty() ? "N/A" :
                        String.format("%.2f", articles.getFirst().similarityScore()));

        return articles;
    }

    /**
     * Generates and stores an embedding for a knowledge base article.
     * Used during initial seeding and when new articles are added.
     *
     * @param articleId the article ID
     * @param text      the text to embed (typically title + content)
     */
    public void embedArticle(UUID articleId, String text) {
        List<Float> embedding = embeddingService.embed(text);
        String vectorString = toPgVectorString(embedding);
        articleRepository.updateEmbedding(articleId, vectorString);
        log.debug("Stored embedding for article {}", articleId);
    }

    /**
     * Backfills embeddings for any articles that don't have one yet.
     * Called at startup or via a scheduled job.
     */
    public int backfillEmbeddings() {
        List<Object[]> articlesWithout = articleRepository.findArticlesWithoutEmbedding();
        log.info("Found {} articles without embeddings, backfilling...", articlesWithout.size());

        int count = 0;
        for (Object[] row : articlesWithout) {
            UUID id = (UUID) row[0];
            String title = (String) row[1];
            String content = (String) row[2];
            embedArticle(id, title + " " + content);
            count++;
        }

        log.info("Backfilled {} embeddings", count);
        return count;
    }

    /**
     * Converts a list of floats to pgvector string format: "[0.1,0.2,0.3]"
     */
    static String toPgVectorString(List<Float> vector) {
        return "[" + vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
