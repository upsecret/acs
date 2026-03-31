package com.acs.gateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class LoggingFilterDelegate {

    private static final Logger log = LoggerFactory.getLogger("gateway.http");
    private static final Set<String> MASKED_HEADERS =
            Set.of("authorization", "cookie", "set-cookie");

    private LoggingFilterDelegate() {}

    static Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain,
                              boolean logHeaders, boolean logBody, int maxBodySize) {

        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getId();
        long startTime = System.nanoTime();

        ServerHttpResponseDecorator responseDecorator =
                createResponseDecorator(exchange, requestId, logHeaders, logBody, maxBodySize, startTime);

        if (!logBody) {
            logRequest(requestId, request, logHeaders, null);
            return chain.filter(exchange.mutate().response(responseDecorator).build());
        }

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    logRequest(requestId, request, logHeaders, truncate(bytes, maxBodySize));

                    ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                        }
                    };

                    return chain.filter(exchange.mutate()
                            .request(requestDecorator)
                            .response(responseDecorator)
                            .build());
                });
    }

    private static ServerHttpResponseDecorator createResponseDecorator(
            ServerWebExchange exchange, String requestId,
            boolean logHeaders, boolean logBody, int maxBodySize, long startTime) {

        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;

                if (logBody) {
                    return DataBufferUtils.join(body)
                            .flatMap(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);

                                logResponse(requestId, getDelegate(), logHeaders,
                                        truncate(bytes, maxBodySize), elapsedMs);

                                return super.writeWith(
                                        Flux.just(getDelegate().bufferFactory().wrap(bytes)));
                            });
                }

                logResponse(requestId, getDelegate(), logHeaders, null, elapsedMs);
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(
                    Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMapSequential(p -> p));
            }
        };
    }

    private static void logRequest(String requestId, ServerHttpRequest request,
                                    boolean logHeaders, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append("[%s] >>> %s %s".formatted(requestId, request.getMethod(), request.getURI()));

        if (logHeaders) {
            sb.append("\n  Headers: ").append(maskHeaders(request.getHeaders()));
        }
        if (body != null && !body.isEmpty()) {
            sb.append("\n  Body: ").append(body);
        }

        log.info(sb.toString());
    }

    private static void logResponse(String requestId, ServerHttpResponse response,
                                     boolean logHeaders, String body, long elapsedMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[%s] <<< %s (%dms)".formatted(requestId, response.getStatusCode(), elapsedMs));

        if (logHeaders) {
            sb.append("\n  Headers: ").append(maskHeaders(response.getHeaders()));
        }
        if (body != null && !body.isEmpty()) {
            sb.append("\n  Body: ").append(body);
        }

        log.info(sb.toString());
    }

    private static String maskHeaders(HttpHeaders headers) {
        return headers.headerNames().stream()
                .map(key -> {
                    if (MASKED_HEADERS.contains(key.toLowerCase())) {
                        return key + ": [MASKED]";
                    }
                    List<String> values = headers.get(key);
                    return key + ": " + (values != null ? String.join(", ", values) : "");
                })
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String truncate(byte[] bytes, int maxSize) {
        if (bytes.length == 0) return "";
        if (bytes.length <= maxSize) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return new String(bytes, 0, maxSize, StandardCharsets.UTF_8)
                + "...(truncated, total=" + bytes.length + " bytes)";
    }
}
