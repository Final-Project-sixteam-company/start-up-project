package com.startup.domain.play.controller;

import com.startup.common.auth.MockUserProvider;
import com.startup.domain.play.dto.ActivePlaySessionResponse;
import com.startup.domain.play.dto.EvidenceUnlockResponse;
import com.startup.domain.play.dto.PlayEvidenceDetailResponse;
import com.startup.domain.play.dto.PlayLocationsResponse;
import com.startup.domain.play.dto.PlaySuspectDetailResponse;
import com.startup.domain.play.enums.PlaySessionStatus;
import com.startup.domain.play.service.PlaySessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
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
    void getActiveSession_usesCurrentUserAndReturnsSuccessWrapper() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(playSessionService.getActiveSession(1L, 10L))
                .thenReturn(new ActivePlaySessionResponse(
                        true,
                        100L,
                        10L,
                        PlaySessionStatus.PLAYING,
                        LocalDateTime.parse("2026-06-04T12:00:00")
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(get("/api/play-sessions/active")
                        .param("scenarioId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.hasActiveSession").value(true))
                .andExpect(jsonPath("$.data.activeSessionId").value(100L))
                .andExpect(jsonPath("$.data.scenarioId").value(10L))
                .andExpect(jsonPath("$.data.status").value("PLAYING"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).getActiveSession(1L, 10L);
    }

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

    @Test
    void unlockEvidence_usesCurrentUserAndReturnsSuccessWrapper() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(playSessionService.unlockEvidence(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(200L),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new EvidenceUnlockResponse(200L, true, LocalDateTime.parse("2026-06-04T12:00:00")));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(post("/api/play-sessions/{sessionId}/evidences/{evidenceId}/unlock", 100L, 200L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.evidenceId").value(200L))
                .andExpect(jsonPath("$.data.isUnlocked").value(true))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).unlockEvidence(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(200L),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getEvidenceDetail_omitsEvidenceCodeForLockedGuidanceCompareEvidence() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(playSessionService.getEvidenceDetail(1L, 100L, 200L))
                .thenReturn(PlayEvidenceDetailResponse.builder()
                        .evidenceId(200L)
                        .title("현재 증거")
                        .description("현재 증거 상세")
                        .importance(null)
                        .guidance(new PlayEvidenceDetailResponse.EvidenceGuidanceInfo(
                                List.of("시간대를 비교한다."),
                                List.of(
                                        new PlayEvidenceDetailResponse.CompareEvidenceInfo(
                                                201L,
                                                "EVIDENCE_UNLOCKED",
                                                "해금 증거",
                                                true,
                                                null
                                        ),
                                        new PlayEvidenceDetailResponse.CompareEvidenceInfo(
                                                202L,
                                                null,
                                                "잠긴 증거",
                                                false,
                                                "조사 단계 진행 시 공개"
                                        )
                                ),
                                List.of()
                        ))
                        .build());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(get("/api/play-sessions/{sessionId}/evidences/{evidenceId}", 100L, 200L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.guidance.compareEvidences[0].evidenceCode")
                        .value("EVIDENCE_UNLOCKED"))
                .andExpect(jsonPath("$.data.guidance.compareEvidences[1].evidenceCode").doesNotExist())
                .andExpect(jsonPath("$.data.guidance.compareEvidences[1].title").value("잠긴 증거"))
                .andExpect(jsonPath("$.data.guidance.compareEvidences[1].isUnlocked").value(false))
                .andExpect(jsonPath("$.data.guidance.compareEvidences[1].unlockHint")
                        .value("조사 단계 진행 시 공개"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).getEvidenceDetail(1L, 100L, 200L);
    }

    @Test
    void getSuspectDetail_usesCurrentUserAndReturnsSuccessWrapper() throws Exception {
        PlaySessionService playSessionService = mock(PlaySessionService.class);
        MockUserProvider mockUserProvider = mock(MockUserProvider.class);
        when(mockUserProvider.currentUserId()).thenReturn(1L);
        when(playSessionService.getSuspectDetail(1L, 100L, 300L))
                .thenReturn(new PlaySuspectDetailResponse(
                        300L,
                        "한지오",
                        "비서실장",
                        "측근",
                        "이사장의 일정을 관리한다.",
                        "회의실에 있었다고 주장한다.",
                        "20시 이후 1층에 있었다고 말한다.",
                        "https://assets.example.com/official/seowolchae/v1/characters/SUSPECT_SECRETARY.png",
                        70,
                        List.of(new PlaySuspectDetailResponse.RelatedEvidenceDto(200L, "해금 증거", true)),
                        List.of()
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaySessionController(playSessionService, mockUserProvider))
                .build();

        mockMvc.perform(get("/api/play-sessions/{sessionId}/suspects/{suspectId}", 100L, 300L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.suspectId").value(300L))
                .andExpect(jsonPath("$.data.portraitImageUrl")
                        .value("https://assets.example.com/official/seowolchae/v1/characters/SUSPECT_SECRETARY.png"))
                .andExpect(jsonPath("$.data.relatedEvidences[0].evidenceId").value(200L))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(playSessionService).getSuspectDetail(1L, 100L, 300L);
    }
}
