package com.linklens.apigateway.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RateLimitServiceIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Test
    void shouldAllowFirstSixtyRequestsAndRejectNextRequest() {

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(
                        redis.getHost(),
                        redis.getMappedPort(6379)
                );

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        try {

            StringRedisTemplate template =
                    new StringRedisTemplate(factory);

            template.afterPropertiesSet();

            RateLimitService service = new RateLimitService(template);

            for (int i = 1; i <= 60; i++) {
                assertTrue(service.isAllowed("integration-user"));
            }

            assertFalse(service.isAllowed("integration-user"));

        } finally {
            factory.destroy();
        }
    }

    @Test
    void shouldMaintainIndependentLimitsForDifferentUsers() {

        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration(
                        redis.getHost(),
                        redis.getMappedPort(6379)
                );

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        try {

            StringRedisTemplate template =
                    new StringRedisTemplate(factory);

            template.afterPropertiesSet();

            RateLimitService service = new RateLimitService(template);

            for (int i = 0; i < 60; i++) {
                service.isAllowed("user-A");
            }

            assertFalse(service.isAllowed("user-A"));
            assertTrue(service.isAllowed("user-B"));

        } finally {
            factory.destroy();
        }
    }
}