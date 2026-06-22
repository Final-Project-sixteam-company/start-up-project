package com.startup.common.error;

import org.springframework.http.HttpStatus;

// 도메인별 ErrorCode enum이 공통으로 구현하는 계약이다.
// 예외 처리기는 이 인터페이스만 보고 HTTP status, 내부 code, 기본 message를 조립한다.
public interface ErrorCode {
    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
