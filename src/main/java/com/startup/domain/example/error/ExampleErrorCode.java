package com.startup.domain.example.error;

import com.startup.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
// E 계열 코드는 Example 도메인 전용 오류다. 실제 도메인은 각자 prefix를 분리해도 된다.
public enum ExampleErrorCode implements ErrorCode {
    EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "Example not found."),
    DUPLICATED_EXAMPLE_TITLE(HttpStatus.CONFLICT, "E002", "Example title already exists."),
    INVALID_EXAMPLE_STATUS(HttpStatus.BAD_REQUEST, "E003", "Invalid example status.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
