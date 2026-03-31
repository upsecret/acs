package com.acs.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class LoggingGatewayFilterFactory
        extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter(
                (exchange, chain) -> LoggingFilterDelegate.filter(
                        exchange, chain,
                        config.isLogHeaders(),
                        config.isLogBody(),
                        config.getMaxBodySize()),
                Ordered.HIGHEST_PRECEDENCE);
    }

    public static class Config {
        private boolean logHeaders = true;
        private boolean logBody = true;
        private int maxBodySize = 1024;

        public boolean isLogHeaders() { return logHeaders; }
        public void setLogHeaders(boolean logHeaders) { this.logHeaders = logHeaders; }

        public boolean isLogBody() { return logBody; }
        public void setLogBody(boolean logBody) { this.logBody = logBody; }

        public int getMaxBodySize() { return maxBodySize; }
        public void setMaxBodySize(int maxBodySize) { this.maxBodySize = maxBodySize; }
    }
}
