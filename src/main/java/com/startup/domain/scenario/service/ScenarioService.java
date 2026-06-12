package com.startup.domain.scenario.service;

import com.startup.common.dto.PageResponse;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.community.repository.ScenarioBookmarkRepository;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.Difficulty;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.enums.ScenarioVisibility;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.*;
import com.startup.domain.scenario.support.ScenarioAssetUrlResolver;
import com.startup.domain.scenario.support.ScenarioPublishValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final HintRepository hintRepository;
    private final ScenarioAssetUrlResolver scenarioAssetUrlResolver;
    private final ScenarioPublishValidator scenarioPublishValidator;
    private final ScenarioBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

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

        // ── N+1 방어: IN 절 벌크 쿼리로 한 번에 카운트 ──
        List<Long> scenarioIds = scenarios.getContent().stream().map(Scenario::getId).toList();

        Map<Long, Integer> suspectCountMap = suspectRepository.countByScenarioIdIn(scenarioIds).stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));
        Map<Long, Integer> evidenceCountMap = evidenceRepository.countByScenarioIdIn(scenarioIds).stream()
                .collect(Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));

        // 북마크 상태 일괄 조회 (N+1 방지)
        Set<Long> bookmarkedScenarioIds;
        if (userId != null) {
            bookmarkedScenarioIds = bookmarkRepository.findScenarioIdsByUserIdAndScenarioIdIn(userId, scenarioIds);
        } else {
            bookmarkedScenarioIds = Collections.emptySet();
        }

        Page<ScenarioSummaryResponse> responsePage = scenarios.map(scenario -> {
            int suspectCount = suspectCountMap.getOrDefault(scenario.getId(), 0);
            int evidenceCount = evidenceCountMap.getOrDefault(scenario.getId(), 0);
            boolean isBookmarked = bookmarkedScenarioIds.contains(scenario.getId());
            String thumbnailUrl = scenarioAssetUrlResolver.resolve(scenario.getCoverAssetKey());
            Boolean canPlay = scenarioAccessService.canPlay(userId, scenario.getId());
            return ScenarioSummaryResponse.from(scenario, suspectCount, evidenceCount, isBookmarked, thumbnailUrl, canPlay);
        });

        return PageResponse.from(responsePage);
    }

    @Transactional(readOnly = true)
    public ScenarioDetailResponse getScenario(Long userId, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // 작성자 본인이 아니면서, 대중에게 공개되지 않은 시나리오에 접근하는 것을 차단
        boolean isCreator = userId != null && userId.equals(scenario.getCreatorId());
        boolean isPubliclyVisible = scenario.getStatus() == ScenarioStatus.PUBLISHED &&
                (scenario.getVisibility() == ScenarioVisibility.PUBLIC ||
                 scenario.getVisibility() == ScenarioVisibility.OFFICIAL ||
                 scenario.getVisibility() == ScenarioVisibility.UNLISTED);  // UNLISTED는 링크 기반 접근 허용

        if (!isCreator && !isPubliclyVisible) {
            // 작성자가 아니면 에러 반환
            log.warn("인증되지 않은 시나리오 접근 시도: userId={}, scenarioId={}, status={}, visibility={}",
                    userId, scenarioId, scenario.getStatus(), scenario.getVisibility());
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND);
        }

        scenarioAccessService.validateViewable(userId, scenarioId);

        String creatorNickname = "운영자";
        if (scenario.getCreatorId() != null) {
            creatorNickname = userRepository.findById(scenario.getCreatorId())
                    .map(com.startup.domain.auth.entity.User::getNickname)
                    .orElse("운영자");
        }

        Boolean isBookmarked = userId != null && bookmarkRepository.existsByUserIdAndScenarioId(userId, scenarioId);
        Boolean canPlay = scenarioAccessService.canPlay(userId, scenarioId);

        int suspectCount = suspectRepository.countByScenarioId(scenarioId);
        int evidenceCount = evidenceRepository.countByScenarioId(scenarioId);
        int hintCount = hintRepository.countByScenarioId(scenarioId);

        String coverImageUrl = scenarioAssetUrlResolver.resolve(scenario.getCoverAssetKey());
        String mapImageUrl = scenarioAssetUrlResolver.resolve(scenario.getMapAssetKey());

        return ScenarioDetailResponse.from(scenario, creatorNickname, suspectCount, evidenceCount, hintCount,
                isBookmarked, canPlay, coverImageUrl, mapImageUrl);

    }

    @Transactional
    public ScenarioCreateResponse createScenario(Long userId, ScenarioCreateRequest request) {
        Scenario scenario = Scenario.builder()
                .title(StringUtils.hasText(request.title()) ? request.title() : "제목 없는 사건")
                .description(StringUtils.hasText(request.description()) ? request.description() : "")
                .synopsis(request.synopsis())
                .scenarioType(ScenarioType.CUSTOM)          // 유저가 만들면 무조건 CUSTOM
                .visibility(ScenarioVisibility.PRIVATE)     // 최초 생성 시 무조건 PRIVATE (스토어 미노출)
                .difficulty(request.difficulty() != null ? request.difficulty() : Difficulty.NORMAL)
                .playerCountMin(request.playerCountMin() != null ? request.playerCountMin() : 1)
                .playerCountMax(request.playerCountMax() != null ? request.playerCountMax() : 1)
                .estimatedPlayTimeMinutes(request.estimatedPlayTimeMinutes() != null ? request.estimatedPlayTimeMinutes() : 30)
                .creatorId(userId)                          // 요청한 유저를 작성자로 매핑
                .status(ScenarioStatus.DRAFT)               // 무조건 DRAFT로 강제 (클라이언트 값 무시)
                .build();

        Scenario saved = scenarioRepository.save(scenario);

        return new ScenarioCreateResponse(saved.getId(), saved.getStatus());
    }

    @Transactional
    public ScenarioUpdateResponse updateScenario(Long userId, Long scenarioId, ScenarioUpdateRequest request) {
        // 낙관적 락 대신 비관적 락 사용:
        // 같은 시나리오를 두 기기에서 동시에 수정할 경우 데이터 충돌 방지
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // 소유권 검증 (타인 시나리오 조작 방어)
        if (!userId.equals(scenario.getCreatorId())) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }

        // 상태 잠금: DRAFT 상태일 때만 수정 허용
        // PUBLISHED된 시나리오를 수정하면 현재 플레이 중인 유저에게 영향을 줄 수 있음
        if (scenario.getStatus() != ScenarioStatus.DRAFT) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH,
                    "DRAFT 상태의 시나리오만 수정할 수 있습니다.");
        }

        // 부분 수정 (null 필드는 기존 값 유지)
        scenario.updateBasicInfo(
                request.title(),
                request.description(),
                request.difficulty(),
                request.estimatedPlayTimeMinutes()
        );

        // @Transactional 내에서 변경 감지(Dirty Checking)가 동작하므로 save() 불필요
        return new ScenarioUpdateResponse(scenarioId, true);
    }


    @Transactional
    public ScenarioPublishResponse publishScenario(Long userId, Long scenarioId, ScenarioPublishRequest request) {
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // 소유권 검증 (null-safe)
        if (!userId.equals(scenario.getCreatorId())) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_ACCESS_DENIED);
        }

        // 상태 전환 가능 여부 검증 (ScenarioStatus.canPublish() 활용)
        if (!scenario.getStatus().canPublish()) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH);
        }

        // 요청된 visibility 검증 (PUBLIC 또는 UNLISTED만 허용)
        if (request.visibility() != ScenarioVisibility.PUBLIC && request.visibility() != ScenarioVisibility.UNLISTED) {
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH, "발행 시에는 PUBLIC 또는 UNLISTED 상태로만 변경할 수 있습니다.");
        }

        // 정합성 검증 (여기서 부족한 항목 체크)
        scenarioPublishValidator.validate(scenario);

        // 상태 PUBLISHED로 변경
        scenario.publish(request.visibility());

        return new ScenarioPublishResponse(
                scenario.getId(),
                scenario.getVisibility(),
                scenario.getStatus(),
                scenario.getPublishedAt()
        );
    }

    private Pageable mapPageableSort(Pageable pageable) {
        Sort mappedSort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            String property = switch (order.getProperty().toLowerCase()) {
                case "popular" -> "playCount";
                case "rating" -> "averageRating";
                case "latest" -> "createdAt";
                default -> {
                    // 지원하지 않는 정렬 키는 기본값(createdAt)으로 대체
                    // 런타임 에러 방지
                    yield "createdAt";
                }
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
