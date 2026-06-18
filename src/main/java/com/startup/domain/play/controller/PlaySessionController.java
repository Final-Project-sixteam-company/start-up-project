package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.PageResponse;
import com.startup.domain.play.dto.*;
import com.startup.domain.play.service.PlaySessionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

    @Operation(summary = "진행 중인 플레이 세션 조회")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ActivePlaySessionResponse>> getActiveSession(
            @RequestParam Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        ActivePlaySessionResponse response = playSessionService.getActiveSession(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 플레이 기록 목록 조회")
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<PageResponse<PlaySessionRecordResponse>>> getMyRecords(
            Pageable pageable
    ) {
        Long userId = mockUserProvider.currentUserId();
        PageResponse<PlaySessionRecordResponse> response = playSessionService.getMyRecords(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "플레이 세션 상세 조회 (기본 정보 및 타이머 동기화)")
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<PlaySessionDetailResponse>> getSessionDetail(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlaySessionDetailResponse response = playSessionService.getSessionDetail(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
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

    @Operation(summary = "용의자 상세 조회")
    @GetMapping("/{sessionId}/suspects/{suspectId}")
    public ResponseEntity<ApiResponse<PlaySuspectDetailResponse>> getSuspectDetail(
            @PathVariable Long sessionId,
            @PathVariable Long suspectId
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlaySuspectDetailResponse response = playSessionService.getSuspectDetail(userId, sessionId, suspectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "타임라인 조회")
    @GetMapping("/{sessionId}/timeline")
    public ResponseEntity<ApiResponse<java.util.List<PlayTimelineResponse>>> getTimeline(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        java.util.List<PlayTimelineResponse> response = playSessionService.getTimeline(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "증거 단건 상세 조회 (스포일러 방지 적용)")
    @GetMapping("/{sessionId}/evidences/{evidenceId}")
    public ResponseEntity<ApiResponse<PlayEvidenceDetailResponse>> getEvidenceDetail(
            @PathVariable Long sessionId,
            @PathVariable Long evidenceId
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlayEvidenceDetailResponse response = playSessionService.getEvidenceDetail(userId, sessionId, evidenceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "증거 수동/조건 해금")
    @PostMapping("/{sessionId}/evidences/{evidenceId}/unlock")
    public ResponseEntity<ApiResponse<EvidenceUnlockResponse>> unlockEvidence(
            @PathVariable Long sessionId,
            @PathVariable Long evidenceId,
            @RequestBody(required = false) EvidenceUnlockRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        EvidenceUnlockResponse response = playSessionService.unlockEvidence(userId, sessionId, evidenceId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "현장/장소 정보 조회")
    @GetMapping("/{sessionId}/locations")
    public ResponseEntity<ApiResponse<PlayLocationsResponse>> getLocations(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlayLocationsResponse response = playSessionService.getLocations(userId, sessionId);
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

    @Operation(summary = "게임 포기/중단")
    @PostMapping("/{sessionId}/abandon")
    public ResponseEntity<ApiResponse<Void>> abandonSession(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        playSessionService.abandonSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
