package com.startup.domain.community.error;

import com.startup.common.error.BusinessException;

public class CommunityException  extends BusinessException {

    public CommunityException(CommunityErrorCode errorCode) {
        super(errorCode);
    }

    public CommunityException(CommunityErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
