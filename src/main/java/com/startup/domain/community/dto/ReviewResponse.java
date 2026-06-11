package com.startup.domain.community.dto;

import com.startup.domain.community.entity.ScenarioReview;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long userId,
        String nickname,
        String profileImageUrl,
        int rating,
        String content,
        boolean isSpoiler,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(ScenarioReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getUser().getProfileImageUrl(),
                review.getRating(),
                review.getContent(),
                review.isSpoiler(),
                review.getCreatedAt()
        );
    }
}
