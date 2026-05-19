package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.FinalDeductionRequest;
import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.dto.ScoringCriteria.KeywordCriteria;
import com.startup.domain.ai.dto.ScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedScorerTest {

    private RuleBasedScorer scorer;
    private ScoringCriteria criteria;

    @BeforeEach
    void setUp() {
        scorer = new RuleBasedScorer();
        criteria = new ScoringCriteria(
                1L, 1L,
                new KeywordCriteria(List.of("아몬드라떼", "아몬드", "견과류", "알레르기", "에피펜", "알러지"), 2, 25),
                new KeywordCriteria(List.of("회계", "자금", "유용", "비리", "횡령", "데모데이", "투자", "폭로"), 2, 20),
                new KeywordCriteria(List.of("메시지", "휴대폰", "사망 이후", "위장", "조작", "단톡", "카톡"), 2, 10),
                List.of(2L, 6L, 7L, 8L, 11L, 12L, 13L, 14L),
                15, 30
        );
    }

    @Nested
    @DisplayName("증거 중복 제거 테스트")
    class EvidenceDeduplicationTest {

        @Test
        @DisplayName("동일 증거 ID 반복 선택 시 1건으로 카운트")
        void duplicateEvidenceIds_countAsOne() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(6L, 6L, 6L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.evidenceMatchCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 포함된 리스트에서 고유 증거만 카운트")
        void mixedDuplicateAndUnique_countsDistinct() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(2L, 2L, 6L, 6L, 7L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.evidenceMatchCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("증거 채점 테스트")
    class EvidenceScoringTest {

        @Test
        @DisplayName("핵심 증거 3개 이상 선택 시 만점")
        void threeKeyEvidences_fullScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(2L, 6L, 7L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.evidenceScore()).isEqualTo(15);
            assertThat(result.evidenceMatchCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("핵심 증거 0개 선택 시 0점")
        void noKeyEvidences_zeroScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(99L, 100L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.evidenceScore()).isEqualTo(0);
            assertThat(result.evidenceMatchCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("핵심 증거 1개 선택 시 부분 점수")
        void oneKeyEvidence_partialScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(2L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.evidenceMatchCount()).isEqualTo(1);
            assertThat(result.evidenceScore()).isEqualTo(5); // 15 * 1 / 3
        }
    }

    @Nested
    @DisplayName("범인 채점 테스트")
    class CulpritScoringTest {

        @Test
        @DisplayName("정답 범인 선택 시 만점")
        void correctCulprit_fullScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(2L, 6L, 7L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.culpritCorrect()).isTrue();
            assertThat(result.culpritScore()).isEqualTo(30);
        }

        @Test
        @DisplayName("오답 범인 선택 시 0점")
        void wrongCulprit_zeroScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    2L, "자금 유용", "아몬드 에피펜", "메시지 위장",
                    List.of(2L, 6L, 7L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.culpritCorrect()).isFalse();
            assertThat(result.culpritScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("키워드 채점 테스트")
    class KeywordScoringTest {

        @Test
        @DisplayName("키워드 minMatch 이상 포함 시 만점")
        void keywordsAboveThreshold_fullScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "회사 자금을 유용했기 때문에", "아몬드라떼를 마시게 하고 에피펜을 숨겼다", "메시지를 보내 위장했다",
                    List.of(2L, 6L, 7L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.motiveScore()).isEqualTo(20);
            assertThat(result.methodScore()).isEqualTo(25);
            assertThat(result.coverUpScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("키워드 1개만 포함 시 부분 점수")
        void oneKeyword_partialScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "돈 때문에", "아몬드", "몰라",
                    List.of(2L)
            );

            ScoringResult result = scorer.score(request, criteria);

            // method: "아몬드" 1개 매칭, minMatch=2 → 25 * 1 / 2 = 12
            assertThat(result.methodScore()).isEqualTo(12);
            // motive: 0개 매칭
            assertThat(result.motiveScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("빈 텍스트 시 0점")
        void emptyText_zeroScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "  ", "  ", null,
                    List.of(2L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.motiveScore()).isEqualTo(0);
            assertThat(result.methodScore()).isEqualTo(0);
            assertThat(result.coverUpScore()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("총점 계산 테스트")
    class TotalScoreTest {

        @Test
        @DisplayName("만점 시나리오")
        void perfectScore() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L,
                    "회사 자금을 유용한 것이 들통날까봐",
                    "아몬드라떼를 마시게 하고 에피펜을 숨겼다",
                    "휴대폰으로 메시지를 보내 위장했다",
                    List.of(2L, 6L, 7L, 8L, 11L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.totalScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("범인만 맞춘 경우")
        void onlyCulpritCorrect() {
            FinalDeductionRequest request = new FinalDeductionRequest(
                    1L, "모르겠다", "모르겠다", "모르겠다",
                    List.of(99L)
            );

            ScoringResult result = scorer.score(request, criteria);

            assertThat(result.culpritScore()).isEqualTo(30);
            assertThat(result.methodScore()).isEqualTo(0);
            assertThat(result.motiveScore()).isEqualTo(0);
            assertThat(result.coverUpScore()).isEqualTo(0);
            assertThat(result.evidenceScore()).isEqualTo(0);
            assertThat(result.totalScore()).isEqualTo(30);
        }
    }
}
