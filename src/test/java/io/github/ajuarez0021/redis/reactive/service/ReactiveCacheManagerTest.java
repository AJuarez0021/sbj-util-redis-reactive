package io.github.ajuarez0021.redis.reactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class ReactiveCacheManagerTest.
 */
class ReactiveCacheManagerTest {

    /** The ttl entries. */
    private Map<String, Long> ttlEntries;

    /** The cache manager. */
    private ReactiveCacheManager cacheManager;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        ttlEntries = new HashMap<>();
        ttlEntries.put("users", 30L);
        ttlEntries.put("products", 60L);
        ttlEntries.put("orders", 15L);

        cacheManager = new ReactiveCacheManager(ttlEntries);
    }

    /**
     * Gets the TT L returns configured TTL.
     *
     * @return the TT L returns configured TTL
     */
    @Test
    void getTTL_ReturnsConfiguredTTL() {
        // When
        Long usersTTL = cacheManager.getTTL("users");
        Long productsTTL = cacheManager.getTTL("products");
        Long ordersTTL = cacheManager.getTTL("orders");

        // Then
        assertEquals(30L, usersTTL);
        assertEquals(60L, productsTTL);
        assertEquals(15L, ordersTTL);
    }

    /**
     * Gets the TT L with non existent cache returns null.
     *
     * @return the TT L with non existent cache returns null
     */
    @Test
    void getTTL_WithNonExistentCache_ReturnsNull() {
        // When
        Long ttl = cacheManager.getTTL("nonexistent");

        // Then
        assertNull(ttl);
    }

    /**
     * Gets the TT L or default with existing cache returns configured TTL.
     *
     * @return the TT L or default with existing cache returns configured TTL
     */
    @Test
    void getTTLOrDefault_WithExistingCache_ReturnsConfiguredTTL() {
        // When
        Long ttl = cacheManager.getTTLOrDefault("users", 10L);

        // Then
        assertEquals(30L, ttl);
    }

    /**
     * Gets the TT L or default with non existent cache returns default.
     *
     * @return the TT L or default with non existent cache returns default
     */
    @Test
    void getTTLOrDefault_WithNonExistentCache_ReturnsDefault() {
        // When
        Long ttl = cacheManager.getTTLOrDefault("nonexistent", 10L);

        // Then
        assertEquals(10L, ttl);
    }

    /**
     * Gets the ttl entries returns all entries.
     *
     * @return the ttl entries returns all entries
     */
    @Test
    void getTtlEntries_ReturnsAllEntries() {
        // When
        Map<String, Long> entries = cacheManager.getTtlEntries();

        // Then
        assertNotNull(entries);
        assertEquals(3, entries.size());
        assertEquals(30L, entries.get("users"));
        assertEquals(60L, entries.get("products"));
        assertEquals(15L, entries.get("orders"));
    }

    /**
     * Reactive cache manager with empty map handles gracefully.
     */
    @Test
    void reactiveCacheManager_WithEmptyMap_HandlesGracefully() {
        // Given
        ReactiveCacheManager emptyCacheManager = new ReactiveCacheManager(new HashMap<>());

        // When
        Long ttl = emptyCacheManager.getTTL("anyCache");
        Long ttlOrDefault = emptyCacheManager.getTTLOrDefault("anyCache", 20L);

        // Then
        assertNull(ttl);
        assertEquals(20L, ttlOrDefault);
    }

    /**
     * Reactive cache manager with null values handles correctly.
     */
    @Test
    void reactiveCacheManager_WithNullValues_HandlesCorrectly() {
        // Given
        Map<String, Long> entriesWithNull = new HashMap<>();
        entriesWithNull.put("cache1", 25L);
        entriesWithNull.put("cache2", null);

        ReactiveCacheManager managerWithNull = new ReactiveCacheManager(entriesWithNull);

        // When
        Long ttl1 = managerWithNull.getTTL("cache1");
        Long ttl2 = managerWithNull.getTTL("cache2");
        Long ttl3 = managerWithNull.getTTLOrDefault("cache2", 15L);

        // Then
        assertEquals(25L, ttl1);
        assertNull(ttl2);
        assertNull(ttl3); // getOrDefault returns null if value is explicitly null
    }
}
