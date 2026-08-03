package com.nexus.common.config;

import com.nexus.common.ratelimit.RateLimitInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration — registers interceptors for API endpoints.
 *
 * <p>Currently registers the {@link RateLimitInterceptor} on all
 * {@code /api/**} paths. The interceptor enforces per-tenant rate
 * limiting using Redis sliding window counters.
 *
 * <p>This configuration is only active when Redis is available.
 * In test environments where Redis auto-configuration is excluded,
 * no rate limiting interceptor is registered.
 */
@Configuration
@ConditionalOnBean(RedisConnectionFactory.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
