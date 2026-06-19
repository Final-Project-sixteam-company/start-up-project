package com.startup.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class RedisScenarioService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisScenarioService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // JavaTimeModule 등 자동 등록
    }

    public PageResponse<ScenarioSummaryResponse> getCachedList(String key) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, new TypeReference<PageResponse<ScenarioSummaryResponse>>() {});
            }
        } catch (Exception e) {
            log.warn("Redis 시나리오 목록 조회 캐시 읽기 실패: {}", e.getMessage());
        }
        return null;
    }

    public void cacheList(String key, PageResponse<ScenarioSummaryResponse> response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, ScenarioCachePolicy.SCENARIO_LIST_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Redis 시나리오 목록 캐시 저장(직렬화) 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Redis 시나리오 목록 캐시 저장 실패: {}", e.getMessage());
        }
    }

    public void evictUserListCache(Long userId) {
        if (userId == null) return;
        // userId 뒤에 콜론(:)을 붙여, 1번 유저 패턴이 10번, 11번 유저의 캐시를 지우지 않도록 정확히 매칭합니다.
        String pattern = "scenario:list:user:" + userId + ":*";
        try {
            Set<String> keysToDelete = new HashSet<>();
            redisTemplate.execute((RedisConnection connection) -> {
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                } catch (Exception ex) {
                    log.warn("Redis Cursor close error: {}", ex.getMessage());
                }
                return null;
            });

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
            }
        } catch (Exception e) {
            log.warn("Redis 시나리오 캐시 무효화 실패 (userId={}): {}", userId, e.getMessage());
        }
    }
}
