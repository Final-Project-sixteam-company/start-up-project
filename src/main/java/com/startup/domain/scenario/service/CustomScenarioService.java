package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.CustomLocationCreateRequest;
import com.startup.domain.scenario.dto.CustomLocationCreateResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.ScenarioLocation;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioLocationRepository locationRepository;
    private final ScenarioAccessService scenarioAccessService;

    @Transactional
    public CustomLocationCreateResponse createLocation(Long userId, Long scenarioId, CustomLocationCreateRequest request) {
        // 작성자 본인인지 확인
        scenarioAccessService.validateEditable(userId, scenarioId);

        // 동시에 장소를 추가하더라도 Race Condition 차단
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 이미 발행된(PUBLISHED) 시나리오에는 더 이상 장소 추가 불가
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // DB Lock이 걸려있으므로 여러 트랜잭션이 중복된 숫자를 가져갈 수 없음
        Integer maxSortOrder = locationRepository.findMaxSortOrderByScenarioId(scenarioId);
        int nextSortOrder = maxSortOrder + 1;

        // 엔티티 생성
        ScenarioLocation location = ScenarioLocation.builder()
                .scenarioId(scenarioId)
                .name(request.getName())
                .description(request.getDescription())
                .floor(request.getFloor())
                .mapX(request.getMapX())
                .mapY(request.getMapY())
                .imageAssetKey(request.getImageAssetKey())
                .sortOrder(nextSortOrder)
                .build();

        ScenarioLocation savedLocation = locationRepository.save(location);

        // 부모 시나리오의 updatedAt 강제 갱신 (AI 검증 결과 우회 차단)
        scenario.forceUpdateModifiedAt();

        // 생성된 자식 ID만 DTO로 매핑하여 반환
        return new CustomLocationCreateResponse(savedLocation.getId());
    }
}
