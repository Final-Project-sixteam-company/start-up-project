package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioDetailResponse;
import com.startup.domain.scenario.dto.ScenarioSearchCondition;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;

    @Transactional(readOnly = true)
    public PageResponse<ScenarioSummaryResponse> getScenarios(Long userId, ScenarioSearchCondition condition, Pageable pageable) {
        // API 정렬 파라미터(popular 등)를 실제 엔티티 필드(playCount 등)로 변환
        Pageable mappedPageable = mapPageableSort(pageable);

        // 상태가 PUBLISHED 이고 가시성이 PUBLIC 또는 OFFICIAL인 시나리오만 조회
        // TODO: 세부 필터링(condition)은 나중에 QueryDSL 도입 시 추가
        List<ScenarioVisibility> allowedVisibilities = List.of(ScenarioVisibility.PUBLIC, ScenarioVisibility.OFFICIAL);
        Page<Scenario> scenarios = scenarioRepository.findAllByStatusAndVisibilityIn(
                ScenarioStatus.PUBLISHED, 
                allowedVisibilities, 
                mappedPageable
        );
        
        Page<ScenarioSummaryResponse> responsePage = scenarios.map(scenario -> 
            ScenarioSummaryResponse.from(scenario, false) // TODO: 실제 북마크 여부 확인 로직 추가하기
        );
        
        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ScenarioDetailResponse getScenario(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // 작성자 본인이 아니면서, 대중에게 공개되지 않은 시나리오에 접근하는 것을 차단
        boolean isCreator = userId != null && userId.equals(scenario.getCreatorId());
        boolean isPubliclyVisible = scenario.getStatus() == ScenarioStatus.PUBLISHED && 
                (scenario.getVisibility() == ScenarioVisibility.PUBLIC || scenario.getVisibility() == ScenarioVisibility.OFFICIAL);
                
        if (!isCreator && !isPubliclyVisible) {
            // 작성자가 아니면 에러 반환
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
        }

        scenarioAccessService.validateViewable(userId, scenarioId);

        // TODO: 실제 작성자 닉네임 조회 및 북마크 여부 확인 로직 구현하기
        String mockCreatorNickname = "운영자"; 
        Boolean isBookmarked = false;
        Boolean canPlay = scenarioAccessService.canPlay(userId, scenarioId);

        return ScenarioDetailResponse.from(scenario, mockCreatorNickname, isBookmarked, canPlay);
    }

    private Pageable mapPageableSort(Pageable pageable) {
        Sort mappedSort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            String property = switch (order.getProperty().toLowerCase()) {
                case "popular" -> "playCount";
                case "rating" -> "averageRating";
                case "latest" -> "createdAt";
                default -> order.getProperty();
            };
            
            // 스프링은 정렬 방향 생략 시 기본값으로 ASC(오름차순)를 주지만,
            // 인기순, 평점순, 최신순은 내림차순(DESC)이 논리적으로 맞으므로 DESC로 엎어칩니다.
            Sort.Direction direction = order.getDirection();
            if (direction == Sort.Direction.ASC && 
               (property.equals("playCount") || property.equals("averageRating") || property.equals("createdAt"))) {
                direction = Sort.Direction.DESC;
            }
            
            mappedSort = mappedSort.and(Sort.by(direction, property));
        }

        // 정렬 조건이 없으면 기본값으로 최신순(createdAt DESC) 정렬
        if (mappedSort.isUnsorted()) {
            mappedSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }
}
