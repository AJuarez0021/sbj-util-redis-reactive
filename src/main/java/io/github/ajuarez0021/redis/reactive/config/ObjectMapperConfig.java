
package io.github.ajuarez0021.redis.reactive.config;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author ajuar
 */
public interface ObjectMapperConfig {
     /**
     * Configure.
     *
     * @return the object mapper
     */
    ObjectMapper configure();
}
