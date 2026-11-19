
package com.linklens.apigateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    void shouldAllowRequestWhenCountIsBelowLimit() {

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(1L);

        boolean result = rateLimitService.isAllowed("user-123");

        assertTrue(result);
    }

    @Test
    void shouldAllowRequestWhenCountIsExactlyLimit() {

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(60L);

        boolean result = rateLimitService.isAllowed("user-123");

        assertTrue(result);
    }

    @Test
    void shouldRejectRequestWhenLimitIsExceeded() {

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(61L);

        boolean result = rateLimitService.isAllowed("user-123");

        assertFalse(result);
    }

    @Test
    void shouldThrowExceptionWhenRedisReturnsNull() {

        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> rateLimitService.isAllowed("user-123")
        );
    }
}
