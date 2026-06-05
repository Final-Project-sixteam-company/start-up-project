package com.startup.domain.scenario.support;

import com.startup.domain.scenario.entity.Scenario;
import com.startup.domain.scenario.error.ScenarioErrorCode;
import com.startup.domain.scenario.error.ScenarioException;
import com.startup.domain.scenario.repository.EvidenceRepository;
import com.startup.domain.scenario.repository.HintRepository;
import com.startup.domain.scenario.repository.SolutionRepository;
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
    private final HintRepository hintRepository;

    public void validate(Scenario scenario) {
        List<String> errors = new ArrayList<>();

        // 1. 제목/설명 필수
        if (!StringUtils.hasText(scenario.getTitle())) {
            errors.add("제목 없음");
        }
        if (!StringUtils.hasText(scenario.getDescription())) {
            errors.add("설명 없음");
        }

        // 2. 용의자 최소 1명
        if (suspectRepository.countByScenarioId(scenario.getId()) < 1) {
            errors.add("용의자 1명 이상 필요");
        }

        // 3. 증거 최소 N개 (예: 1개)
        if (evidenceRepository.countByScenarioId(scenario.getId()) < 1) {
            errors.add("증거 1개 이상 필요");
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
                    if (solution.parseKeyEvidenceIds().isEmpty()) {
                        errors.add("정답에 핵심 증거가 지정되지 않음");
                    }
                    if (!StringUtils.hasText(solution.getMotive())) {
                        errors.add("정답의 동기(motive) 미입력");
                    }
                    if (!StringUtils.hasText(solution.getMethod())) {
                        errors.add("정답의 방법(method) 미입력");
                    }
                    if (!StringUtils.hasText(solution.getCoverUp())) {
                        errors.add("정답의 은폐 방법(coverUp) 미입력");
                    }
                },
                () -> errors.add("정답 미설정")
        );

        // 에러가 하나라도 있으면 커스텀 메시지를 담아 예외 던짐
        if (!errors.isEmpty()) {
            String errorMessage = "발행 불가 사유: " + String.join(", ", errors);
            // 기존 ScenarioException(ErrorCode, String) 생성자 활용!
            throw new ScenarioException(ScenarioErrorCode.SCENARIO_CANNOT_PUBLISH, errorMessage);
        }
    }
}
