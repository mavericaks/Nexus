package com.nexus.common.ratelimit;

/**
 * Result of a rate limit check.
 *
 * @param allowed       whether the request is permitted
 * @param remaining     how many requests remain in the current window
 * @param retryAfterMs  if denied, how many milliseconds until the window resets
 *                      (used for the Retry-After header). 0 if allowed.
 */
public record RateLimitResult(
        boolean allowed,
        long remaining,
        long retryAfterMs
) {
    /** Convenience factory for an allowed result. */
    public static RateLimitResult allowed(long remaining) {
        return new RateLimitResult(true, remaining, 0);
    }

    /** Convenience factory for a denied result. */
    public static RateLimitResult denied(long retryAfterMs) {
        return new RateLimitResult(false, 0, retryAfterMs);
    }
}
