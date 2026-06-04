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

    private void saveLogs(int count) {
        for (int i = 1; i <= count; i++) {
            interrogationLogRepository.save(InterrogationLog.builder()
                    .playSessionId(SESSION_ID)
                    .suspectId(SUSPECT_ID)
                    .questionType(QuestionType.FREE)
                    .question("q" + i)
                    .answer("a" + i)
                    .aiModel("test")
                    .build());
        }
    }

    @Test
    @DisplayName("7턴 저장 + maxTurns=10 -> 7턴 모두 반환 (과거 findTop5 캡이면 5였음)")
    void returnsMoreThanFiveWhenMaxTurnsLarger() {
        saveLogs(7);

        List<ChatTurn> history = provider.getHistory(SESSION_ID, SUSPECT_ID, 10);

        assertThat(history).hasSize(7);
        assertThat(history).extracting(ChatTurn::question)
                .containsExactlyInAnyOrder("q1", "q2", "q3", "q4", "q5", "q6", "q7");
    }

    @Test
    @DisplayName("7턴 저장 + maxTurns=3 -> 최근 3턴만 반환")
    void capsToMaxTurns() {
        saveLogs(7);

        List<ChatTurn> history = provider.getHistory(SESSION_ID, SUSPECT_ID, 3);

        assertThat(history).hasSize(3);
    }

    @Test
    @DisplayName("maxTurns=0 이하 -> 빈 이력")
    void nonPositiveMaxTurnsReturnsEmpty() {
        saveLogs(3);

        assertThat(provider.getHistory(SESSION_ID, SUSPECT_ID, 0)).isEmpty();
    }
}
