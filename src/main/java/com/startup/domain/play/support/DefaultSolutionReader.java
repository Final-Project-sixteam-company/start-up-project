package com.startup.domain.play.support;

import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.MockSolutionReader;
import com.startup.domain.ai.support.SolutionReader;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.entity.ScenarioType;
import com.startup.domain.scenario.entity.ScenarioVariant;
import com.startup.domain.scenario.entity.Solution;
import com.startup.domain.scenario.entity.Suspect;
import com.startup.domain.scenario.entity.VariantSolution;
import com.startup.domain.scenario.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DefaultSolutionReader implements SolutionReader {

    private final ScenarioVariantRepository variantRepository;
    private final VariantSolutionRepository variantSolutionRepository;
    private final EvidenceRepository evidenceRepository;
    // seed 데이터가 없는 환경(개발/데모)에서 SOLUTION_NOT_FOUND를 방지하기 위한 fallback.
    // scenario_variants / variant_solutions 테이블에 행이 채워지면 이 경로는 사용되지 않는다.
    private final MockSolutionReader mockSolutionReader;
    private final SolutionRepository solutionRepository;
    private final SuspectRepository suspectRepository;
    private final ScenarioRepository scenarioRepository;

    @Override
    public SolutionInfo findByScenarioId(Long scenarioId) {
        return findByScenarioIdAndVariantId(scenarioId, null);
    }

    @Override
    public SolutionInfo findByScenarioIdAndVariantId(Long scenarioId, Long variantId) {

        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new AiException(AiErrorCode.SCENARIO_NOT_FOUND));

        if (scenario.getScenarioType() == ScenarioType.CUSTOM) {
            Optional<Solution> customSolutionOpt = solutionRepository.findByScenarioId(scenarioId);

            if (customSolutionOpt.isPresent()) {
                Solution customSolution = customSolutionOpt.get();
                Suspect suspect = suspectRepository.findByIdAndScenarioId(customSolution.getCulpritSuspectId(), scenarioId)
                        .orElseThrow(() -> new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));

            List<Long> keyEvidenceIds = customSolution.parseKeyEvidenceIds();

            // 증거 제목은 DB에서 동적으로 조회
            Map<Long, String> evidenceTitles = evidenceRepository.findAllById(keyEvidenceIds)
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getId(),
                            e -> e.getTitle()
                    ));

            return new SolutionInfo(
                    suspect.getId(),
                    suspect.getName(),
                    suspect.getRole(),
                    customSolution.getMotive(),
                    customSolution.getMethod(),
                    customSolution.getCoverUp(),
                    customSolution.getFullExplanation(),
                    keyEvidenceIds,
                    evidenceTitles
            );
        }
        }

        ScenarioVariant variant = null;

        // 1. 세션에 고정된 variantId가 있으면 그걸 우선 조회
        if (variantId != null) {
            variant = variantRepository.findById(variantId).orElse(null);
        }

        // 2. 세션에 고정된 게 없으면 (과거 세션 등) 현재 active=true인 variant 조회
        if (variant == null) {
            variant = variantRepository
                    .findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(scenarioId)
                    .orElse(null);
        }

        if (variant == null) {
            log.warn("[SolutionReader] variant 찾을 수 없음. scenarioId={}, variantId={} -> Mock으로 fallback", scenarioId, variantId);
            return mockSolutionReader.findByScenarioIdAndVariantId(scenarioId, variantId);
        }

        VariantSolution solution = variantSolutionRepository
                .findByVariantId(variant.getId())
                .orElse(null);

        if (solution == null) {
            log.warn("[SolutionReader] variant_solutions에 데이터 없음. variantId={} -> MockSolutionReader로 fallback", variant.getId());
            return mockSolutionReader.findByScenarioIdAndVariantId(scenarioId, variantId);
        }

        List<Long> keyEvidenceIds = solution.parseKeyEvidenceIds();

        // 증거 제목은 DB에서 동적으로 조회
        Map<Long, String> evidenceTitles = evidenceRepository.findAllById(keyEvidenceIds)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getId(),
                        e -> e.getTitle()
                ));

        return new SolutionInfo(
                solution.getCulpritSuspectId(),
                solution.getCulpritName(),
                solution.getCulpritRole(),
                solution.getMotive(),
                solution.getMethod(),
                solution.getCoverUp(),
                solution.getFullExplanation(),
                keyEvidenceIds,
                evidenceTitles
        );
    }
}
