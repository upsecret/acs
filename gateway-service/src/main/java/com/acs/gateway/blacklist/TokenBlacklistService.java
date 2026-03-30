package com.acs.gateway.blacklist;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Cache<String, Boolean> localCache;

    public TokenBlacklistService(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${blacklist.cache.max-size:10000}") long maxSize,
            @Value("${blacklist.cache.ttl-seconds:30}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .maximumSize(maxSize)
                .build();
    }

    /**
     * Add jti to blacklist: local Caffeine cache + Redis with TTL = remaining token lifetime.
     * Returns false if token is already expired.
     */
    public Mono<Boolean> blacklist(String jti, Instant expiration) {
        Duration ttl = Duration.between(Instant.now(), expiration);
        if (ttl.isNegative() || ttl.isZero()) {
            return Mono.just(false);
        }

        localCache.put(jti, Boolean.TRUE);

        return redisTemplate.opsForValue()
                .set(BLACKLIST_PREFIX + jti, "1", ttl)
                .thenReturn(true)
                .onErrorReturn(true); // local cache was updated regardless
    }

    /**
     * Add to local Caffeine cache only (called by Bus event listener on other instances).
     */
    public void addToLocalCache(String jti) {
        localCache.put(jti, Boolean.TRUE);
    }

    /**
     * Check if jti is blacklisted: local Caffeine cache first, then Redis fallback.
     */
    public Mono<Boolean> isBlacklisted(String jti) {
        Boolean cached = localCache.getIfPresent(jti);
        if (cached != null) {
            return Mono.just(true);
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + jti)
                .onErrorReturn(false)
                .doOnNext(exists -> {
                    if (exists) {
                        localCache.put(jti, Boolean.TRUE);
                    }
                });
    }
}
