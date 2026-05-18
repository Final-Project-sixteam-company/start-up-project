package com.startup.common.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
// 인증 구현 전까지 서비스 계층에서 현재 사용자 ID를 일관되게 얻기 위한 임시 provider다.
// JWT 도입 후에는 SecurityContext 기반 구현으로 교체한다.
public class MockUserProvider {

    private final Long mockUserId;

    public MockUserProvider(@Value("${app.mock-user-id:1}") Long mockUserId) {
        this.mockUserId = mockUserId;
    }

    public Long currentUserId() {
        return mockUserId;
    }
}
