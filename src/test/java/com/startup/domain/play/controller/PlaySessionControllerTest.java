package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.play.dto.PlayLocationsResponse;
import com.startup.domain.play.service.PlaySessionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getLocations_usesCurrentUserAndReturnsSuccessWrapper() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(playSessionService.getLocations(1L, 100L)).thenReturn(new PlayLocationsResponse(
                100L,
                10L,
                "서월채",
                "https://assets.example.com/map.png",
                List.of(new PlayLocationsResponse.LocationDto(
                        1L,
                        "LOC_BEDROOM",
                        "침실",
                        "2F",
                        "피해자가 발견된 방",
                        "official/seowolchae/v1/locations/LOC_BEDROOM.png",
                        "https://assets.example.com/official/seowolchae/v1/locations/LOC_BEDROOM.png",
                        120,
                        80,
                        2,
                        1
                ))
        ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(get("/api/play-sessions/{sessionId}/locations", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(100L))
                .andExpect(jsonPath("$.data.scenarioTitle").value("서월채"))
                .andExpect(jsonPath("$.data.mapImageUrl").value("https://assets.example.com/map.png"))
                .andExpect(jsonPath("$.data.locations[0].locationCode").value("LOC_BEDROOM"))
                .andExpect(jsonPath("$.data.locations[0].imageUrl")
                        .value("https://assets.example.com/official/seowolchae/v1/locations/LOC_BEDROOM.png"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).getLocations(1L, 100L);
    }
}
