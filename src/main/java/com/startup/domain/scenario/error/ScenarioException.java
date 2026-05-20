package com.startup.domain.scenario.error;

import com.startup.common.error.BusinessException;

public class ScenarioException extends BusinessException {

    public ScenarioException(ScenarioErrorCode errorCode) {
        super(errorCode);
    }

    public ScenarioException(ScenarioErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
