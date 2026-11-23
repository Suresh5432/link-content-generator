package com.linklens.apigateway.config;

import com.linklens.apigateway.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void allowsIdempotencyKeyOnCrossOriginRequests() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthConverter.class),
                mock(RateLimitService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/links");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "authorization,content-type,idempotency-key");

        var cors = securityConfig.corsConfigurationSource("http://localhost:5173")
                .getCorsConfiguration(request);

        assertTrue(cors.checkOrigin(request.getHeader("Origin")) != null);
        assertTrue(cors.checkHttpMethod(HttpMethod.POST) != null);
        assertTrue(cors.checkHeaders(List.of("authorization", "content-type", "idempotency-key"))
                .containsAll(List.of("authorization", "content-type", "idempotency-key")));
    }
}
