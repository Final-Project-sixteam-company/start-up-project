package com.startup.infrastructure.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.StringUtils;

@Configuration
// 분산락이 필요한 시점에만 RedissonClient를 생성하도록 @Lazy로 둔다.
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password
    ) {
        Config config = new Config();
        // docker profile에서는 host=redis, local profile에서는 host=localhost를 사용한다.
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
