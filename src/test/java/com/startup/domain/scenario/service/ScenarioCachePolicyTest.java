package com.startup.domain.scenario.service;

import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.enums.ScenarioType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioCachePolicyTest {

    @Test
    @DisplayName("검색어에 타입 구분자와 동일한 문자열이 포함되어도 캐시 키가 충돌하지 않아야 한다 (Base64 인코딩 적용 검증)")
    void buildListCacheKey_PreventCollision() {
        // given
        // Case 1: 검색어가 "foo:type:OFFICIAL" 이고, 타입 조건은 없는 경우
        ScenarioSearchCondition condition1 = new ScenarioSearchCondition();
        condition1.setKeyword("foo:type:OFFICIAL");

        // Case 2: 검색어가 "foo" 이고, 타입 조건이 OFFICIAL인 경우
        ScenarioSearchCondition condition2 = new ScenarioSearchCondition();
        condition2.setKeyword("foo");
        condition2.setType(ScenarioType.OFFICIAL);

        // when
        String key1 = ScenarioCachePolicy.buildListCacheKey(1L, condition1, 0, 20, "createdAt_desc");
        String key2 = ScenarioCachePolicy.buildListCacheKey(1L, condition2, 0, 20, "createdAt_desc");

        // then
        assertThat(key1).isNotEqualTo(key2);
        
        // 실제로 인코딩이 적용되었는지 확인 (키워드 원문이 그대로 노출되지 않아야 함)
        assertThat(key1).doesNotContain("foo:type:OFFICIAL");
        assertThat(key2).doesNotContain("foo:type");
    }
}
