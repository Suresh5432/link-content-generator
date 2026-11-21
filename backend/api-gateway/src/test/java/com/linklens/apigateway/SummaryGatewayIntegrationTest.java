package com.linklens.apigateway;

import com.linklens.apigateway.service.RateLimitService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SummaryGatewayIntegrationTest {

    private static final StubBackend backend = new StubBackend();

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("SUMMARY_SERVICE_URL", () -> "http://127.0.0.1:" + backend.port());
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "https://issuer.invalid/realms/test");
        registry.add("gateway.security.expected-audience", () -> "link-context-gateway");
        registry.add("management.tracing.sampling.probability", () -> "0");
    }

    @Value("${local.server.port}")
    int gatewayPort;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @MockitoBean
    RateLimitService rateLimitService;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void configureSecurityMocks() {
        backend.reset();
        reset(jwtDecoder, rateLimitService);
        when(rateLimitService.isAllowed(anyString())).thenReturn(true);
        when(jwtDecoder.decode("user-token")).thenReturn(jwt("user-1", true));
        when(jwtDecoder.decode("no-role-token")).thenReturn(jwt("user-1", false));
    }

    @AfterAll
    static void stopBackend() {
        backend.close();
    }

    @Test
    void summaryCollectionIsRejectedWithoutJwt() throws Exception {
        var response = get("/api/v1/summaries", null);
        assertEquals(401, response.statusCode());
        assertEquals(0, backend.requests());
    }

    @Test
    void authenticatedUserWithoutUserRoleIsRejected() throws Exception {
        var response = get("/api/v1/summaries", "no-role-token");
        assertEquals(403, response.statusCode());
        assertEquals(0, backend.requests());
    }

    @Test
    void authorizedJwtCanReachAllSummaryRoutesThroughGateway() throws Exception {
        for (String path : new String[]{
                "/api/v1/summaries",
                "/api/v1/summaries/ed9eb441-5416-493c-b1c7-c7cbb8da26b4",
                "/api/v1/summaries/link/4f3b295f-06a3-4b31-a098-234d3343a0e5"}) {
            var response = get(path, "user-token");
            assertEquals(200, response.statusCode(), path);
            assertEquals(path, response.body());
        }
        assertEquals(3, backend.requests());
        verify(rateLimitService, times(3)).isAllowed("user-1");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + gatewayPort + path))
                .GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Jwt jwt(String subject, boolean userRole) {
        Instant now = Instant.now();
        var builder = Jwt.withTokenValue("verified-by-test-decoder")
                .header("alg", "RS256")
                .issuer("https://issuer.invalid/realms/test")
                .audience(List.of("link-context-gateway"))
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        if (userRole) {
            builder.claim("realm_access", Map.of("roles", List.of("USER")));
        }
        return builder.build();
    }

    private static final class StubBackend implements AutoCloseable {
        private final ServerSocket server;
        private final java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger();
        private final Thread thread;

        private StubBackend() {
            try {
                server = new ServerSocket(0, 20, java.net.InetAddress.getByName("127.0.0.1"));
                thread = new Thread(this::serve, "summary-stub-backend");
                thread.setDaemon(true);
                thread.start();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not start summary stub backend", exception);
            }
        }

        int port() {
            return server.getLocalPort();
        }

        int requests() {
            return requestCount.get();
        }

        void reset() {
            requestCount.set(0);
        }

        private void serve() {
            while (!server.isClosed()) {
                try (Socket socket = server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                     OutputStream output = socket.getOutputStream()) {
                    String firstLine = reader.readLine();
                    String headerLine;
                    while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                        // Consume all request headers before writing the response.
                    }
                    String path = firstLine == null ? "" : firstLine.split(" ")[1];
                    byte[] body = path.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    output.write(("HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: "
                            + body.length + "\r\nConnection: close\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
                    output.write(body);
                    output.flush();
                    requestCount.incrementAndGet();
                } catch (java.io.IOException exception) {
                    if (!server.isClosed()) {
                        throw new IllegalStateException("Summary stub backend failed", exception);
                    }
                }
            }
        }

        @Override
        public void close() {
            try {
                server.close();
                thread.join(1000);
            } catch (Exception exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
