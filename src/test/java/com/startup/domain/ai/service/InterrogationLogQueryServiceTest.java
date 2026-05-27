package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.InterrogationLogResponse;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.SuspectReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterrogationLogQueryServiceTest {

    private MockUserProvider mockUserProvider;
    private PlaySessionReader playSessionReader;
    private InterrogationLogRepository interrogationLogRepository;
    private SuspectReader suspectReader;
    private EvidenceReader evidenceReader;

    private InterrogationLogQueryService queryService;

    private static final Long SESSION_ID = 100L;
    private static final Long OWNER_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long SUSPECT_ID_1 = 1L;
    private static final Long SUSPECT_ID_2 = 2L;
    private static final Long EVIDENCE_ID = 3L;

    @BeforeEach
    void setUp() {
        mockUserProvider = mock(MockUserProvider.class);
        playSessionReader = mock(PlaySessionReader.class);
        interrogationLogRepository = mock(InterrogationLogRepository.class);
        suspectReader = mock(SuspectReader.class);
        evidenceReader = mock(EvidenceReader.class);

        queryService = new InterrogationLogQueryService(
                mockUserProvider,
                playSessionReader,
                interrogationLogRepository,
                suspectReader,
                evidenceReader
        );
    }

    @Test
    @DisplayName("세션 전체 심문 로그를 createdAt ASC 순으로 반환한다")
    void list_returnsAllLogsForSession_orderedByCreatedAtAsc() {
        stubOwnerMatch();

        LocalDateTime t1 = LocalDateTime.of(2026, 5, 22, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 5, 22, 10, 5);
        InterrogationLog log1 = buildLog(1L, SUSPECT_ID_1, null, QuestionType.FREE, t1);
        InterrogationLog log2 = buildLog(2L, SUSPECT_ID_2, EVIDENCE_ID, QuestionType.EVIDENCE_PRESENTED, t2);

        when(interrogationLogRepository.findByPlaySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(log1, log2));
        when(suspectReader.findById(SUSPECT_ID_1)).thenReturn(suspectProfile(SUSPECT_ID_1, "박지훈"));
        when(suspectReader.findById(SUSPECT_ID_2)).thenReturn(suspectProfile(SUSPECT_ID_2, "김서연"));
        when(evidenceReader.findById(EVIDENCE_ID)).thenReturn(new EvidenceInfo(EVIDENCE_ID, "독극물 보고서", "설명"));

        List<InterrogationLogResponse> result = queryService.list(SESSION_ID, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).interrogationId()).isEqualTo(1L);
        assertThat(result.get(0).suspectName()).isEqualTo("박지훈");
        assertThat(result.get(0).presentedEvidence()).isNull();
        assertThat(result.get(1).interrogationId()).isEqualTo(2L);
        assertThat(result.get(1).suspectName()).isEqualTo("김서연");
        assertThat(result.get(1).presentedEvidence()).isNotNull();
        assertThat(result.get(1).presentedEvidence().evidenceId()).isEqualTo(EVIDENCE_ID);
        assertThat(result.get(1).presentedEvidence().title()).isEqualTo("독극물 보고서");
    }

    @Test
    @DisplayName("suspectId 필터 시 해당 용의자 로그만 반환한다")
    void list_withSuspectId_filtersBySuspect() {
        stubOwnerMatch();

        InterrogationLog log = buildLog(1L, SUSPECT_ID_1, null, QuestionType.RECOMMENDED,
                LocalDateTime.of(2026, 5, 22, 10, 0));

        when(interrogationLogRepository.findByPlaySessionIdAndSuspectIdOrderByCreatedAtAsc(SESSION_ID, SUSPECT_ID_1))
                .thenReturn(List.of(log));
        when(suspectReader.findById(SUSPECT_ID_1)).thenReturn(suspectProfile(SUSPECT_ID_1, "박지훈"));

        List<InterrogationLogResponse> result = queryService.list(SESSION_ID, SUSPECT_ID_1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).suspectId()).isEqualTo(SUSPECT_ID_1);
    }

    @Test
    @DisplayName("다른 소유자 접근 시 ACCESS_DENIED 예외, repository 조회 없음")
    void list_withDifferentOwner_throwsAccessDenied_andDoesNotQueryLogs() {
        when(mockUserProvider.currentUserId()).thenReturn(OTHER_USER_ID);
        when(playSessionReader.getOwnerUserId(SESSION_ID)).thenReturn(OWNER_USER_ID);

        assertThatThrownBy(() -> queryService.list(SESSION_ID, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED);
                });

        verifyNoInteractions(interrogationLogRepository);
    }

    @Test
    @DisplayName("증거 조회 miss 시 title null로 graceful 반환 (NPE 없음)")
    void list_presentedEvidenceMiss_returnsNullTitle() {
        stubOwnerMatch();

        Long missingEvidenceId = 999L;
        InterrogationLog log = buildLog(1L, SUSPECT_ID_1, missingEvidenceId, QuestionType.EVIDENCE_PRESENTED,
                LocalDateTime.of(2026, 5, 22, 10, 0));

        when(interrogationLogRepository.findByPlaySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of(log));
        when(suspectReader.findById(SUSPECT_ID_1)).thenReturn(suspectProfile(SUSPECT_ID_1, "박지훈"));
        when(evidenceReader.findById(missingEvidenceId)).thenReturn(null);

        List<InterrogationLogResponse> result = queryService.list(SESSION_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).presentedEvidence()).isNotNull();
        assertThat(result.get(0).presentedEvidence().evidenceId()).isEqualTo(missingEvidenceId);
        assertThat(result.get(0).presentedEvidence().title()).isNull();
    }

    @Test
    @DisplayName("심문 로그가 없으면 빈 리스트 반환")
    void list_emptyLogs_returnsEmptyList() {
        stubOwnerMatch();

        when(interrogationLogRepository.findByPlaySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .thenReturn(List.of());

        List<InterrogationLogResponse> result = queryService.list(SESSION_ID, null);

        assertThat(result).isEmpty();
    }

    private void stubOwnerMatch() {
        when(mockUserProvider.currentUserId()).thenReturn(OWNER_USER_ID);
        when(playSessionReader.getOwnerUserId(SESSION_ID)).thenReturn(OWNER_USER_ID);
    }

    private SuspectProfile suspectProfile(Long id, String name) {
        return new SuspectProfile(id, name, "역할", "관계", "프로필", "진술", "알리바이");
    }

    private InterrogationLog buildLog(Long id, Long suspectId, Long evidenceId,
                                      QuestionType type, LocalDateTime createdAt) {
        InterrogationLog log = InterrogationLog.builder()
                .playSessionId(SESSION_ID)
                .suspectId(suspectId)
                .presentedEvidenceId(evidenceId)
                .questionType(type)
                .question("질문입니다")
                .answer("답변입니다")
                .aiModel("MOCK")
                .build();

        try {
            java.lang.reflect.Field idField = InterrogationLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(log, id);

            java.lang.reflect.Field createdAtField = InterrogationLog.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(log, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return log;
    }
}
