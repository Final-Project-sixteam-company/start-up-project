package com.startup.infrastructure.redis;

// @Cacheable/@CacheEvict에서 문자열 오타를 줄이기 위한 캐시 이름 모음이다.
public final class CacheNames {

    public static final String DEFAULT = "default";
    public static final String AI_RESPONSES = "aiResponses";

    private CacheNames() {
    }
}
