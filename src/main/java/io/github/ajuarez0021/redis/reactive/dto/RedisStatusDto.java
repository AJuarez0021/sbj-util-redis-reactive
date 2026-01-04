package io.github.ajuarez0021.redis.reactive.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * The Class RedisStatusDtoBuilder.
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class RedisStatusDto {

    /**
     * The connected.
     */
    private boolean connected;

    /**
     * The response time.
     */
    private long responseTime;

    /**
     * The error message.
     */
    private String errorMessage;

    /**
     * The used memory.
     */
    private Long usedMemory;

    /**
     * The max memory.
     */
    private Long maxMemory;

    /**
     * The connected clients.
     */
    private Integer connectedClients;

    /**
     * The redis version.
     */
    private String redisVersion;
}
