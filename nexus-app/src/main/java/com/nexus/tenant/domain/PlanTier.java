package com.nexus.tenant.domain;

/**
 * Subscription tier for a tenant — controls rate limits,
 * confidence thresholds, and feature access via the Strategy pattern.
 *
 * <p>Example: a PROFESSIONAL tenant might auto-resolve tickets at 75%
 * confidence, while a FREE tenant requires 90% to avoid wasting
 * limited AI quota on uncertain answers.
 */
public enum PlanTier {

    FREE,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE
}
