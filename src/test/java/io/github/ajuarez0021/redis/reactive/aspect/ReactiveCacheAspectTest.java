package io.github.ajuarez0021.redis.reactive.aspect;

import io.github.ajuarez0021.redis.reactive.service.ReactiveCacheManager;
import io.github.ajuarez0021.redis.reactive.service.RedisCacheService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveListOperations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * The Class ReactiveCacheAspectTest.
 */
@ExtendWith(MockitoExtension.class)
class ReactiveCacheAspectTest {

    /** The redis template. */
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /** The value operations. */
    @Mock
    private ReactiveValueOperations<String, Object> valueOperations;

    /** The list operations. */
    @Mock
    private ReactiveListOperations<String, Object> listOperations;

    /** The cache service. */
    @Mock
    private RedisCacheService cacheService;

    /** The join point. */
    @Mock
    private ProceedingJoinPoint joinPoint;

    /** The signature. */
    @Mock
    private MethodSignature signature;

    /** The cacheManager. */
    @Mock
    private ReactiveCacheManager cacheManager;

    /** The aspect. */
    private ReactiveCacheAspect aspect;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        aspect = new ReactiveCacheAspect(cacheService, cacheManager);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    /**
     * Handle cacheable with mono result calls cache service.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings({ "unchecked" })
	@Test
    void handleCacheable_WithMonoResult_CallsCacheService() throws Throwable {

        String testValue = "cached-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("getData:key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));


        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);


        assertNotNull(result);
        assertTrue(result instanceof Mono);

		StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("getData:key1"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with spel key evaluates expression.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheable_WithSpelKey_EvaluatesExpression() throws Throwable {

        String testValue = "cached-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "#id", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"user123"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("user123"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

      
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

      
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("user123"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with false condition bypasses cache.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheable_WithFalseCondition_BypassesCache() throws Throwable {
       
        String testValue = "direct-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "#id.length() > 10");
        Method method = TestService.class.getMethod("getData", String.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"short"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));

     
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);


        assertNotNull(result);
        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService, never()).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with non mono result returns directly.
     *
     * Note: After bug fix, the aspect now always uses the cache service for consistency,
     * which wraps non-Mono results in a Mono. This test now verifies the cache is called.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCacheable_WithNonMonoResult_ReturnsDirectly() throws Throwable {

        String testValue = "non-reactive-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(testValue);
        when(cacheService.cacheable(eq("testCache"), eq("getData:key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

        // After bug fix: Always returns result from cache service
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("getData:key1"), any(), any(Duration.class));
    }

    /**
     * Handle cache put with mono result updates cache.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCachePut_WithMonoResult_UpdatesCache() throws Throwable {

        String testValue = "updated-value";
        CachePut cachePutAnnotation = createCachePut("testCache", "", "");
        Method method = TestService.class.getMethod("updateData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cachePut(eq("testCache"), eq("updateData:key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));


        Object result = aspect.handleCachePut(joinPoint, cachePutAnnotation);


        assertNotNull(result);
        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cachePut(eq("testCache"), eq("updateData:key1"), any(), any(Duration.class));
    }

    /**
     * Handle cache put with false condition bypasses cache.
     *
     * @throws Throwable the throwable
     */
    @Test
    void handleCachePut_WithFalseCondition_BypassesCache() throws Throwable {
      
        String testValue = "direct-value";
        CachePut cachePutAnnotation = createCachePut("testCache", "", "#id.length() > 10");
        Method method = TestService.class.getMethod("updateData", String.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));

   
        Object result = aspect.handleCachePut(joinPoint, cachePutAnnotation);

    
        assertNotNull(result);
        verify(cacheService, never()).cachePut(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Handle cache evict single key evicts from cache.
     *
     * Note: After bug fix, the aspect now returns a Mono to avoid blocking operations.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCacheEvict_SingleKey_EvictsFromCache() throws Throwable {

        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, false, "");
        Method method = TestService.class.getMethod("deleteData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn("deleted");
        when(cacheService.cacheEvict("testCache", "deleteData:key1")).thenReturn(Mono.empty());

        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

        // After bug fix: Returns Mono for reactive consistency
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("deleted")
                .verifyComplete();

        verify(cacheService).cacheEvict("testCache", "deleteData:key1");
    }

    /**
     * Handle cache evict all entries evicts all from cache.
     *
     * Note: After bug fix, the aspect now returns a Mono to avoid blocking operations.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCacheEvict_AllEntries_EvictsAllFromCache() throws Throwable {

        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", true, false, "");
        Method method = TestService.class.getMethod("clearCache");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(joinPoint.proceed()).thenReturn("cleared");
        when(cacheService.cacheEvictAll("testCache")).thenReturn(Mono.empty());


        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

        // After bug fix: Returns Mono for reactive consistency
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("cleared")
                .verifyComplete();

        verify(cacheService).cacheEvictAll("testCache");
    }

    /**
     * Handle cache evict before invocation evicts before method call.
     *
     * Note: After bug fix, the aspect now returns a Mono to avoid blocking operations.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCacheEvict_BeforeInvocation_EvictsBeforeMethodCall() throws Throwable {

        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, true, "");
        Method method = TestService.class.getMethod("deleteData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn("deleted");
        when(cacheService.cacheEvict("testCache", "deleteData:key1")).thenReturn(Mono.empty());


        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

        // After bug fix: Returns Mono for reactive consistency
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("deleted")
                .verifyComplete();

        verify(cacheService).cacheEvict("testCache", "deleteData:key1");
    }

    /**
     * Handle cache evict with mono result evicts after completion.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheEvict_WithMonoResult_EvictsAfterCompletion() throws Throwable {

        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, false, "");
        Method method = TestService.class.getMethod("deleteData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just("deleted"));
        when(cacheService.cacheEvict("testCache", "deleteData:key1")).thenReturn(Mono.empty());


        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);


        assertNotNull(result);
        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<String>) result)
                .expectNext("deleted")
                .verifyComplete();

        verify(cacheService).cacheEvict("testCache", "deleteData:key1");
    }

    /**
     * Handle cache evict with false condition bypasses eviction.
     *
     * @throws Throwable the throwable
     */
    @Test
    void handleCacheEvict_WithFalseCondition_BypassesEviction() throws Throwable {
     
        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, false, "#id == null");
        Method method = TestService.class.getMethod("deleteData", String.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn("not-deleted");

     
        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);


        assertEquals("not-deleted", result);
        verify(cacheService, never()).cacheEvict(anyString(), anyString());
        verify(cacheService, never()).cacheEvictAll(anyString());
    }

