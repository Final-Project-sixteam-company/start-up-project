package com.startup.domain.scenario.enums;

public enum RelationType {
    OWNER, //소유자, 소지자 - 증거물을 소유하고 있거나 해당 증거물이 그 용의자의 소지품일 때
    CREATOR, //작성자, 발신자 - 메시지, 파일, 서류 등을 직접 작성하거나 발신했을 때
    SUBJECT, //대상, 언급 - 증거 내용 중 언급되거나 지목되는 대상일 때
    RELATED, //관련자 - 직간접적으로 연관이 있는 일반적인 상태 (기본값)
    SUSPECTED, //의심 - 해당 증거로 인해 알리바이가 깨지거나 혐의를 입증할 수 있는 대상일 때
}
