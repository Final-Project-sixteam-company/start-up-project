package com.startup.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
        String pattern = "scenario:list:user:" + userId + "*";
        try {
            java.util.Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 시나리오 캐시 무효화 실패 (userId={}): {}", userId, e.getMessage());
        }
    }
}
