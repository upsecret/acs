package com.acs.gateway;

import com.acs.common.jwt.GatewayTokenPayload;
import com.acs.common.jwt.JwtTokenProvider;
import com.acs.gateway.auth.AuthTokenRequest;
import com.acs.gateway.config.InMemoryProxyManager;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatewayAuthIntegrationTest {

    @TestConfiguration
    static class TestBucket4jConfig {
        @Bean
        @Primary
        public AsyncProxyManager<String> testAsyncProxyManager() {
            return new InMemoryProxyManager().asAsync();
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    // ── POST /auth/token ──

    @Test
    @Order(1)
    void issueToken_success() {
        webTestClient.post().uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthTokenRequest("EMP001", "password", "portal"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    @Order(2)
    void issueToken_unknownEmployee_returns401() {
        webTestClient.post().uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthTokenRequest("UNKNOWN", "password", "portal"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── Auth filter on protected routes ──

    @Test
    @Order(3)
    void protectedRoute_noToken_returns401() {
        webTestClient.get().uri("/api/hr/employees")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_001");
    }

    @Test
    @Order(4)
    void protectedRoute_invalidToken_returns401() {
        webTestClient.get().uri("/api/hr/employees")
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_002");
    }

    @Test
    @Order(5)
    void protectedRoute_forbiddenApp_returns403() {
        String token = generateToken("EMP003", "admin", List.of("portal"));

        webTestClient.get().uri("/api/hr/employees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_003");
    }

    @Test
    @Order(6)
    void protectedRoute_validToken_passesAuth() {
        String token = generateToken("EMP001", "portal", List.of("portal", "admin"));

        webTestClient.get().uri("/api/hr/employees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        assertTrue(status != 401 && status != 403,
                                "Expected filter to pass auth, got " + status));
    }

    // ── Logout + Blacklist ──

    @Test
    @Order(7)
    void logout_blacklistsToken() {
        // Issue a token
        String token = generateToken("EMP_LOGOUT", "portal", List.of("portal", "admin"));

        // Verify token works before logout
        webTestClient.get().uri("/api/hr/employees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().value(status ->
                        assertTrue(status != 401 && status != 403,
                                "Token should work before logout, got " + status));

        // Logout
        webTestClient.post().uri("/auth/logout")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        // Verify token is now blacklisted
        webTestClient.get().uri("/api/hr/employees")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AUTH_004");
    }

    @Test
    @Order(8)
    void logout_withoutToken_returns400() {
        webTestClient.post().uri("/auth/logout")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── Actuator ──

    @Test
    @Order(9)
    void actuatorHealth_noAuthRequired() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().value(status ->
                        assertNotEquals(401, (int) status,
                                "Actuator health should not require auth"));
    }

    // ── Rate Limiting (run last — stateful buckets) ──

    @Test
    @Order(20)
    void authToken_rateLimitByIp() {
        int allowed = 0;
        for (int i = 0; i < 50; i++) {
            int status = webTestClient.post().uri("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new AuthTokenRequest("EMP001", "password", "portal"))
                    .exchange()
                    .expectBody().returnResult().getStatus().value();
            if (status == 429) break;
            allowed++;
        }

        assertTrue(allowed > 0, "At least 1 request should be allowed");
        assertTrue(allowed <= 30, "Rate limit should kick in within 30 total requests, allowed=" + allowed);

        webTestClient.post().uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthTokenRequest("EMP001", "password", "portal"))
                .exchange()
                .expectStatus().isEqualTo(429);
    }

    @Test
    @Order(21)
    void hrService_rateLimitByEmployeeApp() {
        String token = generateToken("RATE_TEST", "portal", List.of("portal", "sales"));

        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            int status = webTestClient.get().uri("/api/hr/employees")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectBody().returnResult().getStatus().value();
            if (status == 429) break;
            assertTrue(status != 401 && status != 403,
                    "Auth should pass, got " + status + " at request " + i);
            allowed++;
        }

        assertTrue(allowed > 0, "At least 1 request should be allowed");
        assertTrue(allowed <= 10, "Rate limit (capacity=5) should kick in, allowed=" + allowed);
    }

    // ── helper ──

    private String generateToken(String employee, String requestApp, List<String> allowedApps) {
        GatewayTokenPayload payload = new GatewayTokenPayload(
                null, employee, requestApp, allowedApps,
                Instant.now(), Instant.now().plusSeconds(3600));
        return jwtTokenProvider.generateToken(payload);
    }
}
