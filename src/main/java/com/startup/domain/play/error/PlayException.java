package com.startup.domain.play.error;

import com.startup.common.error.BusinessException;

import java.util.Map;

public class PlayException extends BusinessException {

    public PlayException(PlayErrorCode errorCode) {
        super(errorCode);
    }

    public PlayException(PlayErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public PlayException(PlayErrorCode errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
