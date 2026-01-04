package io.github.ajuarez0021.redis.reactive.annotation;

import io.github.ajuarez0021.redis.reactive.aspect.ReactiveCacheConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Interface EnableReactiveCache.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ReactiveCacheConfig.class)
public @interface EnableReactiveCache {
}
