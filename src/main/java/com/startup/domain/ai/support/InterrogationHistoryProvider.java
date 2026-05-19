package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ChatTurn;

import java.util.List;

public interface InterrogationHistoryProvider {

    List<ChatTurn> getHistory(Long sessionId, Long suspectId, int maxTurns);
}
