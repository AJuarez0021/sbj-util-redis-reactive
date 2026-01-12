package io.github.ajuarez0021.redis.reactive.service;

import io.github.ajuarez0021.redis.reactive.dto.RedisStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.Properties;

/**
 * The Class RedisHealthChecker.
 */
@Slf4j
public class RedisHealthChecker {
    
    /** The redis template. */
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * Instantiates a new redis health checker.
     *
     * @param redisTemplate the redis template
     */
    public RedisHealthChecker(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Creates the status.
     *
     * @param message the message
     * @return the redis status dto
     */
    private RedisStatusDto createStatus(String message) {

        return RedisStatusDto.builder()
                .connected(false)
                .errorMessage(message)
                .responseTime(0L)
                .maxMemory(0L)
                .usedMemory(0L)
                .connectedClients(0)
                .redisVersion("")
                .build();
    }

    /**
     * Checks if is redis active.
     *
     * @return mono of redis status dto
     */
    public Mono<RedisStatusDto> isRedisActive() {

        if (redisTemplate == null) {
            log.error("RedisTemplate is null");
            return Mono.just(createStatus("RedisTemplate is not configured"));
        }

        var connectionFactory = redisTemplate.getConnectionFactory();
        if (connectionFactory == null) {
            log.error("ConnectionFactory is null");
            return Mono.just(createStatus("ConnectionFactory is not available"));
        }

        long startTime = System.currentTimeMillis();

        return Mono.usingWhen(
            Mono.fromSupplier(connectionFactory::getReactiveConnection),

            connection -> Mono.zip(
                connection.ping(),
                connection.serverCommands().info()
            )
            .map(tuple -> {
                String pong = tuple.getT1();
                Properties properties = tuple.getT2();
                long responseTime = System.currentTimeMillis() - startTime;

                boolean isConnected = "PONG".equalsIgnoreCase(pong);
                String message = isConnected
                        ? "Redis is up and running and responding correctly"
                        : "Redis is not responding as expected. Response: " + pong;
                if (!isConnected) {
                    log.warn(message);
                }

                return RedisStatusDto.builder()
                        .connected(isConnected)
                        .errorMessage(message)
                        .responseTime(responseTime)
                        .usedMemory(parseLong(properties.getProperty("used_memory")))
                        .maxMemory(parseLong(properties.getProperty("maxmemory")))
                        .connectedClients(parseInt(properties.getProperty("connected_clients")))
                        .redisVersion(properties.getProperty("redis_version"))
                        .build();
            }),

            connection -> Mono.fromRunnable(connection::close),
            (connection, error) -> Mono.fromRunnable(connection::close),
            connection -> Mono.fromRunnable(connection::close)
        )
        .onErrorResume(e -> {
            log.error("Error checking Redis status: {}", e.getMessage());
            return Mono.just(createStatus(String.format("Error checking Redis status: %s", e.getMessage())));
        });
    }

    /**
     * Safe parse long.
     *
     * @param value the value
     * @return the long
     */
    private Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : 0L;
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Safe parse int.
     *
     * @param value the value
     * @return the integer
     */
    private Integer parseInt(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
