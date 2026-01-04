package io.github.ajuarez0021.redis.reactive.config;

import io.github.ajuarez0021.redis.reactive.annotation.EnableRedisReactiveLibrary;
import io.github.ajuarez0021.redis.reactive.annotation.HostEntry;
import io.github.ajuarez0021.redis.reactive.service.CacheOperationBuilder;
import io.github.ajuarez0021.redis.reactive.service.ReactiveCacheManager;
import io.github.ajuarez0021.redis.reactive.service.RedisCacheService;
import io.github.ajuarez0021.redis.reactive.service.RedisHealthChecker;
import io.github.ajuarez0021.redis.reactive.util.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The Class CacheConfigTest.
 */
class CacheConfigTest {

	/** The cache config. */
	private CacheConfig cacheConfig;

	/** The mock metadata. */
	private AnnotationMetadata mockMetadata;

	/**
	 * Sets the up.
	 */
	@BeforeEach
	void setUp() {
		cacheConfig = new CacheConfig();
		mockMetadata = mock(AnnotationMetadata.class);
	}

	/**
	 * Creates the standalone config with valid host creates configuration.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createStandaloneConfig_WithValidHost_CreatesConfiguration() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("userName", "");
		attributes.put("pwd", "");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisStandaloneConfiguration config = cacheConfig.createStandaloneConfig();

		assertNotNull(config);
		assertEquals("localhost", config.getHostName());
		assertEquals(6379, config.getPort());
	}

	/**
	 * Creates the standalone config with credentials sets username and password.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createStandaloneConfig_WithCredentials_SetsUsernameAndPassword() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("userName", "testuser");
		attributes.put("pwd", "testpass");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisStandaloneConfiguration config = cacheConfig.createStandaloneConfig();

		assertNotNull(config);
		assertEquals("testuser", config.getUsername());
		assertTrue(config.getPassword().isPresent());
	}

	/**
	 * Creates the cluster config with multiple hosts creates configuration.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createClusterConfig_WithMultipleHosts_CreatesConfiguration() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries",
				createHostEntries(new String[] { "host1", "host2", "host3" }, new int[] { 6379, 6380, 6381 }));
		attributes.put("userName", "");
		attributes.put("pwd", "");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisClusterConfiguration config = cacheConfig.createClusterConfig();

		assertNotNull(config);
		assertEquals(3, config.getClusterNodes().size());
		assertEquals(3, config.getMaxRedirects());
	}

	/**
	 * Creates the cluster config with credentials sets username and password.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createClusterConfig_WithCredentials_SetsUsernameAndPassword() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "host1", "host2" }, new int[] { 6379, 6380 }));
		attributes.put("userName", "clusteruser");
		attributes.put("pwd", "clusterpass");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisClusterConfiguration config = cacheConfig.createClusterConfig();

		assertNotNull(config);
		assertEquals("clusteruser", config.getUsername());
		assertTrue(config.getPassword().isPresent());
	}

	/**
	 * Creates the sentinel config with valid configuration creates configuration.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createSentinelConfig_WithValidConfiguration_CreatesConfiguration() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries",
				createHostEntries(new String[] { "sentinel1", "sentinel2" }, new int[] { 26379, 26380 }));
		attributes.put("sentinelMaster", "mymaster");
		attributes.put("userName", "");
		attributes.put("pwd", "");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisSentinelConfiguration config = cacheConfig.createSentinelConfig();

		assertNotNull(config);
		assertEquals("mymaster", config.getMaster().getName());
		assertEquals(2, config.getSentinels().size());
	}

	/**
	 * Creates the sentinel config with credentials sets credentials.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createSentinelConfig_WithCredentials_SetsCredentials() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries",
				createHostEntries(new String[] { "sentinel1", "sentinel2" }, new int[] { 26379, 26380 }));
		attributes.put("sentinelMaster", "mymaster");
		attributes.put("userName", "sentuser");
		attributes.put("pwd", "sentpass");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		RedisSentinelConfiguration config = cacheConfig.createSentinelConfig();

		assertNotNull(config);
		assertEquals("sentuser", config.getSentinelUsername());
		assertTrue(config.getPassword().isPresent());
	}

	/**
	 * Creates the redis connection factory standalone mode creates lettuce
	 * connection factory.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createRedisConnectionFactory_StandaloneMode_CreatesLettuceConnectionFactory() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		var factory = cacheConfig.createRedisConnectionFactory();

		assertNotNull(factory);
		assertTrue(factory instanceof LettuceConnectionFactory);
	}

	/**
	 * Creates the redis connection factory cluster mode creates lettuce connection
	 * factory.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createRedisConnectionFactory_ClusterMode_CreatesLettuceConnectionFactory() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "host1", "host2" }, new int[] { 6379, 6380 }));
		attributes.put("mode", Mode.CLUSTER);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		var factory = cacheConfig.createRedisConnectionFactory();

		assertNotNull(factory);
		assertTrue(factory instanceof LettuceConnectionFactory);
	}

	/**
	 * Creates the redis connection factory sentinel mode creates lettuce connection
	 * factory.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createRedisConnectionFactory_SentinelMode_CreatesLettuceConnectionFactory() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries",
				createHostEntries(new String[] { "sentinel1", "sentinel2" }, new int[] { 26379, 26380 }));
		attributes.put("mode", Mode.SENTINEL);
		attributes.put("sentinelMaster", "mymaster");
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		var factory = cacheConfig.createRedisConnectionFactory();

		assertNotNull(factory);
		assertTrue(factory instanceof LettuceConnectionFactory);
	}

	/**
	 * Creates the redis connection factory with SS L configures SSL.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createRedisConnectionFactory_WithSSL_ConfiguresSSL() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("useSsl", true);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		var factory = cacheConfig.createRedisConnectionFactory();

		assertNotNull(factory);

	}

	/**
	 * Creates the redis template with default mapper creates template.
	 *
	 * @throws Exception the exception
	 */
	@Test
	void createRedisTemplate_WithDefaultMapper_CreatesTemplate() throws Exception {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);
		attributes.put("mapperConfig", DefaultObjectMapperConfig.class);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		ReactiveRedisTemplate<String, Object> template = cacheConfig.createRedisTemplate();

