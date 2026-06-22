package com.startup.infrastructure.redis.lock;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;

// 락 획득 실패는 같은 자원에 대한 동시 요청 충돌로 보고 409로 응답한다.
public class LockException extends BusinessException {

    public LockException(String message) {
        super(CommonErrorCode.CONFLICT, message);
    }

    public LockException(String message, Throwable cause) {
        super(CommonErrorCode.CONFLICT, message, cause);
    }
}
