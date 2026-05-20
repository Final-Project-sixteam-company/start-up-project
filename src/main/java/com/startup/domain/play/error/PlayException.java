package com.startup.domain.play.error;

import com.startup.common.error.BusinessException;

public class PlayException extends BusinessException {

    public PlayException(PlayErrorCode errorCode) {
        super(errorCode);
    }

    public PlayException(PlayErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
