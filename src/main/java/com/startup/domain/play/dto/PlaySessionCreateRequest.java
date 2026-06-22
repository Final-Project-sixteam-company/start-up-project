package com.startup.domain.play.dto;

import jakarta.validation.constraints.NotNull;

// 게임 세션 시작 요청 DTO
public record PlaySessionCreateRequest(
        @NotNull(message = "시나리오 ID는 필수입니다.")
        Long scenarioId
) {
}
