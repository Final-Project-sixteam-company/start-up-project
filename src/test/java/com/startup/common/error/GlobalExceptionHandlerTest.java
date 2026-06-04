package com.startup.common.error;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void businessException_withoutDetails_doesNotRenderDetailsField() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/without-details"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.status").value(403))
                .andExpect(jsonPath("$.error.code").value("C004"))
                .andExpect(jsonPath("$.error.message").value(CommonErrorCode.ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.error.path").value("/without-details"))
                .andExpect(jsonPath("$.error.details").doesNotExist());
    }

    @Test
    void businessException_withDetails_rendersDetailsField() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/with-details"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C004"))
                .andExpect(jsonPath("$.error.details.activeSessionId").value(100L))
                .andExpect(jsonPath("$.error.details.scenarioId").doesNotExist())
                .andExpect(jsonPath("$.error.details.status").doesNotExist());
    }

    @Test
    void businessException_causeConstructors_preserveCause() {
        RuntimeException cause = new RuntimeException("root");

        BusinessException withDefaultMessage = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, cause);
        BusinessException withCustomMessage = new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "custom", cause);

        assertThat(withDefaultMessage).hasCause(cause);
        assertThat(withCustomMessage).hasCause(cause);
        assertThat(withDefaultMessage.getDetails()).isNull();
        assertThat(withCustomMessage.getDetails()).isNull();
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    private static class TestController {

        @GetMapping("/without-details")
        void withoutDetails() {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED);
        }

        @GetMapping("/with-details")
        void withDetails() {
            throw new BusinessException(
                    CommonErrorCode.ACCESS_DENIED,
                    CommonErrorCode.ACCESS_DENIED.getMessage(),
                    Map.of("activeSessionId", 100L)
            );
        }
    }
}
