package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.client.AiCallContext;
import com.startup.domain.ai.client.AiClient;
import com.startup.domain.ai.client.AiRequestParams;
import com.startup.domain.ai.dto.AiFeedbackResult;
import com.startup.domain.ai.dto.DeductionResultResponse;
import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.FinalDeductionResponse;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.entity.FinalDeduction;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.prompt.AiPromptBuilder;
import com.startup.domain.ai.repository.FinalDeductionEvidenceRepository;
import com.startup.domain.ai.support.DeductionScoringDefaults;
import com.startup.domain.ai.support.DeductionContextLoader;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.ai.support.FallbackFeedbackGenerator;
import com.startup.domain.ai.support.HintPenaltyReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.RuleBasedScorer;
import com.startup.domain.ai.support.SolutionReader;
import com.startup.domain.ai.support.SuspectReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiDeductionScorerTest {

    private MockUserProvider mockUserProvider;
    private DeductionContextLoader contextLoader;
    private PlaySessionReader playSessionReader;
    private EvidenceReader evidenceReader;
    private SuspectReader suspectReader;
    private SolutionReader solutionReader;
    private HintPenaltyReader hintPenaltyReader;
    private DeductionScoringDefaults scoringDefaults;
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
        evidenceReader = mock(EvidenceReader.class);
        suspectReader = mock(SuspectReader.class);
        solutionReader = mock(SolutionReader.class);
        hintPenaltyReader = mock(HintPenaltyReader.class);
        scoringDefaults = new DeductionScoringDefaults(30, 15, 25, 20, 10);
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
                evidenceReader,
                suspectReader,
                solutionReader,
                hintPenaltyReader,
                scoringDefaults,
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

        List<Long> solutionKeys = List.of(2L, 6L, 7L, 8L, 11L);
        Map<Long, String> titles = Map.of(
                2L, "증거A", 6L, "증거B", 7L, "증거C", 8L, "증거D", 11L, "증거E"
        );

        SolutionInfo solution = buildSolution(solutionKeys, titles);

        stubResultChainWith(solution);

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

        SolutionInfo solution = buildSolution(keys, titles);

        stubResultChainWith(solution);

        DeductionResultResponse result = scorer.getResult(SESSION_ID);

        assertThat(result.keyEvidences()).allSatisfy(ev -> {
            assertThat(ev.title()).isNotNull();
            assertThat(ev.evidenceId()).isNotNull();
        });
    }

    @Test
    @DisplayName("submitAndScore rejects selected evidence that is not unlocked")
    void submitAndScore_withLockedKeyEvidence_rejected() {
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L, 6L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getErrorCode())
                        .isEqualTo(AiErrorCode.FINAL_DEDUCTION_EVIDENCE_NOT_UNLOCKED));

        verify(ruleBasedScorer, never()).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore rejects culprit outside current scenario")
    void submitAndScore_withCulpritFromOtherScenario_rejected() {
        stubOwnerMatch();
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(suspectReader.findByIdAndScenarioId(999L, SCENARIO_ID))
                .thenThrow(new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));

        FinalDeductionRequest request = buildSubmitRequest(999L, List.of(2L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getErrorCode())
                        .isEqualTo(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));

        verify(evidenceReader, never()).getUnlockedEvidenceIds(SESSION_ID);
        verify(ruleBasedScorer, never()).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore accepts initial public unlocked evidence")
    void submitAndScore_withInitialPublicEvidence_accepted() {
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L));

        FinalDeductionResponse response = scorer.submitAndScore(SESSION_ID, request);

        assertThat(response.resultAvailable()).isTrue();
        assertThat(response.score()).isEqualTo(85);
        verify(ruleBasedScorer, times(1)).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore syncs time unlocks before validation")
    void submitAndScore_callsSyncTimeUnlocks_beforeValidation() {
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L));

        scorer.submitAndScore(SESSION_ID, request);

        InOrder inOrder = inOrder(contextLoader, evidenceReader, suspectReader, ruleBasedScorer);
        inOrder.verify(contextLoader).ensureNotSubmitted(SESSION_ID);
        inOrder.verify(evidenceReader).syncTimeUnlocks(SESSION_ID, OWNER_USER_ID);
        inOrder.verify(suspectReader).findByIdAndScenarioId(1L, SCENARIO_ID);
        inOrder.verify(evidenceReader).getUnlockedEvidenceIds(SESSION_ID);
        inOrder.verify(ruleBasedScorer).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore scores normally when all selected evidence is unlocked")
    void submitAndScore_withAllUnlockedEvidence_scoresNormally() {
        stubSuccessfulSubmitChain(List.of(2L, 6L), new ScoringResult(90, 30, true, 25, 20, 10, 5, 1));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L, 6L));

        FinalDeductionResponse response = scorer.submitAndScore(SESSION_ID, request);

        assertThat(response.score()).isEqualTo(90);
        verify(contextLoader).saveResultAndComplete(eq(SESSION_ID), any(FinalDeduction.class), eq(List.of(2L, 6L)));
    }

    @Test
    @DisplayName("submitAndScore releases final deduction lock when evidence validation fails")
    void submitAndScore_withLockedEvidence_releasesFinalDeductionLock() {
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L, 6L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOf(AiException.class);

        verify(contextLoader).releaseFinalDeductionLock(SESSION_ID);
        verify(ruleBasedScorer, never()).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore releases final deduction lock when culprit validation fails")
    void submitAndScore_withInvalidCulprit_releasesFinalDeductionLock() {
        stubOwnerMatch();
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(suspectReader.findByIdAndScenarioId(999L, SCENARIO_ID))
                .thenThrow(new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));

        FinalDeductionRequest request = buildSubmitRequest(999L, List.of(2L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getErrorCode())
                        .isEqualTo(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));

        verify(contextLoader).releaseFinalDeductionLock(SESSION_ID);
        verify(evidenceReader, never()).getUnlockedEvidenceIds(SESSION_ID);
        verify(ruleBasedScorer, never()).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("submitAndScore stores distinct evidence and duplicate ids do not increase evidence score")
    void submitAndScore_withDuplicateUnlockedEvidence_doesNotIncreaseEvidenceScore() {
        scorer = newScorer(new RuleBasedScorer());
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(0, 0, false, 0, 0, 0, 0, 0));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L, 2L, 2L));

        FinalDeductionResponse response = scorer.submitAndScore(SESSION_ID, request);

        assertThat(response.score()).isEqualTo(35);

        ArgumentCaptor<FinalDeduction> deductionCaptor = ArgumentCaptor.forClass(FinalDeduction.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> evidenceIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(contextLoader).saveResultAndComplete(eq(SESSION_ID), deductionCaptor.capture(), evidenceIdsCaptor.capture());
        assertThat(deductionCaptor.getValue().getScore()).isEqualTo(35);
        assertThat(evidenceIdsCaptor.getValue()).containsExactly(2L);
    }

    @Test
    @DisplayName("submitAndScore builds criteria from SolutionInfo when scenarioId differs from demo criteria")
    void submitAndScore_withSyntheticScenarioId_usesSolutionInfoAndDefaultScores() {
        Long syntheticScenarioId = 2L;
        Long culpritId = 9L;
        List<Long> keyEvidenceIds = List.of(42L, 43L, 44L);
        stubOwnerMatch();
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(syntheticScenarioId);
        when(playSessionReader.getScenarioVariantId(SESSION_ID)).thenReturn(7L);
        when(evidenceReader.getUnlockedEvidenceIds(SESSION_ID)).thenReturn(List.of(42L, 43L));
        when(solutionReader.findByScenarioIdAndVariantId(syntheticScenarioId, 7L))
                .thenReturn(buildSolution(culpritId, keyEvidenceIds, Map.of(
                        42L, "증거A", 43L, "증거B", 44L, "증거C"
                )));
        when(ruleBasedScorer.score(any(FinalDeductionRequest.class), any(ScoringCriteria.class)))
                .thenReturn(new ScoringResult(80, 30, true, 25, 20, 0, 5, 1));
        when(hintPenaltyReader.getTotalPenalty(SESSION_ID)).thenReturn(0);
        when(aiClient.isMockMode()).thenReturn(true);
        when(fallbackFeedbackGenerator.generate(any(ScoringResult.class), any(ScoringCriteria.class)))
                .thenReturn(new AiFeedbackResult(List.of(), List.of(), "feedback"));
        when(contextLoader.saveResultAndComplete(eq(SESSION_ID), any(FinalDeduction.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        FinalDeductionRequest request = buildSubmitRequest(culpritId, List.of(42L, 43L));

        scorer.submitAndScore(SESSION_ID, request);

        ArgumentCaptor<ScoringCriteria> criteriaCaptor = ArgumentCaptor.forClass(ScoringCriteria.class);
        verify(ruleBasedScorer).score(any(FinalDeductionRequest.class), criteriaCaptor.capture());
        ScoringCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.scenarioId()).isEqualTo(syntheticScenarioId);
        assertThat(criteria.culpritSuspectId()).isEqualTo(culpritId);
        assertThat(criteria.keyEvidenceIds()).containsExactlyElementsOf(keyEvidenceIds);
        assertThat(criteria.culpritMaxScore()).isEqualTo(30);
        assertThat(criteria.evidenceMaxScore()).isEqualTo(15);
        assertThat(criteria.method().maxScore()).isEqualTo(25);
        assertThat(criteria.motive().maxScore()).isEqualTo(20);
        assertThat(criteria.coverUp().maxScore()).isEqualTo(10);
        assertThat(criteria.method().minMatch()).isEqualTo(2);
    }

    @Test
    @DisplayName("getResult recalculates with SolutionInfo criteria when scenarioId differs from demo criteria")
    void getResult_withSyntheticScenarioId_usesSolutionInfoAndDefaultScores() {
        Long syntheticScenarioId = 2L;
        Long culpritId = 9L;
        List<Long> keyEvidenceIds = List.of(42L, 43L, 44L);
        stubOwnerMatch();
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(syntheticScenarioId);
        when(playSessionReader.getScenarioVariantId(SESSION_ID)).thenReturn(7L);
        when(solutionReader.findByScenarioIdAndVariantId(syntheticScenarioId, 7L))
                .thenReturn(buildSolution(culpritId, keyEvidenceIds, Map.of(
                        42L, "증거A", 43L, "증거B", 44L, "증거C"
                )));
        when(contextLoader.findBySessionId(SESSION_ID)).thenReturn(buildSavedDeduction(culpritId));
        when(finalDeductionEvidenceRepository.findAllByFinalDeductionId(any()))
                .thenReturn(List.of());
        when(ruleBasedScorer.score(any(FinalDeductionRequest.class), any(ScoringCriteria.class)))
                .thenReturn(new ScoringResult(80, 30, true, 25, 20, 0, 0, 0));

        DeductionResultResponse result = scorer.getResult(SESSION_ID);

        assertThat(result.sessionId()).isEqualTo(SESSION_ID);
        ArgumentCaptor<ScoringCriteria> criteriaCaptor = ArgumentCaptor.forClass(ScoringCriteria.class);
        verify(ruleBasedScorer).score(any(FinalDeductionRequest.class), criteriaCaptor.capture());
        ScoringCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.scenarioId()).isEqualTo(syntheticScenarioId);
        assertThat(criteria.culpritSuspectId()).isEqualTo(culpritId);
        assertThat(criteria.keyEvidenceIds()).containsExactlyElementsOf(keyEvidenceIds);
    }

    @Test
    @DisplayName("submitAndScore rejects incomplete SolutionInfo instead of silently scoring")
    void submitAndScore_withMissingCoverUp_rejectedAsSolutionNotFound() {
        stubOwnerMatch();
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(playSessionReader.getScenarioVariantId(SESSION_ID)).thenReturn(1L);
        when(evidenceReader.getUnlockedEvidenceIds(SESSION_ID)).thenReturn(List.of(2L));
        when(solutionReader.findByScenarioIdAndVariantId(SCENARIO_ID, 1L))
                .thenReturn(new SolutionInfo(
                        1L, "박재민", "CFO",
                        "회사 자금 유용 폭로 방지",
                        "견과류 알레르기 이용 + 에피펜 은닉",
                        "",
                        "박재민 CFO가 범인입니다.",
                        List.of(2L),
                        Map.of(2L, "증거A")
                ));
        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOf(AiException.class)
                .satisfies(ex -> assertThat(((AiException) ex).getErrorCode())
                        .isEqualTo(AiErrorCode.SOLUTION_NOT_FOUND));

        verify(contextLoader).releaseFinalDeductionLock(SESSION_ID);
        verify(ruleBasedScorer, never()).score(any(FinalDeductionRequest.class), any(ScoringCriteria.class));
    }

    @Test
    @DisplayName("AI rate limit은 최종추리 feedback fallback으로 저장하지 않고 전파한다")
    void submitAndScore_withAiRateLimit_propagatesWithoutFallbackOrSave() {
        stubSuccessfulSubmitChain(List.of(2L), new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
        when(aiClient.isMockMode()).thenReturn(false);
        when(promptBuilder.buildDeductionScoringPrompt(
                any(ScoringResult.class),
                any(SolutionInfo.class),
                any(FinalDeductionRequest.class),
                any(ScoringCriteria.class)))
                .thenReturn("deduction prompt");
        when(aiClient.chatWithMetadata(
                anyString(), anyString(), any(AiRequestParams.class), any(AiCallContext.class)))
                .thenThrow(new AiException(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));

        FinalDeductionRequest request = buildSubmitRequest(1L, List.of(2L));

        assertThatThrownBy(() -> scorer.submitAndScore(SESSION_ID, request))
                .isInstanceOfSatisfying(AiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AiErrorCode.AI_DAILY_RATE_LIMIT_EXCEEDED));

        verify(aiClient, never()).recordFallback(any(AiCallContext.class), anyString(), anyLong());
        verify(fallbackFeedbackGenerator, never()).generate(any(ScoringResult.class), any(ScoringCriteria.class));
        verify(contextLoader, never()).saveResultAndComplete(
                eq(SESSION_ID), any(FinalDeduction.class), any());
        verify(contextLoader).releaseFinalDeductionLock(SESSION_ID);
    }

    private void stubOwnerMatch() {
        when(mockUserProvider.currentUserId()).thenReturn(OWNER_USER_ID);
        when(playSessionReader.getOwnerUserId(SESSION_ID)).thenReturn(OWNER_USER_ID);
    }

    private AiDeductionScorer newScorer(RuleBasedScorer ruleBasedScorer) {
        return new AiDeductionScorer(
                mockUserProvider,
                contextLoader,
                playSessionReader,
                evidenceReader,
                suspectReader,
                solutionReader,
                hintPenaltyReader,
                scoringDefaults,
                ruleBasedScorer,
                fallbackFeedbackGenerator,
                promptBuilder,
                aiClient,
                jsonMapper,
                finalDeductionEvidenceRepository
        );
    }

    private void stubSuccessfulSubmitChain(List<Long> unlockedEvidenceIds, ScoringResult scoringResult) {
        stubOwnerMatch();

        List<Long> keys = List.of(2L, 6L, 7L);
        Map<Long, String> titles = Map.of(2L, "증거A", 6L, "증거B", 7L, "증거C");
        SolutionInfo solution = buildSolution(keys, titles);

        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(playSessionReader.getScenarioVariantId(SESSION_ID)).thenReturn(1L);
        when(evidenceReader.getUnlockedEvidenceIds(SESSION_ID)).thenReturn(unlockedEvidenceIds);
        when(solutionReader.findByScenarioIdAndVariantId(eq(SCENARIO_ID), any())).thenReturn(solution);
        when(ruleBasedScorer.score(any(FinalDeductionRequest.class), any(ScoringCriteria.class)))
                .thenReturn(scoringResult);
        when(hintPenaltyReader.getTotalPenalty(SESSION_ID)).thenReturn(0);
        when(aiClient.isMockMode()).thenReturn(true);
        when(fallbackFeedbackGenerator.generate(any(ScoringResult.class), any(ScoringCriteria.class)))
                .thenReturn(new AiFeedbackResult(List.of(), List.of(), "feedback"));
        when(contextLoader.saveResultAndComplete(eq(SESSION_ID), any(FinalDeduction.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    private FinalDeductionRequest buildSubmitRequest(Long selectedCulpritId, List<Long> selectedEvidenceIds) {
        return new FinalDeductionRequest(
                selectedCulpritId,
                "wrong motive",
                "wrong method",
                "wrong cover up",
                selectedEvidenceIds
        );
    }

    private void stubFullResultChain() {
        List<Long> keys = List.of(2L, 6L, 7L);
        Map<Long, String> titles = Map.of(2L, "증거A", 6L, "증거B", 7L, "증거C");
        SolutionInfo solution = buildSolution(keys, titles);
        stubResultChainWith(solution);
    }

    private void stubResultChainWith(SolutionInfo solution) {
        when(contextLoader.findBySessionId(SESSION_ID)).thenReturn(buildSavedDeduction(1L));
        when(playSessionReader.getScenarioId(SESSION_ID)).thenReturn(SCENARIO_ID);
        when(playSessionReader.getScenarioVariantId(SESSION_ID)).thenReturn(1L);
        when(solutionReader.findByScenarioIdAndVariantId(eq(SCENARIO_ID), any())).thenReturn(solution);
        when(finalDeductionEvidenceRepository.findAllByFinalDeductionId(any()))
                .thenReturn(List.of());
        when(ruleBasedScorer.score(any(FinalDeductionRequest.class), any(ScoringCriteria.class)))
                .thenReturn(new ScoringResult(85, 30, true, 25, 20, 10, 0, 0));
    }

    private FinalDeduction buildSavedDeduction(Long selectedCulpritId) {
        return FinalDeduction.builder()
                .playSessionId(SESSION_ID)
                .selectedCulpritId(selectedCulpritId)
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
    }

    private SolutionInfo buildSolution(List<Long> keyEvidenceIds, Map<Long, String> titles) {
        return buildSolution(1L, keyEvidenceIds, titles);
    }

    private SolutionInfo buildSolution(Long culpritSuspectId, List<Long> keyEvidenceIds, Map<Long, String> titles) {
        return new SolutionInfo(
                culpritSuspectId, "박재민", "CFO",
                "회사 자금 유용 폭로 방지",
                "견과류 알레르기 이용 + 에피펜 은닉",
                "피해자 휴대폰으로 메시지 위장",
                "박재민 CFO가 범인입니다. 데모데이 전날 자금 유용 사실이 폭로될 위기에 처하자...",
                keyEvidenceIds,
                titles
        );
    }
}
