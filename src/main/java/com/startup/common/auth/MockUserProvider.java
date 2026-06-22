package com.startup.common.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
// 기존 서비스/테스트 호환을 유지하는 wrapper다. 실제 현재 사용자 판정은 CurrentUserProvider가 담당한다.
public class MockUserProvider {

    private final CurrentUserProvider currentUserProvider;
    private final Long defaultMockUserId;
    private Long mockUserId;

    @Autowired
    public MockUserProvider(
            CurrentUserProvider currentUserProvider,
            @Value("${app.mock-user-id:1}") Long mockUserId
    ) {
        this.currentUserProvider = currentUserProvider;
        this.defaultMockUserId = mockUserId;
        this.mockUserId = mockUserId;
    }

    public MockUserProvider(Long mockUserId) {
        this.currentUserProvider = null;
        this.defaultMockUserId = mockUserId;
        this.mockUserId = mockUserId;
    }

    public Long currentUserId() {
        if (!Objects.equals(mockUserId, defaultMockUserId)) {
            return mockUserId;
        }
        if (currentUserProvider != null) {
            return currentUserProvider.currentUserId();
        }
        return mockUserId;
    }

    public Long currentUserIdOrNull() {
        if (!Objects.equals(mockUserId, defaultMockUserId)) {
            return mockUserId;
        }
        if (currentUserProvider != null) {
            return currentUserProvider.currentUserIdOrNull();
        }
        return mockUserId;
    }
}
