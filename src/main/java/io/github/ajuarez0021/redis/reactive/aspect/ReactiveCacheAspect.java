package io.github.ajuarez0021.redis.reactive.aspect;

import io.github.ajuarez0021.redis.reactive.service.ReactiveCacheManager;
import io.github.ajuarez0021.redis.reactive.service.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class ReactiveCacheAspect.
 */
@Slf4j
@Aspect
@Component
public class ReactiveCacheAspect {

    /** The cache service. */
    private final RedisCacheService cacheService;

    /** The expression parser. */
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /** The parameter name discoverer. */
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /** The CacheManager. **/
    private final ReactiveCacheManager cacheManager;

    /** Cache for parsed SPEL expressions to avoid repeated parsing overhead. */
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    /**
     * Instantiates a new reactive cache aspect.
     *
     * @param cacheService the cache service
     * @param cacheManager the cache manager
     */
    public ReactiveCacheAspect(RedisCacheService cacheService, ReactiveCacheManager cacheManager) {
        this.cacheService = cacheService;
        this.cacheManager = cacheManager;
    }

    /**
     * Handle cacheable.
     *
     * @param joinPoint the join point
     * @param cacheable the cacheable
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(cacheable)")
    public Object handleCacheable(ProceedingJoinPoint joinPoint, Cacheable cacheable) throws Throwable {
        if (!evaluateCondition(cacheable.condition(), joinPoint)) {
            log.debug("Cacheable condition not met, executing method directly");
            return joinPoint.proceed();
        }

        String cacheName = getCacheName(cacheable.cacheNames(), cacheable.value());
        String key = generateKey(cacheable.key(), joinPoint);

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return handleCacheableMono(cacheName, key, mono);
        }

        return result;
    }

    /**
     * Handle cache put.
     *
     * @param joinPoint the join point
     * @param cachePut the cache put
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(cachePut)")
    public Object handleCachePut(ProceedingJoinPoint joinPoint, CachePut cachePut) throws Throwable {
        if (!evaluateCondition(cachePut.condition(), joinPoint)) {
            log.debug("CachePut condition not met, executing method directly");
            return joinPoint.proceed();
        }

        String cacheName = getCacheName(cachePut.cacheNames(), cachePut.value());
        String key = generateKey(cachePut.key(), joinPoint);

        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return handleCachePutMono(cacheName, key, mono);
        }

        return result;
    }

    /**
     * Handle cache evict.
     *
     * @param joinPoint the join point
     * @param cacheEvict the cache evict
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        boolean beforeInvocation = cacheEvict.beforeInvocation();

        if (!evaluateCondition(cacheEvict.condition(), joinPoint)) {
            log.debug("CacheEvict condition not met, executing method directly");
            return joinPoint.proceed();
        }

        String cacheName = getCacheName(cacheEvict.cacheNames(), cacheEvict.value());

        if (beforeInvocation) {
            // First, get the result to check if it's reactive
            Object result = joinPoint.proceed();

            if (result instanceof Mono<?> mono) {
                // For reactive results, defer eviction and chain it before returning the original mono
                return performEviction(cacheName, cacheEvict, joinPoint)
                    .then(mono);
            } else {
                // For non-reactive results, evict on boundedElastic scheduler to avoid blocking reactive threads
                performEviction(cacheName, cacheEvict, joinPoint)
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();
                return result;
            }
        }

        // Handle afterInvocation
        Object result = joinPoint.proceed();

        if (result instanceof Mono<?> mono) {
            return mono.flatMap(value ->
                performEviction(cacheName, cacheEvict, joinPoint)
                    .thenReturn(value)
            );
        } else {
            // For non-reactive results, evict on boundedElastic scheduler to avoid blocking reactive threads
            performEviction(cacheName, cacheEvict, joinPoint)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
        }

        return result;
    }

    /**
     * Handle cacheable mono.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param mono the mono
     * @return the mono
     */
    private <T> Mono<T> handleCacheableMono(String cacheName, String key, Mono<T> mono) {
        return cacheService.cacheable(
            cacheName,
            key,
            () -> mono,
            Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10))
        );
    }

    /**
     * Handle cache put mono.
     *
     * @param <T> the generic type
     * @param cacheName the cache name
     * @param key the key
     * @param mono the mono
     * @return the mono
     */
    private <T> Mono<T> handleCachePutMono(String cacheName, String key, Mono<T> mono) {
        return cacheService.cachePut(
            cacheName,
            key,
            () -> mono,
            Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10))
        );
    }

    /**
     * Perform eviction.
     *
     * @param cacheName the cache name
     * @param cacheEvict the cache evict
     * @param joinPoint the join point
     * @return the mono
     */
    private Mono<Void> performEviction(String cacheName, CacheEvict cacheEvict, ProceedingJoinPoint joinPoint) {
        if (cacheEvict.allEntries()) {
            log.debug("Evicting all entries from cache: {}", cacheName);
            return cacheService.cacheEvictAll(cacheName);
        } else {
            String key = generateKey(cacheEvict.key(), joinPoint);
            log.debug("Evicting cache entry - cacheName: {}, key: {}", cacheName, key);
            return cacheService.cacheEvict(cacheName, key);
        }
    }

    /**
     * Gets or parses a SPEL expression from cache.
     *
     * @param expressionString the expression string
     * @return the parsed expression
     */
    private Expression getOrParseExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString, expressionParser::parseExpression);
    }

    /**
     * Generate key.
     *
     * @param keyExpression the key expression
     * @param joinPoint the join point
     * @return the string
     */
    private String generateKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        if (keyExpression.isEmpty()) {
            return generateDefaultKey(joinPoint);
        }

        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expression = getOrParseExpression(keyExpression);
            return expression.getValue(context, String.class);
        } catch (Exception e) {
            log.warn("Failed to evaluate key expression: {}, using default key", keyExpression);
            return generateDefaultKey(joinPoint);
        }
    }

    /**
     * Generate default key.
     *
     * @param joinPoint the join point
     * @return the string
     */
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "default";
        }
        if (args.length == 1) {
            return String.valueOf(args[0]);
        }
        StringBuilder keyBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                keyBuilder.append("_");
            }
            keyBuilder.append(args[i]);
        }
        return keyBuilder.toString();
    }

    /**
     * Evaluate condition.
     *
     * @param conditionExpression the condition expression
     * @param joinPoint the join point
     * @return true, if successful
     */
    private boolean evaluateCondition(String conditionExpression, ProceedingJoinPoint joinPoint) {
        if (conditionExpression.isEmpty()) {
            return true;
        }

        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expression = getOrParseExpression(conditionExpression);
            Boolean result = expression.getValue(context, Boolean.class);
            return result != null && result;
        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}, assuming true", conditionExpression, e);
            return true;
        }
    }

    /**
     * Creates the evaluation context.
     *
     * @param joinPoint the join point
     * @return the evaluation context
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Object target = joinPoint.getTarget();

        return new MethodBasedEvaluationContext(target, method, args, parameterNameDiscoverer);
    }

    /**
     * Gets the cache name.
     *
     * @param cacheNames the cache names
     * @param values the values
     * @return the cache name
     */
    private String getCacheName(String[] cacheNames, String[] values) {
        if (cacheNames.length > 0) {
            return cacheNames[0];
        }
        if (values.length > 0) {
            return values[0];
        }
        throw new IllegalArgumentException("Cache name must be specified");
    }
}
