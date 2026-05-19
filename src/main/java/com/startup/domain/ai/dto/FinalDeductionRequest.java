package com.startup.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "최종 추리 제출 요청")
public record FinalDeductionRequest(
        @NotNull @Schema(description = "선택한 범인 용의자 ID", example = "1")
        Long selectedCulpritId,

        @NotBlank @Schema(description = "범행 동기 서술")
        String motiveText,

        @NotBlank @Schema(description = "범행 방법 서술")
        String methodText,

        @Schema(description = "은폐 방법 서술")
        String coverUpText,

        @NotNull
        @Size(min = 1, max = 15, message = "증거는 1~15개 선택해야 합니다")
        @Schema(description = "선택한 결정적 증거 ID 목록")
        List<@NotNull Long> selectedEvidenceIds
) {}
