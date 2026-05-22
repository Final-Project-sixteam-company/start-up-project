package com.startup.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "테스트 푸시 발송 요청")
public record TestPushRequest(
        @Schema(description = "알림 제목", example = "ClueRoom")
        @NotBlank(message = "알림 제목은 필수입니다.")
        @Size(max = 100, message = "알림 제목은 100자 이하로 입력해 주세요.")
        String title,

        @Schema(description = "알림 본문", example = "테스트 푸시 알림입니다.")
        @NotBlank(message = "알림 본문은 필수입니다.")
        @Size(max = 500, message = "알림 본문은 500자 이하로 입력해 주세요.")
        String body
) {
}
