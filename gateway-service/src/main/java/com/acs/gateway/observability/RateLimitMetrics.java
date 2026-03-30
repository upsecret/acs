package com.acs.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class RateLimitMetrics {

    private final MeterRegistry registry;

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDenied(String routeId, String key) {
        Counter.builder("rate_limit_denied_total")
                .description("Total number of rate-limited requests")
                .tag("route", routeId)
                .tag("key", key)
                .register(registry)
                .increment();
    }
}
