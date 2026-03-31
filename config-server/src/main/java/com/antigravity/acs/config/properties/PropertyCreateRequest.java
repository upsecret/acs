package com.antigravity.acs.config.properties;

public record PropertyCreateRequest(
        String application,
        String profile,
        String label,
        String propKey,
        String propValue
) {}
