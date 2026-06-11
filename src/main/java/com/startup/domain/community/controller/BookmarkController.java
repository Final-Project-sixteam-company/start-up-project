package com.startup.domain.community.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.community.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scenarios/{scenarioId}/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "시나리오 북마크 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        bookmarkService.addBookmark(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.empty());
    }

    @Operation(summary = "시나리오 북마크 취소")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            @PathVariable Long scenarioId
    ) {
        Long userId = mockUserProvider.currentUserId();
        bookmarkService.removeBookmark(userId, scenarioId);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
