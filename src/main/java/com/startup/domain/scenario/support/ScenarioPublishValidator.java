package com.startup.domain.scenario.support;

import com.startup.domain.ai.repository.ScenarioValidationResultRepository;
import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.enums.ScenarioType;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.HintRepository;
import com.startup.domain.scenario.repository.SolutionRepository;
import com.startup.domain.scenario.repository.SolutionEvidenceRepository;
import com.startup.domain.scenario.repository.SuspectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ScenarioPublishValidator {

    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final SolutionRepository solutionRepository;
    private final SolutionEvidenceRepository solutionEvidenceRepository;
    private final HintRepository hintRepository;
    private final ScenarioValidationResultRepository scenarioValidationResultRepository;

    public void validate(Scenario scenario) {
        List<String> errors = new ArrayList<>();

        // 1. 제목/설명 필수
        if (!StringUtils.hasText(scenario.getTitle())) {
            errors.add("제목 없음");
        }
        if (!StringUtils.hasText(scenario.getDescription())) {
            errors.add("설명 없음");
        }

        // 2. 용의자 최소 2명
        if (suspectRepository.countByScenarioId(scenario.getId()) < 2) {
            errors.add("용의자 2명 이상 필요");
        }

        // 3. 증거 최소 3개
        if (evidenceRepository.countByScenarioId(scenario.getId()) < 3) {
            errors.add("증거 3개 이상 필요");
        }

        // 4. 힌트 최소 1개
        if(hintRepository.countByScenarioId(scenario.getId()) < 1) {
            errors.add("힌트 1개 이상 필요");
        }

        // 5. 정답(Solution) 설정 여부 및 진범 소속 검증
        solutionRepository.findByScenarioId(scenario.getId()).ifPresentOrElse(
                solution -> {
                    if (suspectRepository.findByIdAndScenarioId(solution.getCulpritSuspectId(), scenario.getId()).isEmpty()) {
                        errors.add("정답의 범인이 현재 시나리오의 용의자가 아님");
                    }

                    List<Long> keyEvidenceIds = solutionEvidenceRepository.findAllBySolutionId(solution.getId())
                            .stream()
                            .map(se -> se.getEvidence().getId())
                            .toList();

                    if (keyEvidenceIds.isEmpty()) {
                        errors.add("정답에 핵심 증거가 지정되지 않음");
                    } else {
                        // 핵심 증거가 모두 현재 시나리오 소속인지 검증
                        long validCount = evidenceRepository.countByIdInAndScenarioId(keyEvidenceIds, scenario.getId());

                        if (validCount != keyEvidenceIds.size()) {
                            errors.add("정답의 핵심 증거 중 현재 시나리오에 속하지 않는 증거가 있음");
                        }
                    }

                    if (com.startup.common.util.TextTokenizerUtil.extractValidTokens(solution.getMotive()).isEmpty()) {
                        errors.add("정답의 동기(motive)는 2글자 이상의 단어가 포함되어야 합니다.");
                    }
                    if (com.startup.common.util.TextTokenizerUtil.extractValidTokens(solution.getMethod()).isEmpty()) {
                        errors.add("정답의 방법(method)은 2글자 이상의 단어가 포함되어야 합니다.");
                    }
                    if (com.startup.common.util.TextTokenizerUtil.extractValidTokens(solution.getCoverUp()).isEmpty()) {
                        errors.add("정답의 은폐 방법(coverUp)은 2글자 이상의 단어가 포함되어야 합니다.");
                    }
                },
                () -> errors.add("정답 미설정")
        );

        // 6. AI 검증 통과 여부 검증 (💡 클로드 지적사항 반영)
        // 공식 시나리오는 자체 검수하므로 제외, 커스텀 시나리오인 경우에만 강제
        if (scenario.getScenarioType() == ScenarioType.CUSTOM) {
            scenarioValidationResultRepository.findTopByScenarioIdOrderByCheckedAtDescIdDesc(scenario.getId())
                    .ifPresentOrElse(
                            result -> {
                                String status = result.getValidationStatus();
                                if (scenario.getUpdatedAt() != null && !result.getCheckedAt().isAfter(scenario.getUpdatedAt())) {
                                    errors.add("AI 검증 이후 시나리오가 수정되었습니다. 다시 검증해주세요.");
                                } else if (!"PASSED".equals(status) && !"PASSED_WITH_WARNINGS".equals(status)) {
                                    errors.add("AI 논리 검증을 통과하지 못함 (최근 상태: " + status + ")");
                                }
                            },
                            () -> errors.add("AI 논리 검증 기록이 없음")
                    );
        }

        // 에러가 하나라도 있으면 커스텀 메시지를 담아 예외 던짐
        if (!errors.isEmpty()) {
            String errorMessage = "발행 불가 사유: " + String.join(", ", errors);
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH, errorMessage);
        }
    }
}
