package com.startup.domain.ai.client;

import lombok.Getter;

@Getter
public class AiRequestParams {

    private final double temperature;
    private final int maxTokens;

    private AiRequestParams(double temperature, int maxTokens) {
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public static AiRequestParams interrogation(double temperature, int maxTokens) {
        return new AiRequestParams(temperature, maxTokens);
    }
}
