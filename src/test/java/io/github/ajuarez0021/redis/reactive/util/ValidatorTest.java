package io.github.ajuarez0021.redis.reactive.util;

import io.github.ajuarez0021.redis.reactive.dto.HostsDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationAttributes;

import java.time.Duration;
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

    /**
     * Validate cache name and key valid no exception.
     */
    @Test
    void validateCacheNameAndKey_Valid_NoException() {
        String cacheName = "users";
        String key = "user1";

        assertDoesNotThrow(() -> Validator.validateCacheNameAndKey(cacheName, key));
    }

    /**
     * Validate cache name and key null cache name throws exception.
     */
    @Test
    void validateCacheNameAndKey_NullCacheName_ThrowsException() {
        String cacheName = null;
        String key = "user1";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheNameAndKey(cacheName, key)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache name and key empty cache name throws exception.
     */
    @Test
    void validateCacheNameAndKey_EmptyCacheName_ThrowsException() {
        String cacheName = "";
        String key = "user1";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheNameAndKey(cacheName, key)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache name and key null key throws exception.
     */
    @Test
    void validateCacheNameAndKey_NullKey_ThrowsException() {
        String cacheName = "users";
        String key = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheNameAndKey(cacheName, key)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate cache name and key empty key throws exception.
     */
    @Test
    void validateCacheNameAndKey_EmptyKey_ThrowsException() {
        String cacheName = "users";
        String key = "";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheNameAndKey(cacheName, key)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate TTL valid no exception.
     */
    @Test
    void validateTTL_Valid_NoException() {
        String cacheName = "users";
        Long ttl = 3600L;

        assertDoesNotThrow(() -> Validator.validateTTL(cacheName, ttl));
    }

    /**
     * Validate TTL null cache name throws exception.
     */
    @Test
    void validateTTL_NullCacheName_ThrowsException() {
        String cacheName = null;
        Long ttl = 3600L;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateTTL(cacheName, ttl)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate TTL empty cache name throws exception.
     */
    @Test
    void validateTTL_EmptyCacheName_ThrowsException() {
        String cacheName = "   ";
        Long ttl = 3600L;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateTTL(cacheName, ttl)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate TTL null ttl throws exception.
     */
    @Test
    void validateTTL_NullTTL_ThrowsException() {
        String cacheName = "users";
        Long ttl = null;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTTL(cacheName, ttl)
        );
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate TTL zero ttl throws exception.
     */
    @Test
    void validateTTL_ZeroTTL_ThrowsException() {
        String cacheName = "users";
        Long ttl = 0L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTTL(cacheName, ttl)
        );
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate TTL negative ttl throws exception.
     */
    @Test
    void validateTTL_NegativeTTL_ThrowsException() {
        String cacheName = "users";
        Long ttl = -1L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateTTL(cacheName, ttl)
        );
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate cacheable valid no exception.
     */
    @Test
    void validateCacheable_Valid_NoException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        assertDoesNotThrow(() -> Validator.validateCacheable(cacheName, key, loader, ttl));
    }

    /**
     * Validate cacheable null ttl throws exception.
     */
    @Test
    void validateCacheable_NullTTL_ThrowsException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("ttl is required", exception.getMessage());
    }

    /**
     * Validate cacheable zero ttl throws exception.
     */
    @Test
    void validateCacheable_ZeroTTL_ThrowsException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ZERO;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate cacheable negative ttl throws exception.
     */
    @Test
    void validateCacheable_NegativeTTL_ThrowsException() {
        String cacheName = "users";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(-1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("ttl must be positive", exception.getMessage());
    }

    /**
     * Validate cacheable cache name with colon throws exception.
     */
    @Test
    void validateCacheable_CacheNameWithColon_ThrowsException() {
        String cacheName = "users:cache";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cacheable cache name with asterisk throws exception.
     */
    @Test
    void validateCacheable_CacheNameWithAsterisk_ThrowsException() {
        String cacheName = "users*";
        String key = "user1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cacheable key with colon throws exception.
     */
    @Test
    void validateCacheable_KeyWithColon_ThrowsException() {
        String cacheName = "users";
        String key = "user:1";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Validate cacheable key with asterisk throws exception.
     */
    @Test
    void validateCacheable_KeyWithAsterisk_ThrowsException() {
        String cacheName = "users";
        String key = "user*";
        Supplier<String> loader = () -> "value";
        Duration ttl = Duration.ofMinutes(10);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheable(cacheName, key, loader, ttl)
        );
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Validate cache evict with key valid no exception.
     */
    @Test
    void validateCacheEvict_WithKey_Valid_NoException() {
        String cacheName = "users";
        String key = "user1";

        assertDoesNotThrow(() -> Validator.validateCacheEvict(cacheName, key));
    }

    /**
     * Validate cache evict with key null cache name throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_NullCacheName_ThrowsException() {
        String cacheName = null;
        String key = "user1";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict with key empty cache name throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_EmptyCacheName_ThrowsException() {
        String cacheName = "   ";
        String key = "user1";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict with key null key throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_NullKey_ThrowsException() {
        String cacheName = "users";
        String key = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate cache evict with key empty key throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_EmptyKey_ThrowsException() {
        String cacheName = "users";
        String key = "   ";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("key is required", exception.getMessage());
    }

    /**
     * Validate cache evict with key cache name with colon throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_CacheNameWithColon_ThrowsException() {
        String cacheName = "users:cache";
        String key = "user1";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cache evict with key cache name with asterisk throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_CacheNameWithAsterisk_ThrowsException() {
        String cacheName = "users*";
        String key = "user1";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cache evict with key key with asterisk throws exception.
     */
    @Test
    void validateCacheEvict_WithKey_KeyWithAsterisk_ThrowsException() {
        String cacheName = "users";
        String key = "user*";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheEvict(cacheName, key)
        );
        assertEquals("key cannot contain '*' character", exception.getMessage());
    }

    /**
     * Validate cache evict without key valid no exception.
     */
    @Test
    void validateCacheEvict_WithoutKey_Valid_NoException() {
        String cacheName = "users";

        assertDoesNotThrow(() -> Validator.validateCacheEvict(cacheName));
    }

    /**
     * Validate cache evict without key null cache name throws exception.
     */
    @Test
    void validateCacheEvict_WithoutKey_NullCacheName_ThrowsException() {
        String cacheName = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict without key empty cache name throws exception.
     */
    @Test
    void validateCacheEvict_WithoutKey_EmptyCacheName_ThrowsException() {
        String cacheName = "   ";

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateCacheEvict(cacheName)
        );
        assertEquals("cacheName is required", exception.getMessage());
    }

    /**
     * Validate cache evict without key cache name with colon throws exception.
     */
    @Test
    void validateCacheEvict_WithoutKey_CacheNameWithColon_ThrowsException() {
        String cacheName = "users:cache";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheEvict(cacheName)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate cache evict without key cache name with asterisk throws exception.
     */
    @Test
    void validateCacheEvict_WithoutKey_CacheNameWithAsterisk_ThrowsException() {
        String cacheName = "users*";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Validator.validateCacheEvict(cacheName)
        );
        assertEquals("cacheName cannot contain ':' or '*' characters", exception.getMessage());
    }

    /**
     * Validate attributes valid no exception.
     */
    @Test
    void validateAttributes_Valid_NoException() {
        AnnotationAttributes attributes = new AnnotationAttributes();

        assertDoesNotThrow(() -> Validator.validateAttributes(attributes));
    }

    /**
     * Validate attributes null throws exception.
     */
    @Test
    void validateAttributes_Null_ThrowsException() {
        AnnotationAttributes attributes = null;

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> Validator.validateAttributes(attributes)
        );
        assertTrue(exception.getMessage().contains("Configuration not initialized"));
        assertTrue(exception.getMessage().contains("@EnableRedisReactiveLibrary"));
    }
}
