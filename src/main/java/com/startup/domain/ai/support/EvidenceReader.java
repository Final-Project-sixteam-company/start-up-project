package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.EvidenceInfo;

import java.util.List;

/**
 * 증거 조회 인터페이스.
 * 소수경의 play 도메인 구현 후 실제 구현체로 교체한다.
 *
 * 반환 계약:
 * - null을 반환하지 않는다.
 * - 목록 조회 결과가 없으면 빈 리스트를 반환한다.
 * - 단건 조회 대상이 없으면 기존 시그니처를 크게 깨지 않는 선에서 AiException 사용을 우선한다.
 */
public interface EvidenceReader {

    List<Long> getUnlockedEvidenceIds(Long sessionId);

    List<EvidenceInfo> getUnlockedEvidences(Long sessionId);

    EvidenceInfo findById(Long evidenceId);
}
