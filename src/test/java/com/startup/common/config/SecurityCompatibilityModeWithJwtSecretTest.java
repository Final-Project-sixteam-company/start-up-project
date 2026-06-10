package com.startup.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clueroom.auth.require-authentication=false",
        "clueroom.auth.mock-fallback-enabled=true",
        "clueroom.auth.jwt.secret=12345678901234567890123456789012"
})
@AutoConfigureMockMvc
class SecurityCompatibilityModeWithJwtSecretTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compatibilityModeAllowsMissingBearerWhenJwtSecretIsConfigured() throws Exception {
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk());
    }

    @Test
    void compatibilityModeRejectsInvalidBearerWhenJwtSecretIsConfigured() throws Exception {
        mockMvc.perform(get("/api/scenarios")
                        .header("Authorization", "Bearer mock_jwt_token_here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUTH_003"));
    }
}
