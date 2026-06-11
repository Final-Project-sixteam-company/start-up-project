package com.startup.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomHintCreateRequest {

    @NotBlank(message = "힌트 내용은 필수입니다.")
    private String content;

    @PositiveOrZero(message = "해금 시간은 0 이상이어야 합니다.")
    private Integer unlockAfterMinutes;

    @PositiveOrZero(message = "페널티 점수는 0 이상이어야 합니다.")
    private Integer penaltyScore;

    @Positive(message = "힌트 레벨은 1 이상이어야 합니다.")
    private Integer hintLevel;
}
