package com.nexus.common.ratelimit;

/**
 * Strategy interface for rate limiting — one of the playbook's explicit
 * requirements (§5 Phase 6: "per-tenant rate limiting keyed to plan tier,
 * Strategy pattern").
 *
 * <p>The Strategy pattern allows swapping rate-limiting algorithms without
 * changing the interceptor. Current implementation: sliding window counter
 * backed by Redis. Future implementations could include token bucket,
 * leaky bucket, or different limits per plan tier.
 *
 * <p>Each implementation is expected to be thread-safe and stateless
 * (state lives in Redis, not in-memory).
 */
public interface RateLimitStrategy {

    /**
     * Attempts to acquire a permit for the given key (typically tenant ID).
     *
     * @param key the rate limit key (e.g., tenant ID)
     * @return a {@link RateLimitResult} indicating whether the request is allowed
     */
    RateLimitResult tryAcquire(String key);
}
