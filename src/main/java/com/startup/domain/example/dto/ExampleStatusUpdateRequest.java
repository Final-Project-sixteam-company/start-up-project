package com.startup.domain.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Example 상태 변경 요청")
// 부분 변경 API는 전용 DTO를 분리해 의도를 좁게 유지한다.
public record ExampleStatusUpdateRequest(
        @Schema(description = "변경할 상태", example = "ARCHIVED")
        @NotBlank(message = "상태는 필수입니다.")
        String status
) {
}
