package com.startup.domain.play.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.play.dto.*;
import com.startup.domain.play.entity.PlaySession;
import com.startup.domain.play.entity.UnlockedEvidence;
import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.error.PlayErrorCode;
import com.startup.domain.play.error.PlayException;
import com.startup.domain.play.repository.PlaySessionRepository;
import com.startup.domain.play.repository.UnlockedEvidenceRepository;
import com.startup.domain.play.repository.UsedHintRepository;
import com.startup.domain.play.entity.UsedHint;
import com.startup.domain.play.support.EvidenceVariantDescriptionResolver;
import com.startup.domain.play.support.EvidenceUnlockPolicy;
import com.startup.domain.play.support.FinalDeductionLockManager;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.repository.HintRepository;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.*;
import com.startup.domain.scenario.service.ScenarioAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaySessionService {

    private final PlaySessionRepository playSessionRepository;
    private final UnlockedEvidenceRepository unlockedEvidenceRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final EvidenceRepository evidenceRepository;
    private final SuspectRepository suspectRepository;
    private final VictimRepository victimRepository;
    private final ScenarioLocationRepository scenarioLocationRepository;
    private final EvidenceSuspectRepository evidenceSuspectRepository;
    private final EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    private final HintRepository hintRepository;
    private final UsedHintRepository usedHintRepository;
    private final InterrogationLogRepository interrogationLogRepository;
    private final ScenarioVariantRepository scenarioVariantRepository;
    private final FinalDeductionLockManager finalDeductionLockManager;
    private final EvidenceVariantDescriptionResolver evidenceVariantDescriptionResolver;
    private final EvidenceUnlockPolicy evidenceUnlockPolicy;
    private final TimelineEventRepository timelineEventRepository;

    //게임 시작 세션
    @Transactional
    public PlaySessionCreateResponse createSession(Long userId, PlaySessionCreateRequest request) {
        Long scenarioId = request.scenarioId();

        // 시나리오 존재 확인
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        // 접근 권한 확인
        scenarioAccessService.validatePlayable(userId, scenarioId);

        // 이미 진행 중인 세션이 있는지 확인
        playSessionRepository.findByUserIdAndScenarioIdAndStatus(userId, scenarioId, PlaySessionStatus.PLAYING)
                .ifPresent(existing -> {
                    throw new PlayException(PlayErrorCode.SESSION_ALREADY_EXISTS);
                });

        Long variantId = selectVariantId(scenarioId);

        // 플레이 세션 생성
        PlaySession session = PlaySession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .scenarioVariantId(variantId)
                .build();

        try {
            // saveAndFlush로 즉시 DB에 반영 → active_key UNIQUE 제약 위반 즉시 캐치
            playSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 또는 이미 PLAYING 중인 세션 존재 시 처리
            throw new PlayException(PlayErrorCode.SESSION_ALREADY_EXISTS);
        }

        // 시나리오 플레이 카운트 증가
        scenarioRepository.incrementPlayCount(scenarioId);

        // 초기 공개 증거 자동 해금
        List<Evidence> initialEvidences = evidenceRepository.findAllByScenarioIdAndIsInitialPublicTrue(scenarioId);
        for (Evidence evidence : initialEvidences) {
            UnlockedEvidence unlocked = UnlockedEvidence.builder()
                    .playSessionId(session.getId())
                    .evidenceId(evidence.getId())
                    .unlockedReason("INITIAL_PUBLIC")
                    .build();
            unlockedEvidenceRepository.save(unlocked);
        }

        log.info("게임 세션 생성: sessionId={}, userId={}, scenarioId={}, 초기 해금 증거={}건",
                session.getId(), userId, scenarioId, initialEvidences.size());

        return PlaySessionCreateResponse.from(session);
    }

    @Transactional(readOnly = true)
    public PlaySessionDetailResponse getSessionDetail(Long userId, Long sessionId) {
        // 세션 존재 여부 확인
        PlaySession session = playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));

        // 소유권 검증: 남의 게임 세션을 URL ID 추측으로 훔쳐보지 못하도록 차단
        if (!session.getUserId().equals(userId)) {
            throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);
        }

        // 3. DTO 변환 및 반환
        return PlaySessionDetailResponse.from(session);
    }


    //대시보드 조회
    @Transactional
    public DashboardResponse getDashboard(Long userId, Long sessionId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        Scenario scenario = scenarioRepository.findById(session.getScenarioId())
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        //카운트 조회 전에 자동 해금 조건을 먼저 동기화
        processAutomaticUnlocks(session);

        // 해금 증거 수
        int unlockedCount = unlockedEvidenceRepository.countByPlaySessionId(sessionId);
        int totalCount = evidenceRepository.countByScenarioId(session.getScenarioId());

        // 경과 시간 계산 (세션 시작부터 현재까지)
        int elapsedSeconds = calculateElapsedSeconds(session);

        // 피해자 정보로 브리핑 구성
        DashboardResponse.BriefingDto briefing = buildBriefing(session.getScenarioId());

        return new DashboardResponse(
                session.getId(),
                session.getScenarioId(),
                scenario.getTitle(),
                session.getStatus(),
                elapsedSeconds,
                unlockedCount,
                totalCount,
                usedHintRepository.countByPlaySessionId(sessionId),
                interrogationLogRepository.countByPlaySessionId(sessionId),
                briefing
        );
    }

    // 증거 목록 조회 - 조회 시점에 자동 해금 조건 처리 후 반환
    @Transactional
    public List<PlayEvidenceResponse> getEvidences(Long userId, Long sessionId, Boolean includeLocked, String status) {

        if (status != null) {
            if (status.trim().isEmpty()) {
                status = null; // 빈 문자열은 null로 취급하여 기본 로직을 타게 함
            } else if (!"unlocked".equalsIgnoreCase(status) && !"locked".equalsIgnoreCase(status)) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE); // 이상한 문자열은 400 에러
            }
        }

        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        // 조회 시점에 자동 해금 조건 처리 (Lazy Evaluation)
        processAutomaticUnlocks(session);

        // 시나리오의 전체 증거 조회
        List<Evidence> allEvidences = evidenceRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId());

        // 해금된 증거 ID Set
        Set<Long> unlockedEvidenceIds = unlockedEvidenceRepository.findAllByPlaySessionId(sessionId)
                .stream()
                .map(UnlockedEvidence::getEvidenceId)
                .collect(Collectors.toSet());

        // 증거-용의자 관계 조회
        List<Long> evidenceIds = allEvidences.stream().map(Evidence::getId).toList();
        Map<Long, List<EvidenceSuspect>> evidenceSuspectMap = buildEvidenceSuspectMap(evidenceIds);

        // 용의자 이름 맵 (suspectId -> name)
        Map<Long, String> suspectNameMap = buildSuspectNameMap(session.getScenarioId());

        // 장소 이름 맵 (locationId -> name)
        Map<Long, String> locationNameMap = buildLocationNameMap(allEvidences);

        List<PlayEvidenceResponse> result = new ArrayList<>();
        for (Evidence evidence : allEvidences) {
            boolean isUnlocked = unlockedEvidenceIds.contains(evidence.getId());

            // status 파라미터 필터링
            if ("unlocked".equalsIgnoreCase(status) && !isUnlocked) continue; // unlocked 요청인데 미해금이면 스킵
            if ("locked".equalsIgnoreCase(status) && isUnlocked) continue;    // locked 요청인데 해금됐으면 스킵
            // status가 없을 때: includeLocked=false(기본)이면 해금된 증거만 반환
            if (status == null && !Boolean.TRUE.equals(includeLocked) && !isUnlocked) continue;

            // 관련 용의자 정보 (해금된 증거만 표시)
            List<PlayEvidenceResponse.RelatedSuspectDto> relatedSuspects = new ArrayList<>();
            if(isUnlocked) {
                List<EvidenceSuspect> relations = evidenceSuspectMap.getOrDefault(evidence.getId(), List.of());
                for (EvidenceSuspect rel : relations) {
                    String suspectName = suspectNameMap.getOrDefault(rel.getSuspectId(), "알 수 없음");
                    relatedSuspects.add(new PlayEvidenceResponse.RelatedSuspectDto(rel.getSuspectId(), suspectName));
                }
            }

            // 미해금 증거는 상세/이미지/장소를 마스킹한다.
            String description = isUnlocked
                    ? evidenceVariantDescriptionResolver.resolve(evidence, session.getScenarioVariantId())
                    : null;
            String oneLine = isUnlocked ? evidence.getOneLine() : null;
            String imageAssetKey = isUnlocked ? evidence.getImageAssetKey() : null;
            String locationName = isUnlocked && evidence.getLocationId() != null
                    ? locationNameMap.get(evidence.getLocationId())
                    : null;
            String unlockHint = isUnlocked ? null : buildUnlockHint(evidence);
            String imageUrl = isUnlocked ? evidence.getImageUrl() : null;

            result.add(new PlayEvidenceResponse(
                    evidence.getId(),
                    evidence.getTitle(),
                    oneLine,
                    description,
                    imageAssetKey,
                    locationName,
                    evidence.getImportance(),
                    isUnlocked,
                    unlockHint,
                    imageUrl,
                    relatedSuspects.isEmpty() ? null : relatedSuspects
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<PlayLocationResponse> getLocations(Long userId, Long sessionId) {
        // 세션 존재 확인 및 소유권 검증 (보안)
        PlaySession session = playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);
        }

        // 현재 시나리오의 모든 장소 조회
        Long scenarioId = session.getScenarioId();
        List<ScenarioLocation> locations = scenarioLocationRepository.findAllByScenarioIdOrderBySortOrder(scenarioId);

        // 장소별 증거 총 개수를 맵으로 한 번에 가져옴 (N+1 성능 방어)
        java.util.Map<Long, Integer> evidenceCountMap = evidenceRepository.countByLocationIdForScenario(scenarioId).stream()
                .collect(java.util.stream.Collectors.toMap(obj -> (Long) obj[0], obj -> ((Number) obj[1]).intValue()));

        // 장소 리스트와 카운트 맵을 조합하여 응답 DTO 생성
        return locations.stream()
                .map(location -> {
                    int count = evidenceCountMap.getOrDefault(location.getId(), 0);
                    return PlayLocationResponse.from(location, count);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.List<PlayTimelineResponse> getTimeline(Long userId, Long sessionId) {
        PlaySession session = playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));

        // 소유권 검증 (동시성/보안 방어)
        if (!session.getUserId().equals(userId)) {
            throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);
        }

        // 해당 시나리오의 전체 타임라인 사건을 순서대로 모두 가져옴
        java.util.List<TimelineEvent> allEvents = timelineEventRepository.findAllByScenarioIdOrderByEventOrder(session.getScenarioId());

        // 현재 유저가 지금까지 게임하면서 '해금한(찾은) 증거 ID' 목록을 Set으로 변환 (O(1) 조회를 위해 Set 사용)
        java.util.Set<Long> unlockedEvidenceIds = unlockedEvidenceRepository.findAllByPlaySessionId(sessionId).stream()
                .map(unlocked -> unlocked.getEvidenceId())
                .collect(java.util.stream.Collectors.toSet());

        // 필터링 로직 (스포일러 방지)
        return allEvents.stream()
                .filter(event -> {
                    // 특정 타임라인 사건이 어떤 증거(관련 증거 ID)와 연결되어 있다면?
                    if (event.getRelatedEvidenceId() != null) {
                        // 유저가 그 증거를 찾았을 때만 타임라인에 보여준다! (못 찾았으면 숨김 처리)
                        return unlockedEvidenceIds.contains(event.getRelatedEvidenceId());
                    }
                    // 관련 증거가 없는 사건 중, 대외 공개(PUBLIC) 설정된 사건만 노출 (스포일러 방어)
                    return "PUBLIC".equalsIgnoreCase(event.getVisibility());
                })
                .map(PlayTimelineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayEvidenceDetailResponse getEvidenceDetail(Long userId, Long sessionId, Long evidenceId) {
        // 소유권 및 세션 검증
        PlaySession session = playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);
        }

        // 증거 존재 확인 및 타 시나리오 증거 찌르기 방어
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.EVIDENCE_NOT_FOUND));

        if (!evidence.getScenarioId().equals(session.getScenarioId())) {
            // 다른 시나리오의 증거 ID를 입력한 경우 못 찾은 척(404) 튕겨냄
            throw new PlayException(PlayErrorCode.EVIDENCE_NOT_FOUND);
        }

        // 조회 시점에 자동 해금 조건 동기화 처리 (단건 상세 조회 시점의 최신 상태 반영)
        processAutomaticUnlocks(session);

        // 해금 여부 검증 (스포일러 완벽 방어)
        // 기본 제공 증거(isInitialPublic)가 아니라면, 반드시 UnlockedEvidence에 기록이 있어야 함
        if (!evidence.getIsInitialPublic()) {
            boolean isUnlocked = unlockedEvidenceRepository.existsByPlaySessionIdAndEvidenceId(sessionId, evidenceId);
            if (!isUnlocked) {
                // 아직 못 얻은 증거라면 마스킹할 필요 없이 단호하게 403 Forbidden 에러 반환!
                throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED, "아직 해금되지 않은 증거입니다.");
            }
        }

        // 연관 데이터 조회 (Location, Suspects, TimelineEvents)
        // 장소 정보
        ScenarioLocation location = null;
        if (evidence.getLocationId() != null) {
            location = scenarioLocationRepository.findById(evidence.getLocationId()).orElse(null);
        }

        // 연관 용의자 정보
        java.util.List<Long> suspectIds = evidenceSuspectRepository.findAllByEvidenceIdIn(java.util.List.of(evidenceId)).stream()
                .map(es -> es.getSuspectId())
                .toList();
        java.util.List<Suspect> relatedSuspects = suspectIds.isEmpty() ? java.util.List.of() : suspectRepository.findAllById(suspectIds);

        // 연관 타임라인 사건 정보
        java.util.List<TimelineEvent> relatedTimelines = timelineEventRepository.findAllByRelatedEvidenceIdOrderByEventOrder(evidenceId);

        // 변이(Variant) 전용 설명 해석 (단건 조회 시에도 선택된 변이 경로에 맞는 단서를 보여줌)
        String resolvedDescription = evidenceVariantDescriptionResolver.resolve(evidence, session.getScenarioVariantId());

        // 모든 정보를 조립하여 반환
        return PlayEvidenceDetailResponse.of(evidence, resolvedDescription, location, relatedSuspects, relatedTimelines);
    }


    private Long selectVariantId(Long scenarioId) {
        List<ScenarioVariant> activeVariants =
                scenarioVariantRepository.findAllByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(scenarioId);
        if (activeVariants.isEmpty()) {
            return null;
        }

        int totalWeight = activeVariants.stream()
                .map(ScenarioVariant::getWeight)
                .filter(Objects::nonNull)
                .mapToInt(weight -> Math.max(weight, 0))
                .sum();

        if (totalWeight <= 0) {
            return activeVariants.getFirst().getId();
        }

        int ticket = ThreadLocalRandom.current().nextInt(totalWeight);
        int cursor = 0;
        for (ScenarioVariant variant : activeVariants) {
            cursor += Math.max(variant.getWeight() == null ? 0 : variant.getWeight(), 0);
            if (ticket < cursor) {
                return variant.getId();
            }
        }
        return activeVariants.getLast().getId();
    }

    //힌트 목록 조회
    @Transactional(readOnly = true)
    public List<PlayHintResponse> getHints(Long userId, Long sessionId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        // 현재 경과 시간
        int elapsedMinutes = calculateElapsedSeconds(session) / 60;

        List<Hint> hints = hintRepository.findAllByScenarioIdOrderByHintLevel(session.getScenarioId());

        // 사용한 힌트 ID Set
        Set<Long> usedHintIds = usedHintRepository.findAllByPlaySessionId(sessionId)
                .stream()
                .map(UsedHint::getHintId)
                .collect(Collectors.toSet());

        return hints.stream()
                .map(hint -> {
                    // 해금 가능 여부: unlock_after_minutes가 null(즉시)이거나 경과 시간을 넘겼을 때
                    boolean isAvailable = hint.getUnlockAfterMinutes() == null
                            || elapsedMinutes >= hint.getUnlockAfterMinutes();
                    boolean isUsed = usedHintIds.contains(hint.getId());

                    // 사용하지 않은 힌트는 content를 null로 숨긴다
                    String content = isUsed ? hint.getContent() : null;

                    // 미해금 상태이면 남은 시간을 내려준다
                    Integer remainingMinutes = !isAvailable ? hint.getUnlockAfterMinutes() - elapsedMinutes : null;

                    return new PlayHintResponse(
                            hint.getId(),
                            hint.getHintLevel(),
                            content,
                            isAvailable,
                            isUsed,
                            remainingMinutes,
                            hint.getPenaltyScore()
                    );
                })
                .toList();
    }

    //힌트 사용
    @Transactional
    public HintUseResponse useHint(Long userId, Long sessionId, Long hintId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        if (!session.isPlaying()) {
            throw new PlayException(PlayErrorCode.SESSION_NOT_PLAYING);
        }

        // AI 채점이 진행 중(in-flight lock)이면 힌트 사용 차단
        if (finalDeductionLockManager.isLocked(sessionId)) {
            throw new PlayException(PlayErrorCode.SESSION_NOT_PLAYING);
        }

        Hint hint = hintRepository.findById(hintId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.HINT_NOT_FOUND));

        // 이 힌트가 현재 플레이 중인 시나리오의 힌트인지 검증 (스포일러/타 시나리오 접근 방어)
        if (!hint.getScenarioId().equals(session.getScenarioId())) {
            throw new PlayException(PlayErrorCode.HINT_NOT_FOUND);
        }

        // 1. 이미 사용된 힌트인지 확인 (exists 대신 findBy 사용!)
        var existingUsedHint = usedHintRepository.findByPlaySessionIdAndHintId(sessionId, hintId);
        if (existingUsedHint.isPresent()) {
            // 이미 까본 힌트면 DB에 적혀있는 "최초 사용 시각"을 그대로 반환! (시간 갱신 방지)
            return new HintUseResponse(hint.getId(), hint.getContent(), hint.getPenaltyScore(), existingUsedHint.get().getUsedAt());
        }

        // 2. 해금 가능 여부 (시간) 검증
        int elapsedMinutes = calculateElapsedSeconds(session) / 60;
        if (hint.getUnlockAfterMinutes() != null && elapsedMinutes < hint.getUnlockAfterMinutes()) {
            throw new PlayException(PlayErrorCode.HINT_NOT_AVAILABLE);
        }

        LocalDateTime nowTime = LocalDateTime.now();

        // 3. 처음 사용하는 경우 DB에 INSERT IGNORE (동시성 방어)
        int insertedRow = usedHintRepository.insertIgnoreUsedHint(sessionId, hintId, nowTime);

        LocalDateTime usedAt;
        if (insertedRow > 0) {
            // 성공적으로 인서트 했으면 힌트 카운트 증가 + 현재 시간 부여
            session.incrementHintCount();
            usedAt = nowTime;
        } else {
            // 0.001초 차이로 동시 클릭해서 실패한 거면, 방금 들어간 최초 시간 다시 꺼내옴
            log.warn("[useHint] 중복 힌트 사용 감지(동시 요청 무시). sessionId={}, hintId={}", sessionId, hintId);
            usedAt = usedHintRepository.findByPlaySessionIdAndHintId(sessionId, hintId)
                    .map(com.startup.domain.play.entity.UsedHint::getUsedAt)
                    .orElse(nowTime);
        }

        return new HintUseResponse(hint.getId(), hint.getContent(), hint.getPenaltyScore(), usedAt);
    }


    //용의자 목록 조회
    @Transactional(readOnly = true)
    public List<PlaySuspectResponse> getSuspects(Long userId, Long sessionId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        List<Suspect> suspects = suspectRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId());

        //세션 내 모든 용의자의 심문 횟수를 한번의 쿼리로 조회하여 map으로 변환
        Map<Long, Integer> interrogationCountMap = interrogationLogRepository
                .countInterrogationsPerSuspect(sessionId)
                .stream()
                .collect(Collectors.toMap(
                        InterrogationLogRepository.SuspectInterrogationCount::getSuspectId,
                        countDto -> countDto.getCount().intValue()
                ));

        // 용의자별 심문 횟수 조회
        return suspects.stream()
                .map(suspect -> new PlaySuspectResponse(
                        suspect.getId(),
                        suspect.getName(),
                        suspect.getRole(),
                        suspect.getRelationToVictim(),
                        suspect.getPublicStatement(),
                        suspect.getAlibi(),
                        suspect.getSuspicionLevel(),
                        interrogationCountMap.getOrDefault(suspect.getId(), 0) //map에서 가져오고 없으면 0
                ))
                .toList();
    }

    // 최종 추리 완료 시 세션 상태 전환
    //score/grade는 FinalDeduction 테이블에 저장되므로 여기서는 상태 전환과 active_key 해제만 처리
    @Transactional
    public void completeSession(Long sessionId) {
        PlaySession session = getSessionForUpdateOrThrow(sessionId);

        if(!session.isPlaying()){
            log.warn("이미 종료된 세션에 대한 완료 요청: sessionId={}", sessionId);
            throw new PlayException(PlayErrorCode.SESSION_NOT_PLAYING);
        }

        session.markCompleted();
        log.info("세션 완료 처리: sessionId={}", sessionId);
    }

    // 유저가 게임을 포기할 때
    @Transactional
    public void abandonSession(Long userId, Long sessionId) {
        PlaySession session = getSessionForUpdateOrThrow(sessionId);
        validateSessionOwner(session, userId);

        if (!session.isPlaying()) {
            throw new PlayException(PlayErrorCode.SESSION_NOT_PLAYING);
        }

        session.abandon(); // active_key도 null로 초기화됨
        log.info("세션 포기 처리: sessionId={}", sessionId);
    }

    // ──────────────────────────────────────────────
    // 내부 헬퍼 메서드
    // ──────────────────────────────────────────────

    private PlaySession getSessionOrThrow(Long sessionId) {
        return playSessionRepository.findById(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));
    }

    private PlaySession getSessionForUpdateOrThrow(Long sessionId) {
        return playSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new PlayException(PlayErrorCode.SESSION_NOT_FOUND));
    }

    private void validateSessionOwner(PlaySession session, Long userId) {
        if (!session.getUserId().equals(userId)) {
            throw new PlayException(PlayErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    private int calculateElapsedSeconds(PlaySession session) {
        if (session.isPlaying()) {
            return (int) Duration.between(session.getStartedAt(), LocalDateTime.now()).getSeconds();
        }
        return session.getCurrentElapsedSeconds();
    }

    private DashboardResponse.BriefingDto buildBriefing(Long scenarioId) {
        Optional<Victim> victimOpt = victimRepository.findFirstByScenarioId(scenarioId);
        if (victimOpt.isEmpty()) {
            return new DashboardResponse.BriefingDto("알 수 없음", "알 수 없음", "사건 정보가 없습니다.");
        }

        Victim victim = victimOpt.get();
        String foundLocation = "알 수 없음";
        if (victim.getFoundLocationId() != null) {
            foundLocation = scenarioLocationRepository.findById(victim.getFoundLocationId())
                    .map(ScenarioLocation::getName)
                    .orElse("알 수 없음");
        }

        String summary = String.format("%s이(가) %s에서 사망했다.", victim.getName(), foundLocation);

        return new DashboardResponse.BriefingDto(victim.getName(), foundLocation, summary);
    }

    private Map<Long, List<EvidenceSuspect>> buildEvidenceSuspectMap(List<Long> evidenceIds) {
        if (evidenceIds.isEmpty()) {
            return Map.of();
        }
        return evidenceSuspectRepository.findAllByEvidenceIdIn(evidenceIds)
                .stream()
                .collect(Collectors.groupingBy(EvidenceSuspect::getEvidenceId));
    }

    private Map<Long, String> buildSuspectNameMap(Long scenarioId) {
        return suspectRepository.findAllByScenarioIdOrderBySortOrder(scenarioId)
                .stream()
                .collect(Collectors.toMap(Suspect::getId, Suspect::getName));
    }

    private Map<Long, String> buildLocationNameMap(List<Evidence> evidences) {
        Set<Long> locationIds = evidences.stream()
                .map(Evidence::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        return scenarioLocationRepository.findAllById(new ArrayList<>(locationIds))
                .stream()
                .collect(Collectors.toMap(ScenarioLocation::getId, ScenarioLocation::getName));
    }

    // 경과 시간/phase에 따라 자동 해금 조건이 충족된 증거를 DB에 기록한다.
    // 이미 해금된 증거 id를 먼저 조회해 중복 저장을 시도하지 않는다
    private void processAutomaticUnlocks(PlaySession session) {
        if (!session.isPlaying()) return;

        int elapsedMinutes = calculateElapsedSeconds(session) / 60;

        //먼저 해금된 증거 id를 set으로 가져옴
        Set<Long> alreadyUnlockedIds = unlockedEvidenceRepository
                .findAllByPlaySessionId(session.getId())
                .stream()
                .map(UnlockedEvidence::getEvidenceId)
                .collect(Collectors.toSet());

        List<Evidence> scenarioEvidences = evidenceRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId());
        Map<Long, EvidenceUnlockRule> unlockRulesByEvidenceId = evidenceUnlockRuleRepository
                .findAllByScenarioIdOrderBySortOrder(session.getScenarioId())
                .stream()
                .collect(Collectors.toMap(
                        EvidenceUnlockRule::getEvidenceId,
                        Function.identity(),
                        (left, right) -> left
                ));
        Set<String> unlockedEvidenceCodes = scenarioEvidences.stream()
                .filter(evidence -> alreadyUnlockedIds.contains(evidence.getId()))
                .map(Evidence::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Evidence> autoUnlockEvidences = scenarioEvidences
                .stream()
                .filter(e -> !alreadyUnlockedIds.contains(e.getId())) //이미 해금된건 제외
                .filter(e -> evidenceUnlockPolicy.canAutoUnlock(
                        e,
                        unlockRulesByEvidenceId.get(e.getId()),
                        elapsedMinutes,
                        unlockedEvidenceCodes
                ))
                .toList();

        for (Evidence evidence : autoUnlockEvidences) {
            unlockedEvidenceRepository.insertIgnoreUnlockedEvidence(
                    session.getId(), evidence.getId(), evidence.getUnlockType().name() + "_AUTO");

            log.info("증거 자동 해금 시도: sessionId={}, evidenceId={}, unlockType={}, elapsedMinutes={}",
                    session.getId(), evidence.getId(), evidence.getUnlockType(), elapsedMinutes);
        }
    }

    private String buildUnlockHint(Evidence evidence) {
        return evidenceUnlockPolicy.buildUnlockHint(evidence);
    }
}
