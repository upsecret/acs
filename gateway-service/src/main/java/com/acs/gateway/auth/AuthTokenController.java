package com.acs.gateway.auth;

import com.acs.common.jwt.GatewayTokenPayload;
import com.acs.common.jwt.JwtTokenProvider;
import com.acs.gateway.blacklist.TokenBlacklistEvent;
import com.acs.gateway.blacklist.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;

@RestController
@RequestMapping("/internal/auth")
public class AuthTokenController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService blacklistService;
    private final ApplicationEventPublisher eventPublisher;
    private final long expirationSeconds;
    private final String applicationName;

    public AuthTokenController(AuthService authService,
                               JwtTokenProvider jwtTokenProvider,
                               TokenBlacklistService blacklistService,
                               ApplicationEventPublisher eventPublisher,
                               @Value("${jwt.expiration-seconds:3600}") long expirationSeconds,
                               @Value("${spring.application.name}") String applicationName) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.blacklistService = blacklistService;
        this.eventPublisher = eventPublisher;
        this.expirationSeconds = expirationSeconds;
        this.applicationName = applicationName;
    }

    @PostMapping("/token")
    public Mono<AuthTokenResponse> issueToken(@RequestBody AuthTokenRequest request) {
        return authService.authenticate(request.employeeNumber(), request.password())
                .map(allowedApps -> {
                    Instant now = Instant.now();
                    GatewayTokenPayload payload = new GatewayTokenPayload(
                            null,
                            request.employeeNumber(),
                            request.requestApp(),
                            allowedApps,
                            now,
                            now.plusSeconds(expirationSeconds)
                    );
                    String token = jwtTokenProvider.generateToken(payload);
                    return new AuthTokenResponse(token, expirationSeconds);
                });
    }

    @PostMapping("/logout")
    public Mono<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing Bearer token"));
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));
        }

        GatewayTokenPayload payload = jwtTokenProvider.parsePayload(token);

        return blacklistService.blacklist(payload.jti(), payload.expiration())
                .doOnNext(added -> {
                    if (added) {
                        eventPublisher.publishEvent(new TokenBlacklistEvent(
                                this, applicationName, payload.jti(), payload.expiration()));
                    }
                })
                .then();
    }
}
