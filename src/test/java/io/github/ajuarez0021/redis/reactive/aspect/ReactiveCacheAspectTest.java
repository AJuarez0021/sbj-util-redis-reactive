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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveListOperations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Duration;

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
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

      
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

    
        assertNotNull(result);
        assertTrue(result instanceof Mono);
  
		StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("key1"), any(), any(Duration.class));
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

        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"user123"});
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));

     
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
     * @throws Throwable the throwable
     */
    @Test
    void handleCacheable_WithNonMonoResult_ReturnsDirectly() throws Throwable {
       
        String testValue = "non-reactive-value";
        Cacheable cacheableAnnotation = createCacheable("testCache", "", "");
        Method method = TestService.class.getMethod("getData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenReturn(testValue);

     
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

      
        assertEquals(testValue, result);
        verify(cacheService, never()).cacheable(anyString(), anyString(), any(), any(Duration.class));
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cachePut(eq("testCache"), eq("key1"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

  
        Object result = aspect.handleCachePut(joinPoint, cachePutAnnotation);

   
        assertNotNull(result);
        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cachePut(eq("testCache"), eq("key1"), any(), any(Duration.class));
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));

   
        Object result = aspect.handleCachePut(joinPoint, cachePutAnnotation);

    
        assertNotNull(result);
        verify(cacheService, never()).cachePut(anyString(), anyString(), any(), any(Duration.class));
    }

    /**
     * Handle cache evict single key evicts from cache.
     *
     * @throws Throwable the throwable
     */
    @Test
    void handleCacheEvict_SingleKey_EvictsFromCache() throws Throwable {
  
        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, false, "");
        Method method = TestService.class.getMethod("deleteData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenReturn("deleted");
        when(cacheService.cacheEvict("testCache", "key1")).thenReturn(Mono.empty());

 
        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

      
        assertEquals("deleted", result);
        verify(cacheService).cacheEvict("testCache", "key1");
    }

    /**
     * Handle cache evict all entries evicts all from cache.
     *
     * @throws Throwable the throwable
     */
    @Test
    void handleCacheEvict_AllEntries_EvictsAllFromCache() throws Throwable {
   
        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", true, false, "");
        Method method = TestService.class.getMethod("clearCache");

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        lenient().when(joinPoint.getArgs()).thenReturn(new Object[]{});
        when(joinPoint.proceed()).thenReturn("cleared");
        when(cacheService.cacheEvictAll("testCache")).thenReturn(Mono.empty());


        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

       
        assertEquals("cleared", result);
        verify(cacheService).cacheEvictAll("testCache");
    }

    /**
     * Handle cache evict before invocation evicts before method call.
     *
     * @throws Throwable the throwable
     */
    @Test
    void handleCacheEvict_BeforeInvocation_EvictsBeforeMethodCall() throws Throwable {
   
        CacheEvict cacheEvictAnnotation = createCacheEvict("testCache", "", false, true, "");
        Method method = TestService.class.getMethod("deleteData", String.class);

        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(joinPoint.getTarget()).thenReturn(new TestService());
        when(joinPoint.getArgs()).thenReturn(new Object[]{"key1"});
        when(joinPoint.proceed()).thenReturn("deleted");
        when(cacheService.cacheEvict("testCache", "key1")).thenReturn(Mono.empty());

    
        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

   
        assertEquals("deleted", result);
        verify(cacheService).cacheEvict("testCache", "key1");
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
        when(joinPoint.proceed()).thenReturn(Mono.just("deleted"));
        when(cacheService.cacheEvict("testCache", "key1")).thenReturn(Mono.empty());

    
        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);

       
        assertNotNull(result);
        assertTrue(result instanceof Mono);
        StepVerifier.create((Mono<String>) result)
                .expectNext("deleted")
                .verifyComplete();

        verify(cacheService).cacheEvict("testCache", "key1");
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
        when(joinPoint.proceed()).thenReturn("not-deleted");

     
        Object result = aspect.handleCacheEvict(joinPoint, cacheEvictAnnotation);


        assertEquals("not-deleted", result);
        verify(cacheService, never()).cacheEvict(anyString(), anyString());
        verify(cacheService, never()).cacheEvictAll(anyString());
    }

    /**
     * Handle cacheable with multiple args generates composite key.
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("arg1_arg2"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

    
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

     
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("arg1_arg2"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with no args uses default key.
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("default"), any(), any(Duration.class)))
                .thenReturn(Mono.just(testValue));

       
        Object result = aspect.handleCacheable(joinPoint, cacheableAnnotation);

     
        StepVerifier.create((Mono<String>) result)
                .expectNext(testValue)
                .verifyComplete();

        verify(cacheService).cacheable(eq("testCache"), eq("default"), any(), any(Duration.class));
    }

    /**
     * Handle cacheable with invalid spel expression uses default key.
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
        when(joinPoint.proceed()).thenReturn(Mono.just(testValue));
        when(cacheService.cacheable(eq("testCache"), eq("key1"), any(), any(Duration.class)))
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
    }
}
