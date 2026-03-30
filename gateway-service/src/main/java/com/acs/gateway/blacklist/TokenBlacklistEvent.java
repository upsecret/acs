package com.acs.gateway.blacklist;

import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

import java.time.Instant;

public class TokenBlacklistEvent extends RemoteApplicationEvent {

    private String jti;
    private Instant expiration;

    // Required for deserialization
    public TokenBlacklistEvent() {
    }

    public TokenBlacklistEvent(Object source, String originService, String jti, Instant expiration) {
        super(source, originService, DEFAULT_DESTINATION_FACTORY.getDestination("**"));
        this.jti = jti;
        this.expiration = expiration;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }
}
