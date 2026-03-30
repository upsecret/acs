package com.acs.common.jwt;

import java.time.Instant;
import java.util.List;

public record GatewayTokenPayload(
        String jti,
        String employeeNumber,
        String requestApp,
        List<String> allowedApps,
        Instant issuedAt,
        Instant expiration
) {
}
