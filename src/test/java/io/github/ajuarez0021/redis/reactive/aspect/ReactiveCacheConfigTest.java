package io.github.ajuarez0021.redis.reactive.aspect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class ReactiveCacheConfigTest.
 */
class ReactiveCacheConfigTest {

    /**
     * Reactive cache config should be instantiable.
     */
    @Test
    void reactiveCacheConfig_ShouldBeInstantiable() {
        ReactiveCacheConfig config = new ReactiveCacheConfig();

        assertNotNull(config);
    }
    
}
