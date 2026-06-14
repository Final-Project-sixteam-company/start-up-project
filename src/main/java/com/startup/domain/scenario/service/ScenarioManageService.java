package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.community.repository.ScenarioBookmarkRepository;
import com.startup.domain.scenario.dto.ScenarioDeleteResponse;
import com.startup.domain.scenario.dto.ScenarioHideResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.ScenarioRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import com.startup.domain.scenario.support.ScenarioAssetUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioManageService {

    private final ScenarioRepository scenarioRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final ScenarioBookmarkRepository bookmarkRepository;
    private final ScenarioAssetUrlResolver scenarioAssetUrlResolver;

    @Transactional(readOnly = true)
    public PageResponse<ScenarioSummaryResponse> getMyScenarios(Long userId, Pageable pageable) {
        // 내가 작성한 시나리오 목록 조회 (삭제된 것은 제외)
        Page<Scenario> scenarios = scenarioRepository.findAllByCreatorIdAndStatusNot(
                userId,
                ScenarioStatus.DELETED,
                ensureStableSort(pageable)
        );

        List<Long> scenarioIds = scenarios.getContent().stream().map(Scenario::getId).toList();

        final Map<Long, Integer> suspectCountMap;
        final Map<Long, Integer> evidenceCountMap;
        final Set<Long> bookmarkedScenarioIds;

        if (!scenarioIds.isEmpty()) {
            suspectCountMap = suspectRepository.countByScenarioIdIn(scenarioIds).stream()
                    .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));
            evidenceCountMap = evidenceRepository.countByScenarioIdIn(scenarioIds).stream()
                    .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));

            if (userId != null) {
                bookmarkedScenarioIds = bookmarkRepository.findScenarioIdsByUserIdAndScenarioIdIn(userId, scenarioIds);
            } else {
                bookmarkedScenarioIds = Collections.emptySet();
            }
        } else {
            suspectCountMap = Collections.emptyMap();
            evidenceCountMap = Collections.emptyMap();
            bookmarkedScenarioIds = Collections.emptySet();
        }

        Page<ScenarioSummaryResponse> responsePage = scenarios.map(scenario -> {
            int suspectCount = suspectCountMap.getOrDefault(scenario.getId(), 0);
            int evidenceCount = evidenceCountMap.getOrDefault(scenario.getId(), 0);
            boolean isBookmarked = bookmarkedScenarioIds.contains(scenario.getId());
            String thumbnailUrl = scenarioAssetUrlResolver.resolve(scenario.getCoverAssetKey());

            // 내 시나리오는 항상 플레이 가능하다고 간주하거나 별도의 로직 불필요 시 true 반환
            // (권한 체크는 이미 creator_id로 쿼리에서 끝났으므로)
            Boolean canPlay = true;
            return ScenarioSummaryResponse.from(scenario, suspectCount, evidenceCount, isBookmarked, thumbnailUrl, canPlay);
        });

        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ScenarioSummaryResponse> getBookmarkedScenarios(Long userId, Pageable pageable) {
        // 내가 북마크한 시나리오 목록 조회 (삭제/숨김 방어 + 링크 공유 시나리오 포함)
        List<ScenarioVisibility> allowedVisibilities = List.of(
                ScenarioVisibility.PUBLIC, 
                ScenarioVisibility.OFFICIAL, 
                ScenarioVisibility.UNLISTED
        );

        Page<Scenario> scenarios = scenarioRepository.findBookmarkedScenarios(
                userId,
                ScenarioStatus.PUBLISHED,
                allowedVisibilities,
                ensureStableSort(pageable)
        );

        List<Long> scenarioIds = scenarios.getContent().stream().map(Scenario::getId).toList();

        final Map<Long, Integer> suspectCountMap;
        final Map<Long, Integer> evidenceCountMap;

        if (!scenarioIds.isEmpty()) {
            suspectCountMap = suspectRepository.countByScenarioIdIn(scenarioIds).stream()
                    .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));
            evidenceCountMap = evidenceRepository.countByScenarioIdIn(scenarioIds).stream()
                    .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));
        } else {
            suspectCountMap = Collections.emptyMap();
            evidenceCountMap = Collections.emptyMap();
        }

        Page<ScenarioSummaryResponse> responsePage = scenarios.map(scenario -> {
            int suspectCount = suspectCountMap.getOrDefault(scenario.getId(), 0);
            int evidenceCount = evidenceCountMap.getOrDefault(scenario.getId(), 0);

            boolean isBookmarked = true;
            String thumbnailUrl = scenarioAssetUrlResolver.resolve(scenario.getCoverAssetKey());

            // canPlay 로직을 ScenarioAccessService에 위임하는 것이 정확하지만, 일단 북마크한 PUBLISHED 상태의 시나리오이므로 조회 시점엔 true로 간주.
            Boolean canPlay = true;
            return ScenarioSummaryResponse.from(scenario, suspectCount, evidenceCount, isBookmarked, thumbnailUrl, canPlay);
        });

        return PageResponse.from(responsePage);
    }

    @Transactional
    public ScenarioDeleteResponse deleteScenario(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getStatus() == ScenarioStatus.DELETED) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ALREADY_DELETED);
        }

        if (!userId.equals(scenario.getCreatorId())) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }

        if (!scenario.getStatus().canDelete()) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_DELETE);
        }

        scenario.delete();
        return new ScenarioDeleteResponse(scenarioId, scenario.getStatus());
    }

    @Transactional
    public ScenarioHideResponse hideScenario(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        if (!userId.equals(scenario.getCreatorId())) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }

        if (!scenario.getStatus().canHide()) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_HIDE);
        }

        scenario.hide();
        return new ScenarioHideResponse(scenarioId, scenario.getStatus());
    }

    private Pageable ensureStableSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                            .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
            );
        }

        // 이미 정렬 조건이 있더라도, id 기준 정렬을 후순위로 추가하여 완벽히 Stable하게 만듦
        return org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
        );
    }
}