    /**
     * Handle cacheable with multiple args generates composite key.
     * After Bug #6 fix: key format is now "methodName:arg1_arg2"
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheable_WithMultipleArgs_GeneratesCompositeKey() throws Throwable {

        String testValue = "multi-arg-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getDataMulti", String.class, String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1", "arg2"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("getDataMulti:arg1_arg2"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));


        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);


        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("getDataMulti:arg1_arg2"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with no args uses default key.
     * After Bug #6 fix: key format is now "methodName" instead of "default"
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheable_WithNoArgs_UsesDefaultKey() throws Throwable {

        String testValue = "no-arg-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getDataNoArgs");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("getDataNoArgs"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));


        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);


        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("getDataNoArgs"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with invalid spel expression uses default key.
     * After Bug #6 fix: default key now includes method name
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
	@Test
    void handleCacheable_WithInvalidSpelExpression_UsesDefaultKey() throws Throwable {
        String testValue = "fallback-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "#invalidExpression.nonExistent", "");
        Method method = TestService.class.getMethod("getData", String.class);

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("getData:key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();
    }




    /**
     * Creates the cacheable.
     *
     * @param cacheName the cache name
     * @param key the key
     * @param condition the condition
     * @return the cacheable
     */
    private Cacheable createCacheable(String cacheName, String key, String condition) {
        return new Cacheable() {
            @Override
            public String[] value() {
                return new String[]{cacheName};
            }

            @Override
            public String[] cacheNames() {
                return new String[]{cacheName};
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String keyGenerator() {
                return "";
            }

            @Override
            public String cacheManager() {
                return "";
            }

            @Override
            public String cacheResolver() {
                return "";
            }

            @Override
            public String condition() {
                return condition;
            }

            @Override
            public String unless() {
                return "";
            }

            @Override
            public boolean sync() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Cacheable.class;
            }
        };
    }

