package io.github.ajuarez0021.redis.reactive.config;

import io.github.ajuarez0021.redis.reactive.annotation.EnableReactiveCache;
import io.github.ajuarez0021.redis.reactive.annotation.EnableRedisReactiveLibrary;
import io.github.ajuarez0021.redis.reactive.dto.HostsDto;
import io.github.ajuarez0021.redis.reactive.service.CacheOperationBuilder;
import io.github.ajuarez0021.redis.reactive.service.ReactiveCacheManager;
import io.github.ajuarez0021.redis.reactive.service.RedisCacheService;
import io.github.ajuarez0021.redis.reactive.service.RedisHealthChecker;
import io.github.ajuarez0021.redis.reactive.util.Mode;
import io.github.ajuarez0021.redis.reactive.util.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Class CacheConfig.
 *
 * @author ajuar
 */
@Configuration
@Slf4j
@EnableReactiveCache
@ComponentScan(basePackages = {"io.github.ajuarez0021.redis.reactive.config"})
public class CacheConfig implements ImportAware {

    /**
     * The attribute username.
     */
    private static final String ATTRIBUTE_USER_NAME = "userName";
    /**
     * The attributes.
     */
    private AnnotationAttributes attributes;

    /**
     * Sets the import metadata.
     *
     * @param importMetadata the new import metadata
     */
    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        Map<String, Object> annotationAttrs = importMetadata.getAnnotationAttributes(
                EnableRedisReactiveLibrary.class.getName());

        if (annotationAttrs == null) {
            throw new IllegalStateException(
                    """
                    Unable to find @EnableRedisReactiveLibrary annotation attributes.
                    Ensure the annotation is present on a @Configuration class.
                    """);
        }

