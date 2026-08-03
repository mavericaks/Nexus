package com.nexus.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Caching configuration — enables Spring's {@code @Cacheable} abstraction
 * backed by Redis.
 *
 * <p>Cache names and their TTLs:
 * <ul>
 *   <li>{@code rag_search} — 1 hour TTL. Caches RAG similarity search results
 *       to avoid hitting the embedding API + pgvector for identical queries.
 *       An hour is safe because KB articles change infrequently.</li>
 * </ul>
 *
 * <p>Values are serialized as JSON (not Java serialization) so they're
 * human-readable in Redis and not coupled to class versions.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    /** Cache name for RAG search results. */
    public static final String RAG_SEARCH_CACHE = "rag_search";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis cache manager");

        // Default config: JSON serialization, 30-minute TTL
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(30));

        // Per-cache overrides
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                RAG_SEARCH_CACHE, defaults.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
