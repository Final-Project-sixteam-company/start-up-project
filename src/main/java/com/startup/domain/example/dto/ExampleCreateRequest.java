package com.startup.domain.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Example 생성 요청")
// 요청 DTO는 record로 만들고, 입력 검증은 Bean Validation annotation에 모은다.
public record ExampleCreateRequest(
        @Schema(description = "제목", example = "첫 번째 예제")
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
        String title,

        @Schema(description = "본문", example = "팀 컨벤션을 맞추기 위한 예제 데이터입니다.")
        @NotBlank(message = "본문은 필수입니다.")
        @Size(max = 1000, message = "본문은 1000자 이하로 입력해 주세요.")
        String content,

        @Schema(description = "상태. DRAFT, ACTIVE, ARCHIVED 또는 한글 라벨을 사용할 수 있습니다.", example = "ACTIVE")
        String status
) {
}
