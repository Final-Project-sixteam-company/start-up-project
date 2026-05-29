package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.dto.DeductionResultResponse;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringCriteria.KeywordCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.entity.FinalDeduction;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.FinalDeductionEvidenceRepository;
import com.startup.domain.ai.support.DeductionContextLoader;
import com.startup.domain.ai.support.FallbackFeedbackGenerator;
import com.startup.domain.ai.support.HintPenaltyReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.RuleBasedScorer;
import com.startup.domain.ai.support.ScoringCriteriaProvider;
import com.startup.domain.ai.support.SolutionReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiDeductionScorerTest {

    private MockUserProvider mockUserProvider;
    private DeductionContextLoader contextLoader;
    private PlaySessionReader playSessionReader;
    private SolutionReader solutionReader;
    private HintPenaltyReader hintPenaltyReader;
    private ScoringCriteriaProvider scoringCriteriaProvider;
    private RuleBasedScorer ruleBasedScorer;
    private FallbackFeedbackGenerator fallbackFeedbackGenerator;
    private AiPromptBuilder promptBuilder;
    private AiClient aiClient;
    private JsonMapper jsonMapper;
    private FinalDeductionEvidenceRepository finalDeductionEvidenceRepository;

    private AiDeductionScorer scorer;

    private static final Long SESSION_ID = 100L;
    private static final Long SCENARIO_ID = 1L;
    private static final Long OWNER_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;

    @BeforeEach
    void setUp() {
        mockUserProvider = mock(MockUserProvider.class);
        contextLoader = mock(DeductionContextLoader.class);
        playSessionReader = mock(PlaySessionReader.class);
        solutionReader = mock(SolutionReader.class);
        hintPenaltyReader = mock(HintPenaltyReader.class);
        scoringCriteriaProvider = mock(ScoringCriteriaProvider.class);
        ruleBasedScorer = mock(RuleBasedScorer.class);
        fallbackFeedbackGenerator = mock(FallbackFeedbackGenerator.class);
        promptBuilder = mock(AiPromptBuilder.class);
        aiClient = mock(AiClient.class);
        jsonMapper = JsonMapper.builder().build();
        finalDeductionEvidenceRepository = mock(FinalDeductionEvidenceRepository.class);

        scorer = new AiDeductionScorer(
                mockUserProvider,
                contextLoader,
                playSessionReader,
                solutionReader,
                hintPenaltyReader,
                scoringCriteriaProvider,
                ruleBasedScorer,
                fallbackFeedbackGenerator,
                promptBuilder,
                aiClient,
                jsonMapper,
                finalDeductionEvidenceRepository
        );
    }

    @Test
    @DisplayName("다른 소유자의 결과 조회 시 ACCESS_DENIED 예외")
    void getResult_withDifferentOwner_throwsAccessDenied() {
        when(mockUserProvider.currentUserId()).thenReturn(OTHER_USER_ID);
        when(playSessionReader.getOwnerUserId(SESSION_ID)).thenReturn(OWNER_USER_ID);

        assertThatThrownBy(() -> scorer.getResult(SESSION_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED);
                });
    }

    @Test
    @DisplayName("소유자 본인의 결과 조회 시 정상 반환")
    void getResult_withOwner_returnsResult() {
        stubOwnerMatch();
        stubFullResultChain();

        DeductionResultResponse result = scorer.getResult(SESSION_ID);

        assertThat(result.sessionId()).isEqualTo(SESSION_ID);
        assertThat(result.score()).isEqualTo(85);
        assertThat(result.grade()).isEqualTo("A");
    }

    @Test
    @DisplayName("keyEvidences가 SolutionInfo.keyEvidenceIds 기준으로 생성됨")
    void getResult_keyEvidences_useSolutionKeyEvidenceIds() {
        stubOwnerMatch();

        List<Long> criteriaKeys = List.of(2L, 6L, 7L);
        List<Long> solutionKeys = List.of(2L, 6L, 7L, 8L, 11L);
        Map<Long, String> titles = Map.of(
                2L, "증거A", 6L, "증거B", 7L, "증거C", 8L, "증거D", 11L, "증거E"
        );

        ScoringCriteria criteria = buildCriteria(criteriaKeys);
        SolutionInfo solution = buildSolution(solutionKeys, titles);

        stubResultChainWith(criteria, solution);

        DeductionResultResponse result = scorer.getResult(SESSION_ID);

        assertThat(result.keyEvidences()).hasSize(5);
        List<Long> returnedIds = result.keyEvidences().stream()
                .map(DeductionResultResponse.EvidenceDto::evidenceId)
                .toList();
        assertThat(returnedIds).containsExactlyElementsOf(solutionKeys);
    }

    @Test
    @DisplayName("MockSolution 기반 keyEvidences의 title이 모두 non-null")
    void getResult_keyEvidences_haveNonNullTitles_forMockSolution() {
        stubOwnerMatch();

        List<Long> keys = List.of(2L, 6L, 7L, 8L, 11L, 12L, 13L, 14L);
        Map<Long, String> titles = Map.of(
                2L, "알레르기 유발 음료 주문 기록",
                6L, "박재민 카드 결제 내역",
                7L, "에피펜 은닉 장소 지문",
                8L, "사라진 에피펜 처방 기록",
                11L, "박재민 사무실 자금 서류",
                12L, "이서연의 감사 보고서 초안",
                13L, "데모데이 전날 협박 문자",
                14L, "피해자 사후 발송된 메시지 로그"
        );

        ScoringCriteria criteria = buildCriteria(keys);
        SolutionInfo solution = buildSolution(keys, titles);

        stubResultChainWith(criteria, solution);

        DeductionResultResponse result = scorer.getResult(SESSION_ID);

        assertThat(result.keyEvidences()).allSatisfy(ev -> {
            assertThat(ev.title()).isNotNull();
            assertThat(ev.evidenceId()).isNotNull();
        });
    }

    private void stubOwnerMatch() {
        when(mockUserProvider.currentUserId()).thenReturn(OWNER_USER_ID);
        when(playSessionReader.getOwnerUserId(SESSION_ID)).thenReturn(OWNER_USER_ID);
    }

    private void stubFullResultChain() {
        List<Long> keys = List.of(2L, 6L, 7L);
        Map<Long, String> titles = Map.of(2L, "증거A", 6L, "증거B", 7L, "증거C");
        ScoringCriteria criteria = buildCriteria(keys);
        SolutionInfo solution = buildSolution(keys, titles);
        stubResultChainWith(criteria, solution);
    }

    private void stubResultChainWith(ScoringCriteria criteria, SolutionInfo solution) {
        FinalDeduction deduction = FinalDeduction.builder()
                .playSessionId(SESSION_ID)
                .selectedCulpritId(1L)
                .motiveText("자금 유용")
                .methodText("아몬드 에피펜")
                .coverUpText("메시지 위장")
                .score(85)
                .grade("A")
                .feedback("잘 추리했습니다.")
                .matchedParts("[\"범인\",\"동기\"]")
                .missedParts("[\"은폐\"]")
                .submittedAt(LocalDateTime.now())
                .build();

        when(contextLoader.findBySessionId(SESSION_ID)).thenReturn(deduction);
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(solutionReader.findByScenarioId(SCENARIO_ID)).thenReturn(solution);
        when(scoringCriteriaProvider.getByCriteria(SCENARIO_ID)).thenReturn(criteria);
        when(finalDeductionEvidenceRepository.findAllByFinalDeductionId(any()))
                .thenReturn(List.of());
        when(ruleBasedScorer.score(any(FinalDeductionRequest.class), any(ScoringCriteria.class)))
                .thenReturn(new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
    }

    private ScoringCriteria buildCriteria(List<Long> keyEvidenceIds) {
        return new ScoringCriteria(
                SCENARIO_ID, 1L,
                new KeywordCriteria(List.of("아몬드", "에피펜"), 2, 25),
                new KeywordCriteria(List.of("자금", "유용"), 2, 20),
                new KeywordCriteria(List.of("메시지", "위장"), 2, 10),
                keyEvidenceIds,
                15, 30
        );
    }

    private SolutionInfo buildSolution(List<Long> keyEvidenceIds, Map<Long, String> titles) {
        return new SolutionInfo(
                1L, "박재민", "CFO",
                "회사 자금 유용 폭로 방지",
                "견과류 알레르기 이용 + 에피펜 은닉",
                "피해자 휴대폰으로 메시지 위장",
                "박재민 CFO가 범인입니다. 데모데이 전날 자금 유용 사실이 폭로될 위기에 처하자...",
                keyEvidenceIds,
                titles
        );
    }
}
