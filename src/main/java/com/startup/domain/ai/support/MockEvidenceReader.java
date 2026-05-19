package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.EvidenceInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockEvidenceReader implements EvidenceReader {

    private static final Map<Long, EvidenceInfo> EVIDENCES = Map.of(
            1L, new EvidenceInfo(1L, "사건 현장 사진", "데모룸 테이블 위에 라벨이 찢긴 커피 컵과 쓰러진 피해자가 보인다."),
            2L, new EvidenceInfo(2L, "피해자 알레르기 정보", "피해자 강도현은 아몬드 알레르기가 있으며, 에피펜을 항상 소지했다."),
            3L, new EvidenceInfo(3L, "카페 영수증", "22시 5분 카페에서 오트라떼 1잔, 아몬드라떼 1잔 결제."),
            4L, new EvidenceInfo(4L, "피해자와 박재민의 메시지", "강도현이 박재민에게 회계자료 관련 메시지를 보냈다."),
            5L, new EvidenceInfo(5L, "박재민 서랍에서 발견된 에피펜", "박재민의 책상 서랍에서 피해자의 이름이 붙은 에피펜이 발견되었다.")
    );

    @Override
    public List<Long> getUnlockedEvidenceIds(Long sessionId) {
        return List.of(1L, 2L);
    }

    @Override
    public List<EvidenceInfo> getUnlockedEvidences(Long sessionId) {
        return getUnlockedEvidenceIds(sessionId).stream()
                .map(EVIDENCES::get)
                .filter(e -> e != null)
                .toList();
    }

    @Override
    public EvidenceInfo findById(Long evidenceId) {
        return EVIDENCES.get(evidenceId);
    }
}
