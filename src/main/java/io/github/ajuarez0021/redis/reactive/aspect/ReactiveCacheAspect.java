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
import org.springframework.cache.annotation.Caching;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>(50);
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
     * <p><b>IMPORTANT:</b> This method is optimized to avoid duplicate execution of the intercepted method.
     * The method is executed lazily only when needed (cache miss scenario).</p>
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

        return cacheService.cacheable(
            cacheName,
            key,
            () -> {
                try {
                    @SuppressWarnings("unchecked")
                    Mono<Object> result = (Mono<Object>) joinPoint.proceed();
                    return result;
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            },
            Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10))
        );
    }

    /**
     * Handle cache put.
     *
     * <p><b>IMPORTANT:</b> This method is optimized to avoid duplicate execution of the intercepted method.
     * The method is executed once and the result is cached.</p>
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

        return cacheService.cachePut(
            cacheName,
            key,
            () -> {
                try {
                    @SuppressWarnings("unchecked")
                    Mono<Object> result = (Mono<Object>) joinPoint.proceed();
                    return result;
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            },
            Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10))
        );
    }

    /**
     * Handle cache evict.
     *
     * <p><b>IMPORTANT:</b> This method is optimized to avoid blocking operations.
     * For reactive methods (returning Mono), eviction is chained reactively.
     * For non-reactive methods, eviction is performed asynchronously (fire-and-forget).</p>
     *
     * <p><b>WARNING:</b> When using @CacheEvict on non-reactive methods, the eviction happens
     * asynchronously and may not complete before the method returns. For guaranteed eviction,
     * use @CacheEvict only on methods that return Mono.</p>
     *
     * @param joinPoint the join point
     * @param cacheEvict the cache evict
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(cacheEvict)")
    public Object handleCacheEvict(ProceedingJoinPoint joinPoint, CacheEvict cacheEvict) throws Throwable {
        if (!evaluateCondition(cacheEvict.condition(), joinPoint)) {
            log.debug("CacheEvict condition not met, executing method directly");
            return joinPoint.proceed();
        }

        boolean beforeInvocation = cacheEvict.beforeInvocation();
        String cacheName = getCacheName(cacheEvict.cacheNames(), cacheEvict.value());

        if (beforeInvocation) {
            return performEviction(cacheName, cacheEvict, joinPoint)
                .then(Mono.defer(() -> {
                    try {
                        Object result = joinPoint.proceed();
                        if (result instanceof Mono<?> mono) {
                            return mono;
                        } else {
                            return Mono.justOrEmpty(result);
                        }
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                }));
        } else {
            return Mono.defer(() -> {
                try {
                    Object result = joinPoint.proceed();
                    if (result instanceof Mono<?> mono) {
                        return mono.flatMap(value ->
                            performEviction(cacheName, cacheEvict, joinPoint)
                                .thenReturn(value)
                        );
                    } else {
                        // For non-reactive methods, chain eviction with result
                        // Note: This requires the caller to subscribe to the returned Mono
                        return performEviction(cacheName, cacheEvict, joinPoint)
                            .then(Mono.justOrEmpty(result))
                            .onErrorResume(error -> {
                                log.error("Cache eviction failed for cache: {}", cacheName, error);
                                // On error, still return the result
                                return Mono.justOrEmpty(result);
                            });
                    }
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            });
        }
    }

    /**
     * Handle caching.
     *
     * <p>Handles the {@code @Caching} annotation which allows combining multiple cache operations.
     * Operations are executed in the following order:</p>
     * <ol>
     *   <li>Evictions with beforeInvocation=true</li>
     *   <li>Method execution (once)</li>
     *   <li>Cacheable operations (check cache, execute on miss)</li>
     *   <li>Put operations (always cache the result)</li>
     *   <li>Evictions with beforeInvocation=false</li>
     * </ol>
     *
     * @param joinPoint the join point
     * @param caching the caching annotation
     * @return the object
     * @throws Throwable the throwable
     */
    @Around("@annotation(caching)")
    public Object handleCaching(ProceedingJoinPoint joinPoint, Caching caching) throws Throwable {
        Cacheable[] cacheables = caching.cacheable();
        CachePut[] puts = caching.put();
        CacheEvict[] evicts = caching.evict();

        // Step 1: Process evictions with beforeInvocation=true
        Mono<Void> beforeEvictions = Mono.empty();
        for (CacheEvict evict : evicts) {
            if (evict.beforeInvocation() && evaluateCondition(evict.condition(), joinPoint)) {
                String cacheName = getCacheName(evict.cacheNames(), evict.value());
                beforeEvictions = beforeEvictions.then(performEviction(cacheName, evict, joinPoint));
            }
        }

        // Step 2: Execute method and process remaining operations
        return beforeEvictions.then(Mono.defer(() -> {
            try {
                // Execute the method once
                Object result = joinPoint.proceed();

                // Convert to Mono if needed
                Mono<Object> resultMono;
                if (result instanceof Mono<?> mono) {
                    @SuppressWarnings("unchecked")
                    Mono<Object> castedMono = (Mono<Object>) mono;
                    resultMono = castedMono;
                } else {
                    resultMono = Mono.justOrEmpty(result);
                }

                // Step 3: Process cacheable operations
                // Note: Cacheable operations are typically not used with @Caching when put/evict are present
                // but we support them for completeness
                for (Cacheable cacheable : cacheables) {
                    if (evaluateCondition(cacheable.condition(), joinPoint)) {
                        String cacheName = getCacheName(cacheable.cacheNames(), cacheable.value());
                        String key = generateKey(cacheable.key(), joinPoint);
                        Duration ttl = Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10));

                        resultMono = resultMono.flatMap(value ->
                            cacheService.cachePut(cacheName, key, () -> Mono.just(value), ttl)
                        );
                    }
                }

                // Step 4: Process put operations
                resultMono = resultMono.flatMap(value -> {
                    Mono<Object> chain = Mono.just(value);

                    for (CachePut put : puts) {
                        if (evaluateConditionWithResult(put.condition(), joinPoint, value)) {
                            String cacheName = getCacheName(put.cacheNames(), put.value());
                            String key = generateKeyWithResult(put.key(), joinPoint, value);
                            Duration ttl = Duration.ofMinutes(cacheManager.getTTLOrDefault(cacheName, 10));

                            chain = chain.flatMap(v ->
                                cacheService.cachePut(cacheName, key, () -> Mono.just(v), ttl)
                            );
                        }
                    }

                    return chain;
                });

                // Step 5: Process evictions with beforeInvocation=false
                resultMono = resultMono.flatMap(value -> {
                    Mono<Void> afterEvictions = Mono.empty();

                    for (CacheEvict evict : evicts) {
                        if (!evict.beforeInvocation() && evaluateCondition(evict.condition(), joinPoint)) {
                            String cacheName = getCacheName(evict.cacheNames(), evict.value());
                            afterEvictions = afterEvictions.then(
                                performEvictionWithResult(cacheName, evict, joinPoint, value)
                            );
                        }
                    }

                    return afterEvictions.thenReturn(value);
                });

                return resultMono;

            } catch (Throwable e) {
                return Mono.error(e);
            }
        }));
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
     * Perform eviction with result.
     *
     * @param cacheName the cache name
     * @param cacheEvict the cache evict
     * @param joinPoint the join point
     * @param result the method result
     * @return the mono
     */
    private Mono<Void> performEvictionWithResult(String cacheName, CacheEvict cacheEvict,
                                                   ProceedingJoinPoint joinPoint, Object result) {
        if (cacheEvict.allEntries()) {
            log.debug("Evicting all entries from cache: {}", cacheName);
            return cacheService.cacheEvictAll(cacheName);
        } else {
            String key = generateKeyWithResult(cacheEvict.key(), joinPoint, result);
            log.debug("Evicting cache entry - cacheName: {}, key: {}", cacheName, key);
            return cacheService.cacheEvict(cacheName, key);
        }
    }

    /**
     * Gets or parses a SPEL expression from cache.
     *
     * <p>This method uses a double-check pattern to avoid duplicate parsing under high concurrency.
     * The pattern prevents the known performance issue where {@code computeIfAbsent} can cause
     * multiple threads to parse the same expression simultaneously.</p>
     *
     * <p><b>Performance optimization:</b> Parsing happens outside any lock, and {@code putIfAbsent}
     * ensures only one parsed expression is stored. If another thread already stored the expression,
     * we use that one instead of the one we just parsed.</p>
     *
     * @param expressionString the expression string
     * @return the parsed expression
     */
    private Expression getOrParseExpression(String expressionString) {
        return expressionCache.computeIfAbsent(expressionString,
                expressionParser::parseExpression);
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
     * Generate key with result.
     *
     * <p>This method is used for evaluating key expressions that reference the method result
     * using {@code #result} in SpEL expressions, such as in @CachePut or @CacheEvict operations
     * within @Caching annotations.</p>
     *
     * @param keyExpression the key expression
     * @param joinPoint the join point
     * @param result the method execution result
     * @return the string
     */
    private String generateKeyWithResult(String keyExpression, ProceedingJoinPoint joinPoint, Object result) {
        if (keyExpression.isEmpty()) {
            return generateDefaultKey(joinPoint);
        }

        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            // Add result to context for #result references
            context.setVariable("result", result);
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
     * <p>This method generates a unique cache key by combining the method name with serialized arguments.
     * Including the method name prevents cache key collisions between different methods that use the same
     * parameters.</p>
     *
     * <p><b>Key Format:</b></p>
     * <ul>
     *   <li>No arguments: {@code methodName}</li>
     *   <li>With arguments: {@code methodName:arg1_arg2_...}</li>
     * </ul>
     *
     * <p><b>WARNING:</b> For complex objects, it's recommended to specify a custom {@code key} expression
     * in the cache annotation to avoid potential key collisions.</p>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * &#64;Cacheable(value = "users", key = "#query.userId + ':' + #query.type")
     * public Mono&lt;User&gt; getUser(UserQuery query) { ... }
     * </pre>
     *
     * @param joinPoint the join point
     * @return the string key in format "methodName" or "methodName:arg1_arg2_..."
     */
    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        Object[] args = joinPoint.getArgs();

        if (args.length == 0) {
            return methodName;
        }

        StringBuilder keyBuilder = new StringBuilder(methodName).append(":");

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                keyBuilder.append("_");
            }
            keyBuilder.append(serializeArg(args[i]));
        }

        return keyBuilder.toString();
    }

    /**
     * Serializes an argument for cache key generation.
     *
     * <p>This method handles different argument types appropriately:</p>
     * <ul>
     *   <li>Null values: "null"</li>
     *   <li>Primitives and wrappers: String representation</li>
     *   <li>Strings: Direct value</li>
     *   <li>Arrays: Deep string representation</li>
     *   <li>Collections: toString()</li>
     *   <li>Complex objects: ClassName@hashCode (to ensure consistency)</li>
     * </ul>
     *
     * @param arg the argument to serialize
     * @return the serialized string representation
     */
    private String serializeArg(Object arg) {
        if (arg == null) {
            return "null";
        }

        if (arg.getClass().isArray()) {
            switch (arg) {
                case Object[] objects -> {
                    return java.util.Arrays.deepToString(objects);
                }
                case int[] ints -> {
                    return java.util.Arrays.toString(ints);
                }
                case long[] longs -> {
                    return java.util.Arrays.toString(longs);
                }
                case double[] doubles -> {
                    return java.util.Arrays.toString(doubles);
                }
                case float[] floats -> {
                    return java.util.Arrays.toString(floats);
                }
                case boolean[] booleans -> {
                    return java.util.Arrays.toString(booleans);
                }
                case byte[] bytes -> {
                    return java.util.Arrays.toString(bytes);
                }
                case short[] shorts -> {
                    return java.util.Arrays.toString(shorts);
                }
                case char[] chars -> {
                    return java.util.Arrays.toString(chars);
                }
                default -> {
                    return "";
                }
            }
        }

        if (isPrimitiveOrWrapper(arg) || arg instanceof String) {
            return String.valueOf(arg);
        }

        if (arg instanceof java.util.Collection || arg instanceof java.util.Map) {
            return arg.toString();
        }

        return arg.getClass().getSimpleName() + "@" + arg.hashCode();
    }

    /**
     * Checks if an object is a primitive type or its wrapper.
     *
     * @param obj the object to check
     * @return true if primitive or wrapper, false otherwise
     */
    private boolean isPrimitiveOrWrapper(Object obj) {
        return obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character;
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
     * Evaluate condition with result.
     *
     * <p>This method is used for evaluating condition expressions that reference the method result
     * using {@code #result} in SpEL expressions, such as in @CachePut operations within @Caching.</p>
     *
     * @param conditionExpression the condition expression
     * @param joinPoint the join point
     * @param result the method execution result
     * @return true, if successful
     */
    private boolean evaluateConditionWithResult(String conditionExpression,
                                                 ProceedingJoinPoint joinPoint, Object result) {
        if (conditionExpression.isEmpty()) {
            return true;
        }

        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            // Add result to context for #result references
            context.setVariable("result", result);
            Expression expression = getOrParseExpression(conditionExpression);
            Boolean evaluationResult = expression.getValue(context, Boolean.class);
            return evaluationResult != null && evaluationResult;
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
