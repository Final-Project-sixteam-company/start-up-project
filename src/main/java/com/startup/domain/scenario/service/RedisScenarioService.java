package com.startup.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisScenarioService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
}
