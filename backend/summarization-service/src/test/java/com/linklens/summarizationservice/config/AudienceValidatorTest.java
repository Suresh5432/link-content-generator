package com.linklens.summarizationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("summarization-service");

    @Test
    void acceptsJwtContainingExpectedAudience() {
        assertTrue(validator.validate(jwt(List.of("web-client", "summarization-service"))).hasErrors() == false);
    }

    @Test
    void rejectsJwtMissingExpectedAudience() {
        assertFalse(validator.validate(jwt(List.of("web-client"))).hasErrors() == false);
    }

    private Jwt jwt(List<String> audience) {
        Instant now = Instant.now();
        return new Jwt("token", now, now.plusSeconds(60), Map.of("alg", "none"),
                Map.of("sub", "user-1", "aud", audience));
    }
}
