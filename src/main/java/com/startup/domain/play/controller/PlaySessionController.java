package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.play.dto.*;
import com.startup.domain.play.service.PlaySessionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/play-sessions")
@RequiredArgsConstructor
public class PlaySessionController {

    private final PlaySessionService playSessionService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "playSession 생성 - 게임 세션 시작")
    @PostMapping
    public ResponseEntity<ApiResponse<PlaySessionCreateResponse>> createSession(
            @Valid @RequestBody PlaySessionCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlaySessionCreateResponse response = playSessionService.createSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "탐정 대시보드 조회")
    @GetMapping("/{sessionId}/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        DashboardResponse response = playSessionService.getDashboard(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "현재 해금된 증거 목록 조회")
    @GetMapping("/{sessionId}/evidences")
    public ResponseEntity<ApiResponse<List<PlayEvidenceResponse>>> getEvidences(
            @PathVariable Long sessionId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeLocked,
            @RequestParam(required = false) String status
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlayEvidenceResponse> response = playSessionService.getEvidences(userId, sessionId, includeLocked, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "용의자 목록 조회")
    @GetMapping("/{sessionId}/suspects")
    public ResponseEntity<ApiResponse<List<PlaySuspectResponse>>> getSuspects(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlaySuspectResponse> response = playSessionService.getSuspects(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "사용 가능 힌트 목록 조회")
    @GetMapping("/{sessionId}/hints")
    public ResponseEntity<ApiResponse<List<PlayHintResponse>>> getHints(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlayHintResponse> response = playSessionService.getHints(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게임 세션 포기")
    @PostMapping("/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<Void>> abandonSession(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        playSessionService.abandonSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "힌트 사용 및 열람")
    @PostMapping("/{sessionId}/hints/{hintId}/use")
    public ResponseEntity<ApiResponse<HintUseResponse>> useHint(
            @PathVariable Long sessionId,
            @PathVariable Long hintId
    ) {
        Long userId = mockUserProvider.currentUserId();
        HintUseResponse response = playSessionService.useHint(userId, sessionId, hintId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
