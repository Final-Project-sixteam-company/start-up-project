package com.startup.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CustomSolutionCreateRequest {

    @NotNull(message = "범인 용의자 ID는 필수입니다.")
    private Long culpritSuspectId;

    @NotBlank(message = "범행 동기는 필수입니다.")
    private String motive;

    @NotBlank(message = "범행 수법은 필수입니다.")
    private String method;

    private String coverUp;
    private String fullExplanation;
    
    // 증거 ID들을 받아 쉼표로 구분된 문자열로 변환하여 저장
    private List<Long> keyEvidenceIds;
}
