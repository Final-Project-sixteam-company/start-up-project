package com.startup.domain.ai.controller;

import com.startup.common.error.GlobalExceptionHandler;
import com.startup.domain.ai.service.AiInterrogationService;
import com.startup.domain.ai.service.InterrogationLogQueryService;
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

@DisplayName("심문 요청 검증 (QA-3, QA-12 질문 길이)")
class InterrogationRequestValidationTest {

    private AiInterrogationService interrogationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        interrogationService = mock(AiInterrogationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiInterrogationController(
                        interrogationService, mock(InterrogationLogQueryService.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("QA-3: EVIDENCE_PRESENTED인데 presentedEvidenceId=null -> 400, AI 미호출")
    void evidencePresentedWithoutId() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspectId\":1,\"questionType\":\"EVIDENCE_PRESENTED\","
                                + "\"question\":\"증거 질문\",\"presentedEvidenceId\":null}"))
                .andExpect(status().isBadRequest());

        verify(interrogationService, never()).interrogate(anyLong(), any());
    }

    @Test
    @DisplayName("EVIDENCE_PRESENTED + presentedEvidenceId 있으면 통과(AI 호출)")
    void evidencePresentedWithId() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspectId\":1,\"questionType\":\"EVIDENCE_PRESENTED\","
                                + "\"question\":\"증거 질문\",\"presentedEvidenceId\":3}"))
                .andExpect(status().isOk());

        verify(interrogationService).interrogate(anyLong(), any());
    }

    @Test
    @DisplayName("FREE + presentedEvidenceId=null은 정상 통과")
    void freeWithoutEvidence() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspectId\":1,\"questionType\":\"FREE\",\"question\":\"자유 질문\"}"))
                .andExpect(status().isOk());

        verify(interrogationService).interrogate(anyLong(), any());
    }

    @Test
    @DisplayName("QA-12: 질문 500자 초과 -> 400")
    void questionTooLong() throws Exception {
        String longQuestion = "a".repeat(501);
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspectId\":1,\"questionType\":\"FREE\",\"question\":\"" + longQuestion + "\"}"))
                .andExpect(status().isBadRequest());

        verify(interrogationService, never()).interrogate(anyLong(), any());
    }
}
