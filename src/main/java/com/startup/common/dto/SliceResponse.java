package com.startup.common.dto;

import org.springframework.data.domain.Slice;

import java.util.List;

// total count 없이 다음 페이지 존재 여부만 필요한 목록 응답에 사용한다.
// 대용량 목록에서는 PageResponse보다 SliceResponse가 DB count 쿼리를 줄이기 쉽다.
public record SliceResponse<T>(
        List<T> content,
        boolean hasNext,
        int size
) {
    public static <T> SliceResponse<T> of(List<T> content, boolean hasNext, int size) {
        return new SliceResponse<>(content, hasNext, size);
    }

    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return new SliceResponse<>(slice.getContent(), slice.hasNext(), slice.getSize());
    }
}
