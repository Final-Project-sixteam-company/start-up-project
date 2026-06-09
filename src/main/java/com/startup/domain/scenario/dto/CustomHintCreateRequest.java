package com.startup.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomHintCreateRequest {

    @NotBlank(message = "힌트 내용은 필수입니다.")
    private String content;

    private Integer unlockAfterMinutes;
    private Integer penaltyScore;
    private Integer hintLevel;
}
