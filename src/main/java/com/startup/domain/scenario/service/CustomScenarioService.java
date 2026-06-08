package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.ScenarioLocation;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.entity.Victim;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.repository.ScenarioLocationRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import com.startup.domain.scenario.repository.VictimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioLocationRepository locationRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;

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

    // 상단에 private final VictimRepository victimRepository; 추가 필요

    @Transactional
    public CustomVictimCreateResponse createOrUpdateVictim(Long userId, Long scenarioId, CustomVictimCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 상태 방어 (발행된 시나리오는 수정 불가)
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // 입력받은 발견 장소가 현재 시나리오 소속인지 검사
        if (request.getFoundLocationId() != null) {
            ScenarioLocation location = locationRepository.findById(request.getFoundLocationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다."));
            if (!location.getScenarioId().equals(scenarioId)) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "다른 시나리오의 장소를 피해자 발견 위치로 지정할 수 없습니다.");
            }
        }

        // UPSERT 분기 (이미 있으면 덮어쓰기, 없으면 새로 생성)
        Victim savedVictim;
        Optional<Victim> existingVictim = victimRepository.findByScenarioId(scenarioId);

        if (existingVictim.isPresent()) {
            Victim victim = existingVictim.get();
            victim.updateInfo(
                    request.getFoundLocationId(), request.getName(), request.getAge(),
                    request.getRole(), request.getDescription(), request.getCauseOfDeath(),
                    request.getFoundCondition()
            );
            savedVictim = victim;
        } else {
            Victim newVictim = Victim.builder()
                    .scenarioId(scenarioId)
                    .foundLocationId(request.getFoundLocationId())
                    .name(request.getName())
                    .age(request.getAge())
                    .role(request.getRole())
                    .description(request.getDescription())
                    .causeOfDeath(request.getCauseOfDeath())
                    .foundCondition(request.getFoundCondition())
                    .build();
            savedVictim = victimRepository.save(newVictim);
        }

        // 부모 시나리오의 updatedAt 강제 갱신 -> AI 논리 검증 우회 전면 차단
        scenario.forceUpdateModifiedAt();

        return new CustomVictimCreateResponse(savedVictim.getId());
    }

    @Transactional
    public CustomSuspectCreateResponse createSuspect(Long userId, Long scenarioId, CustomSuspectCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 발행된 시나리오는 수정 불가
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        Integer maxSortOrder = suspectRepository.findMaxSortOrderByScenarioId(scenarioId);
        int nextSortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 1;

        // 엔티티 생성 및 저장
        Suspect newSuspect = Suspect.builder()
                .scenarioId(scenarioId)
                .name(request.getName())
                .role(request.getRole())
                .characterType(request.getCharacterType())
                .culpritEligible(request.getCulpritEligible() != null ? request.getCulpritEligible() : true)
                .relationToVictim(request.getRelationToVictim())
                .publicProfile(request.getPublicProfile())
                .publicStatement(request.getPublicStatement())
                .alibi(request.getAlibi())
                .personalityPrompt(request.getPersonalityPrompt())
                .portraitAssetKey(request.getPortraitAssetKey())
                .suspicionLevel(0) // 초기 기본값
                .sortOrder(nextSortOrder)
                .build();

        Suspect savedSuspect = suspectRepository.save(newSuspect);

        // 부모 시나리오의 updatedAt 강제 갱신
        scenario.forceUpdateModifiedAt();

        return new CustomSuspectCreateResponse(savedSuspect.getId());
    }


}
