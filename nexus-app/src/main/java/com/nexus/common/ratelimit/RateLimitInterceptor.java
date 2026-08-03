package com.nexus.common.ratelimit;

import com.nexus.common.multitenancy.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that enforces per-tenant rate limiting
 * on all API endpoints.
 *
 * <p>This interceptor runs AFTER the Spring Security filter chain
 * and the TenantContextFilter, so by the time it executes:
 * <ul>
 *   <li>The user is authenticated (JWT validated)</li>
 *   <li>The tenant ID is available via {@link TenantContext}</li>
 * </ul>
 *
 * <p>If no tenant is set (unauthenticated request, public endpoint),
 * rate limiting is skipped — Spring Security handles those denials.
 *
 * <p>When the limit is exceeded, returns HTTP 429 Too Many Requests
 * with a JSON error body and a {@code Retry-After} header.
 *
 * <p>This bean is only created when Redis is available
 * (same condition as {@link SlidingWindowRateLimiter}).
 */
@Component
@ConditionalOnBean(RedisConnectionFactory.class)
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitStrategy rateLimitStrategy;

    public RateLimitInterceptor(RateLimitStrategy rateLimitStrategy) {
        this.rateLimitStrategy = rateLimitStrategy;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();

        // Skip rate limiting if no tenant is set (unauthenticated requests)
        if (tenantId == null) {
            return true;
        }

        RateLimitResult result = rateLimitStrategy.tryAcquire(tenantId);

        // Always set rate limit headers (standard practice for API consumers)
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            log.warn("Rate limit exceeded for tenant {} on {} {}",
                    tenantId, request.getMethod(), request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After",
                    String.valueOf(result.retryAfterMs() / 1000));

            response.getWriter().write("""
                    {
                      "error": "Too Many Requests",
                      "message": "Rate limit exceeded. Please try again later.",
                      "retryAfterSeconds": %d
                    }
                    """.formatted(result.retryAfterMs() / 1000));

            return false; // Block the request
        }

        return true; // Allow the request
    }
}
