package com.startup.domain.community.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewCreateRequest(
        @NotNull(message = "별점은 필수입니다.")
        @Min(value = 1, message = "별점은 최소 1점이어야 합니다.")
        @Max(value = 5, message = "별점은 최대 5점이어야 합니다.")
        Integer rating,

        @Size(max = 1000, message = "리뷰 내용은 1000자를 넘을 수 없습니다.")
        String content,

        boolean isSpoiler
) {
}
