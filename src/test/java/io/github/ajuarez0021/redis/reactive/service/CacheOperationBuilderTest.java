package io.github.ajuarez0021.redis.reactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The Class CacheOperationBuilderTest.
 */
@ExtendWith(MockitoExtension.class)
class CacheOperationBuilderTest {

    /** The cache service. */
    @Mock
    private RedisCacheService cacheService;

    /** The factory. */
    private CacheOperationBuilder.Factory factory;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        factory = new CacheOperationBuilder.Factory(cacheService);
    }

    /**
     * Cacheable with all parameters calls service correctly.
     */
    @Test
    void cacheable_WithAllParameters_CallsServiceCorrectly() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(value);
        Duration ttl = Duration.ofMinutes(5);

        when(cacheService.exists(eq("users"), eq("user1"))).thenReturn(Mono.just(false));
        when(cacheService.cacheable(eq(cacheName), eq(key), any(), eq(ttl)))
                .thenReturn(Mono.just(value));

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .ttl(ttl);

        StepVerifier.create(builder.cacheable())
                .expectNext(value)
                .verifyComplete();

        verify(cacheService).cacheable(eq(cacheName), eq(key), any(), eq(ttl));
    }

    /**
     * Cacheable with cache hit callback executes callback.
     */
    @Test
    void cacheable_WithCacheHitCallback_ExecutesCallback() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        Supplier<Mono<String>> loader = () -> Mono.just(value);

        when(cacheService.exists(eq("users"), eq("user1"))).thenReturn(Mono.just(true));
        when(cacheService.cacheable(eq(cacheName), eq(key), any(), any()))
                .thenReturn(Mono.just(value));

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .onCacheHit(v -> callbackExecuted.set(true));

        StepVerifier.create(builder.cacheable())
                .expectNext(value)
                .verifyComplete();

        assert callbackExecuted.get();
    }

    /**
     * Cacheable with cache miss callback executes callback.
     */
    @Test
    void cacheable_WithCacheMissCallback_ExecutesCallback() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        Supplier<Mono<String>> loader = () -> Mono.just(value);

        when(cacheService.exists(eq("users"), eq("user1"))).thenReturn(Mono.just(false));
        when(cacheService.cacheable(eq(cacheName), eq(key), any(), any()))
                .thenReturn(Mono.just(value));

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .onCacheMiss(v -> callbackExecuted.set(true));

        StepVerifier.create(builder.cacheable())
                .expectNext(value)
                .verifyComplete();

        assert callbackExecuted.get();
    }

    /**
     * Cacheable with condition false bypasses cache.
     */
    @Test
    void cacheable_WithConditionFalse_BypassesCache() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(value);

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .condition(false);

        StepVerifier.create(builder.cacheable())
                .expectNext(value)
                .verifyComplete();

        verify(cacheService, never()).cacheable(anyString(), anyString(), any(), any());
    }

    /**
     * Cacheable with condition true uses cache.
     */
    @Test
    void cacheable_WithConditionTrue_UsesCache() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(value);

        when(cacheService.exists(eq("users"), eq("user1"))).thenReturn(Mono.just(false));
        when(cacheService.cacheable(eq(cacheName), eq(key), any(), any()))
                .thenReturn(Mono.just(value));

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .condition(true);

        StepVerifier.create(builder.cacheable())
                .expectNext(value)
                .verifyComplete();

        verify(cacheService).cacheable(eq(cacheName), eq(key), any(), any());
    }

    /**
     * Cache put updates cache.
     */
    @Test
    void cachePut_UpdatesCache() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(value);
        Duration ttl = Duration.ofMinutes(5);

        when(cacheService.cachePut(eq(cacheName), eq(key), any(), eq(ttl)))
                .thenReturn(Mono.just(value));

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key)
                .loader(loader)
                .ttl(ttl);

        StepVerifier.create(builder.cachePut())
                .expectNext(value)
                .verifyComplete();

        verify(cacheService).cachePut(eq(cacheName), eq(key), any(), eq(ttl));
    }

    /**
     * Cache evict evicts cache.
     */
    @Test
    void cacheEvict_EvictsCache() {
        String cacheName = "users";
        String key = "user1";

        when(cacheService.cacheEvict(eq(cacheName), eq(key))).thenReturn(Mono.empty());

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName)
                .key(key);

        StepVerifier.create(builder.cacheEvict())
                .verifyComplete();

        verify(cacheService).cacheEvict(eq(cacheName), eq(key));
    }

    /**
     * Cache evict all evicts all entries in cache.
     */
    @Test
    void cacheEvictAll_EvictsAllEntries() {
        String cacheName = "users";

        when(cacheService.cacheEvictAll(eq(cacheName))).thenReturn(Mono.empty());

        CacheOperationBuilder<String> builder = factory.<String>create()
                .cacheName(cacheName);

        StepVerifier.create(builder.cacheEvictAll())
                .verifyComplete();

        verify(cacheService).cacheEvictAll(eq(cacheName));
    }

    /**
     * Builder supports fluent chaining.
     */
    @Test
    void builder_SupportsFluentChaining() {
        String cacheName = "users";
        String key = "user1";
        String value = "John Doe";
        Supplier<Mono<String>> loader = () -> Mono.just(value);
        Duration ttl = Duration.ofMinutes(5);

        when(cacheService.exists(anyString(), anyString())).thenReturn(Mono.just(false));
        when(cacheService.cacheable(anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(value));

        StepVerifier.create(
                factory.<String>create()
                        .cacheName(cacheName)
                        .key(key)
                        .loader(loader)
                        .ttl(ttl)
                        .condition(true)
                        .onCacheHit(v -> {})
                        .onCacheMiss(v -> {})
                        .cacheable()
        ).expectNext(value).verifyComplete();
    }

    /**
     * Factory creates new instance each time.
     */
    @Test
    void factory_CreatesNewInstanceEachTime() {
        CacheOperationBuilder<String> builder1 = factory.create();
        CacheOperationBuilder<String> builder2 = factory.create();

        assert builder1 != builder2;
    }
}
