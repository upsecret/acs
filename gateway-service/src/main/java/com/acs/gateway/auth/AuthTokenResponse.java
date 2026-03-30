package com.acs.gateway.auth;

public record AuthTokenResponse(
        String token,
        long expiresIn
) {
}
