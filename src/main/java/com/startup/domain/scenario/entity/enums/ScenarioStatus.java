package com.startup.domain.scenario.entity.enums;

public enum ScenarioStatus {
    //임시 저장 (작성 중) - 작성자 본인만 볼 수 있음
    DRAFT,

    //검증 중 - 시나리오에 논리적 모순이나 오류가 없는지 AI가 검증을 진행하는 단계
    VALIDATING,

    //발행 완료
    PUBLISHED,

    //숨김
    HIDDEN,

    //삭제
    DELETED;

    //해당 상태에서 발행 상태로 전환 가능한지 확인
    public boolean canPublish() {
        return this == DRAFT || this == VALIDATING || this == HIDDEN;
    }
}
