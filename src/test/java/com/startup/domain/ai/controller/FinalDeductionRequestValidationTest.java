package com.startup.domain.ai.controller;

import com.startup.common.error.GlobalExceptionHandler;
import com.startup.domain.ai.service.AiDeductionScorer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("QA-12: 최종 추리 텍스트 길이 제한")
class FinalDeductionRequestValidationTest {

    private AiDeductionScorer scorer;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        scorer = mock(AiDeductionScorer.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiDeductionController(scorer))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("motiveText 1000자 초과 -> 400, 채점 미호출")
    void motiveTooLong() throws Exception {
        String tooLong = "a".repeat(1001);
        mockMvc.perform(post("/api/play-sessions/1/final-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedCulpritId\":1,\"motiveText\":\"" + tooLong + "\","
                                + "\"methodText\":\"m\",\"coverUpText\":\"c\",\"selectedEvidenceIds\":[1]}"))
                .andExpect(status().isBadRequest());

        verify(scorer, never()).submitAndScore(anyLong(), any());
    }

    @Test
    @DisplayName("coverUpText 1000자 초과 -> 400")
    void coverUpTooLong() throws Exception {
        String tooLong = "a".repeat(1001);
        mockMvc.perform(post("/api/play-sessions/1/final-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedCulpritId\":1,\"motiveText\":\"m\",\"methodText\":\"x\","
                                + "\"coverUpText\":\"" + tooLong + "\",\"selectedEvidenceIds\":[1]}"))
                .andExpect(status().isBadRequest());

        verify(scorer, never()).submitAndScore(anyLong(), any());
    }

    @Test
    @DisplayName("1000자 이하 텍스트는 정상 통과")
    void withinLimitPasses() throws Exception {
        String okText = "a".repeat(1000);
        mockMvc.perform(post("/api/play-sessions/1/final-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedCulpritId\":1,\"motiveText\":\"" + okText + "\","
                                + "\"methodText\":\"m\",\"coverUpText\":\"c\",\"selectedEvidenceIds\":[1]}"))
                .andExpect(status().isOk());

        verify(scorer).submitAndScore(anyLong(), any());
    }
}
