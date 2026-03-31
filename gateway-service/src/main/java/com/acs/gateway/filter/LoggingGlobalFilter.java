package com.acs.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(name = "gateway.logging.enabled", havingValue = "true")
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private final boolean logHeaders;
    private final boolean logBody;
    private final int maxBodySize;

    public LoggingGlobalFilter(
            @Value("${gateway.logging.log-headers:true}") boolean logHeaders,
            @Value("${gateway.logging.log-body:true}") boolean logBody,
            @Value("${gateway.logging.max-body-size:1024}") int maxBodySize) {
        this.logHeaders = logHeaders;
        this.logBody = logBody;
        this.maxBodySize = maxBodySize;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return LoggingFilterDelegate.filter(exchange, chain, logHeaders, logBody, maxBodySize);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