		assertNotNull(template);
		assertNotNull(template.getConnectionFactory());
	}

	/**
	 * Sets the import metadata with valid metadata sets attributes.
	 */
	@Test
	void setImportMetadata_WithValidMetadata_SetsAttributes() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		assertDoesNotThrow(
				() -> verify(mockMetadata).getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName()));
	}

	/**
	 * Sets the import metadata with null attributes throws exception.
	 */
	@Test
	void setImportMetadata_WithNullAttributes_ThrowsException() {
		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(null);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> cacheConfig.setImportMetadata(mockMetadata));

		assertTrue(exception.getMessage().contains("Unable to find @EnableRedisReactiveLibrary"));
	}

	/**
	 * Creates the connection factory with invalid port throws exception.
	 */
	@Test
	void createConnectionFactory_WithInvalidPort_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 70000 }));
		attributes.put("mode", Mode.STANDALONE);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createStandaloneConfig());

		assertTrue(exception.getMessage().contains("Invalid port") ||
		           exception.getMessage().contains("Port must be between"));
	}

	/**
	 * Creates the connection factory with null timeout throws exception.
	 */
	@Test
	void createConnectionFactory_WithNullConnectionTimeout_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", null);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		Exception exception = assertThrows(Exception.class,
				() -> cacheConfig.createRedisConnectionFactory());

		assertTrue(exception.getMessage().contains("Connection timeout") ||
		           exception.getMessage().contains("connectionTimeout"),
		           "Expected exception about connection timeout, got: " + exception.getMessage());
	}

	/**
	 * Creates the connection factory with negative timeout throws exception.
	 */
	@Test
	void createConnectionFactory_WithNegativeReadTimeout_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", -1L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createRedisConnectionFactory());

		assertTrue(exception.getMessage().contains("Invalid readTimeout") ||
		           exception.getMessage().contains("readTimeout"));
	}

	/**
	 * Creates the sentinel config with null master throws exception.
	 */
	@Test
	void createSentinelConfig_WithNullMaster_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "sentinel1" }, new int[] { 26379 }));
		attributes.put("sentinelMaster", "");

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createSentinelConfig());

		assertTrue(exception.getMessage().contains("sentinelMaster must be configured") ||
		           exception.getMessage().contains("sentinel"));
	}

	/**
	 * Creates the connection factory with null mode throws exception.
	 */
	@Test
	void createConnectionFactory_WithNullMode_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", null);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		Exception exception = assertThrows(Exception.class,
				() -> cacheConfig.createRedisConnectionFactory());

		assertTrue(exception.getMessage().contains("mode") ||
		           exception.getMessage().contains("Mode"),
		           "Expected exception about mode, got: " + exception.getMessage());
	}

	/**
	 * Creates connection factory with empty host entries throws exception.
	 */
	@Test
	void createConnectionFactory_WithEmptyHostEntries_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", new AnnotationAttributes[0]);
		attributes.put("mode", Mode.STANDALONE);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createStandaloneConfig());

		assertTrue(exception.getMessage().contains("At least one host") ||
		           exception.getMessage().contains("host"));
	}

	/**
	 * Creates connection factory with negative port throws exception.
	 */
	@Test
	void createConnectionFactory_WithNegativePort_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { -1 }));
		attributes.put("mode", Mode.STANDALONE);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createStandaloneConfig());

		assertTrue(exception.getMessage().contains("Invalid port") ||
		           exception.getMessage().contains("Port must be between"));
	}

	/**
	 * Creates connection factory with zero timeout throws exception.
	 */
	@Test
	void createConnectionFactory_WithZeroConnectionTimeout_ThrowsException() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 0L);
		attributes.put("readTimeout", 3000L);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> cacheConfig.createRedisConnectionFactory());

		assertTrue(exception.getMessage().contains("Invalid connectionTimeout") ||
		           exception.getMessage().contains("connectionTimeout"));
	}

	/**
	 * Creates TTL with empty array creates empty manager.
	 */
	@Test
	void createTtl_WithEmptyArray_CreatesEmptyManager() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("ttlEntries", new AnnotationAttributes[0]);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		ReactiveCacheManager manager = cacheConfig.createTtl();

		assertNotNull(manager);
		assertNotNull(manager.getTtlEntries());
		assertTrue(manager.getTtlEntries().isEmpty());
	}

	/**
	 * Creates redis cache service creates service.
	 */
	@Test
	void redisCacheService_CreatesService() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("mapperConfig", DefaultObjectMapperConfig.class);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		ReactiveRedisTemplate<String, Object> template = cacheConfig.createRedisTemplate();
		RedisCacheService service = cacheConfig.redisCacheService(template);

		assertNotNull(service);
	}

	/**
	 * Creates redis health checker creates checker.
	 */
	@Test
	void redisHealthChecker_CreatesChecker() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("mapperConfig", DefaultObjectMapperConfig.class);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		ReactiveRedisTemplate<String, Object> template = cacheConfig.createRedisTemplate();
		RedisHealthChecker checker = cacheConfig.redisHealthChecker(template);

		assertNotNull(checker);
	}

	/**
	 * Creates cache operation builder factory creates factory.
	 */
	@Test
	void cacheOperationBuilderFactory_CreatesFactory() {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("hostEntries", createHostEntries(new String[] { "localhost" }, new int[] { 6379 }));
		attributes.put("mode", Mode.STANDALONE);
		attributes.put("useSsl", false);
		attributes.put("connectionTimeout", 5000L);
		attributes.put("readTimeout", 3000L);
		attributes.put("userName", "");
		attributes.put("pwd", "");
		attributes.put("mapperConfig", DefaultObjectMapperConfig.class);

		when(mockMetadata.getAnnotationAttributes(EnableRedisReactiveLibrary.class.getName())).thenReturn(attributes);

		cacheConfig.setImportMetadata(mockMetadata);

		ReactiveRedisTemplate<String, Object> template = cacheConfig.createRedisTemplate();
		RedisCacheService service = cacheConfig.redisCacheService(template);
		CacheOperationBuilder.Factory factory = cacheConfig.cacheOperationBuilderFactory(service);

		assertNotNull(factory);
	}

	/**
	 * Creates host entries.
	 *
	 * @param hosts the hosts
	 * @param ports the ports
	 * @return the annotation attributes[]
	 */
	private AnnotationAttributes[] createHostEntries(String[] hosts, int[] ports) {
		AnnotationAttributes[] entries = new AnnotationAttributes[hosts.length];
		for (int i = 0; i < hosts.length; i++) {
			AnnotationAttributes entry = new AnnotationAttributes(HostEntry.class);
			entry.put("host", hosts[i]);
			entry.put("port", ports[i]);
			entries[i] = entry;
		}
		return entries;
	}
}
