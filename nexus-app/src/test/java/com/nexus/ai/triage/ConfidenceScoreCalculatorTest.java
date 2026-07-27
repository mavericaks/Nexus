package com.nexus.ai.triage;

import com.nexus.ai.rag.RetrievedArticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ConfidenceScoreCalculator}.
 *
 * <p>These test the confidence derivation logic — NOT raw LLM self-report.
 * The calculator combines three signals: RAG similarity, parse success,
 * and category agreement.
 *
 * <p>Pure domain logic — no Spring context, no mocks, millisecond tests.
 */
@DisplayName("ConfidenceScoreCalculator")
class ConfidenceScoreCalculatorTest {

    private static final String BILLING_CATEGORY = "BILLING";
    private static final String ACCOUNT_CATEGORY = "ACCOUNT";

    private static RetrievedArticle article(String category, double similarity) {
        return new RetrievedArticle("Test Article", "content", category, similarity);
    }

    @Nested
    @DisplayName("High confidence scenarios")
    class HighConfidence {

        @Test
        @DisplayName("High RAG similarity + successful parse + category match → high confidence")
        void allSignalsStrong() {
            var articles = List.of(
                    article(BILLING_CATEGORY, 0.92),
                    article(BILLING_CATEGORY, 0.85)
            );

            double score = ConfidenceScoreCalculator.calculate(articles, true, BILLING_CATEGORY);

            // RAG: avg(0.92, 0.85) = 0.885 * 0.50 = 0.4425
            // Parse: 1.0 * 0.25 = 0.25
            // Category: 2/2 match * 0.25 = 0.25
            // Total: 0.9425
            assertTrue(score > 0.9, "Expected high confidence, got " + score);
        }
    }

    @Nested
    @DisplayName("Medium confidence scenarios")
    class MediumConfidence {

        @Test
        @DisplayName("Good RAG + parse success + no category match → medium confidence")
        void noCategoryMatch() {
            var articles = List.of(
                    article(BILLING_CATEGORY, 0.80),
                    article(BILLING_CATEGORY, 0.75)
            );

            double score = ConfidenceScoreCalculator.calculate(articles, true, ACCOUNT_CATEGORY);

            // RAG: avg(0.80, 0.75) = 0.775 * 0.50 = 0.3875
            // Parse: 1.0 * 0.25 = 0.25
            // Category: 0/2 match * 0.25 = 0.0
            // Total: 0.6375
            assertTrue(score > 0.5 && score < 0.75, "Expected medium confidence, got " + score);
        }

        @Test
        @DisplayName("Moderate RAG + parse success + full category match → medium-high")
        void moderateRag() {
            var articles = List.of(
                    article(ACCOUNT_CATEGORY, 0.60),
                    article(ACCOUNT_CATEGORY, 0.55)
            );

            double score = ConfidenceScoreCalculator.calculate(articles, true, ACCOUNT_CATEGORY);

            // RAG: avg(0.60, 0.55) = 0.575 * 0.50 = 0.2875
            // Parse: 1.0 * 0.25 = 0.25
            // Category: 2/2 match * 0.25 = 0.25
            // Total: 0.7875
            assertTrue(score > 0.7 && score < 0.85, "Expected medium-high confidence, got " + score);
        }
    }

    @Nested
    @DisplayName("Low confidence scenarios")
    class LowConfidence {

        @Test
        @DisplayName("No KB articles found → low confidence")
        void noArticles() {
            double score = ConfidenceScoreCalculator.calculate(List.of(), true, BILLING_CATEGORY);

            // RAG: 0.0 * 0.50 = 0.0
            // Parse: 1.0 * 0.25 = 0.25
            // Category: 0.0 * 0.25 = 0.0 (no articles to compare)
            // Total: 0.25
            assertEquals(0.25, score, 0.001);
        }

        @Test
        @DisplayName("Parse failed → reduced confidence")
        void parseFailed() {
            var articles = List.of(article(BILLING_CATEGORY, 0.80));

            double score = ConfidenceScoreCalculator.calculate(articles, false, BILLING_CATEGORY);

            // RAG: 0.80 * 0.50 = 0.40
            // Parse: 0.0 * 0.25 = 0.0
            // Category: 1/1 match * 0.25 = 0.25
            // Total: 0.65
            assertTrue(score < 0.7, "Expected lower confidence due to parse failure, got " + score);
        }

        @Test
        @DisplayName("No articles + parse failed → minimal confidence")
        void worstCase() {
            double score = ConfidenceScoreCalculator.calculate(List.of(), false, null);

            assertEquals(0.0, score, 0.001);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Null category → category score is 0")
        void nullCategory() {
            var articles = List.of(article(BILLING_CATEGORY, 0.80));

            double score = ConfidenceScoreCalculator.calculate(articles, true, null);

            // RAG: 0.80 * 0.50 = 0.40
            // Parse: 1.0 * 0.25 = 0.25
            // Category: null → 0.0 * 0.25 = 0.0
            // Total: 0.65
            assertEquals(0.65, score, 0.001);
        }

        @Test
        @DisplayName("Score is always between 0.0 and 1.0")
        void clampedRange() {
            var articles = List.of(
                    article(BILLING_CATEGORY, 1.0),
                    article(BILLING_CATEGORY, 1.0)
            );

            double score = ConfidenceScoreCalculator.calculate(articles, true, BILLING_CATEGORY);

            assertTrue(score >= 0.0 && score <= 1.0, "Score out of range: " + score);
        }
    }
}
