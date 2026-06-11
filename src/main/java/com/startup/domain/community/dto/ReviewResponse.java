package com.startup.domain.community.dto;

import com.startup.domain.community.entity.ScenarioReview;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        ReviewUserResponse user,
        int rating,
        String content,
        boolean isSpoiler,
        LocalDateTime createdAt
) {
    public record ReviewUserResponse(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {}

    public static ReviewResponse from(ScenarioReview review) {
        return new ReviewResponse(
                review.getId(),
                new ReviewUserResponse(
                        review.getUser().getId(),
                        review.getUser().getNickname(),
                        review.getUser().getProfileImageUrl()
                ),
                review.getRating(),
                review.getContent(),
                review.isSpoiler(),
                review.getCreatedAt()
        );
    }
}
