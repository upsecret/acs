package com.acs.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(GatewayTokenPayload payload) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(payload.employeeNumber())
                .claim("requestApp", payload.requestApp())
                .claim("allowedApps", payload.allowedApps())
                .issuedAt(Date.from(payload.issuedAt()))
                .expiration(Date.from(payload.expiration()))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public GatewayTokenPayload parsePayload(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new GatewayTokenPayload(
                claims.getId(),
                claims.getSubject(),
                claims.get("requestApp", String.class),
                claims.get("allowedApps", List.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }
}
