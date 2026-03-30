package com.acs.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;

@SpringBootApplication(
        scanBasePackages = "com.acs",
        excludeName = "org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration"
)
@RemoteApplicationEventScan(basePackages = "com.acs.gateway.blacklist")
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