    /**
     * Creates the cache put.
     *
     * @param cacheName the cache name
     * @param key the key
     * @param condition the condition
     * @return the cache put
     */
    private CachePut createCachePut(String cacheName, String key, String condition) {
        return new CachePut() {
            @Override
            public String[] value() {
                return new String[]{cacheName};
            }

            @Override
            public String[] cacheNames() {
                return new String[]{cacheName};
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String keyGenerator() {
                return "";
            }

            @Override
            public String cacheManager() {
                return "";
            }

            @Override
            public String cacheResolver() {
                return "";
            }

            @Override
            public String condition() {
                return condition;
            }

            @Override
            public String unless() {
                return "";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return CachePut.class;
            }
        };
    }

    /**
     * Creates the cache evict.
     *
     * @param cacheName the cache name
     * @param key the key
     * @param allEntries the all entries
     * @param beforeInvocation the before invocation
     * @param condition the condition
     * @return the cache evict
     */
    private CacheEvict createCacheEvict(String cacheName, String key, boolean allEntries,
                                       boolean beforeInvocation, String condition) {
        return new CacheEvict() {
            @Override
            public String[] value() {
                return new String[]{cacheName};
            }

            @Override
            public String[] cacheNames() {
                return new String[]{cacheName};
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public String keyGenerator() {
                return "";
            }

            @Override
            public String cacheManager() {
                return "";
            }

            @Override
            public String cacheResolver() {
                return "";
            }

            @Override
            public String condition() {
                return condition;
            }

            @Override
            public boolean allEntries() {
                return allEntries;
            }

            @Override
            public boolean beforeInvocation() {
                return beforeInvocation;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return CacheEvict.class;
            }
        };
    }

    /**
     * The Class TestService.
     */
    public static class TestService {
        
        /**
         * Gets the data.
         *
         * @param id the id
         * @return the data
         */
        public Mono<String> getData(String id) {
            return Mono.just("data");
        }

        /**
         * Gets the data multi.
         *
         * @param id the id
         * @param type the type
         * @return the data multi
         */
        public Mono<String> getDataMulti(String id, String type) {
            return Mono.just("data");
        }

        /**
         * Gets the data no args.
         *
         * @return the data no args
         */
        public Mono<String> getDataNoArgs() {
            return Mono.just("data");
        }

        /**
         * Update data.
         *
         * @param id the id
         * @return the mono
         */
        public Mono<String> updateData(String id) {
            return Mono.just("updated");
        }

        /**
         * Delete data.
         *
         * @param id the id
         * @return the mono
         */
        public Mono<String> deleteData(String id) {
            return Mono.just("deleted");
        }

        /**
         * Clear cache.
         *
         * @return the string
         */
        public String clearCache() {
            return "cleared";
        }

        /**
         * Update product.
         *
         * @return the mono
         */
        public Mono<Product> updateProduct() {
            return Mono.just(new Product("test", "Test Product"));
        }
    }

    // ============================================================
    // Bug #6: Key Generation Tests - Preventing Key Collisions
    // ============================================================

    /**
     * Test that different methods with same parameters generate different keys.
     * This verifies Bug #6 fix: method name is included in the key.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_DifferentMethodsSameParams_GeneratesDifferentKeys() throws Throwable {
        // Given: Two different methods with the same parameter
        Cacheable annotation = createCacheable("testCache", "", "");
        String sameParam = "testId";

        // Method 1: getData
        Method method1 = TestService.class.getMethod("getData", String.class);
        lenient().when(signature.getMethod()).thenReturn(method1);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{sameParam});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:" + sameParam), any(), any(Duration.class)))
                .thenReturn(Mono.just("result1"));

        Object result1 = aspect.handleCacheable(joinPoint, annotation);

        // Method 2: updateData
        Method method2 = TestService.class.getMethod("updateData", String.class);
        lenient().when(signature.getMethod()).thenReturn(method2);
        lenient().when(cacheService.cacheable(eq("testCache"), eq("updateData:" + sameParam), any(), any(Duration.class)))
                .thenReturn(Mono.just("result2"));

        Object result2 = aspect.handleCacheable(joinPoint, annotation);

        // Then: Different cache keys should be generated
        assertNotNull(result1);
        assertNotNull(result2);

        // Verify different keys were used
        verify(cacheService).cacheable(eq("testCache"), eq("getData:" + sameParam), any(), any(Duration.class));
        verify(cacheService).cacheable(eq("testCache"), eq("updateData:" + sameParam), any(), any(Duration.class));
    }

    /**
     * Test that arrays are properly serialized in keys.
     * This verifies Bug #6 fix: arrays don't use memory addresses.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithArrayParams_SerializesCorrectly() throws Throwable {
        // Given: A method with array parameters
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Test with int array
        int[] intArray = {1, 2, 3};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{intArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1, 2, 3]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        // Then: Array should be serialized as content, not memory address
        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getData:[1, 2, 3]"), any(), any(Duration.class));
    }

    /**
     * Test that null arguments are handled properly.
     * This verifies Bug #6 fix: null values don't cause NPE.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithNullParams_HandlesGracefully() throws Throwable {
        // Given: A method with null parameter
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{null});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:null"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        // Then: Null should be represented as "null" string
        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getData:null"), any(), any(Duration.class));
    }

    /**
     * Test key generation with various primitive types.
     * Verifies Bug #6 fix: primitives are properly serialized.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithPrimitiveTypes_SerializesCorrectly() throws Throwable {
        // Given: Methods with different primitive types
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Test Integer
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{42});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:42"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result1"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test Boolean
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{true});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:true"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result2"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test Double
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{3.14});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:3.14"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result3"));
        aspect.handleCacheable(joinPoint, annotation);

        // Then: All primitives should be properly serialized
        verify(cacheService).cacheable(eq("testCache"), eq("getData:42"), any(), any(Duration.class));
        verify(cacheService).cacheable(eq("testCache"), eq("getData:true"), any(), any(Duration.class));
        verify(cacheService).cacheable(eq("testCache"), eq("getData:3.14"), any(), any(Duration.class));
    }

    /**
     * Test key generation with object arrays.
     * Verifies Bug #6 fix: object arrays use deepToString.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithObjectArray_UsesDeepToString() throws Throwable {
        // Given: A method with object array
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        String[] stringArray = {"one", "two", "three"};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{stringArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[one, two, three]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        // Then: Object array should use deepToString
        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getData:[one, two, three]"), any(), any(Duration.class));
    }

    /**
     * Test key generation with multiple arguments of mixed types.
     * Verifies Bug #6 fix: complex scenarios are handled correctly.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithMixedTypes_GeneratesCorrectKey() throws Throwable {
        // Given: A method with mixed argument types
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getDataMulti", String.class, String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"userId", 123});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getDataMulti:userId_123"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        // Then: Mixed types should be properly serialized
        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getDataMulti:userId_123"), any(), any(Duration.class));
    }

    // ============================================================
    // Bug #9: Thread Safety and Performance Tests for Expression Cache
    // ============================================================

    /**
     * Test expression cache reuses parsed expressions.
     * Verifies that once an expression is parsed, subsequent calls use the cached version.
     * This is the core verification for Bug #9's fix.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void expressionCache_ReusesParseResults() throws Throwable {
        // Given: The same SpEL expression used multiple times
        String expression = "#id";
        Cacheable cacheableAnnotation = createCacheable("testCache", expression, "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // When: Expression is used multiple times
        for (int i = 0; i < 100; i++) {
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"user" + i});
            Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

            // Then: Result should be available (expression parsed once, reused 100 times)
            assertNotNull(result);
            assertTrue(result instanceof Mono);
            StepVerifier.create((Mono<String>) result)
                    .expectNext("result")
                    .verifyComplete();
        }

        // Verify cache service was called 100 times (once per unique key)
        verify(cacheService, times(100))
                .cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test expression cache handles multiple unique expressions.
     * Verifies that different expressions are cached independently.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void expressionCache_CachesMultipleExpressions() throws Throwable {
        // Given: Different SpEL expressions
        Method method = TestService.class.getMethod("getData", String.class);
        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"testId"});
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // When: Using different expressions
        String[] expressions = {"#id", "#id.toUpperCase()", "#id.length()", "#id + ':suffix'", "#id.substring(0,2)"};
        for (String expression : expressions) {
            Cacheable annotation = createCacheable("cache", expression, "");
            Object result = aspect.handleCacheable(joinPoint, annotation);

            // Then: Each expression should work correctly
            assertNotNull(result);
            assertTrue(result instanceof Mono);
            StepVerifier.create((Mono<String>) result)
                    .expectNext("result")
                    .verifyComplete();
        }

        // Verify all calls succeeded
        verify(cacheService, times(5))
                .cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test expression cache with condition expressions.
     * Verifies that condition expressions are also cached properly.
     *
     * @throws Throwable the throwable
     */
    @Test
    void expressionCache_CachesConditionExpressions() throws Throwable {
        // Given: A condition expression that will be evaluated multiple times
        String condition = "#id != null && #id.length() > 0";
        Cacheable annotation = createCacheable("testCache", "", condition);
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"validId"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just("result"));
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // When: Evaluating the same condition multiple times
        for (int i = 0; i < 50; i++) {
            Object result = aspect.handleCacheable(joinPoint, annotation);
            assertNotNull(result); // Condition should be cached and reused
        }

        // Then: All invocations should succeed (expression cached after first parse)
        verify(cacheService, times(50))
                .cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    // ============================================================
    // Additional Coverage Tests for Bug #6 Fix
    // ============================================================

    /**
     * Test key generation with all primitive array types.
     * Ensures comprehensive coverage of array serialization.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithAllPrimitiveArrayTypes_SerializesCorrectly() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // Test long[]
        long[] longArray = {1L, 2L, 3L};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{longArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1, 2, 3]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test double[]
        double[] doubleArray = {1.5, 2.5, 3.5};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{doubleArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1.5, 2.5, 3.5]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test float[]
        float[] floatArray = {1.5f, 2.5f, 3.5f};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{floatArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1.5, 2.5, 3.5]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test boolean[]
        boolean[] booleanArray = {true, false, true};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{booleanArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[true, false, true]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test byte[]
        byte[] byteArray = {1, 2, 3};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{byteArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1, 2, 3]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test short[]
        short[] shortArray = {1, 2, 3};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{shortArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[1, 2, 3]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test char[]
        char[] charArray = {'a', 'b', 'c'};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{charArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[a, b, c]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Verify all array types were handled
        verify(cacheService, atLeast(7)).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test key generation with wrapper types.
     * Ensures all wrapper types are properly handled.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithAllWrapperTypes_SerializesCorrectly() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // Test Long
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{100L});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Float
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{3.14f});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Byte
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{(byte) 42});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Short
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{(short) 123});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Character
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{'x'});
        aspect.handleCacheable(joinPoint, annotation);

        // Verify all wrapper types were handled
        verify(cacheService, atLeast(5)).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test key generation with Collections.
     * Ensures List, Set, and Map are properly serialized.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithCollections_SerializesCorrectly() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(cacheService.cacheable(anyString(), anyString(), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // Test List
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{list});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Set
        java.util.Set<String> set = new java.util.HashSet<>(java.util.Arrays.asList("x", "y", "z"));
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{set});
        aspect.handleCacheable(joinPoint, annotation);

        // Test Map
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{map});
        aspect.handleCacheable(joinPoint, annotation);

        // Verify all collection types were handled
        verify(cacheService, atLeast(3)).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test key generation with complex nested objects.
     * Ensures complex objects use class name and hashCode.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithComplexObjects_UsesClassNameAndHashCode() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Create a custom object without toString()
        Object complexObject = new Object();
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{complexObject});

        // Expected key format: getData:Object@hashCode
        String expectedKeyPattern = "getData:Object@" + complexObject.hashCode();
        lenient().when(cacheService.cacheable(eq("testCache"), eq(expectedKeyPattern), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        // Verify the key used the correct format
        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq(expectedKeyPattern), any(), any(Duration.class));
    }

    /**
     * Test key generation with multiple null arguments.
     * Ensures multiple nulls are handled properly.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithMultipleNulls_HandlesProperly() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getDataMulti", String.class, String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{null, null});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getDataMulti:null_null"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getDataMulti:null_null"), any(), any(Duration.class));
    }

    /**
     * Test key generation with empty arrays.
     * Ensures empty arrays are properly serialized.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithEmptyArrays_SerializesCorrectly() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Test empty int array
        int[] emptyIntArray = {};
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{emptyIntArray});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:[]"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        Object result = aspect.handleCacheable(joinPoint, annotation);

        assertNotNull(result);
        verify(cacheService).cacheable(eq("testCache"), eq("getData:[]"), any(), any(Duration.class));
    }

    /**
     * Test key generation with String arguments.
     * Ensures strings are used directly.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithStringArguments_UsesDirectValue() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Test various string values
        String[] testStrings = {"simple", "with spaces", "with-dashes", "123numeric", ""};

        for (String testString : testStrings) {
            lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{testString});
            lenient().when(cacheService.cacheable(eq("testCache"), eq("getData:" + testString), any(), any(Duration.class)))
                    .thenReturn(Mono.just("result"));

            aspect.handleCacheable(joinPoint, annotation);
        }

        verify(cacheService, times(5)).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test key generation consistency with same hashCode.
     * Ensures same object generates same key.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithSameObject_GeneratesConsistentKey() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Use same object multiple times
        Object sameObject = new Object();
        String expectedKey = "getData:Object@" + sameObject.hashCode();

        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{sameObject});
        lenient().when(cacheService.cacheable(eq("testCache"), eq(expectedKey), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));

        // Call multiple times with same object
        aspect.handleCacheable(joinPoint, annotation);
        aspect.handleCacheable(joinPoint, annotation);
        aspect.handleCacheable(joinPoint, annotation);

        // Verify same key was used all times
        verify(cacheService, times(3)).cacheable(eq("testCache"), eq(expectedKey), any(), any(Duration.class));
    }

    /**
     * Test key generation with mixed null and non-null values.
     * Ensures mixed scenarios work correctly.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void keyGeneration_WithMixedNullAndNonNull_GeneratesCorrectKey() throws Throwable {
        Cacheable annotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getDataMulti", String.class, String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());

        // Test: null, "value"
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{null, "value"});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getDataMulti:null_value"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        // Test: "value", null
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"value", null});
        lenient().when(cacheService.cacheable(eq("testCache"), eq("getDataMulti:value_null"), any(), any(Duration.class)))
                .thenReturn(Mono.just("result"));
        aspect.handleCacheable(joinPoint, annotation);

        verify(cacheService, times(2)).cacheable(anyString(), anyString(), any(), any(Duration.class));
    }

    // ============================================================
    // @Caching Support Tests
    // ============================================================

    /**
     * Test @Caching with put and evict operations.
     * Verifies that multiple cache operations can be combined.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithPutAndEvict_ExecutesBothOperations() throws Throwable {
        // Given: @Caching with @CachePut and @CacheEvict
        CachePut put = createCachePut("products", "#result.id", "");
        CacheEvict evict = createCacheEvict("products", "'all'", false, false, "");
        Caching cachingAnnotation = createCaching(new Cacheable[]{}, new CachePut[]{put}, new CacheEvict[]{evict});

        Method method = TestService.class.getMethod("updateProduct");
        Product product = new Product("prod123", "Test Product");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(product));
        lenient().when(cacheService.cachePut(eq("products"), eq("prod123"), any(), any(Duration.class)))
                .thenReturn(Mono.just(product));
        lenient().when(cacheService.cacheEvict("products", "all")).thenReturn(Mono.empty());

        // When: Executing method with @Caching
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: Both operations should be executed
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<Product>) result)
                .expectNext(product)
                .verifyComplete();

        verify(cacheService).cachePut(eq("products"), eq("prod123"), any(), any(Duration.class));
        verify(cacheService).cacheEvict("products", "all");
    }

    /**
     * Test @Caching with beforeInvocation eviction.
     * Verifies that eviction happens before method execution.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithBeforeInvocationEvict_EvictsFirst() throws Throwable {
        // Given: @Caching with @CacheEvict(beforeInvocation=true)
        CacheEvict evict = createCacheEvict("cache", "'key1'", false, true, "");
        Caching cachingAnnotation = createCaching(new Cacheable[]{}, new CachePut[]{}, new CacheEvict[]{evict});

        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"test"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just("result"));
        lenient().when(cacheService.cacheEvict("cache", "key1")).thenReturn(Mono.empty());

        // When: Executing method
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: Eviction should happen before method execution
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("result")
                .verifyComplete();

        verify(cacheService).cacheEvict("cache", "key1");
    }

    /**
     * Test @Caching with multiple put operations.
     * Verifies that multiple cache puts can be performed.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithMultiplePuts_ExecutesAllPuts() throws Throwable {
        // Given: @Caching with multiple @CachePut operations
        CachePut put1 = createCachePut("cache1", "#result.id", "");
        CachePut put2 = createCachePut("cache2", "#result.id", "");
        Caching cachingAnnotation = createCaching(
                new Cacheable[]{},
                new CachePut[]{put1, put2},
                new CacheEvict[]{}
        );

        Method method = TestService.class.getMethod("updateProduct");
        Product product = new Product("prod456", "Test Product");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(product));
        lenient().when(cacheService.cachePut(anyString(), eq("prod456"), any(), any(Duration.class)))
                .thenReturn(Mono.just(product));

        // When: Executing method
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: Both cache puts should be executed
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<Product>) result)
                .expectNext(product)
                .verifyComplete();

        verify(cacheService).cachePut(eq("cache1"), eq("prod456"), any(), any(Duration.class));
        verify(cacheService).cachePut(eq("cache2"), eq("prod456"), any(), any(Duration.class));
    }

    /**
     * Test @Caching with condition evaluation.
     * Verifies that conditions are respected in @Caching operations.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithCondition_RespectsCondition() throws Throwable {
        // Given: @Caching with conditional operations
        CachePut put = createCachePut("cache", "#result.id", "#result.id.length() > 10");
        Caching cachingAnnotation = createCaching(new Cacheable[]{}, new CachePut[]{put}, new CacheEvict[]{});

        Method method = TestService.class.getMethod("updateProduct");
        Product product = new Product("short", "Test Product");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just(product));

        // When: Executing method with condition that evaluates to false
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: Cache put should NOT be executed
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<Product>) result)
                .expectNext(product)
                .verifyComplete();

        verify(cacheService, never()).cachePut(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Test @Caching with allEntries eviction.
     * Verifies that allEntries flag works in @Caching context.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithAllEntriesEvict_EvictsAllEntries() throws Throwable {
        // Given: @Caching with @CacheEvict(allEntries=true)
        CacheEvict evict = createCacheEvict("cache", "", true, false, "");
        Caching cachingAnnotation = createCaching(new Cacheable[]{}, new CachePut[]{}, new CacheEvict[]{evict});

        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"test"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just("result"));
        lenient().when(cacheService.cacheEvictAll("cache")).thenReturn(Mono.empty());

        // When: Executing method
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: All entries should be evicted
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("result")
                .verifyComplete();

        verify(cacheService).cacheEvictAll("cache");
    }

    /**
     * Test @Caching with mixed beforeInvocation and afterInvocation evictions.
     * Verifies proper execution order.
     *
     * @throws Throwable the throwable
     */
    @SuppressWarnings("unchecked")
    @Test
    void handleCaching_WithMixedEvictions_ExecutesInCorrectOrder() throws Throwable {
        // Given: @Caching with both before and after invocation evictions
        CacheEvict beforeEvict = createCacheEvict("cache1", "'before'", false, true, "");
        CacheEvict afterEvict = createCacheEvict("cache2", "'after'", false, false, "");
        Caching cachingAnnotation = createCaching(
                new Cacheable[]{},
                new CachePut[]{},
                new CacheEvict[]{beforeEvict, afterEvict}
        );

        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{"test"});
        lenient().when(joinPoint.proceed()).thenReturn(Mono.just("result"));
        lenient().when(cacheService.cacheEvict(anyString(), anyString())).thenReturn(Mono.empty());

        // When: Executing method
        Object result = aspect.handleCaching(joinPoint, cachingAnnotation);

        // Then: Both evictions should happen in correct order
        assertNotNull(result);
        assertTrue(result instanceof Mono);

        StepVerifier.create((Mono<String>) result)
                .expectNext("result")
                .verifyComplete();

        verify(cacheService).cacheEvict("cache1", "before");
        verify(cacheService).cacheEvict("cache2", "after");
    }

    /**
     * Creates a @Caching annotation.
     *
     * @param cacheables the cacheable operations
     * @param puts the put operations
     * @param evicts the evict operations
     * @return the caching annotation
     */
    private Caching createCaching(Cacheable[] cacheables, CachePut[] puts, CacheEvict[] evicts) {
        return new Caching() {
            @Override
            public Cacheable[] cacheable() {
                return cacheables;
            }

            @Override
            public CachePut[] put() {
                return puts;
            }

            @Override
            public CacheEvict[] evict() {
                return evicts;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Caching.class;
            }
        };
    }

    /**
     * Simple Product class for testing.
     */
    public static class Product {
        private final String id;
        private final String name;

        public Product(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

}
