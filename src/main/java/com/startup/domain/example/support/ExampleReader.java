package com.startup.domain.example.support;

import com.startup.domain.example.entity.Example;
import com.startup.domain.example.error.ExampleErrorCode;
import com.startup.domain.example.error.ExampleException;
import com.startup.domain.example.repository.ExampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 여러 서비스에서 재사용할 수 있는 조회 + not found 예외 변환 전용 컴포넌트다.
public class ExampleReader {

    private final ExampleRepository exampleRepository;

    public Example findById(Long exampleId) {
        return exampleRepository.findById(exampleId)
                .orElseThrow(() -> new ExampleException(ExampleErrorCode.EXAMPLE_NOT_FOUND));
    }
}
