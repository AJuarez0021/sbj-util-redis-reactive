package io.github.ajuarez0021.redis.reactive.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.ReactiveServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The Class RedisHealthCheckerTest.
 */
@ExtendWith(MockitoExtension.class)
class RedisHealthCheckerTest {

    /** The redis template. */
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /** The connection factory. */
    @Mock
    private ReactiveRedisConnectionFactory connectionFactory;

    /** The connection. */
    @Mock
    private ReactiveRedisConnection connection;

    /** The server commands. */
    @Mock
    private ReactiveServerCommands serverCommands;

    /** The health checker. */
    private RedisHealthChecker healthChecker;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        healthChecker = new RedisHealthChecker(redisTemplate);
    }

    /**
     * Checks if is redis active when redis template is null returns error status.
     */
    @Test
    void isRedisActive_WhenRedisTemplateIsNull_ReturnsErrorStatus() {
        healthChecker = new RedisHealthChecker(null);

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertFalse(status.isConnected());
                    assertEquals("RedisTemplate is not configured", status.getErrorMessage());
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when connection factory is null returns error status.
     */
    @Test
    void isRedisActive_WhenConnectionFactoryIsNull_ReturnsErrorStatus() {
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertFalse(status.isConnected());
                    assertEquals("ConnectionFactory is not available", status.getErrorMessage());
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when redis responds with pong returns connected status.
     */
    @Test
    void isRedisActive_WhenRedisRespondsWithPong_ReturnsConnectedStatus() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("PONG"));
        when(connection.serverCommands()).thenReturn(serverCommands);

        Properties properties = new Properties();
        properties.setProperty("used_memory", "1048576");
        properties.setProperty("maxmemory", "4194304");
        properties.setProperty("connected_clients", "5");
        properties.setProperty("redis_version", "7.0.0");

        when(serverCommands.info()).thenReturn(Mono.just(properties));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertTrue(status.isConnected());
                    assertEquals("Redis is up and running and responding correctly", status.getErrorMessage());
                    assertEquals(1048576L, status.getUsedMemory());
                    assertEquals(4194304L, status.getMaxMemory());
                    assertEquals(5, status.getConnectedClients());
                    assertEquals("7.0.0", status.getRedisVersion());
                    assertTrue(status.getResponseTime() >= 0);
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when redis responds with non pong returns disconnected status.
     */
    @Test
    void isRedisActive_WhenRedisRespondsWithNonPong_ReturnsDisconnectedStatus() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("INVALID"));
        when(connection.serverCommands()).thenReturn(serverCommands);

        Properties properties = new Properties();
        properties.setProperty("used_memory", "1048576");
        properties.setProperty("maxmemory", "4194304");
        properties.setProperty("connected_clients", "5");
        properties.setProperty("redis_version", "7.0.0");

        when(serverCommands.info()).thenReturn(Mono.just(properties));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertFalse(status.isConnected());
                    assertTrue(status.getErrorMessage().contains("not responding as expected"));
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when properties have invalid numbers uses default values.
     */
    @Test
    void isRedisActive_WhenPropertiesHaveInvalidNumbers_UsesDefaultValues() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("PONG"));
        when(connection.serverCommands()).thenReturn(serverCommands);

        Properties properties = new Properties();
        properties.setProperty("used_memory", "invalid");
        properties.setProperty("maxmemory", "not_a_number");
        properties.setProperty("connected_clients", "xyz");
        properties.setProperty("redis_version", "7.0.0");

        when(serverCommands.info()).thenReturn(Mono.just(properties));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertTrue(status.isConnected());
                    assertEquals(0L, status.getUsedMemory());
                    assertEquals(0L, status.getMaxMemory());
                    assertEquals(0, status.getConnectedClients());
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when properties are null uses default values.
     */
    @Test
    void isRedisActive_WhenPropertiesAreNull_UsesDefaultValues() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("PONG"));
        when(connection.serverCommands()).thenReturn(serverCommands);

        Properties properties = new Properties();
        
        when(serverCommands.info()).thenReturn(Mono.just(properties));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertTrue(status.isConnected());
                    assertEquals(0L, status.getUsedMemory());
                    assertEquals(0L, status.getMaxMemory());
                    assertEquals(0, status.getConnectedClients());
                    assertNull(status.getRedisVersion());
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active when exception occurs returns error status.
     */
    @Test
    void isRedisActive_WhenExceptionOccurs_ReturnsErrorStatus() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.error(new RuntimeException("Connection failed")));
        when(connection.serverCommands()).thenReturn(serverCommands);
        when(serverCommands.info()).thenReturn(Mono.just(new Properties()));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertFalse(status.isConnected());
                    assertTrue(status.getErrorMessage().contains("Error checking Redis status"));
                    assertTrue(status.getErrorMessage().contains("Connection failed"));
                })
                .verifyComplete();
    }

    /**
     * Checks if is redis active case insensitive pong returns connected status.
     */
    @Test
    void isRedisActive_CaseInsensitivePong_ReturnsConnectedStatus() {
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getReactiveConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn(Mono.just("pong")); // lowercase
        when(connection.serverCommands()).thenReturn(serverCommands);

        Properties properties = new Properties();
        properties.setProperty("redis_version", "7.0.0");

        when(serverCommands.info()).thenReturn(Mono.just(properties));

        StepVerifier.create(healthChecker.isRedisActive())
                .assertNext(status -> {
                    assertTrue(status.isConnected());
                })
                .verifyComplete();
    }
}
