package com.startup.domain.scenario.repository;

import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.entity.Scenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// QueryDSL 기반의 시나리오 복합 조건 검색 인터페이스.
// ScenarioRepository가 이 인터페이스를 상속하면 스프링 Data JPA가 자동으로 Impl 클래스를 연결한다.
public interface ScenarioRepositoryCustom {

    // 시나리오 목록 조회 시 키워드·난이도·인원·플레이시간 등 복합 조건을 동적으로 필터링한다.
    Page<Scenario> searchByCondition(ScenarioSearchCondition condition, Pageable pageable);
}
