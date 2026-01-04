package io.github.ajuarez0021.redis.reactive.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * The Class ReactiveCacheManager.
 */
@Getter
@AllArgsConstructor
public class ReactiveCacheManager {
    
    /** The ttl entries. */
    private Map<String, Long> ttlEntries;

    /**
     * Gets the ttl.
     *
     * @param name the name
     * @return the ttl
     */
    public Long getTTL(String name) {
        return ttlEntries.get(name);
    }

    /**
     * Gets the TTL or default.
     *
     * @param name the name
     * @param ttl the ttl
     * @return the TTL or default
     */
    public Long getTTLOrDefault(String name, long ttl) {
        return ttlEntries.getOrDefault(name, ttl);
    }
}
