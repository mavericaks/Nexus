package com.nexus.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sliding window rate limiter backed by Redis.
 *
 * <p>Uses a Lua script executed atomically on Redis to implement a
 * sliding window counter. This is the standard production pattern because:
 * <ul>
 *   <li>Atomic — the INCR + EXPIRE + check happens in one round-trip</li>
 *   <li>Distributed — works across multiple app instances</li>
 *   <li>Tenant-isolated — each tenant gets its own Redis key</li>
 *   <li>Self-cleaning — keys expire automatically after the window</li>
 * </ul>
 *
 * <p>The Lua script increments a counter for the tenant's current window.
 * If it's the first request in the window, it sets a TTL equal to the
 * window duration. If the counter exceeds the limit, the request is denied.
 *
 * <p>This bean is only created when a {@link RedisConnectionFactory} is
 * available. In test environments where Redis is excluded, this bean
 * (and the interceptor that depends on it) won't be created.
 *
 * <p>Configuration is via application.yml:
 * <ul>
 *   <li>{@code nexus.rate-limit.requests-per-window} — max requests allowed</li>
 *   <li>{@code nexus.rate-limit.window-seconds} — window duration in seconds</li>
 * </ul>
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class SlidingWindowRateLimiter implements RateLimitStrategy {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final int windowSeconds;

    /**
     * Lua script for atomic sliding window rate limiting.
     *
     * KEYS[1] = rate limit key (e.g., "rate_limit:tenant:{tenantId}")
     * ARGV[1] = max requests per window
     * ARGV[2] = window duration in seconds
     *
     * Returns: [current_count, ttl_remaining_ms]
     */
    private static final RedisScript<List> RATE_LIMIT_SCRIPT = RedisScript.of(
            """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = tonumber(redis.call('GET', key) or '0')
            if current >= limit then
                local ttl = redis.call('PTTL', key)
                return {current, ttl}
            end
            current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            local ttl = redis.call('PTTL', key)
            return {current, ttl}
            """,
            List.class
    );

    public SlidingWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${nexus.rate-limit.requests-per-window:100}") int maxRequests,
            @Value("${nexus.rate-limit.window-seconds:60}") int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        log.info("Rate limiter configured: {} requests per {} seconds", maxRequests, windowSeconds);
    }

    @Override
    public RateLimitResult tryAcquire(String key) {
        String redisKey = "rate_limit:tenant:" + key;

        @SuppressWarnings("unchecked")
        List<Long> result = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(redisKey),
                String.valueOf(maxRequests),
                String.valueOf(windowSeconds)
        );

        if (result == null || result.size() < 2) {
            // Redis unavailable — fail open (allow the request)
            log.warn("Rate limit check failed (Redis unavailable?) — allowing request for key: {}", key);
            return RateLimitResult.allowed(maxRequests);
        }

        long currentCount = result.get(0);
        long ttlMs = result.get(1);

        if (currentCount > maxRequests) {
            log.info("Rate limit exceeded for tenant {}: {}/{} requests, retry after {}ms",
                    key, currentCount, maxRequests, ttlMs);
            return RateLimitResult.denied(ttlMs > 0 ? ttlMs : windowSeconds * 1000L);
        }

        long remaining = maxRequests - currentCount;
        log.debug("Rate limit OK for tenant {}: {}/{} requests ({} remaining)",
                key, currentCount, maxRequests, remaining);
        return RateLimitResult.allowed(remaining);
    }
}
