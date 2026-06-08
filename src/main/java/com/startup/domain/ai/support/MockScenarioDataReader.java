package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ScenarioValidationData;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockScenarioDataReader implements ScenarioDataReader {

    private static final List<Long> KEY_EVIDENCE_IDS = List.of(2L, 6L, 7L, 8L, 11L, 12L, 13L, 14L);

    @Override
    public ScenarioValidationData loadForValidation(Long scenarioId) {
        if (scenarioId != 1L) {
            throw new AiException(AiErrorCode.SCENARIO_NOT_FOUND);
        }

        return new ScenarioValidationData(
                buildScenarioBasicInfo(),
                buildVictimInfo(),
                buildLocations(),
                buildSuspects(),
                buildEvidences(),
                buildHints(),
                buildTimelineEvents(),
                buildSolution(),
                buildSolutionEvidences(),
                buildResponsePolicies(),
                buildSuspectSecrets()
        );
    }

    private ScenarioValidationData.ScenarioBasicInfo buildScenarioBasicInfo() {
        return new ScenarioValidationData.ScenarioBasicInfo(
                1L,
                "데모데이 전야 살인사건",
                "AI 스타트업 대표가 데모데이 전날 사무실 데모룸에서 사망한 사건",
                "NORMAL",
                "DRAFT"
        );
    }

    private ScenarioValidationData.VictimInfo buildVictimInfo() {
        return new ScenarioValidationData.VictimInfo(
                "강도현",
                "대표",
                "견과류 알레르기 쇼크",
                "데모룸 테이블 옆에서 쓰러진 채 발견됨"
        );
    }

    private List<ScenarioValidationData.LocationInfo> buildLocations() {
        return List.of(
                new ScenarioValidationData.LocationInfo(1L, "데모룸", "시연 장비와 발표 자료가 놓인 회의 공간"),
                new ScenarioValidationData.LocationInfo(2L, "재무팀 자리", "박재민의 업무 공간"),
                new ScenarioValidationData.LocationInfo(3L, "서버실", "CTO 이준호가 시연 코드를 점검하던 장소"),
                new ScenarioValidationData.LocationInfo(4L, "사내 카페", "사건 전 음료가 결제된 장소")
        );
    }

    private List<ScenarioValidationData.SuspectValidationInfo> buildSuspects() {
        return List.of(
                new ScenarioValidationData.SuspectValidationInfo(1L, "박재민", "CFO", "재무팀 자리에서 투자 자료를 정리하고 있었다고 주장한다.",
                        "사건 당시 데모룸 근처에는 가지 않았습니다.", "회사 재무를 담당하는 공동창업자", true),
                new ScenarioValidationData.SuspectValidationInfo(2L, "이준호", "CTO", "서버실에서 시연 코드를 점검하고 있었다고 주장한다.",
                        "시연 코드 때문에 계속 서버실에 있었습니다.", "핵심 AI 모델 개발을 주도한 기술 책임자", false),
                new ScenarioValidationData.SuspectValidationInfo(3L, "서유라", "마케팅 리드", "회사 밖 주차장에 있었다고 주장한다.",
                        "대표님과 사적으로 연락한 적 없습니다.", "발표 자료와 언론 대응 담당자", false),
                new ScenarioValidationData.SuspectValidationInfo(4L, "김나은", "인턴", "회의실 정리와 자료 정리를 하고 있었다고 주장한다.",
                        "대표님 물건을 만진 적 없습니다.", "데모데이 현장 세팅 담당 인턴", false),
                new ScenarioValidationData.SuspectValidationInfo(5L, "오세훈", "투자사 심사역", "밤 10시 이전에 퇴근했다고 주장한다.",
                        "사건 당시에는 건물 안에 없었습니다.", "투자 조건을 두고 대표와 갈등이 있던 심사역", false)
        );
    }

    private List<ScenarioValidationData.EvidenceValidationInfo> buildEvidences() {
        return List.of(
                evidence(1L, "사건 현장 사진", "라벨이 찢긴 커피 컵과 쓰러진 피해자가 보인다.", "NORMAL", true, List.of()),
                evidence(2L, "피해자 알레르기 정보", "강도현은 아몬드 알레르기가 있으며 에피펜을 항상 소지했다.", "KEY", true, List.of(1L)),
                evidence(3L, "카페 영수증", "22시 5분 오트라떼와 아몬드라떼가 결제되었다.", "NORMAL", true, List.of(1L)),
                evidence(4L, "피해자 계정 메시지", "사망 추정 시각 이후 피해자 계정으로 단톡방 메시지가 전송되었다.", "NORMAL", false, List.of(1L)),
                evidence(5L, "서버실 출입 기록", "이준호가 22시 전후 서버실을 출입한 기록이다.", "FAKE", false, List.of(2L)),
                evidence(6L, "박재민 법인카드 결제 내역", "박재민 법인카드로 사건 직전 카페 음료가 결제되었다.", "KEY", false, List.of(1L)),
                evidence(7L, "박재민이 보낸 단톡 메시지", "박재민이 피해자 계정 메시지 직후 알리바이를 강조했다.", "KEY", false, List.of(1L)),
                evidence(8L, "사라진 에피펜", "피해자의 가방에서 항상 있던 에피펜이 사라졌다.", "KEY", false, List.of(1L)),
                evidence(9L, "마케팅 자료 수정 로그", "서유라가 발표 자료를 늦게까지 수정했다.", "FAKE", false, List.of(3L)),
                evidence(10L, "투자 계약 초안", "오세훈과 대표가 투자 조건으로 충돌한 흔적이다.", "FAKE", false, List.of(5L)),
                evidence(11L, "찢긴 컵 라벨", "피해자가 마신 컵의 라벨 일부가 찢겨 있었다.", "KEY", false, List.of(1L)),
                evidence(12L, "박재민 서랍 속 에피펜", "박재민 책상 서랍에서 피해자 이름이 붙은 에피펜이 발견되었다.", "KEY", false, List.of(1L)),
                evidence(13L, "박재민 휴대폰 위치 기록", "사망 추정 시각에 박재민 휴대폰이 데모룸 근처에 있었다.", "KEY", false, List.of(1L)),
                evidence(14L, "암호화된 회계 파일", "피해자가 자금 유용 정황을 데모데이에 공개하려 했다.", "KEY", false, List.of(1L)),
                evidence(15L, "인턴 업무 체크리스트", "김나은이 회의실 정리를 맡았다는 기록이다.", "FAKE", false, List.of(4L))
        );
    }

    private ScenarioValidationData.EvidenceValidationInfo evidence(Long id, String title, String description,
                                                                   String importance, boolean initialPublic,
                                                                   List<Long> relatedSuspectIds) {
        return new ScenarioValidationData.EvidenceValidationInfo(
                id, title, description, importance, initialPublic, relatedSuspectIds);
    }

    private List<ScenarioValidationData.HintValidationInfo> buildHints() {
        return List.of(
                new ScenarioValidationData.HintValidationInfo(1L, 1, "피해자가 마신 음료와 알레르기 정보를 함께 보세요.", 5),
                new ScenarioValidationData.HintValidationInfo(2L, 2, "사라진 에피펜이 어디서 발견되는지 확인하세요.", 10),
                new ScenarioValidationData.HintValidationInfo(3L, 3, "사망 이후 메시지를 보낼 수 있었던 사람을 추적하세요.", 15)
        );
    }

    private List<ScenarioValidationData.TimelineEventInfo> buildTimelineEvents() {
        return List.of(
                new ScenarioValidationData.TimelineEventInfo("22:05", "카페 결제", "오트라떼와 아몬드라떼가 결제됨", "FACT", true),
                new ScenarioValidationData.TimelineEventInfo("22:16", "음료 배치", "데모룸 앞에 커피 컵이 놓임", "FACT", true),
                new ScenarioValidationData.TimelineEventInfo("22:30", "피해자 쓰러짐", "강도현이 데모룸에서 쓰러짐", "FACT", true),
                new ScenarioValidationData.TimelineEventInfo("22:36", "단톡 메시지", "피해자 계정으로 메시지가 전송됨", "FACT", true)
        );
    }

    private ScenarioValidationData.SolutionValidationInfo buildSolution() {
        return new ScenarioValidationData.SolutionValidationInfo(
                1L,
                "박재민",
                "CFO",
                "회사 자금 유용 사실이 데모데이에서 공개될 것을 막기 위해",
                "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 하고, 에피펜을 숨겨 응급처치를 막았다.",
                "피해자의 휴대폰으로 사망 이후 메시지를 보내 생존한 것처럼 위장했다.",
                "범인은 CFO 박재민입니다. 그는 자금 유용 사실이 공개될 위기에 놓이자 피해자의 알레르기를 이용했고, 에피펜을 숨긴 뒤 피해자 계정 메시지로 사망 시각을 위장했습니다.",
                KEY_EVIDENCE_IDS
        );
    }

    private List<ScenarioValidationData.SolutionEvidenceInfo> buildSolutionEvidences() {
        return List.of(
                new ScenarioValidationData.SolutionEvidenceInfo(2L, "피해자의 알레르기와 범행 방법을 설명한다."),
                new ScenarioValidationData.SolutionEvidenceInfo(6L, "박재민이 문제의 음료를 살 수 있었음을 보여준다."),
                new ScenarioValidationData.SolutionEvidenceInfo(7L, "사망 이후 알리바이 조작 정황을 보여준다."),
                new ScenarioValidationData.SolutionEvidenceInfo(8L, "응급처치를 막은 정황을 보여준다."),
                new ScenarioValidationData.SolutionEvidenceInfo(11L, "아몬드라떼 라벨 은폐 정황을 보여준다."),
                new ScenarioValidationData.SolutionEvidenceInfo(12L, "박재민이 에피펜을 숨겼음을 직접 뒷받침한다."),
                new ScenarioValidationData.SolutionEvidenceInfo(13L, "박재민이 데모룸 근처에 있었음을 보여준다."),
                new ScenarioValidationData.SolutionEvidenceInfo(14L, "범행 동기를 설명한다.")
        );
    }

    private List<ScenarioValidationData.ResponsePolicyInfo> buildResponsePolicies() {
        return List.of(
                policy(1L, "DEFAULT", "커피 구매는 인정하지 않고 재무 업무 중이었다고 주장한다.", 0),
                policy(1L, "EVIDENCE_6_UNLOCKED", "커피 구매는 인정하지만 피해자에게 준 것은 아니라고 주장한다.", 10),
                policy(2L, "DEFAULT", "기술 이슈 때문에 서버실에 있었다고 짧게 답한다.", 0),
                policy(3L, "DEFAULT", "회사 밖에 있었다는 알리바이를 강조한다.", 0),
                policy(4L, "DEFAULT", "현장 정리 외에는 모른다고 답한다.", 0),
                policy(5L, "DEFAULT", "투자 갈등은 있었지만 퇴근했다고 답한다.", 0)
        );
    }

    private ScenarioValidationData.ResponsePolicyInfo policy(Long suspectId, String conditionKey,
                                                             String policyText, int priority) {
        return new ScenarioValidationData.ResponsePolicyInfo(suspectId, conditionKey, policyText, priority);
    }

    private List<ScenarioValidationData.SuspectSecretInfo> buildSuspectSecrets() {
        return List.of(
                new ScenarioValidationData.SuspectSecretInfo(1L, "자금 유용", "박재민은 회사 자금을 유용했고 이를 숨기려 했다.", true),
                new ScenarioValidationData.SuspectSecretInfo(2L, "성과 갈등", "이준호는 대표가 기술 성과를 가로챘다고 불만을 품고 있다.", false),
                new ScenarioValidationData.SuspectSecretInfo(3L, "사적 관계", "서유라는 피해자와 과거 연인 관계였다.", false),
                new ScenarioValidationData.SuspectSecretInfo(4L, "책임 전가", "김나은은 대표에게 반복적으로 책임을 떠넘겨진 적이 있다.", false),
                new ScenarioValidationData.SuspectSecretInfo(5L, "투자 조건", "오세훈은 투자 조건을 두고 피해자와 크게 다퉜다.", false)
        );
    }
}
