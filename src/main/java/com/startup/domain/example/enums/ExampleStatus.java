package com.startup.domain.example.enums;

import com.startup.domain.example.error.ExampleErrorCode;
import com.startup.domain.example.error.ExampleException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
// enum은 DB 저장값(name)과 화면 표시값(label)을 분리한다.
public enum ExampleStatus {
    DRAFT("작성중"),
    ACTIVE("활성"),
    ARCHIVED("보관");

    private final String label;

    public static ExampleStatus from(String value) {
        // API 요청에서는 enum name과 한글 label을 모두 허용해 예제 사용성을 높인다.
        if (value == null || value.isBlank()) {
            throw new ExampleException(ExampleErrorCode.INVALID_EXAMPLE_STATUS);
        }

        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(value.trim())
                        || status.label.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new ExampleException(ExampleErrorCode.INVALID_EXAMPLE_STATUS));
    }
}
