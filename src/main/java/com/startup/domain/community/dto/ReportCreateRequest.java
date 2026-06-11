package com.startup.domain.community.dto;

import com.startup.domain.community.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @NotNull(message = "신고 사유는 필수입니다.")
        ReportReason reason,

        @Size(max = 1000, message = "신고 상세 내용은 1000자를 넘을 수 없습니다.")
        String detail
) {
}
