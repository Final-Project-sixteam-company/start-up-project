package com.startup.domain.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Example 수정 요청")
// PUT 예제이므로 수정에 필요한 값을 명시적으로 모두 받는다.
public record ExampleUpdateRequest(
        @Schema(description = "제목", example = "수정된 예제")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
        String title,

        @Schema(description = "본문", example = "수정된 예제 본문입니다.")
        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 1000, message = "본문은 1000자 이하로 입력해 주세요.")
        String content,

        @Schema(description = "상태", example = "ACTIVE")
        @NotBlank(message = "상태는 필수입니다.")
        String status
) {
}
