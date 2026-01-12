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
        Long usersTTL = cacheManager.getTTL("users").get();
        Long productsTTL = cacheManager.getTTL("products").get();
        Long ordersTTL = cacheManager.getTTL("orders").get();

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
    void getTTL_WithNonExistentCache_ReturnsEmpty() {
        // When
        var ttl = cacheManager.getTTL("nonexistent");

        // Then
        assertTrue(ttl.isEmpty());
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
        var ttl = emptyCacheManager.getTTL("anyCache");
        Long ttlOrDefault = emptyCacheManager.getTTLOrDefault("anyCache", 20L);

        // Then
        assertTrue(ttl.isEmpty());
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
        Long ttl1 = managerWithNull.getTTL("cache1").orElse(null);
        var ttl2 = managerWithNull.getTTL("cache2");
        Long ttl3 = managerWithNull.getTTLOrDefault("cache2", 15L);

        // Then
        assertEquals(25L, ttl1);
        assertTrue(ttl2.isEmpty()); // Optional.ofNullable(null) returns empty Optional
        assertNull(ttl3); // getOrDefault returns null if value is explicitly null
    }

    /**
     * Gets the TTL with optional API demonstrates proper usage patterns.
     */
    @Test
    void getTTL_WithOptionalAPI_DemonstratesProperUsagePatterns() {
        // Test 1: Using orElse for default values
        Long ttlWithDefault = cacheManager.getTTL("users").orElse(10L);
        assertEquals(30L, ttlWithDefault);

        Long ttlWithDefaultForMissing = cacheManager.getTTL("nonexistent").orElse(10L);
        assertEquals(10L, ttlWithDefaultForMissing);

        // Test 2: Using isPresent/isEmpty for conditional logic
        assertTrue(cacheManager.getTTL("products").isPresent());
        assertFalse(cacheManager.getTTL("nonexistent").isPresent());

        // Test 3: Using ifPresent for side effects
        var executionFlag = new boolean[]{false};
        cacheManager.getTTL("orders").ifPresent(ttl -> {
            executionFlag[0] = true;
            assertEquals(15L, ttl);
        });
        assertTrue(executionFlag[0]);

        // Test 4: Using orElseThrow for cases where value must exist
        Long requiredTtl = cacheManager.getTTL("users")
            .orElseThrow(() -> new IllegalStateException("Users cache TTL must be configured"));
        assertEquals(30L, requiredTtl);

        // Test 5: Verify empty Optional doesn't throw when used properly
        assertDoesNotThrow(() -> {
            cacheManager.getTTL("nonexistent").ifPresent(ttl -> {
                fail("Should not execute for empty Optional");
            });
        });
    }
}
