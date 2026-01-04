# Spring Boot Redis Reactive Library

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Coverage](https://img.shields.io/badge/coverage-80%25-brightgreen.svg)]()

A production-ready Spring Boot library for reactive Redis operations with comprehensive caching support, health monitoring, and flexible deployment modes.

## Features

**Reactive-First Design**
- Fully non-blocking operations using Project Reactor (`Mono<T>` and `Flux<T>`)
- Optimized for high-throughput reactive applications
- Zero blocking calls in critical paths

**Flexible Deployment Modes**
- **Standalone**: Single Redis instance
- **Cluster**: Multi-node Redis cluster with automatic failover
- **Sentinel**: High availability with Redis Sentinel

**Annotation-Driven Configuration**
- Simple setup with `@EnableRedisReactiveLibrary`
- Support for standard Spring Cache annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`)
- Custom TTL configuration per cache

**Programmatic API**
- `RedisCacheService`: Direct reactive cache operations
- `CacheOperationBuilder`: Fluent builder pattern for complex operations
- Support for cache hit/miss callbacks and conditional caching

**Health Monitoring**
- Built-in `RedisHealthChecker` for connection monitoring
- Real-time metrics: response time, memory usage, connected clients
- Redis server version detection

**Enterprise-Ready**
- SSL/TLS support
- Authentication (username/password)
- Configurable connection and read timeouts
- Custom Jackson `ObjectMapper` configuration

**High Test Coverage**
- 80%+ code coverage
- Comprehensive unit and integration tests
- Reactor `StepVerifier` for testing reactive flows

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
  - [Standalone Mode](#standalone-mode)
  - [Cluster Mode](#cluster-mode)
  - [Sentinel Mode](#sentinel-mode)
- [Usage](#usage)
  - [Annotation-Based Caching](#annotation-based-caching)
  - [Programmatic API](#programmatic-api)
  - [Fluent Builder API](#fluent-builder-api)
- [Health Monitoring](#health-monitoring)
- [Advanced Configuration](#advanced-configuration)
- [Architecture](#architecture)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Requirements

- **Java**: 21+
- **Spring Boot**: 4.0.1+
- **Redis**: 5.0+ (Standalone, Cluster, or Sentinel)
- **Maven**: 3.6+ or **Gradle**: 7.0+

## Installation

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ajuarez0021.redis.reactive</groupId>
    <artifactId>sbj-util-redis-reactive</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.github.ajuarez0021.redis.reactive:sbj-util-redis-reactive:1.0.0-SNAPSHOT'
```

## Quick Start

### 1. Enable the Library

Add `@EnableRedisReactiveLibrary` to your Spring Boot configuration class:

```java
import io.github.ajuarez0021.redis.reactive.annotation.EnableRedisReactiveLibrary;
import io.github.ajuarez0021.redis.reactive.annotation.HostEntry;
import io.github.ajuarez0021.redis.reactive.annotation.TTLEntry;
import io.github.ajuarez0021.redis.reactive.util.Mode;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRedisReactiveLibrary(
    mode = Mode.STANDALONE,
    hostEntries = {
        @HostEntry(host = "localhost", port = 6379)
    },
    connectionTimeout = 5000,
    readTimeout = 3000,
    ttlEntries = {
        @TTLEntry(name = "users", ttl = 3600),      // 1 hour
        @TTLEntry(name = "products", ttl = 1800)    // 30 minutes
    }
)
public class RedisConfig {
}
```

### 2. Use Cache Annotations

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    @Cacheable(cacheNames = "users", key = "#userId")
    public Mono<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    @CachePut(cacheNames = "users", key = "#user.id")
    public Mono<User> updateUser(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(cacheNames = "users", key = "#userId")
    public Mono<Void> deleteUser(String userId) {
        return userRepository.deleteById(userId);
    }

    @CacheEvict(cacheNames = "users", allEntries = true)
    public Mono<Void> clearAllUsers() {
        return Mono.empty();
    }
}
```

### 3. Or Use Programmatic API

```java
import io.github.ajuarez0021.redis.reactive.service.RedisCacheService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ProductService {

    private final RedisCacheService cacheService;
    private final ReactiveProductRepository productRepository;

    public ProductService(RedisCacheService cacheService,
                         ReactiveProductRepository productRepository) {
        this.cacheService = cacheService;
        this.productRepository = productRepository;
    }

    public Mono<Product> getProduct(String productId) {
        return cacheService.cacheable(
            "products",
            productId,
            () -> productRepository.findById(productId),
            Duration.ofMinutes(30)
        );
    }
}
```

## Configuration

### Standalone Mode

For a single Redis instance:

```java
@Configuration
@EnableRedisReactiveLibrary(
    mode = Mode.STANDALONE,
    hostEntries = {
        @HostEntry(host = "redis.example.com", port = 6379)
    },
    userName = "admin",
    pwd = "password",
    useSsl = true,
    connectionTimeout = 5000,
    readTimeout = 3000
)
public class RedisStandaloneConfig {
}
```

### Cluster Mode

For Redis Cluster deployments:

```java
@Configuration
@EnableRedisReactiveLibrary(
    mode = Mode.CLUSTER,
    hostEntries = {
        @HostEntry(host = "redis-node1.example.com", port = 6379),
        @HostEntry(host = "redis-node2.example.com", port = 6379),
        @HostEntry(host = "redis-node3.example.com", port = 6379)
    },
    userName = "admin",
    pwd = "password",
    useSsl = true,
    connectionTimeout = 5000,
    readTimeout = 3000
)
public class RedisClusterConfig {
}
```

### Sentinel Mode

For high availability with Redis Sentinel:

```java
@Configuration
@EnableRedisReactiveLibrary(
    mode = Mode.SENTINEL,
    sentinelMaster = "mymaster",
    hostEntries = {
        @HostEntry(host = "sentinel1.example.com", port = 26379),
        @HostEntry(host = "sentinel2.example.com", port = 26379),
        @HostEntry(host = "sentinel3.example.com", port = 26379)
    },
    userName = "admin",
    pwd = "password",
    useSsl = false
)
public class RedisSentinelConfig {
}
```

### Configuration Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `mode` | `Mode` | `STANDALONE` | Redis deployment mode: `STANDALONE`, `CLUSTER`, or `SENTINEL` |
| `hostEntries` | `HostEntry[]` | `[]` | Redis server host/port configurations |
| `userName` | `String` | `""` | Redis username (Redis 6.0+) |
| `pwd` | `String` | `""` | Redis password |
| `useSsl` | `boolean` | `false` | Enable SSL/TLS connection |
| `connectionTimeout` | `long` | `5000` | Connection timeout in milliseconds |
| `readTimeout` | `long` | `3000` | Read timeout in milliseconds |
| `sentinelMaster` | `String` | `""` | Sentinel master name (required for SENTINEL mode) |
| `ttlEntries` | `TTLEntry[]` | `[]` | Custom TTL configurations per cache |
| `mapperConfig` | `Class<? extends ObjectMapperConfig>` | `DefaultObjectMapperConfig.class` | Custom Jackson ObjectMapper configuration |

## Usage

### Annotation-Based Caching

The library supports standard Spring Cache annotations with reactive types.

#### @Cacheable

Cache the result of a method. On cache hit, returns cached value; on miss, executes method and caches result.

```java
@Cacheable(cacheNames = "users", key = "#userId")
public Mono<User> getUser(String userId) {
    return userRepository.findById(userId);
}

// With SpEL expressions
@Cacheable(cacheNames = "users", key = "#user.id", condition = "#user.active")
public Mono<User> getActiveUser(User user) {
    return userRepository.findById(user.getId());
}
```

#### @CachePut

Always execute the method and update the cache with the result.

```java
@CachePut(cacheNames = "users", key = "#user.id")
public Mono<User> updateUser(User user) {
    return userRepository.save(user);
}
```

#### @CacheEvict

Remove entries from the cache.

```java
// Evict single entry
@CacheEvict(cacheNames = "users", key = "#userId")
public Mono<Void> deleteUser(String userId) {
    return userRepository.deleteById(userId);
}

// Evict all entries
@CacheEvict(cacheNames = "users", allEntries = true)
public Mono<Void> clearCache() {
    return Mono.empty();
}

// Before invocation (default is after)
@CacheEvict(cacheNames = "users", key = "#userId", beforeInvocation = true)
public Mono<Void> invalidateUser(String userId) {
    return userRepository.deleteById(userId);
}
```

### Programmatic API

The `RedisCacheService` provides direct reactive cache operations.

#### Basic Operations

```java
@Service
public class OrderService {

    private final RedisCacheService cacheService;

    public Mono<Order> getOrder(String orderId) {
        return cacheService.cacheable(
            "orders",                              // cache name
            orderId,                               // cache key
            () -> orderRepository.findById(orderId), // loader function
            Duration.ofMinutes(15)                 // TTL
        );
    }

    public Mono<Order> updateOrder(Order order) {
        return cacheService.cachePut(
            "orders",
            order.getId(),
            () -> orderRepository.save(order),
            Duration.ofMinutes(15)
        );
    }

    public Mono<Void> deleteOrder(String orderId) {
        return orderRepository.deleteById(orderId)
            .then(cacheService.cacheEvict("orders", orderId));
    }
}
```

#### Advanced Operations

```java
// Check if key exists
Mono<Boolean> exists = cacheService.exists("orders", "123");

// Get remaining TTL
Mono<Duration> ttl = cacheService.getTTL("orders", "123");

// Evict multiple keys
Mono<Void> evictMultiple = cacheService.cacheEvictMultiple("orders", "1", "2", "3");

// Evict by pattern
Mono<Void> evictPattern = cacheService.cacheEvictByPattern("orders:user:*");

// Evict all entries in cache
Mono<Void> evictAll = cacheService.cacheEvictAll("orders");
```

### Fluent Builder API

The `CacheOperationBuilder` provides a fluent API for complex cache operations.

```java
@Service
public class ProductService {

    private final CacheOperationBuilder.Factory cacheBuilderFactory;
    private final ReactiveProductRepository productRepository;

    public Mono<Product> getProduct(String productId) {
        return cacheBuilderFactory.create()
            .cacheName("products")
            .key(productId)
            .loader(() -> productRepository.findById(productId))
            .ttl(Duration.ofHours(1))
            .onCacheHit(product -> log.info("Cache HIT for product: {}", productId))
            .onCacheMiss(product -> log.info("Cache MISS for product: {}", productId))
            .cacheable();
    }

    public Mono<Product> updateProduct(Product product) {
        return cacheBuilderFactory.create()
            .cacheName("products")
            .key(product.getId())
            .loader(() -> productRepository.save(product))
            .ttl(Duration.ofHours(1))
            .cachePut();
    }

    // Conditional caching
    public Mono<Product> getProductIfAvailable(String productId, boolean useCache) {
        return cacheBuilderFactory.create()
            .cacheName("products")
            .key(productId)
            .loader(() -> productRepository.findById(productId))
            .condition(useCache)  // Only cache if condition is true
            .cacheable();
    }
}
```

## Health Monitoring

The library includes a `RedisHealthChecker` for monitoring Redis connection health and metrics.

```java
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final RedisHealthChecker redisHealthChecker;

    @GetMapping("/redis")
    public Mono<RedisStatusDto> checkRedisHealth() {
        return redisHealthChecker.isRedisActive();
    }
}
```

**Response Example:**

```json
{
  "connected": true,
  "responseTime": 15,
  "errorMessage": "Redis is up and running and responding correctly",
  "usedMemory": 1048576,
  "maxMemory": 2147483648,
  "connectedClients": 5,
  "redisVersion": "7.0.12"
}
```

## Advanced Configuration

### Custom ObjectMapper Configuration

Customize Jackson serialization for Redis values:

```java
public class CustomObjectMapperConfig implements ObjectMapperConfig {

