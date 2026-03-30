package com.acs.gateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Configuration
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler globalErrorHandler() {
        return (exchange, ex) -> {
            ServerHttpResponse response = exchange.getResponse();

            if (response.isCommitted()) {
                return Mono.error(ex);
            }

            HttpStatusCode status;
            String code;
            String message;

            if (ex instanceof ResponseStatusException rse) {
                status = rse.getStatusCode();
                code = "HTTP_" + rse.getStatusCode().value();
                message = rse.getReason() != null ? rse.getReason() : "Unexpected error";
            } else {
                log.error("Unhandled exception on {}", exchange.getRequest().getPath(), ex);
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                code = "INTERNAL_ERROR";
                message = "Internal server error";
            }

            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = """
                    {"code":"%s","message":"%s","timestamp":"%s"}"""
                    .formatted(code, escapeJson(message), Instant.now());

            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        };
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
