package com.acs.gateway.blacklist;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TokenBlacklistListener {

    private final TokenBlacklistService blacklistService;

    public TokenBlacklistListener(TokenBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @EventListener
    public void onTokenBlacklisted(TokenBlacklistEvent event) {
        blacklistService.addToLocalCache(event.getJti());
    }
}
