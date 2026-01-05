package io.github.ajuarez0021.redis.reactive.annotation;

import io.github.ajuarez0021.redis.reactive.config.CacheConfig;
import io.github.ajuarez0021.redis.reactive.config.DefaultObjectMapperConfig;
import io.github.ajuarez0021.redis.reactive.config.ObjectMapperConfig;
import io.github.ajuarez0021.redis.reactive.util.Mode;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author ajuar
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CacheConfig.class)
public @interface EnableRedisReactiveLibrary {
    /**
     * Database.
     * Only for sentinel and standalone mode
     *
     * @return The database
     */
    int database() default 0;
    /**
     * Hosts.
     *
     * @return the string[]
     */
    HostEntry[] hostEntries() default {};

    /**
     * User name.
     *
     * @return the string
     */
    String userName() default "";

    /**
     * Pwd.
     *
     * @return the string
     */
    String pwd() default "";

    /**
     * Mode.
     *
     * @return the mode
     */
    Mode mode() default Mode.STANDALONE;

    /**
     * Connection timeout.
     *
     * @return the long
     */
    long connectionTimeout() default 5000;

    /**
     * Read timeout.
     *
     * @return the long
     */
    long readTimeout() default 3000;

    /**
     * Error handler.
     *
     * @return the class<? extends object mapper config>
     */
    Class<? extends ObjectMapperConfig> mapperConfig() default DefaultObjectMapperConfig.class;

    /**
     * Use ssl.
     *
     * @return true, if successful
     */
    boolean useSsl() default false;

    /**
     * Sentinel master name. Required when mode is SENTINEL.
     *
     * @return the sentinel master name
     */
    String sentinelMaster() default "";

    /**
     * Ttl entries.
     *
     * @return the TTL entry[]
     */
    TTLEntry[] ttlEntries() default {};
 
}
