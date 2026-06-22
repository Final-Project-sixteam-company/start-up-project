package com.startup.common.util;

import java.util.Arrays;
import java.util.List;

public class TextTokenizerUtil {
    
    /**
     * 구두점 및 공백을 기준으로 텍스트를 분리하고, 2글자 이상인 유효 토큰만 추출합니다.
     */
    public static List<String> extractValidTokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[\\s\\p{Punct}]+"))
                .filter(w -> w.length() >= 2)
                .toList();
    }
}
