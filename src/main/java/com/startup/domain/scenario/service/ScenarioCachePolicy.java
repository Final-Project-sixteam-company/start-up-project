package com.startup.domain.scenario.service;

import java.time.Duration;

import com.startup.domain.scenario.dto.ScenarioSearchCondition;

public class ScenarioCachePolicy {
    public static final String SCENARIO_LIST_PREFIX = "scenario:list:user:%d:page:%d:size:%d";
    public static final Duration SCENARIO_LIST_TTL = Duration.ofSeconds(30);

    public static String buildListCacheKey(Long userId, ScenarioSearchCondition condition, int page, int size, String sortParam) {
        long effectiveUserId = (userId != null) ? userId : 0L;
        StringBuilder base = new StringBuilder(String.format(SCENARIO_LIST_PREFIX, effectiveUserId, page, size));
        if (sortParam != null && !sortParam.isBlank()) {
            base.append(":sort:").append(sortParam);
        }
        if (condition != null) {
            if (condition.getKeyword() != null) base.append(":kw:").append(condition.getKeyword());
            if (condition.getType() != null) base.append(":type:").append(condition.getType());
            if (condition.getDifficulty() != null) base.append(":diff:").append(condition.getDifficulty());
            if (condition.getVisibility() != null) base.append(":vis:").append(condition.getVisibility());
            if (condition.getMinPlayers() != null) base.append(":minp:").append(condition.getMinPlayers());
            if (condition.getMaxPlayers() != null) base.append(":maxp:").append(condition.getMaxPlayers());
            if (condition.getMaxPlayTime() != null) base.append(":maxt:").append(condition.getMaxPlayTime());
        }
        return base.toString();
    }
}
