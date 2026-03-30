package com.acs.common.model;

import java.util.List;

public final class AuthConstants {

    private AuthConstants() {
    }

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_REQUEST_APP = "X-Request-App";
    public static final String HEADER_ALLOWED_APPS = "X-Allowed-Apps";

    public static final List<String> AUTH_EXCLUDE_PATHS = List.of(
            "/auth/token",
            "/actuator/health"
    );

    public static final String EXCHANGE_ATTR_TOKEN_PAYLOAD = "GATEWAY_TOKEN_PAYLOAD";
}
