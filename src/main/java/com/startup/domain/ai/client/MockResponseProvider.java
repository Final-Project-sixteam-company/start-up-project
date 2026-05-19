package com.startup.domain.ai.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockResponseProvider {

    private static final String DEFAULT_FALLBACK = "지금은 정확히 답하기 어렵습니다. 다른 증거를 확인한 뒤 다시 질문해 주세요.";

    @Value("classpath:mock/interrogation_mock_responses.json")
    private Resource mockResource;

    private final ObjectMapper objectMapper;

    private List<MockEntry> entries = Collections.emptyList();

    @PostConstruct
    void init() {
        try {
            entries = objectMapper.readValue(
                    mockResource.getInputStream(),
                    new TypeReference<>() {});
        } catch (IOException e) {
            log.warn("Mock 응답 파일 로드 실패, Fallback만 사용합니다", e);
        }
    }

    public String getResponse(Long suspectId, boolean hasPresented) {
        return entries.stream()
                .filter(e -> e.suspectId().equals(suspectId) && e.hasPresented() == hasPresented)
                .findFirst()
                .map(MockEntry::answer)
                .orElse(DEFAULT_FALLBACK);
    }

    public String getFallbackResponse() {
        return DEFAULT_FALLBACK;
    }

    record MockEntry(Long suspectId, boolean hasPresented, String condition, String answer) {}
}
