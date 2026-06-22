package com.startup.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// 모든 API 응답의 최상위 포맷을 통일한다.
// 성공 응답은 data만, 실패 응답은 error만 내려가도록 null 필드는 제외한다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorResponse error
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> empty() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, ErrorResponse.builder()
                .code(code)
                .message(message)
                .build());
    }
}
