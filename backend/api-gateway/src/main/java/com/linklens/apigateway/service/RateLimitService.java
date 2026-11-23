package com.linklens.apigateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RateLimitService {

    private static final int REQUEST_LIMIT=60;

    private static final int WINDOW_SECONDS=60;

    private final StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimitService(StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;

        this.rateLimitScript= new DefaultRedisScript<>();

        this.rateLimitScript.setLocation(
                new ClassPathResource("scripts/rate-limit.lua")
        );

        this.rateLimitScript.setResultType(Long.class);
    }

    public boolean isAllowed(String userId){
        return isAllowed(userId, "api", REQUEST_LIMIT);
    }

    public boolean isAllowed(String userId, String bucket, int limit){
        String key="rate-limit:api-gateway:"+bucket+":"+userId;

        Long requestCount=redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );

        if (requestCount==null){
            throw new IllegalStateException(
                    "Unable to retrieve rate limit count from Redis"
            );
        }

        if (requestCount>limit){
            log.warn(
                    "Rate limit exceeded | bucket={} | userId={} | requestCount={}",
                    bucket,
                    userId,
                    requestCount
            );

            return false;
        }

        return true;

    }
}
