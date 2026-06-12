package com.startup.domain.scenario.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.ScenarioDeleteResponse;
import com.startup.domain.scenario.dto.ScenarioHideResponse;
import com.startup.domain.scenario.dto.ScenarioSummaryResponse;
import com.startup.domain.scenario.service.ScenarioManageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scenarios")
public class ScenarioManageController {

    private final ScenarioManageService scenarioManageService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "내가 작성한 시나리오 목록 조회")
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

    @Operation(summary = "내가 북마크한 시나리오 목록 조회")
    @GetMapping("/bookmarked")
    public ApiResponse<PageResponse<ScenarioSummaryResponse>> getBookmarkedScenarios(Pageable pageable) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        PageResponse<ScenarioSummaryResponse> response = scenarioManageService.getBookmarkedScenarios(userId, pageable);
        return ApiResponse.success(response);
    }

    @Operation(summary = "시나리오 삭제")
    @DeleteMapping("/{scenarioId}")
    public ApiResponse<ScenarioDeleteResponse> deleteScenario(@PathVariable Long scenarioId) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        ScenarioDeleteResponse response = scenarioManageService.deleteScenario(userId, scenarioId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "시나리오 숨김")
    @PostMapping("/{scenarioId}/hide")
    public ApiResponse<ScenarioHideResponse> hideScenario(@PathVariable Long scenarioId) {
        Long userId = mockUserProvider.currentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "인증이 필요합니다.");
        }

        ScenarioHideResponse response = scenarioManageService.hideScenario(userId, scenarioId);
        return ApiResponse.success(response);
    }
}