    @Override
    public ObjectMapper configure() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Add custom serializers/deserializers
        SimpleModule customModule = new SimpleModule();
        customModule.addSerializer(CustomType.class, new CustomTypeSerializer());
        mapper.registerModule(customModule);

        return mapper;
    }
}

@Configuration
@EnableRedisReactiveLibrary(
    mode = Mode.STANDALONE,
    hostEntries = {@HostEntry(host = "localhost", port = 6379)},
    mapperConfig = CustomObjectMapperConfig.class
)
public class RedisConfig {
}
```

### TTL Configuration

Configure different TTLs for different caches:

```java
@EnableRedisReactiveLibrary(
    mode = Mode.STANDALONE,
    hostEntries = {@HostEntry(host = "localhost", port = 6379)},
    ttlEntries = {
        @TTLEntry(name = "users", ttl = 3600),       // 1 hour
        @TTLEntry(name = "products", ttl = 1800),    // 30 minutes
        @TTLEntry(name = "sessions", ttl = 900),     // 15 minutes
        @TTLEntry(name = "cache", ttl = 300)         // 5 minutes
    }
)
```

The library will automatically use the configured TTL when using annotations without explicit TTL.

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  (@Cacheable, @CachePut, @CacheEvict annotations)          │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│                 ReactiveCacheAspect                         │
│  (AOP interceptor for cache annotations)                    │
│  • Expression evaluation (SpEL)                             │
│  • Condition checking                                        │
│  • Expression caching                                        │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│              RedisCacheService                              │
│  • cacheable()  - Get or load                               │
│  • cachePut()   - Update cache                              │
│  • cacheEvict() - Delete cache                              │
│  • exists()     - Check existence                           │
│  • getTTL()     - Get remaining TTL                         │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│           ReactiveRedisTemplate                             │
│  (Spring Data Redis - Lettuce Client)                       │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
   Standalone        Cluster/Sentinel
```

