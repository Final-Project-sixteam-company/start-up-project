package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("QA-9: 대화 이력이 5턴으로 고정되지 않고 maxTurns 설정을 따른다")
class RecentTurnsHistoryProviderTest {

    @Autowired
    private RecentTurnsHistoryProvider provider;

    @Autowired
    private InterrogationLogRepository interrogationLogRepository;

    private static final Long SESSION_ID = 9001L;
    private static final Long SUSPECT_ID = 9002L;

    @AfterEach
    void tearDown() {
        interrogationLogRepository.deleteAllInBatch();
    }

    // createdAt(@CreatedDate, updatable=false)이 저장마다 명확히 구분되도록 짧은 간격을 둔다.
    // 이래야 "최근 N턴" 선택과 시간순(오래된→최신) 정렬을 결정적으로 검증할 수 있다.
    private void saveLogs(int count) {
        for (int i = 1; i <= count; i++) {
            interrogationLogRepository.saveAndFlush(InterrogationLog.builder()
                    .playSessionId(SESSION_ID)
                    .suspectId(SUSPECT_ID)
                    .questionType(QuestionType.FREE)
                    .question("q" + i)
                    .answer("a" + i)
                    .aiModel("test")
                    .build());
            sleepMillis(2);
        }
    }

    private void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("7턴 저장 + maxTurns=10 -> 7턴 모두, 오래된→최신 순서로 반환 (과거 findTop5 캡이면 5였음)")
    void returnsMoreThanFiveWhenMaxTurnsLarger() {
        saveLogs(7);

        List<ChatTurn> history = provider.getHistory(SESSION_ID, SUSPECT_ID, 10);

        // 캡 제거 검증(7 > 과거 5) + 시간순(reversed) 검증
        assertThat(history).extracting(ChatTurn::question)
                .containsExactly("q1", "q2", "q3", "q4", "q5", "q6", "q7");
    }

    @Test
    @DisplayName("7턴 저장 + maxTurns=3 -> 가장 최근 3턴만, 오래된→최신 순서로 반환")
    void capsToMaxTurns() {
        saveLogs(7);

        List<ChatTurn> history = provider.getHistory(SESSION_ID, SUSPECT_ID, 3);

        // 오래된 3턴(q1,q2,q3)이 아니라 최근 3턴(q5,q6,q7)이어야 한다.
        assertThat(history).extracting(ChatTurn::question)
                .containsExactly("q5", "q6", "q7");
    }

    @Test
    @DisplayName("maxTurns=0 이하 -> 빈 이력")
    void nonPositiveMaxTurnsReturnsEmpty() {
        saveLogs(3);

        assertThat(provider.getHistory(SESSION_ID, SUSPECT_ID, 0)).isEmpty();
    }
}
