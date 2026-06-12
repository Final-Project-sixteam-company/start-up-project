package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.service.ScenarioManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scenarios")
public class ScenarioManageController {

    private final ScenarioManageService scenarioManageService;
    private final MockUserProvider mockUserProvider;

    @GetMapping("/me")
    public ApiResponse<PageResponse<ScenarioSummaryResponse>> getMyScenarios(Pageable pageable) {
        // 인증된 사용자 ID 가져오기
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new com.startup.common.error.BusinessException(
                    com.startup.common.error.CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getMyScenarios(userId, pageable);
        return ApiResponse.success(response);
    }
}
