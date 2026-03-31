package com.antigravity.acs.config.properties;

import java.time.LocalDateTime;

public record PropertyEntity(
        Long id,
        String application,
        String profile,
        String label,
        String propKey,
        String propValue,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
