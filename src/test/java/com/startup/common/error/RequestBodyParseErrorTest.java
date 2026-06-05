package com.startup.common.error;

import com.startup.domain.ai.controller.AiDeductionController;
import com.startup.domain.ai.controller.AiInterrogationController;
import com.startup.domain.ai.service.AiDeductionScorer;
import com.startup.domain.ai.service.AiInterrogationService;
import com.startup.domain.ai.service.InterrogationLogQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("QA-2: 잘못된 요청 본문(파싱/타입/enum)은 500이 아니라 400으로 매핑된다")
class RequestBodyParseErrorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AiInterrogationController interrogationController = new AiInterrogationController(
                mock(AiInterrogationService.class), mock(InterrogationLogQueryService.class));
        AiDeductionController deductionController = new AiDeductionController(mock(AiDeductionScorer.class));
        mockMvc = MockMvcBuilders
                .standaloneSetup(interrogationController, deductionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("malformed JSON -> 400 C001")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    @DisplayName("invalid questionType enum -> 400")
    void invalidEnum() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/interrogations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"suspectId\":1,\"questionType\":\"HACK\",\"question\":\"q\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }

    @Test
    @DisplayName("selectedEvidenceIds가 배열이 아닌 숫자 -> 400")
    void wrongTypeBody() throws Exception {
        mockMvc.perform(post("/api/play-sessions/1/final-deduction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedCulpritId\":1,\"motiveText\":\"m\",\"methodText\":\"x\","
                                + "\"coverUpText\":\"c\",\"selectedEvidenceIds\":261}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.status").value(400))
                .andExpect(jsonPath("$.error.code").value("C001"));
    }
}
