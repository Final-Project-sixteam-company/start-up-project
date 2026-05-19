package com.startup.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// total count가 필요한 목록 응답에 사용한다.
// Spring Page의 0-base page number는 API 응답에서 1-base로 변환한다.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalPages,
        long totalElements,
        boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> springPage) {
        return new PageResponse<>(
                springPage.getContent(),
                springPage.getNumber() + 1,
                springPage.getSize(),
                springPage.getTotalPages(),
                springPage.getTotalElements(),
                springPage.hasNext()
        );
    }
}
