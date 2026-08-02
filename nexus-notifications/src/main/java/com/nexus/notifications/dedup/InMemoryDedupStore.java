package com.nexus.notifications.dedup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory deduplication store for the Idempotent Consumer pattern.
 *
 * <p>Kafka guarantees at-least-once delivery, which means a consumer
 * may receive the same event twice (e.g., after a rebalance or crash
 * recovery). Without dedup, we'd send duplicate emails/Slack messages.
 *
 * <p>This implementation uses a thread-safe {@code ConcurrentHashMap} as
 * a set. The dedup key is "{ticketId}:{newStatus}" — so the same ticket
 * transitioning to ESCALATED twice produces the same key, and the second
 * attempt is silently skipped.
 *
 * <p><b>Limitations:</b> In-memory only — lost on restart. In production,
 * this would be backed by Redis ({@code SETNX} with TTL) or a dedicated
 * {@code processed_events} database table.
 */
@Component
public class InMemoryDedupStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDedupStore.class);

    private final Set<String> processedKeys = ConcurrentHashMap.newKeySet();

    /**
     * Attempts to mark an event as processed.
     *
     * @param dedupKey unique key for this event (e.g., "{ticketId}:{newStatus}")
     * @return true if this is the FIRST time we've seen this key (proceed),
     *         false if it's a duplicate (skip)
     */
    public boolean tryProcess(String dedupKey) {
        boolean isNew = processedKeys.add(dedupKey);
        if (!isNew) {
            log.warn("Duplicate event detected, skipping: {}", dedupKey);
        }
        return isNew;
    }
}