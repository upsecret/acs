package com.acs.gateway.filter;

import com.acs.common.jwt.GatewayTokenPayload;
import com.acs.common.jwt.JwtTokenProvider;
import com.acs.common.model.AuthConstants;
import com.acs.gateway.blacklist.TokenBlacklistService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService blacklistService;
    private final ObservationRegistry observationRegistry;

    public AuthGatewayFilterFactory(JwtTokenProvider jwtTokenProvider,
                                    TokenBlacklistService blacklistService,
                                    ObservationRegistry observationRegistry) {
        super(Config.class);
        this.jwtTokenProvider = jwtTokenProvider;
        this.blacklistService = blacklistService;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {

            Observation observation = Observation.createNotStarted("gateway.auth", observationRegistry)
                    .lowCardinalityKeyValue("path", exchange.getRequest().getPath().value());

            return observation.observe(() -> doFilter(exchange, chain));
        }, Ordered.HIGHEST_PRECEDENCE + 1);
    }

    private Mono<Void> doFilter(ServerWebExchange exchange,
                                org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "AUTH_001",
                    "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "AUTH_002",
                    "Invalid or expired token");
        }

        GatewayTokenPayload payload = jwtTokenProvider.parsePayload(token);

        return blacklistService.isBlacklisted(payload.jti())
                .flatMap(blacklisted -> {
                    if (blacklisted) {
                        return writeError(exchange, HttpStatus.UNAUTHORIZED, "AUTH_004",
                                "Token has been revoked");
                    }

                    if (!payload.allowedApps().contains(payload.requestApp())) {
                        return writeError(exchange, HttpStatus.FORBIDDEN, "AUTH_003",
                                "Application not authorized");
                    }

                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header(AuthConstants.HEADER_USER_ID, payload.employeeNumber())
                            .header(AuthConstants.HEADER_REQUEST_APP, payload.requestApp())
                            .header(AuthConstants.HEADER_ALLOWED_APPS,
                                    String.join(",", payload.allowedApps()))
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    mutatedExchange.getAttributes().put(
                            AuthConstants.EXCHANGE_ATTR_TOKEN_PAYLOAD, payload);

                    return chain.filter(mutatedExchange);
                });
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status,
                                  String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"code":"%s","message":"%s","timestamp":"%s"}""".formatted(code, message, Instant.now());

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
    }
}
