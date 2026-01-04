package io.github.ajuarez0021.redis.reactive.util;

import io.github.ajuarez0021.redis.reactive.dto.HostsDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class ValidatorTest.
 */
class ValidatorTest {

    /**
     * Validate hosts valid hosts no exception.
     */
    @Test
    void validateHosts_ValidHosts_NoException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(6379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(6380).build()
        );

        assertDoesNotThrow(() -> Validator.validateHosts(hosts));
    }

    /**
     * Validate hosts null list throws exception.
     */
    @Test
    void validateHosts_NullList_ThrowsException() {
        List<HostsDto> hosts = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("At least one host must be configured", exception.getMessage());
    }

    /**
     * Validate hosts empty list throws exception.
     */
    @Test
    void validateHosts_EmptyList_ThrowsException() {
        List<HostsDto> hosts = Collections.emptyList();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("At least one host must be configured", exception.getMessage());
    }

    /**
     * Validate hosts null host name throws exception.
     */
    @Test
    void validateHosts_NullHostName_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName(null).port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("Host name cannot be empty", exception.getMessage());
    }

    /**
     * Validate hosts empty host name throws exception.
     */
    @Test
    void validateHosts_EmptyHostName_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("   ").port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("Host name cannot be empty", exception.getMessage());
    }

    /**
     * Validate hosts invalid port too low throws exception.
     */
    @Test
    void validateHosts_InvalidPortTooLow_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(0).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("Invalid port 0. Port must be between 1 and 65535", exception.getMessage());
    }

    /**
     * Validate hosts invalid port too high throws exception.
     */
    @Test
    void validateHosts_InvalidPortTooHigh_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(65536).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateHosts(hosts)
        );
        assertEquals("Invalid port 65536. Port must be between 1 and 65535", exception.getMessage());
    }

    /**
     * Validate timeout valid timeouts no exception.
     */
    @Test
    void validateTimeout_ValidTimeouts_NoException() {
        Long connectionTimeout = 5000L;
        Long readTimeout = 3000L;

        assertDoesNotThrow(() -> Validator.validateTimeout(connectionTimeout, readTimeout));
    }

    /**
     * Validate timeout null connection timeout throws exception.
     */
    @Test
    void validateTimeout_NullConnectionTimeout_ThrowsException() {
        Long connectionTimeout = null;
        Long readTimeout = 3000L;

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> Validator.validateTimeout(connectionTimeout, readTimeout)
        );
        assertEquals("Connection timeout cannot be null", exception.getMessage());
    }

    /**
     * Validate timeout null read timeout throws exception.
     */
    @Test
    void validateTimeout_NullReadTimeout_ThrowsException() {
        Long connectionTimeout = 5000L;
        Long readTimeout = null;

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> Validator.validateTimeout(connectionTimeout, readTimeout)
        );
        assertEquals("Read timeout cannot be null", exception.getMessage());
    }

    /**
     * Validate timeout negative connection timeout throws exception.
     */
    @Test
    void validateTimeout_NegativeConnectionTimeout_ThrowsException() {
        Long connectionTimeout = -1L;
        Long readTimeout = 3000L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTimeout(connectionTimeout, readTimeout)
        );
        assertTrue(exception.getMessage().contains("Invalid connectionTimeout"));
    }

    /**
     * Validate timeout zero connection timeout throws exception.
     */
    @Test
    void validateTimeout_ZeroConnectionTimeout_ThrowsException() {
        Long connectionTimeout = 0L;
        Long readTimeout = 3000L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTimeout(connectionTimeout, readTimeout)
        );
        assertTrue(exception.getMessage().contains("Invalid connectionTimeout"));
    }

    /**
     * Validate timeout negative read timeout throws exception.
     */
    @Test
    void validateTimeout_NegativeReadTimeout_ThrowsException() {
        Long connectionTimeout = 5000L;
        Long readTimeout = -1L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTimeout(connectionTimeout, readTimeout)
        );
        assertTrue(exception.getMessage().contains("Invalid readTimeout"));
    }

    /**
     * Validate standalone hosts one host no exception.
     */
    @Test
    void validateStandaloneHosts_OneHost_NoException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(6379).build()
        );

        assertDoesNotThrow(() -> Validator.validateStandaloneHosts(hosts));
    }

    /**
     * Validate standalone hosts multiple hosts throws exception.
     */
    @Test
    void validateStandaloneHosts_MultipleHosts_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(6379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(6380).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateStandaloneHosts(hosts)
        );
        assertEquals("Standalone mode requires exactly one host entry", exception.getMessage());
    }

    /**
     * Validate cluster hosts multiple hosts no exception.
     */
    @Test
    void validateClusterHosts_MultipleHosts_NoException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(6379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(6380).build()
        );

        assertDoesNotThrow(() -> Validator.validateClusterHosts(hosts));
    }

    /**
     * Validate cluster hosts one host throws exception.
     */
    @Test
    void validateClusterHosts_OneHost_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(6379).build()
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateClusterHosts(hosts)
        );
        assertEquals("Cluster mode requires host entries for proper redundancy", exception.getMessage());
    }

    /**
     * Validate sentinel hosts valid configuration no exception.
     */
    @Test
    void validateSentinelHosts_ValidConfiguration_NoException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(26380).build()
        );
        String sentinelMaster = "mymaster";

        assertDoesNotThrow(() -> Validator.validateSentinelHosts(hosts, sentinelMaster));
    }

    /**
     * Validate sentinel hosts null sentinel master throws exception.
     */
    @Test
    void validateSentinelHosts_NullSentinelMaster_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(26380).build()
        );
        String sentinelMaster = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, sentinelMaster)
        );
        assertEquals("sentinelMaster must be configured when using SENTINEL mode", exception.getMessage());
    }

    /**
     * Validate sentinel hosts empty sentinel master throws exception.
     */
    @Test
    void validateSentinelHosts_EmptySentinelMaster_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(26379).build(),
                HostsDto.builder().hostName("127.0.0.1").port(26380).build()
        );
        String sentinelMaster = "   ";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, sentinelMaster)
        );
        assertEquals("sentinelMaster must be configured when using SENTINEL mode", exception.getMessage());
    }

    /**
     * Validate sentinel hosts one sentinel throws exception.
     */
    @Test
    void validateSentinelHosts_OneSentinel_ThrowsException() {
        List<HostsDto> hosts = List.of(
                HostsDto.builder().hostName("localhost").port(26379).build()
        );
        String sentinelMaster = "mymaster";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateSentinelHosts(hosts, sentinelMaster)
        );
        assertEquals("Sentinel mode requires host entries for proper redundancy", exception.getMessage());
    }

    /**
     * Validate required fields all valid no exception.
     */
    @Test
    void validateRequiredFields_AllValid_NoException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> "value";

        assertDoesNotThrow(() -> Validator.validateRequiredFields(cacheName, key, loader));
    }

    /**
     * Validate required fields null cache name throws exception.
     */
    @Test
    void validateRequiredFields_NullCacheName_ThrowsException() {
        String cacheName = null;
        String key = "user1";
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateRequiredFields(cacheName, key, loader)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate required fields empty cache name throws exception.
     */
    @Test
    void validateRequiredFields_EmptyCacheName_ThrowsException() {
        String cacheName = "   ";
        String key = "user1";
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateRequiredFields(cacheName, key, loader)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate required fields null key throws exception.
     */
    @Test
    void validateRequiredFields_NullKey_ThrowsException() {
        String cacheName = "users";
        String key = null;
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateRequiredFields(cacheName, key, loader)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate required fields empty key throws exception.
     */
    @Test
    void validateRequiredFields_EmptyKey_ThrowsException() {
        String cacheName = "users";
        String key = "   ";
        Supplier<String> loader = () -> "value";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateRequiredFields(cacheName, key, loader)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate required fields null loader throws exception.
     */
    @Test
    void validateRequiredFields_NullLoader_ThrowsException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateRequiredFields(cacheName, key, loader)
        );
        assertEquals("loader is required", exception.getMessage());
    }
}
