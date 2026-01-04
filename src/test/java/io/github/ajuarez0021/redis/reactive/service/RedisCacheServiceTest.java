package io.github.ajuarez0021.redis.reactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The Class RedisCacheServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    /** The redis template. */
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /** The value operations. */
    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    /** The cache service. */
    private RedisCacheService cacheService;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * Cacheable cache hit returns from cache.
     */
    @Test
    void cacheable_CacheHit_ReturnsFromCache() {
        String cacheName = "users";
        String key = "user1";
        String cachedValue = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just("Loaded from DB");

        when(valueOperations.get("users:user1")).thenReturn(Mono.just(cachedValue));

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10)))
                .expectNext(cachedValue)
                .verifyComplete();

        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    /**
     * Cacheable cache miss loads and caches.
     */
    @Test
    void cacheable_CacheMiss_LoadsAndCaches() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(loadedValue);

        when(valueOperations.get("users:user1")).thenReturn(Mono.empty());
        when(valueOperations.set("users:user1", loadedValue, Duration.ofMinutes(10)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10)))
                .expectNext(loadedValue)
                .verifyComplete();

        verify(valueOperations).get("users:user1");
        verify(valueOperations).set("users:user1", loadedValue, Duration.ofMinutes(10));
    }

    /**
     * Cacheable cache miss null result does not cache.
     */
    @Test
    void cacheable_CacheMissNullResult_DoesNotCache() {
        String cacheName = "users";
        String key = "user1";
        Supplier<Mono<String>> loader = () -> Mono.empty();

        when(valueOperations.get("users:user1")).thenReturn(Mono.empty());

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10)))
                .verifyComplete();

        verify(valueOperations).get("users:user1");
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    /**
     * Cacheable with default TT L uses default TTL.
     */
    @Test
    void cacheable_WithDefaultTTL_UsesDefaultTTL() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(loadedValue);

        when(valueOperations.get("users:user1")).thenReturn(Mono.empty());
        when(valueOperations.set("users:user1", loadedValue, Duration.ofMinutes(10)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader))
                .expectNext(loadedValue)
                .verifyComplete();

        verify(valueOperations).set("users:user1", loadedValue, Duration.ofMinutes(10));
    }

    /**
     * Cacheable with error falls back to loader.
     */
    @Test
    void cacheable_WithError_FallsBackToLoader() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(loadedValue);

        when(valueOperations.get("users:user1"))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader, Duration.ofMinutes(10)))
                .expectNext(loadedValue)
                .verifyComplete();
    }

    /**
     * Cache put updates cache.
     */
    @Test
    void cachePut_UpdatesCache() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Jane Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(newValue);

        when(valueOperations.set("users:user1", newValue, Duration.ofMinutes(10)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cachePut(cacheName, key, loader, Duration.ofMinutes(10)))
                .expectNext(newValue)
                .verifyComplete();

        verify(valueOperations).set("users:user1", newValue, Duration.ofMinutes(10));
    }

    /**
     * Cache put empty mono completes without caching.
     */
    @Test
    void cachePut_EmptyMono_CompletesWithoutCaching() {
        String cacheName = "users";
        String key = "user1";
        Supplier<Mono<String>> loader = Mono::empty;

        StepVerifier.create(cacheService.cachePut(cacheName, key, loader, Duration.ofMinutes(10)))
                .verifyComplete();

        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verify(redisTemplate, never()).delete(anyString());
    }

    /**
     * Cache put with default TT L uses default TTL.
     */
    @Test
    void cachePut_WithDefaultTTL_UsesDefaultTTL() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Jane Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(newValue);

        when(valueOperations.set("users:user1", newValue, Duration.ofMinutes(10)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cachePut(cacheName, key, loader))
                .expectNext(newValue)
                .verifyComplete();

        verify(valueOperations).set("users:user1", newValue, Duration.ofMinutes(10));
    }

    /**
     * Cache evict deletes key.
     */
    @Test
    void cacheEvict_DeletesKey() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(Mono.just(1L));

        StepVerifier.create(cacheService.cacheEvict(cacheName, key))
                .verifyComplete();

        verify(redisTemplate).delete("users:user1");
    }

    /**
     * Cache evict key not found completes successfully.
     */
    @Test
    void cacheEvict_KeyNotFound_CompletesSuccessfully() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(Mono.just(0L));

        StepVerifier.create(cacheService.cacheEvict(cacheName, key))
                .verifyComplete();

        verify(redisTemplate).delete("users:user1");
    }

    /**
     * Cache evict all deletes all matching keys.
     */
    @Test
    void cacheEvictAll_DeletesAllMatchingKeys() {
        String cacheName = "users";

        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenReturn(Flux.just("users:1", "users:2", "users:3"));
        when(redisTemplate.delete(any(String[].class))).thenReturn(Mono.just(3L));

        StepVerifier.create(cacheService.cacheEvictAll(cacheName))
                .verifyComplete();

        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate).delete(any(String[].class));
    }

    /**
     * Cache evict multiple deletes multiple keys.
     */
    @Test
    void cacheEvictMultiple_DeletesMultipleKeys() {
        String cacheName = "users";
        String[] keys = {"user1", "user2", "user3"};

        when(redisTemplate.delete(any(String[].class))).thenReturn(Mono.just(3L));

        StepVerifier.create(cacheService.cacheEvictMultiple(cacheName, keys))
                .verifyComplete();

        verify(redisTemplate, times(1)).delete(any(String[].class));
    }

    /**
     * Cache evict by pattern deletes matching keys.
     */
    @Test
    void cacheEvictByPattern_DeletesMatchingKeys() {
        String pattern = "users:*";

        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenReturn(Flux.just("users:1", "users:2"));
        when(redisTemplate.delete(any(String[].class))).thenReturn(Mono.just(2L));

        StepVerifier.create(cacheService.cacheEvictByPattern(pattern))
                .verifyComplete();

        verify(redisTemplate).scan(any(ScanOptions.class));
        verify(redisTemplate).delete(any(String[].class));
    }

    /**
     * Exists key exists returns true.
     */
    @Test
    void exists_KeyExists_ReturnsTrue() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.exists(cacheName, key))
                .expectNext(true)
                .verifyComplete();

        verify(redisTemplate).hasKey("users:user1");
    }

    /**
     * Exists key does not exist returns false.
     */
    @Test
    void exists_KeyDoesNotExist_ReturnsFalse() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.hasKey("users:user1")).thenReturn(Mono.just(false));

        StepVerifier.create(cacheService.exists(cacheName, key))
                .expectNext(false)
                .verifyComplete();

        verify(redisTemplate).hasKey("users:user1");
    }

    /**
     * Gets the TT L returns remaining TTL.
     *
     * @return the TT L returns remaining TTL
     */
    @Test
    void getTTL_ReturnsRemainingTTL() {
        String cacheName = "users";
        String key = "user1";
        Duration expectedTTL = Duration.ofMinutes(5);

        when(redisTemplate.getExpire("users:user1")).thenReturn(Mono.just(expectedTTL));

        StepVerifier.create(cacheService.getTTL(cacheName, key))
                .expectNext(expectedTTL)
                .verifyComplete();

        verify(redisTemplate).getExpire("users:user1");
    }

    /**
     * Gets the TT L with error returns empty.
     *
     * @return the TT L with error returns empty
     */
    @Test
    void getTTL_WithError_ReturnsEmpty() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.getExpire("users:user1"))
                .thenReturn(Mono.error(new RuntimeException("Error")));

        StepVerifier.create(cacheService.getTTL(cacheName, key))
                .verifyComplete();
    }

    /**
     * Cacheable without TT L uses default TTL.
     */
    @Test
    void cacheable_WithoutTTL_UsesDefaultTTL() {
        String cacheName = "users";
        String key = "user1";
        String loadedValue = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(loadedValue);

        when(valueOperations.get("users:user1")).thenReturn(Mono.empty());
        when(valueOperations.set(eq("users:user1"), eq(loadedValue), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cacheable(cacheName, key, loader))
                .expectNext(loadedValue)
                .verifyComplete();

        verify(valueOperations).set(eq("users:user1"), eq(loadedValue), eq(Duration.ofMinutes(10)));
    }

    /**
     * Cache put without TT L uses default TTL.
     */
    @Test
    void cachePut_WithoutTTL_UsesDefaultTTL() {
        String cacheName = "users";
        String key = "user1";
        String newValue = "Jane Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(newValue);

        when(valueOperations.set(eq("users:user1"), eq(newValue), any(Duration.class)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(cacheService.cachePut(cacheName, key, loader))
                .expectNext(newValue)
                .verifyComplete();

        verify(valueOperations).set(eq("users:user1"), eq(newValue), eq(Duration.ofMinutes(10)));
    }

    /**
     * Cache evict with key not found logs debug.
     */
    @Test
    void cacheEvict_WithKeyNotFound_LogsDebug() {
        String cacheName = "users";
        String key = "user1";

        when(redisTemplate.delete("users:user1")).thenReturn(Mono.just(0L));

        StepVerifier.create(cacheService.cacheEvict(cacheName, key))
                .verifyComplete();

        verify(redisTemplate).delete("users:user1");
    }

    /**
     * Cache evict all with empty keys handles gracefully.
     */
    @Test
    void cacheEvictAll_WithEmptyKeys_HandlesGracefully() {
        String cacheName = "users";

        when(redisTemplate.scan(any(ScanOptions.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(cacheService.cacheEvictAll(cacheName))
                .verifyComplete();
    }
}
