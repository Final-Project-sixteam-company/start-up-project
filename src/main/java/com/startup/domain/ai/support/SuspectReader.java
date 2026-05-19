package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SuspectProfile;

/**
 * 용의자 조회 인터페이스.
 * 소수경의 scenario 도메인 구현 후 실제 구현체로 교체한다.
 */
public interface SuspectReader {

    SuspectProfile findById(Long suspectId);
}
