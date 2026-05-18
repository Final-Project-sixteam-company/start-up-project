package com.startup.domain.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {
    FREE("자유 질문"),
    SUGGESTED("추천 질문"),
    EVIDENCE_PRESENTED("증거 제시");

    private final String label;
}
