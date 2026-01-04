package io.github.ajuarez0021.redis.reactive.aspect;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * The Class ReactiveCacheConfig.
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = { "io.github.ajuarez0021.redis.reactive.aspect" })
public class ReactiveCacheConfig {

}
