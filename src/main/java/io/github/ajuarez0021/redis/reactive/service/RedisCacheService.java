
package io.github.ajuarez0021.redis.reactive.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

import io.github.ajuarez0021.redis.reactive.util.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Mono;

/**
 * The Class RedisCacheService.
 *
 * @author ajuar
 */
@Slf4j
public class RedisCacheService {
    
    /** The redis template. */
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * Instantiates a new redis cache service.
     *
     * @param redisTemplate the redis template
     */
    public RedisCacheService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
     /**
     * Equivalent to @Cacheable Searches the cache, if it does not exist,
     * execute the loader and save the result.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @param ttl the ttl
     * @return the mono object
     */
    @SuppressWarnings("unchecked")
    public <T> Mono<T> cacheable(String cacheName, String key, Supplier<Mono<T>> loader, Duration ttl) {
        Validator.validateCacheable(cacheName, key, loader, ttl);

        String fullKey = buildKey(cacheName, key);

        return redisTemplate.opsForValue().get(fullKey)
                .doOnNext(cached -> log.debug("Cache HIT - Key: {}", fullKey))
                .cast((Class<T>) Object.class)
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.debug("Cache MISS - Key: {}", fullKey);
                            return loader.get()
                                    .flatMap(result -> {
                                        if (result != null) {
                                            return redisTemplate.opsForValue()
                                                    .set(fullKey, result, ttl)
                                                    .doOnSuccess(v -> log.debug("Cached data - Key: {}", fullKey))
                                                    .thenReturn(result);
                                        }
                                        return Mono.just(result);
                                    });
                        })
                )
                .onErrorResume(e -> {
                    log.error("Error in cacheable operation for key {}: {}", fullKey, e.getMessage());
                    return loader.get();
                });
    }


    
    /**
     * Overhead with default TTL of 10 minutes.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @return the mono object
     */
    public <T> Mono<T> cacheable(String cacheName, String key, Supplier<Mono<T>> loader) {
        return cacheable(cacheName, key, loader, Duration.ofMinutes(10));
    }

    /**
     * Equivalent to @CachePut.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @param ttl the ttl
     * @return the mono object
     */
    public <T> Mono<T> cachePut(String cacheName, String key, Supplier<Mono<T>> loader, Duration ttl) {
        String fullKey = buildKey(cacheName, key);

        return loader.get()
                .flatMap(result -> {
                    if (result != null) {
                        return redisTemplate.opsForValue()
                                .set(fullKey, result, ttl)
                                .doOnSuccess(v -> log.debug("Cache UPDATED - Key: {}", fullKey))
                                .thenReturn(result);
                    } else {
                        return redisTemplate.delete(fullKey)
                                .doOnSuccess(v -> log.debug("Cache EVICTED (null result) - Key: {}", fullKey))
                                .thenReturn(result);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error in cachePut operation for key {}: {}", fullKey, e.getMessage());
                    return loader.get();
                });
    }

    /**
     * Overloading with TTL by default.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param loader the loader
     * @return the mono object
     */
    public <T> Mono<T> cachePut(String cacheName, String key, Supplier<Mono<T>> loader) {
        return cachePut(cacheName, key, loader, Duration.ofMinutes(10));
    }

    /**
     * Equivalent to @CacheEvict.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return mono indicating completion
     */
    public Mono<Void> cacheEvict(String cacheName, String key) {
        String fullKey = buildKey(cacheName, key);

        return redisTemplate.delete(fullKey)
                .doOnNext(deleted -> {
                    if (deleted > 0) {
                        log.debug("Cache EVICTED - Key: {}", fullKey);
                    } else {
                        log.debug("Cache key not found - Key: {}", fullKey);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error evicting cache key {}: {}", fullKey, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Equivalent to @CacheEvict(allEntries = true).
     *
     * @param cacheName the cache name
     * @return mono indicating completion
     */
    public Mono<Void> cacheEvictAll(String cacheName) {
        String pattern = cacheName + ":*";

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        return redisTemplate.scan(options)
                .buffer(100)
                .flatMap(keys -> {
                    if (!keys.isEmpty()) {
                        return redisTemplate.delete(keys.toArray(new String[0]))
                                .then(Mono.just(keys.size()));
                    }
                    return Mono.just(0);
                })
                .reduce(0, Integer::sum)
                .doOnSuccess(total -> log.debug("Cache EVICTED ALL - Pattern: {}, Total: {}", pattern, total))
                .onErrorResume(e -> {
                    log.error("Error evicting all entries for cache {}: {}", cacheName, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Evict multiple specific keys.
     *
     * @param cacheName the cache name
     * @param keys the keys
     * @return mono indicating completion
     */
    public Mono<Void> cacheEvictMultiple(String cacheName, String... keys) {
        String[] fullKeys = Arrays.stream(keys)
                .map(key -> buildKey(cacheName, key))
                .toArray(String[]::new);

        return redisTemplate.delete(fullKeys)
                .doOnNext(deleted -> log.debug("Cache EVICTED MULTIPLE - Count: {}", deleted))
                .onErrorResume(e -> {
                    log.error("Error evicting multiple keys: {}", e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Evict by custom pattern.
     *
     * @param pattern the pattern
     * @return mono indicating completion
     */
    public Mono<Void> cacheEvictByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        return redisTemplate.scan(options)
                .buffer(100)
                .flatMap(keys -> {
                    if (!keys.isEmpty()) {
                        return redisTemplate.delete(keys.toArray(new String[0]))
                                .then(Mono.just(keys.size()));
                    }
                    return Mono.just(0);
                })
                .reduce(0, Integer::sum)
                .doOnSuccess(total -> log.debug("Cache EVICTED BY PATTERN - Pattern: {}, Total: {}", pattern, total))
                .onErrorResume(e -> {
                    log.error("Error evicting by pattern {}: {}", pattern, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Check if a cached key exists.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return mono with true if exists, false otherwise
     */
    public Mono<Boolean> exists(String cacheName, String key) {
        String fullKey = buildKey(cacheName, key);

        return redisTemplate.hasKey(fullKey)
                .onErrorResume(e -> {
                    log.error("Error checking key existence {}: {}", fullKey, e.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Gets the remaining TTL of a key.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return mono with the ttl duration
     */
    public Mono<Duration> getTTL(String cacheName, String key) {
        String fullKey = buildKey(cacheName, key);

        return redisTemplate.getExpire(fullKey)
                .onErrorResume(e -> {
                    log.error("Error getting TTL for key {}: {}", fullKey, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Build the complete key.
     *
     * @param cacheName the cache name
     * @param key the key
     * @return the string
     */
    private String buildKey(String cacheName, String key) {
        return cacheName + ":" + key;
    }
}
