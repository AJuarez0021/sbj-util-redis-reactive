package io.github.ajuarez0021.redis.reactive.service;

import io.github.ajuarez0021.redis.reactive.util.Validator;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

public final class CacheOperationBuilder <T> {
    /** The cache service. */
    private final RedisCacheService cacheService;

    /** The cache name. */
    private String cacheName;

    /** The key. */
    private String key;

    /** The loader. */
    private Supplier<Mono<T>> loader;

    /** The ttl. */
    private Duration ttl = Duration.ofMinutes(10);

    /** The condition. */
    private boolean condition = true;

    /** The on hit. */
    private Consumer<T> onHit;

    /** The on miss. */
    private Consumer<T> onMiss;

    /**
     * Private constructor - use {@link Factory#create()} to create instances.
     *
     * @param cacheService the cache service
     */
    private CacheOperationBuilder(RedisCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Cache name.
     *
     * @param cacheName the cache name
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> cacheName(String cacheName) {
        this.cacheName = cacheName;
        return this;
    }

    /**
     * Key.
     *
     * @param key the key
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Loader.
     *
     * @param loader the loader (must return Mono)
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> loader(Supplier<Mono<T>> loader) {
        this.loader = loader;
        return this;
    }

    /**
     * Ttl.
     *
     * @param ttl the ttl
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> ttl(Duration ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * Condition.
     *
     * @param condition the condition
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> condition(boolean condition) {
        this.condition = condition;
        return this;
    }

    /**
     * On cache hit.
     *
     * @param callback the callback
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> onCacheHit(Consumer<T> callback) {
        this.onHit = callback;
        return this;
    }

    /**
     * On cache miss.
     *
     * @param callback the callback
     * @return the cache operation builder
     */
    public CacheOperationBuilder<T> onCacheMiss(Consumer<T> callback) {
        this.onMiss = callback;
        return this;
    }

    /**
     * Cacheable.
     *
     * @return mono of the cached value
     */
    public Mono<T> cacheable() {
        Validator.validateRequiredFields(cacheName, key, loader);

        if (!condition) {
            return loader.get();
        }

        return cacheService.exists(cacheName, key)
                .flatMap(exists ->
                    cacheService.cacheable(cacheName, key, loader, ttl)
                        .doOnNext(result -> {
                            if (Objects.equals(Boolean.TRUE, exists) && onHit != null) {
                                onHit.accept(result);
                            } else if (Objects.equals(Boolean.FALSE, exists) && onMiss != null) {
                                onMiss.accept(result);
                            }
                        })
                );
    }


    /**
     * Cache put.
     *
     * <p>Executes the loader and stores the result in the cache, regardless of whether
     * a cached value already exists. This is equivalent to {@code @CachePut}.</p>
     *
     * @return mono of the cached value
     * @throws IllegalStateException if cacheName, key, or loader are not set
     */
    public Mono<T> cachePut() {
        Validator.validateRequiredFields(cacheName, key, loader);
        return cacheService.cachePut(cacheName, key, loader, ttl);
    }

    /**
     * Cache evict.
     *
     * <p>Removes a specific entry from the cache. This is equivalent to
     * {@code @CacheEvict}.</p>
     *
     * @return mono indicating completion
     * @throws IllegalStateException if cacheName or key are not set
     */
    public Mono<Void> cacheEvict() {
        Validator.validateCacheNameAndKey(cacheName, key);
        return cacheService.cacheEvict(cacheName, key);
    }

    /**
     * Cache evict all.
     *
     * <p>Removes all entries from the specified cache. This is equivalent to
     * {@code @CacheEvict(allEntries = true)}.</p>
     *
     * @return mono indicating completion
     * @throws IllegalStateException if cacheName is not set
     */
    public Mono<Void> cacheEvictAll() {
        Validator.validateCacheEvict(cacheName);
        return cacheService.cacheEvictAll(cacheName);
    }

    /**
     * Factory for creating {@link CacheOperationBuilder} instances.
     *
     * <p>This factory provides a thread-safe way to obtain new builder instances.
     * The factory itself is a singleton bean and can be safely injected and shared
     * across threads. Each call to {@link #create()} returns a new, independent
     * builder instance.</p>
     *
     * <p><b>Thread Safety:</b> This class is fully thread-safe. Multiple threads
     * can safely call {@link #create()} concurrently.</p>
     *
     * <p><b>Usage:</b></p>
     * <pre>{@code
     * @Service
     * public class UserService {
     *     @Autowired
     *     private CacheOperationBuilder.Factory builderFactory;
     *     @Autowired
     *     private ReactiveUserRepository userRepository;
     *
     *     public Mono<User> getUser(String userId) {
     *         return builderFactory.create()
     *             .cacheName("users")
     *             .key(userId)
     *             .loader(() -> userRepository.findById(userId))
     *             .cacheable();
     *     }
     * }
     * }</pre>
     *
     * @author ajuar
     */
    public static class Factory {

        /** The cache service. */
        private final RedisCacheService cacheService;

        /**
         * Instantiates a new cache operation builder factory.
         *
         * @param cacheService the cache service
         */
        public Factory(RedisCacheService cacheService) {
            this.cacheService = cacheService;
        }

        /**
         * Creates a new {@link CacheOperationBuilder} instance.
         *
         * <p>Each invocation returns a new, independent builder instance that
         * should be used for a single cache operation and then discarded.</p>
         *
         * @param <T> the type of cached value
         * @return a new cache operation builder
         */
        public <T> CacheOperationBuilder<T> create() {
            return new CacheOperationBuilder<>(cacheService);
        }
    }
}
