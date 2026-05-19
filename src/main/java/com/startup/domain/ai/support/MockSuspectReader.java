package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MockSuspectReader implements SuspectReader {

    private static final Map<Long, SuspectProfile> SUSPECTS = Map.of(
            1L, new SuspectProfile(1L, "박재민", "CFO / 재무이사", "공동창업자, 5년간 함께 일함",
                    "회사 재무를 담당하는 차분한 성격의 인물",
                    "사건 당시 재무팀 자리에서 투자 자료를 정리하고 있었습니다. 데모룸 근처에는 가지 않았습니다.",
                    "사건 당시 재무팀 자리에서 투자 자료를 정리하고 있었다고 주장한다."),
            2L, new SuspectProfile(2L, "이준호", "CTO / 기술총괄", "기술 성과를 두고 갈등",
                    "핵심 AI 모델 개발을 주도했지만, 대표가 기술 성과를 자신의 공로로 포장하려 한다고 느끼고 있었다.",
                    "사건 당시 서버실과 제 자리 주변을 오가며 데모 시연 코드를 점검하고 있었습니다.",
                    "서버실에서 시연 코드를 점검하고 있었다고 주장한다."),
            3L, new SuspectProfile(3L, "서유라", "마케팅 리드", "과거 연인 관계, 현재 갈등",
                    "데모데이 발표자료와 언론 대응을 맡고 있었다.",
                    "사건 당시 회사 밖에 있었습니다. 대표님과 사적으로 연락한 적도 없습니다.",
                    "사건 당시 회사 밖 주차장 또는 외부에 있었다고 주장한다."),
            4L, new SuspectProfile(4L, "김나은", "인턴 / 운영보조", "피해자에게 책임을 떠넘겨진 경험이 있음",
                    "데모데이 현장 세팅과 자료 정리를 맡은 인턴이다.",
                    "대표님 노트북이나 개인 물건을 만진 적 없습니다. 사건이 난 줄도 몰랐습니다.",
                    "사건 당시 회의실 정리와 자료 정리를 하고 있었다고 주장한다."),
            5L, new SuspectProfile(5L, "오세훈", "투자사 심사역", "투자 조건을 두고 갈등",
                    "다음 날 데모데이를 평가할 투자사 심사역이다.",
                    "밤 10시 이전에 회사를 나갔습니다. 사건 당시에는 건물 안에 없었습니다.",
                    "밤 10시 이전에 퇴근했다고 주장한다.")
    );

    @Override
    public SuspectProfile findById(Long suspectId) {
        SuspectProfile profile = SUSPECTS.get(suspectId);
        if (profile == null) {
            throw new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND);
        }
        return profile;
    }
}
