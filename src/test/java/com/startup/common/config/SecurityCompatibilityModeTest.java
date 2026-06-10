package com.startup.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "clueroom.auth.require-authentication=false",
        "clueroom.auth.mock-fallback-enabled=true",
        "clueroom.auth.jwt.secret="
})
@AutoConfigureMockMvc
class SecurityCompatibilityModeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compatibilityModeIgnoresMockBearerWhenJwtSecretIsNotConfigured() throws Exception {
        mockMvc.perform(get("/api/scenarios")
                        .header("Authorization", "Bearer mock_jwt_token_here"))
                .andExpect(status().isOk());
    }
}
