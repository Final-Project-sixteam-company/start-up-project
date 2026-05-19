package com.startup.domain.ai.support;

public interface PlaySessionCompleter {

    void lockForFinalDeduction(Long sessionId);

    void complete(Long sessionId);
}
