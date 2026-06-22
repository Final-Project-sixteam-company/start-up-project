package com.startup.domain.community.dto;

import com.startup.domain.community.enums.ReportStatus;

public record ReportStatusResponse(
        Long reportId,
        ReportStatus status
) {
}
