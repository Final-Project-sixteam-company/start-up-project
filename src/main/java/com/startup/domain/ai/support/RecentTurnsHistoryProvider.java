package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RecentTurnsHistoryProvider implements InterrogationHistoryProvider {

    private final InterrogationLogRepository interrogationLogRepository;

    @Override
    public List<ChatTurn> getHistory(Long sessionId, Long suspectId, int maxTurns) {
        List<InterrogationLog> logs = interrogationLogRepository
                .findTop5ByPlaySessionIdAndSuspectIdOrderByCreatedAtDesc(sessionId, suspectId);

        return logs.reversed().stream()
                .limit(maxTurns)
                .map(log -> new ChatTurn(log.getQuestion(), log.getAnswer()))
                .toList();
    }
}
