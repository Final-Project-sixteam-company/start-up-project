package com.startup.common.dto;

import java.util.List;
import java.util.function.Function;

// cursor 기반 무한 스크롤 응답이다.
// repository에서는 size + 1개를 조회하고, 여기서 초과 row를 잘라 hasNext와 nextCursor를 계산한다.
public record CursorSliceResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    public static <T> CursorSliceResponse<T> of(List<T> rows, int size, Function<T, Long> cursorExtractor) {
        boolean hasNext = rows.size() > size;
        List<T> content = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = content.isEmpty() || !hasNext
                ? null
                : cursorExtractor.apply(content.get(content.size() - 1));

        return new CursorSliceResponse<>(content, nextCursor, hasNext, size);
    }
}
