package com.acs.gateway.config;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bucket4j.redis.enabled", havingValue = "true", matchIfMissing = true)
public class Bucket4jRedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        return RedisClient.create(RedisURI.create(host, port));
    }

    @Bean
    public AsyncProxyManager<String> asyncProxyManager(RedisClient bucket4jRedisClient) {
        StatefulRedisConnection<String, byte[]> connection = bucket4jRedisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return Bucket4jLettuce.casBasedBuilder(connection).build().asAsync();
    }
}
