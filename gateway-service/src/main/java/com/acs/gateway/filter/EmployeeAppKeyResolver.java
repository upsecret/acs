package com.acs.gateway.filter;

import com.acs.common.jwt.GatewayTokenPayload;
import com.acs.common.model.AuthConstants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Primary
public class EmployeeAppKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        GatewayTokenPayload payload = exchange.getAttribute(AuthConstants.EXCHANGE_ATTR_TOKEN_PAYLOAD);
        if (payload == null) {
            return Mono.just("anonymous");
        }
        return Mono.just(payload.employeeNumber() + ":" + payload.requestApp());
    }
}