        this.attributes = AnnotationAttributes.fromMap(annotationAttrs);

    }

    /**
     * The map ttl.
     *
     * @return The Ttl object.
     */
    @Bean
    ReactiveCacheManager createTtl() {
        AnnotationAttributes[] ttlArray = attributes.getAnnotationArray("ttlEntries");
        Map<String, Long> ttlsMap = new HashMap<>();
        for (AnnotationAttributes entry : ttlArray) {
            if (entry != null) {
                String key = entry.getString("name");
                Long value = entry.getNumber("ttl");
                Validator.validateTTL(key, value);
                ttlsMap.put(key, value);
            }
        }
        return new ReactiveCacheManager(ttlsMap);
    }

    /**
     * Redis health checker.
     *
     * @param redisTemplate the redis template
     * @return the redis health checker
     */
    @Bean
    RedisHealthChecker redisHealthChecker(ReactiveRedisTemplate<String, Object> redisTemplate) {
        return new RedisHealthChecker(redisTemplate);
    }

    /**
     * Redis cache service.
     *
     * @param redisTemplate the redis template
     * @return the redis cache service
     */
    @Bean
    RedisCacheService redisCacheService(ReactiveRedisTemplate<String, Object> redisTemplate) {
        return new RedisCacheService(redisTemplate);
    }

    /**
     * Cache operation builder factory.
     *
     * @param redisCacheService the redis cache service
     * @return the cache operation builder factory
     */
    @Bean
    CacheOperationBuilder.Factory cacheOperationBuilderFactory(RedisCacheService redisCacheService) {
        return new CacheOperationBuilder.Factory(redisCacheService);
    }

    /**
     * Gets the host entries.
     *
     * @return the host entries
     */
    private List<HostsDto> getHostEntries() {
        List<HostsDto> list = new ArrayList<>();
        AnnotationAttributes[] hostArray = attributes.getAnnotationArray("hostEntries");

        for (AnnotationAttributes entry : hostArray) {
            if (entry != null) {
                String hostName = entry.getString("host");
                Number portNumber = entry.getNumber("port");
                int port = portNumber.intValue();
                list.add(HostsDto.builder().hostName(hostName).port(port).build());
            }
        }

        return list;
    }

    /**
     * Creates the standalone config.
     *
     * @return the redis standalone configuration
     */
    @Bean
    RedisStandaloneConfiguration createStandaloneConfig() {
        List<HostsDto> hosts = getHostEntries();
        Validator.validateStandaloneHosts(hosts);
        HostsDto host = hosts.getFirst();
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host.getHostName());
        redisConfig.setPort(host.getPort());

        String userName = attributes.getString(ATTRIBUTE_USER_NAME);
        String pwd = attributes.getString("pwd");

        if (StringUtils.hasText(userName)) {
            redisConfig.setUsername(userName);
        }

        if (StringUtils.hasText(pwd)) {
            redisConfig.setPassword(pwd);
        }

        return redisConfig;
    }

    /**
     * Creates the cluster config.
     *
     * @return the redis cluster configuration
     */
    @Bean
    RedisClusterConfiguration createClusterConfig() {
        List<HostsDto> hosts = getHostEntries();
        Validator.validateClusterHosts(hosts);
        List<String> nodes = hosts.stream()
                .map(h -> String.format("%s:%d", h.getHostName(), h.getPort()))
                .toList();
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(nodes);
        String userName = attributes.getString(ATTRIBUTE_USER_NAME);
        String pwd = attributes.getString("pwd");

        if (StringUtils.hasText(userName)) {
            clusterConfig.setUsername(userName);
        }

        if (StringUtils.hasText(pwd)) {
            clusterConfig.setPassword(pwd);
        }
        clusterConfig.setMaxRedirects(3);
        return clusterConfig;
    }

    /**
     * Creates the sentinel config.
     *
     * @return the redis sentinel configuration
     */
    @Bean
    RedisSentinelConfiguration createSentinelConfig() {
        String sentinelMaster = attributes.getString("sentinelMaster");
        List<HostsDto> hosts = getHostEntries();
        Validator.validateSentinelHosts(hosts, sentinelMaster);
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(sentinelMaster);

        for (HostsDto host : hosts) {
            sentinelConfig.sentinel(host.getHostName(), host.getPort());
        }
        String userName = attributes.getString(ATTRIBUTE_USER_NAME);
        String pwd = attributes.getString("pwd");
        if (StringUtils.hasText(userName)) {
            sentinelConfig.setSentinelUsername(userName);
        }
        if (StringUtils.hasText(pwd)) {
            sentinelConfig.setPassword(pwd);
        }

        return sentinelConfig;
    }

    /**
     * Creates the redis connection factory.
     *
     * @return the reactive redis connection factory
     */
    @Bean
    @ConditionalOnMissingBean
    ReactiveRedisConnectionFactory createRedisConnectionFactory() {
        LettuceClientConfiguration clientConfiguration;
        boolean useSsl = attributes.getBoolean("useSsl");
        Long connectionTimeout = attributes.getNumber("connectionTimeout");
        Long readTimeout = attributes.getNumber("readTimeout");

        Validator.validateTimeout(connectionTimeout, readTimeout);
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder
                = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(connectionTimeout))
                .shutdownTimeout(Duration.ofMillis(readTimeout));
        clientConfiguration = useSsl
                ? clientConfigurationBuilder.useSsl().and().build()
                : clientConfigurationBuilder.build();

        Mode mode = attributes.getEnum("mode");

        return switch (mode) {
            case Mode.CLUSTER ->
                    new LettuceConnectionFactory(Objects.requireNonNull(createClusterConfig()), clientConfiguration);
            case Mode.STANDALONE ->
                    new LettuceConnectionFactory(Objects.requireNonNull(createStandaloneConfig()), clientConfiguration);
            case Mode.SENTINEL ->
                    new LettuceConnectionFactory(Objects.requireNonNull(createSentinelConfig()), clientConfiguration);
            default -> throw new IllegalArgumentException("Invalid mode");
        };
    }

    /**
     * Creates the redis template.
     *
     * @param <T> the generic type
     * @return the reactive redis template
     */
    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("unchecked")
    <T> ReactiveRedisTemplate<String, T> createRedisTemplate() {

        ObjectMapperConfig mapperConfig = getObjectMapperConfig();
        CustomJackson2JsonRedisSerializer<T> jsonSerializer = new CustomJackson2JsonRedisSerializer<>(
                mapperConfig.configure(), (Class<T>) Object.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializationContext<String, T> serializationContext
                = RedisSerializationContext.<String, T>newSerializationContext(
                        stringSerializer)
                .hashKey(stringSerializer)
                .key(stringSerializer)
                .value(jsonSerializer)
                .hashValue(jsonSerializer)
                .build();

        return new ReactiveRedisTemplate<>(createRedisConnectionFactory(), serializationContext);
    }

    /**
     * Gets the object mapper config.
     *
     * @return the object mapper config
     */
    private ObjectMapperConfig getObjectMapperConfig() {
        Class<?> configClass = attributes.getClass("mapperConfig");

        if (configClass == DefaultObjectMapperConfig.class) {
            log.debug("Using default ObjectMapperConfig");
            return new DefaultObjectMapperConfig();
        }

        if (!ObjectMapperConfig.class.isAssignableFrom(configClass)) {
            throw new IllegalArgumentException(
                    String.format("Mapper config class '%s' must implement ObjectMapperConfig interface",
                            configClass.getName()));
        }
        try {

            log.debug("Attempting to instantiate custom ObjectMapperConfig: {}",
                    configClass.getName());
            ObjectMapperConfig config = (ObjectMapperConfig) configClass
                    .getDeclaredConstructor()
                    .newInstance();
            log.debug("Successfully instantiated custom ObjectMapperConfig: {}",
                    configClass.getName());
            return config;
        } catch (ReflectiveOperationException ex) {
            log.error("""
                              Failed to instantiate custom ObjectMapperConfig: {}.
                              Ensure the class has a public no-args constructor.
                              Falling back to default configuration. {}
                            """,
                    configClass.getName(), ex.getMessage());
            return new DefaultObjectMapperConfig();
        }
    }
}
