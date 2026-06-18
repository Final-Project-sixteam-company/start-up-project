package com.startup.domain.scenario.service;

import java.time.Duration;

public class ScenarioCachePolicy {
    public static final String SCENARIO_LIST_PREFIX = "scenario:list:user:%d:page:%d:size:%d";
    public static final Duration SCENARIO_LIST_TTL = Duration.ofSeconds(30);

    public static String buildListCacheKey(Long userId, int page, int size, String sortParam) {
        long effectiveUserId = (userId != null) ? userId : 0L;
        String base = String.format(SCENARIO_LIST_PREFIX, effectiveUserId, page, size);
        if (sortParam != null && !sortParam.isBlank()) {
            return base + ":sort:" + sortParam;
        }
        return base;
    }
}
