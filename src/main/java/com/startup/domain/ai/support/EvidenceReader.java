package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.EvidenceInfo;

import java.util.List;

/**
 * 증거 조회 인터페이스.
 * 소수경의 play 도메인 구현 후 실제 구현체로 교체한다.
 */
public interface EvidenceReader {

    List<Long> getUnlockedEvidenceIds(Long sessionId);

    List<EvidenceInfo> getUnlockedEvidences(Long sessionId);

    EvidenceInfo findById(Long evidenceId);
}
