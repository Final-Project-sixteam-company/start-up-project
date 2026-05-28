package com.startup.domain.play.support;

import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.SolutionReader;
import com.startup.domain.scenario.entity.ScenarioVariant;
import com.startup.domain.scenario.entity.VariantSolution;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.ScenarioVariantRepository;
import com.startup.domain.scenario.repository.VariantSolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Primary
@Component
@RequiredArgsConstructor
public class DefaultSolutionReader implements SolutionReader {

    private final ScenarioVariantRepository variantRepository;
    private final VariantSolutionRepository variantSolutionRepository;
    private final EvidenceRepository evidenceRepository;

    @Override
    public SolutionInfo findByScenarioId(Long scenarioId) {
        // MVP: is_active=true인 variant 1개를 조회 (SECRETARY 고정)
        ScenarioVariant variant = variantRepository
                .findFirstByScenarioIdAndIsActiveTrueOrderBySortOrderAsc(scenarioId)
                .orElseThrow(() -> new AiException(AiErrorCode.SOLUTION_NOT_FOUND));

        VariantSolution solution = variantSolutionRepository
                .findByVariantId(variant.getId())
                .orElseThrow(() -> new AiException(AiErrorCode.SOLUTION_NOT_FOUND));

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
