package com.startup.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotBlank(message = "신고 사유는 필수입니다.")
        @Size(max = 255, message = "신고 사유는 255자를 넘을 수 없습니다.")
        String reason,

        @Size(max = 1000, message = "신고 상세 내용은 1000자를 넘을 수 없습니다.")
        String detail
) {
}
