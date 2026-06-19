package com.startup.domain.scenario.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.entity.QScenario;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

// QueryDSL 기반 시나리오 복합 조건 검색 구현체.
// Spring Data JPA 네이밍 규칙에 따라 ScenarioRepositoryCustom + "Impl" 접미사를 사용한다.
@Repository
@RequiredArgsConstructor
public class ScenarioRepositoryCustomImpl implements ScenarioRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QScenario scenario = QScenario.scenario;

    @Override
    public Page<Scenario> searchByCondition(ScenarioSearchCondition condition, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        // ── 기본 필터: PUBLISHED + PUBLIC/OFFICIAL만 노출 ──
        builder.and(scenario.status.eq(ScenarioStatus.PUBLISHED));
        builder.and(scenario.visibility.in(ScenarioVisibility.PUBLIC, ScenarioVisibility.OFFICIAL));

        // ── 동적 조건 ──

        // 1. 키워드 검색 (제목 OR 설명)
        if (StringUtils.hasText(condition.getKeyword())) {
            String keyword = condition.getKeyword().trim();
            builder.and(
                    scenario.title.containsIgnoreCase(keyword)
                            .or(scenario.description.containsIgnoreCase(keyword))
            );
        }

        // 2. 시나리오 타입 필터
        if (condition.getType() != null) {
            builder.and(scenario.scenarioType.eq(condition.getType()));
        }

        // 3. 난이도 필터
        if (condition.getDifficulty() != null) {
            builder.and(scenario.difficulty.eq(condition.getDifficulty()));
        }

        // 4. 공개 범위 필터 (예: OFFICIAL만 보기)
        if (condition.getVisibility() != null) {
            builder.and(scenario.visibility.eq(condition.getVisibility()));
        }

        // 5. 최소 플레이 인원 필터
        if (condition.getMinPlayers() != null) {
            builder.and(scenario.playerCountMin.loe(condition.getMinPlayers()));
        }

        // 6. 최대 플레이 인원 필터
        if (condition.getMaxPlayers() != null) {
            builder.and(scenario.playerCountMax.goe(condition.getMaxPlayers()));
        }

        // 7. 최대 플레이 시간 필터
        if (condition.getMaxPlayTime() != null) {
            builder.and(scenario.estimatedPlayTimeMinutes.loe(condition.getMaxPlayTime()));
        }

        // ── 컨텐츠 쿼리 (페이징 적용) ──
        JPAQuery<Scenario> contentQuery = queryFactory
                .selectFrom(scenario)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // 정렬 조건 적용
        for (OrderSpecifier<?> orderSpecifier : getOrderSpecifiers(pageable)) {
            contentQuery.orderBy(orderSpecifier);
        }

        List<Scenario> content = contentQuery.fetch();

        // ── 카운트 쿼리 (총 건수) ──
        JPAQuery<Long> countQuery = queryFactory
                .select(scenario.count())
                .from(scenario)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // Pageable의 Sort 정보를 QueryDSL OrderSpecifier로 변환한다.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<OrderSpecifier<?>> getOrderSpecifiers(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;
                PathBuilder<Scenario> pathBuilder = new PathBuilder<>(Scenario.class, "scenario");
                orderSpecifiers.add(new OrderSpecifier(direction, pathBuilder.get(order.getProperty())));
            }
        } else {
            // 기본 정렬: 최신순
            orderSpecifiers.add(scenario.createdAt.desc());
        }

        return orderSpecifiers;
    }
}
