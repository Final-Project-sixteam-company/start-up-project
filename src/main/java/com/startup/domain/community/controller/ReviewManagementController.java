package com.startup.domain.community.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.dto.ApiResponse;
import com.startup.domain.community.dto.ReviewResponse;
import com.startup.domain.community.dto.ReviewUpdateRequest;
import com.startup.domain.community.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewManagementController {

    private final ReviewService reviewService;
    private final MockUserProvider mockUserProvider;

    @Operation(summary = "시나리오 리뷰 수정")
    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request
    ) {
        Long userId = mockUserProvider.currentUserId();
        ReviewResponse response = reviewService.updateReview(userId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "시나리오 리뷰 삭제")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId
    ) {
        Long userId = mockUserProvider.currentUserId();
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.empty());
    }
}