### Key Design Patterns

1. **Annotation-Driven Configuration**: Uses `@Import` and `ImportAware` for automatic bean registration
2. **Builder Pattern**: `CacheOperationBuilder` for fluent cache operations
3. **Factory Pattern**: Thread-safe `CacheOperationBuilder.Factory` for builder creation
4. **Strategy Pattern**: Mode-based configuration (Standalone/Cluster/Sentinel)
5. **Reactive Patterns**:
   - Resource management with `Mono.usingWhen()`
   - Non-blocking operations throughout
   - Error handling with `onErrorResume()`

### Key Features

- **Zero Blocking Operations**: Uses `Schedulers.boundedElastic()` for unavoidable blocking
- **Connection Leak Prevention**: Proper resource cleanup with `Mono.usingWhen()`
- **Performance Optimization**: SPEL expression caching to avoid repeated parsing
- **Safe Bulk Operations**: Uses Redis `SCAN` instead of `KEYS` for production safety

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# View coverage report
open target/jacoco-report/index.html
```

### Code Coverage

The project maintains **80%+ code coverage** with comprehensive unit and integration tests.

Coverage report location: `target/jacoco-report/jacoco.csv`

### Testing Reactive Code

Example using Reactor `StepVerifier`:

```java
@Test
void testCacheable() {
    Mono<User> result = cacheService.cacheable(
        "users",
        "user123",
        () -> Mono.just(new User("user123", "John Doe")),
        Duration.ofMinutes(10)
    );

    StepVerifier.create(result)
        .expectNextMatches(user -> user.getId().equals("user123"))
        .verifyComplete();
}
```

## Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Quality Standards

- Maintain **80%+ code coverage**
- Follow **Checkstyle** rules (see `src/main/resources/checkstyle.xml`)
- Write **comprehensive tests** for new features
- Use **reactive patterns** (avoid blocking operations)
- Document public APIs with **Javadoc**

### Development Setup

```bash
# Clone the repository
git clone https://github.com/ajuarez0021/sbj-util-redis-reactive.git

# Build the project
cd sbj-util-redis-reactive
mvn clean install

# Run tests
mvn test

# Check code style
mvn checkstyle:check
```

## License

This project is licensed under the **Apache License 2.0** - see the [LICENSE](https://www.apache.org/licenses/LICENSE-2.0) file for details.

```
Copyright 2024 AJuarez

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Support and Contact

- **Author**: AJuarez
- **Email**: programacion0025@gmail.com
- **GitHub**: [@ajuarez0021](https://github.com/ajuarez0021)
- **Issues**: [GitHub Issues](https://github.com/ajuarez0021/sbj-util-redis-reactive/issues)

## Acknowledgments

- Spring Boot Team for the excellent reactive framework
- Lettuce team for the high-performance Redis client
- Project Reactor team for the reactive streams implementation

---

**Built with using Spring Boot and Project Reactor**
