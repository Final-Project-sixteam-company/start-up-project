package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Hint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomHintResponse {
    private Long id;
    private Long scenarioId;
    private Integer hintLevel;
    private String content;
    private Integer unlockAfterMinutes;
    private Integer penaltyScore;
    private LocalDateTime createdAt;

    public static CustomHintResponse from(Hint hint) {
        if (hint == null) return null;
        return CustomHintResponse.builder()
                .id(hint.getId())
                .scenarioId(hint.getScenarioId())
                .hintLevel(hint.getHintLevel())
                .content(hint.getContent())
                .unlockAfterMinutes(hint.getUnlockAfterMinutes())
                .penaltyScore(hint.getPenaltyScore())
                .createdAt(hint.getCreatedAt())
                .build();
    }
}
