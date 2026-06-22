package com.startup.infrastructure.redis;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
// RedisTemplate과 Spring Cache의 기본 직렬화/TTL 정책을 한곳에서 관리한다.
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            JsonMapper jsonMapper
    ) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        // key는 사람이 읽기 쉬운 문자열, value는 타입 정보를 포함한 JSON으로 저장한다.
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            JsonMapper jsonMapper
    ) {
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .computePrefixWith(cacheName -> "cache:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 캐시별 TTL은 기능 특성에 맞춰 별도로 조정한다.
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        CacheNames.DEFAULT, defaultConfig.entryTtl(Duration.ofMinutes(10)),
                        CacheNames.AI_RESPONSES, defaultConfig.entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }
}
