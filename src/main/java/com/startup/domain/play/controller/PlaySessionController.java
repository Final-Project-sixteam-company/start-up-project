package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.play.dto.*;
import com.startup.domain.play.service.PlaySessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/play-sessions")
@RequiredArgsConstructor
public class PlaySessionController {

    private final PlaySessionService playSessionService;
    private final MockUserProvider mockUserProvider;

    //게임 세션 시작
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlaySessionCreateResponse> createSession(
            @Valid @RequestBody PlaySessionCreateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        PlaySessionCreateResponse response = playSessionService.createSession(userId, request);
        return ApiResponse.success(response);
    }

    //탐정 대시보드 조회
    @GetMapping("/{sessionId}/dashboard")
    public ApiResponse<DashboardResponse> getDashboard(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        DashboardResponse response = playSessionService.getDashboard(userId, sessionId);
        return ApiResponse.success(response);
    }

    //현재 해금된 증거 목록 조회
    @GetMapping("/{sessionId}/evidences")
    public ApiResponse<List<PlayEvidenceResponse>> getEvidences(
            @PathVariable Long sessionId,
            @RequestParam(required = false, defaultValue = "false") Boolean includeLocked
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlayEvidenceResponse> response = playSessionService.getEvidences(userId, sessionId, includeLocked);
        return ApiResponse.success(response);
    }

    //용의자 목록 조회
    @GetMapping("/{sessionId}/suspects")
    public ApiResponse<List<PlaySuspectResponse>> getSuspects(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlaySuspectResponse> response = playSessionService.getSuspects(userId, sessionId);
        return ApiResponse.success(response);
    }

    //사용 가능 힌트 목록 조회 - isAvailable=true면 해금 가능, isUsed=true면 이미 열람한 힌트 (content 포함)
    @GetMapping("/{sessionId}/hints")
    public ApiResponse<List<PlayHintResponse>> getHints(
            @PathVariable Long sessionId
    ) {
        Long userId = mockUserProvider.currentUserId();
        List<PlayHintResponse> response = playSessionService.getHints(userId, sessionId);
        return ApiResponse.success(response);
    }
}
