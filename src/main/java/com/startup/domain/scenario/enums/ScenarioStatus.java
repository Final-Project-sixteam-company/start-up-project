package com.startup.domain.scenario.enums;

public enum ScenarioStatus {
    DRAFT,
    VALIDATING,
    PUBLISHED,
    HIDDEN,
    DELETED;

    public boolean canPublish() {
        return this == DRAFT || this == VALIDATING || this == HIDDEN;
    }
}
