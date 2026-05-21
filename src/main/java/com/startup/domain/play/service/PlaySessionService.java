package com.startup.domain.play.service;

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
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
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
    private final HintRepository hintRepository;
    private final UsedHintRepository usedHintRepository;

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

        // 플레이 세션 생성
        PlaySession session = PlaySession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .build();
        playSessionRepository.save(session);

        // 시나리오 플레이 카운트 증가
        // TODO: 동시성이 중요해지면 atomic update 또는 @Version 적용 검토

        // 초기 공개 증거(is_initial_public = true) 자동 해금
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

    //대시보드 조회
    @Transactional
    public DashboardResponse getDashboard(Long userId, Long sessionId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        Scenario scenario = scenarioRepository.findById(session.getScenarioId())
                .orElseThrow(() -> new ScenarioException(ScenarioErrorCode.SCENARIO_NOT_FOUND));

        //카운트 조회 전에 시간 기반 해금을 먼저 동기화
        processTimeBasedUnlocks(session);

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
                session.getHintCount(),
                session.getInterrogationCount(),
                briefing
        );
    }

    // 증거 목록 조회 - 조회 시점에 시간 기반 자동 해금 처리후 반환
    @Transactional
    public List<PlayEvidenceResponse> getEvidences(Long userId, Long sessionId, Boolean includeLocked) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        // 조회 시점에 시간 기반으로 자동 해금 처리 (Lazy Evaluation)
        processTimeBasedUnlocks(session);

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

            // includeLocked가 false(기본)이면 해금된 증거만 반환
            if (!Boolean.TRUE.equals(includeLocked) && !isUnlocked) {
                continue;
            }

            // 관련 용의자 정보 (해금된 증거만 관련 용의자를 표시)
            List<PlayEvidenceResponse.RelatedSuspectDto> relatedSuspects = new ArrayList<>();
            if (isUnlocked) {
                List<EvidenceSuspect> relations = evidenceSuspectMap.getOrDefault(evidence.getId(), List.of());
                for (EvidenceSuspect rel : relations) {
                    String suspectName = suspectNameMap.getOrDefault(rel.getSuspectId(), "알 수 없음");
                    relatedSuspects.add(new PlayEvidenceResponse.RelatedSuspectDto(rel.getSuspectId(), suspectName));
                }
            }

            // 미해금 증거는 설명을 숨기고 힌트만 표시
            String description = isUnlocked ? evidence.getDescription() : null;
            String unlockHint = isUnlocked ? null : buildUnlockHint(evidence);

            result.add(new PlayEvidenceResponse(
                    evidence.getId(),
                    evidence.getTitle(),
                    description,
                    locationNameMap.get(evidence.getLocationId()),
                    evidence.getImportance(),
                    isUnlocked,
                    unlockHint,
                    relatedSuspects.isEmpty() ? null : relatedSuspects
            ));
        }

        return result;
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

    //용의자 목록 조회
    @Transactional(readOnly = true)
    public List<PlaySuspectResponse> getSuspects(Long userId, Long sessionId) {
        PlaySession session = getSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        List<Suspect> suspects = suspectRepository.findAllByScenarioIdOrderBySortOrder(session.getScenarioId());

        // 용의자별 심문 횟수 조회 (현재 심문 로그 테이블 미구현 → 0으로 반환)
        // TODO: InterrogationLog 테이블 연결 후 용의자별 심문 횟수 계산 로직 추가
        return suspects.stream()
                .map(suspect -> new PlaySuspectResponse(
                        suspect.getId(),
                        suspect.getName(),
                        suspect.getRole(),
                        suspect.getRelationToVictim(),
                        suspect.getPublicStatement(),
                        suspect.getAlibi(),
                        suspect.getSuspicionLevel(),
                        0 // TODO: 실제 용의자별 심문 횟수 계산
                ))
                .toList();
    }

    // ──────────────────────────────────────────────
    // 내부 헬퍼 메서드
    // ──────────────────────────────────────────────

    private PlaySession getSessionOrThrow(Long sessionId) {
        return playSessionRepository.findById(sessionId)
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

    // 경과 시간에 따라 자동 해금 조건이 충족된 증거를 DB에 기록한다.
    // 이미 해금된 증거는 중복 삽입하지 않도록 existsByPlaySessionIdAndEvidenceId로 체크한다.
    private void processTimeBasedUnlocks(PlaySession session) {
        if (!session.isPlaying()) return;

        int elapsedMinutes = calculateElapsedSeconds(session) / 60;

        List<Evidence> timeBasedEvidences = evidenceRepository
                .findAllByScenarioIdOrderBySortOrder(session.getScenarioId())
                .stream()
                .filter(e -> !e.getIsInitialPublic())
                .filter(e -> e.getUnlockAfterMinutes() != null)
                .filter(e -> elapsedMinutes >= e.getUnlockAfterMinutes())
                .toList();

        for (Evidence evidence : timeBasedEvidences) {
            try {
                unlockedEvidenceRepository.save(
                        UnlockedEvidence.builder()
                                .playSessionId(session.getId())
                                .evidenceId(evidence.getId())
                                .unlockedReason("TIME_BASED")
                                .build()
                );
                log.info("시간 기반 증거 자동 해금: sessionId={}, evidenceId={}, elapsedMinutes={}",
                        session.getId(), evidence.getId(), elapsedMinutes);
            } catch (DataIntegrityViolationException e) {
                log.debug("시간 기반 증거 이미 해금됨 (중복 삽입 방어): sessionId={}, evidenceId={}", session.getId(), evidence.getId());
            }
        }
    }

    private String buildUnlockHint(Evidence evidence) {
        if (evidence.getUnlockAfterMinutes() != null) {
            return evidence.getUnlockAfterMinutes() + "분 후 공개";
        }
        if (EvidenceUnlockType.INTERROGATION == evidence.getUnlockType()) {
            return "심문을 통해 해금";
        }
        if (EvidenceUnlockType.EVIDENCE_PRESENTED == evidence.getUnlockType()) {
            return "증거 제시로 해금";
        }
        return "조건 충족 시 해금";
    }
}
