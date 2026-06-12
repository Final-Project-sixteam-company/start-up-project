package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.service.ScenarioManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getMyScenarios(userId, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/bookmarked")
    public ApiResponse<PageResponse<ScenarioSummaryResponse>> getBookmarkedScenarios(Pageable pageable) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getBookmarkedScenarios(userId, pageable);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{scenarioId}")
    public ApiResponse<Void> deleteScenario(@PathVariable Long scenarioId) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        scenarioManageService.deleteScenario(userId, scenarioId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{scenarioId}/hide")
    public ApiResponse<Void> hideScenario(@PathVariable Long scenarioId) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        scenarioManageService.hideScenario(userId, scenarioId);
        return ApiResponse.success(null);
    }
}
