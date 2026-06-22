package com.startup.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

// GlobalExceptionHandler가 내려주는 표준 에러 바디다.
// 프론트/앱에서는 code를 기준으로 분기하고 message는 사용자 안내에 활용한다.
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final Map<String, Object> details;
}
