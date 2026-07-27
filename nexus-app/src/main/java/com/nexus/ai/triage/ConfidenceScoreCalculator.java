package com.nexus.ai.triage;

import com.nexus.ai.rag.RetrievedArticle;

import java.util.List;

/**
 * Derives a confidence score from measurable signals — NOT from
 * asking the LLM "how confident are you" (which is a known
 * miscalibration failure mode per guardrails §9.3).
 *
 * <p>The score combines three independent signals:
 * <ol>
 *   <li><b>RAG similarity</b> — did the knowledge base have relevant
 *       content? High similarity = the KB covers this topic.</li>
 *   <li><b>Parse success</b> — did the LLM output valid structured
 *       JSON with all required fields? Failed parse = low confidence.</li>
 *   <li><b>Category agreement</b> — does the AI's chosen category
 *       match the KB article's category? Agreement = higher confidence.</li>
 * </ol>
 */
public final class ConfidenceScoreCalculator {

    // Weights for each signal (must sum to 1.0)
    private static final double RAG_WEIGHT = 0.50;
    private static final double PARSE_WEIGHT = 0.25;
    private static final double CATEGORY_WEIGHT = 0.25;

    private ConfidenceScoreCalculator() {
        // utility class
    }

    /**
     * Calculates a composite confidence score from measurable signals.
     *
     * @param retrievedArticles the articles found by RAG retrieval
     * @param parseSucceeded    whether the LLM response parsed into valid structured output
     * @param aiCategory        the category the AI chose
     * @return confidence score between 0.0 and 1.0
     */
    public static double calculate(List<RetrievedArticle> retrievedArticles,
                                    boolean parseSucceeded,
                                    String aiCategory) {

        // Signal 1: RAG retrieval quality
        // Average similarity of top retrieved articles (0.0 if none found)
        double ragScore = retrievedArticles.isEmpty() ? 0.0 :
                retrievedArticles.stream()
                        .mapToDouble(RetrievedArticle::similarityScore)
                        .average()
                        .orElse(0.0);

        // Signal 2: Parse success (binary — either the output parsed or it didn't)
        double parseScore = parseSucceeded ? 1.0 : 0.0;

        // Signal 3: Category agreement
        // Does the AI's category match any of the retrieved articles' categories?
        double categoryScore = 0.0;
        if (aiCategory != null && !retrievedArticles.isEmpty()) {
            long matchingArticles = retrievedArticles.stream()
                    .filter(a -> a.category() != null &&
                                 a.category().equalsIgnoreCase(aiCategory))
                    .count();
            categoryScore = (double) matchingArticles / retrievedArticles.size();
        }

        // Composite score
        double composite = (RAG_WEIGHT * ragScore) +
                           (PARSE_WEIGHT * parseScore) +
                           (CATEGORY_WEIGHT * categoryScore);

        // Clamp to [0.0, 1.0]
        return Math.max(0.0, Math.min(1.0, composite));
    }
}
