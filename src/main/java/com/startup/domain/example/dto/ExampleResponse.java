package com.startup.domain.example.dto;

import com.startup.domain.example.entity.Example;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Example 응답")
// Entity를 그대로 노출하지 않고 API 응답 전용 DTO로 변환한다.
public record ExampleResponse(
        @Schema(description = "식별자", example = "1")
        Long id,

        @Schema(description = "제목", example = "첫 번째 예제")
        String title,

        @Schema(description = "본문", example = "팀 컨벤션을 맞추기 위한 예제 데이터입니다.")
        String content,

        @Schema(description = "상태", example = "ACTIVE")
        String status,

        @Schema(description = "상태 라벨", example = "활성")
        String statusLabel,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
    public static ExampleResponse from(Example example) {
        // enum name과 사용자 표시용 label을 함께 내려 프론트/앱에서 선택적으로 사용할 수 있게 한다.
        return new ExampleResponse(
                example.getId(),
                example.getTitle(),
                example.getContent(),
                example.getStatus().name(),
                example.getStatus().getLabel(),
                example.getCreatedAt(),
                example.getUpdatedAt()
        );
    }
}
