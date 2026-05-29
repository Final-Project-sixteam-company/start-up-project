package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.play.service.PlaySessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaySessionControllerTest {

    @Test
    void abandonSession_usesCurrentUserAndReturnsSuccessWrapper() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(post("/api/play-sessions/{sessionId}/abandon", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).abandonSession(1L, 100L);
    }
}
