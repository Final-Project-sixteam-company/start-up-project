package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecentTurnsHistoryProvider implements InterrogationHistoryProvider {

    private final InterrogationLogRepository interrogationLogRepository;

    @Override
    public List<ChatTurn> getHistory(Long sessionId, Long suspectId, int maxTurns) {
        if (maxTurns <= 0) {
            return List.of();
        }
        // 최신순으로 maxTurns개만 조회한 뒤, 프롬프트에는 오래된→최신 순서로 넣는다.
        List<InterrogationLog> logs = interrogationLogRepository
                .findByPlaySessionIdAndSuspectIdOrderByCreatedAtDesc(
                        sessionId, suspectId, PageRequest.of(0, maxTurns));

        return logs.reversed().stream()
                .map(log -> new ChatTurn(log.getQuestion(), log.getAnswer()))
                .toList();
    }
}
