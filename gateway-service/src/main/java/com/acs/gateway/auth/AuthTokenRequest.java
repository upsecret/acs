package com.acs.gateway.auth;

public record AuthTokenRequest(
        String employeeNumber,
        String password,
        String requestApp
) {
}
