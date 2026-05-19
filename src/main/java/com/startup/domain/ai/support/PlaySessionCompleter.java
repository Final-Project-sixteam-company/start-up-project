package com.startup.domain.ai.support;

public interface PlaySessionCompleter {

    void lockAndComplete(Long sessionId);
}
