package com.startup.domain.scenario.support;

import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.entity.SuspectResponsePolicy;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import com.startup.domain.ai.support.MockSolutionReader;
import com.startup.domain.ai.support.ScenarioDataReader;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.scenario.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
// 시나리오 검증 API 전용. Solution·SuspectSecret 등 정답 데이터를 포함하므로
// 플레이 API 또는 심문 프롬프트에서 절대 사용하지 않는다.
public class DefaultScenarioDataReader implements ScenarioDataReader {

    private final ScenarioRepository scenarioRepository;
    private final VictimRepository victimRepository;
    private final ScenarioLocationRepository locationRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSuspectRepository evidenceSuspectRepository;
    private final HintRepository hintRepository;
    private final ScenarioVariantRepository variantRepository;
    private final VariantSolutionRepository variantSolutionRepository;
    private final SuspectResponsePolicyRepository policyRepository;
    private final MockSolutionReader mockSolutionReader;
    private final SolutionRepository solutionRepository;

    @Override
    @Transactional(readOnly = true)
    public ScenarioValidationData loadForValidation(Long scenarioId) {
        // 1. 시나리오 기본 정보
        var scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCENARIO_NOT_FOUND));

        ScenarioValidationData.ScenarioBasicInfo basicInfo = new ScenarioValidationData.ScenarioBasicInfo(
                scenario.getId(),
                scenario.getTitle(),
                scenario.getDescription(),
                scenario.getDifficulty().name()
        );

        // 2. 피해자 정보 (시나리오당 1명, 없으면 검증 불가)
        Victim victim = victimRepository.findFirstByScenarioId(scenarioId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCENARIO_DATA_INCOMPLETE));

        ScenarioValidationData.VictimInfo victimInfo = new ScenarioValidationData.VictimInfo(
                victim.getName(),
                victim.getRole(),
                victim.getCauseOfDeath(),
                victim.getFoundCondition()
        );

        // 3. 장소 목록
        List<ScenarioValidationData.LocationInfo> locations = locationRepository
                .findAllByScenarioIdOrderBySortOrder(scenarioId)
                .stream()
                .map(loc -> new ScenarioValidationData.LocationInfo(
                        loc.getId(),
                        loc.getName(),
                        loc.getDescription()
                ))
                .toList();

        // 4. 용의자 목록 + 응답 정책
        List<Suspect> suspects = suspectRepository.findAllByScenarioIdOrderBySortOrder(scenarioId);
        List<Long> suspectIds = suspects.stream().map(Suspect::getId).toList();

        // 용의자별 응답 정책을 한 번에 조회 후 suspectId로 그룹핑
        Map<Long, List<SuspectResponsePolicy>> policyBySuspectId = policyRepository
                .findAllBySuspectIdIn(suspectIds)
                .stream()
                .collect(Collectors.groupingBy(SuspectResponsePolicy::getSuspectId));

        // 정답 정보 (범인 판별용) - variant가 없으면 isCulprit은 모두 false로 처리
        Long culpritSuspectId = resolveCulpritSuspectId(scenarioId);

        List<ScenarioValidationData.SuspectValidationInfo> suspectInfos = suspects.stream()
                .map(s -> new ScenarioValidationData.SuspectValidationInfo(
                        s.getId(),
                        s.getName(),
                        s.getRole(),
                        s.getAlibi(),
                        s.getPublicStatement(),
                        s.getPublicProfile(),
                        s.getId().equals(culpritSuspectId)
                ))
                .toList();

        // 5. 증거 목록 + 관련 용의자 매핑
        List<Evidence> evidences = evidenceRepository.findAllByScenarioIdOrderBySortOrder(scenarioId);
        List<Long> evidenceIds = evidences.stream().map(Evidence::getId).toList();

        // 증거별 관련 용의자 ID를 한 번에 조회 후 evidenceId로 그룹핑
        Map<Long, List<Long>> relatedSuspectIdsByEvidence = evidenceSuspectRepository
                .findAllByEvidenceIdIn(evidenceIds)
                .stream()
                .collect(Collectors.groupingBy(
                        EvidenceSuspect::getEvidenceId,
                        Collectors.mapping(EvidenceSuspect::getSuspectId, Collectors.toList())
                ));

        List<ScenarioValidationData.EvidenceValidationInfo> evidenceInfos = evidences.stream()
                .map(e -> new ScenarioValidationData.EvidenceValidationInfo(
                        e.getId(),
                        e.getTitle(),
                        e.getDescription(),
                        e.getImportance().name(),
                        e.getIsInitialPublic(),
                        relatedSuspectIdsByEvidence.getOrDefault(e.getId(), List.of())
                ))
                .toList();

        // 6. 힌트 목록
        List<ScenarioValidationData.HintValidationInfo> hints = hintRepository
                .findAllByScenarioIdOrderByHintLevel(scenarioId)
                .stream()
                .map(h -> new ScenarioValidationData.HintValidationInfo(
                        h.getId(),
                        h.getHintLevel(),
                        h.getContent(),
                        h.getPenaltyScore()
                ))
                .toList();

        // 7. 타임라인 이벤트 - 별도 테이블/엔티티가 없으므로 빈 리스트로 처리
        // TODO: 타임라인 테이블이 추가되면 여기에 조회 로직을 추가한다.
        List<ScenarioValidationData.TimelineEventInfo> timelineEvents = List.of();

        // 8. 정답(Solution) + 핵심 증거 목록
        ScenarioValidationData.SolutionValidationInfo solutionInfo = buildSolutionInfo(scenarioId);
        List<ScenarioValidationData.SolutionEvidenceInfo> solutionEvidences = buildSolutionEvidences(
                scenarioId, solutionInfo != null ? solutionInfo.keyEvidenceIds() : List.of()
        );

        // 9. 응답 정책 목록 (suspectId, conditionKey 단위 평탄화)
        List<ScenarioValidationData.ResponsePolicyInfo> responsePolicies = policyBySuspectId.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(p -> new ScenarioValidationData.ResponsePolicyInfo(
                                p.getSuspectId(),
                                p.getConditionKey(),
                                p.getPolicyText(),
                                p.getPriority()
                        ))
                )
                .toList();

        // 10. 용의자 비밀(SuspectSecret) - 별도 테이블/엔티티가 없으므로 빈 리스트로 처리
        // TODO: SuspectSecret 테이블이 추가되면 여기에 조회 로직을 추가한다.
        List<ScenarioValidationData.SuspectSecretInfo> suspectSecrets = List.of();

        log.info("[ScenarioDataReader] 시나리오 검증 데이터 로드 완료. scenarioId={}, 용의자={}, 증거={}, 힌트={}",
                scenarioId, suspects.size(), evidences.size(), hints.size());

        return new ScenarioValidationData(
                basicInfo,
                victimInfo,
                locations,
                suspectInfos,
                evidenceInfos,
                hints,
                timelineEvents,
                solutionInfo,
                solutionEvidences,
                responsePolicies,
                suspectSecrets
        );
    }

    /**
     * 활성 variant의 범인 suspectId를 조회한다.
     * variant 데이터가 없으면 MockSolutionReader로 fallback한다.
     */
    private Long resolveCulpritSuspectId(Long scenarioId) {
        Long culpritId = variantRepository.findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(scenarioId)
                .flatMap(variant -> variantSolutionRepository.findByVariantId(variant.getId()))
                .map(VariantSolution::getCulpritSuspectId)
                .orElseGet(() -> solutionRepository.findByScenarioId(scenarioId)
                                .map(Solution::getCulpritSuspectId).orElse(null));

        if (culpritId == null) {
            try {
                return mockSolutionReader.findByScenarioId(scenarioId).culpritSuspectId();
            } catch (AiException e) {
                return null;
            }
        }
        return culpritId;
    }

    /**
     * 활성 variant의 정답 정보를 SolutionValidationInfo로 조립한다.
     * variant 또는 solution 데이터가 없으면 MockSolutionReader로 fallback한다.
     */
    private ScenarioValidationData.SolutionValidationInfo buildSolutionInfo(Long scenarioId) {
        return variantRepository.findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(scenarioId)
                .flatMap(variant -> variantSolutionRepository.findByVariantId(variant.getId()))
                .map(solution -> new ScenarioValidationData.SolutionValidationInfo(
                        solution.getCulpritSuspectId(),
                        solution.getCulpritName(),
                        solution.getCulpritRole(),
                        solution.getMotive(),
                        solution.getMethod(),
                        solution.getCoverUp(),
                        solution.getFullExplanation(),
                        solution.parseKeyEvidenceIds()
                ))
                .orElseGet(() -> solutionRepository.findByScenarioId(scenarioId)
                        .map(solution -> {
                            // 커스텀 정답엔 이름/역할 컬럼이 없으므로 용의자 테이블에서 즉시 조회
                            Suspect suspect = suspectRepository.findById(solution.getCulpritSuspectId()).orElse(null);
                            return new ScenarioValidationData.SolutionValidationInfo(
                                    solution.getCulpritSuspectId(),
                                    suspect != null ? suspect.getName() : "알 수 없음",
                                    suspect != null ? suspect.getRole() : "알 수 없음",
                                    solution.getMotive(),
                                    solution.getMethod(),
                                    solution.getCoverUp(),
                                    solution.getFullExplanation(),
                                    solution.parseKeyEvidenceIds()
                            );
                        })
                .orElseGet(() -> {
                    log.warn("[ScenarioDataReader] 활성 variant/solution 없음. scenarioId={} -> Mock으로 fallback", scenarioId);
                    try {
                        var mock = mockSolutionReader.findByScenarioId(scenarioId);
                        return new ScenarioValidationData.SolutionValidationInfo(
                                mock.culpritSuspectId(),
                                mock.culpritName(),
                                mock.culpritRole(),
                                mock.motive(),
                                mock.method(),
                                mock.coverUp(),
                                mock.fullExplanation(),
                                mock.keyEvidenceIds()
                        );
                    } catch (AiException e) {
                        return null;
                    }
                }));
    }

    /**
     * 핵심 증거 ID 목록으로 SolutionEvidenceInfo를 조립한다.
     * 증거가 해당 시나리오 소속인지 확인하고, 요청된 갯수와 다르면 데이터 불일치 예외를 던진다.
     */
    private List<ScenarioValidationData.SolutionEvidenceInfo> buildSolutionEvidences(
            Long scenarioId, List<Long> keyEvidenceIds) {
        if (keyEvidenceIds.isEmpty()) {
            return List.of();
        }

        List<Evidence> evidences = evidenceRepository.findAllById(keyEvidenceIds);

        // 1. 해당 시나리오 소속인지 검증
        List<Evidence> validEvidences = evidences.stream()
                .filter(e -> e.getScenarioId().equals(scenarioId))
                .toList();

        // 2. 요청된 ID 개수와 조회된 유효 증거 개수가 다르면 데이터 불일치 예외 발생
        if (validEvidences.size() != keyEvidenceIds.size()) {
            log.error("[ScenarioDataReader] 핵심 증거 ID 불일치. 요청={}, 유효={}", keyEvidenceIds.size(), validEvidences.size());
            throw new AiException(AiErrorCode.SCENARIO_DATA_INCOMPLETE);
        }

        return validEvidences.stream()
                .map(e -> new ScenarioValidationData.SolutionEvidenceInfo(
                        e.getId(),
                        e.getTitle() // reason 컬럼이 없으므로 제목으로 대체
                ))
                .toList();
    }

}
