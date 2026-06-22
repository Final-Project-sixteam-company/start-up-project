package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MockSolutionReader implements SolutionReader {

    // OFFICIAL_SCENARIO_DEMO_DAY.md 섹션 15.3 INSERT 순서 기준:
    // 2=ev_allergy, 6=ev_park_card_payment, 7=ev_park_message,
    // 8=ev_missing_epipen, 11=ev_label_piece, 12=ev_park_drawer_epipen,
    // 13=ev_phone_location, 14=ev_accounting_file
    private static final List<Long> KEY_EVIDENCE_IDS = List.of(2L, 6L, 7L, 8L, 11L, 12L, 13L, 14L);

    private static final Map<Long, String> EVIDENCE_TITLES = Map.of(
            2L, "피해자 알레르기 정보",
            6L, "박재민 법인카드 결제 내역",
            7L, "박재민이 보낸 단톡 메시지",
            8L, "사라진 에피펜",
            11L, "찢긴 컵 라벨",
            12L, "박재민 서랍 속 에피펜",
            13L, "박재민 휴대폰 위치 기록",
            14L, "암호화된 회계 파일"
    );

    @Override
    public SolutionInfo findByScenarioId(Long scenarioId) {
        return findByScenarioIdAndVariantId(scenarioId, null);
    }

    @Override
    public SolutionInfo findByScenarioIdAndVariantId(Long scenarioId, Long variantId) {
        if (scenarioId != 1L) {
            throw new AiException(AiErrorCode.SOLUTION_NOT_FOUND);
        }

        return new SolutionInfo(
                1L,
                "박재민",
                "CFO",
                "회사 자금 유용 사실이 데모데이에서 공개될 것을 막기 위해",
                "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 하고, 에피펜을 숨겨 응급처치를 막았다.",
                "피해자의 휴대폰으로 사망 이후 메시지를 보내 생존한 것처럼 위장했다.",
                "범인은 CFO 박재민입니다. 그는 회사 자금 유용 사실이 데모데이 발표에서 공개될 위기에 놓이자, "
                        + "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 했습니다. "
                        + "또한 피해자의 에피펜을 미리 빼돌려 응급처치를 막았고, "
                        + "사망 이후 피해자의 휴대폰으로 단체 채팅방에 메시지를 보내 생존한 것처럼 위장했습니다.",
                KEY_EVIDENCE_IDS,
                EVIDENCE_TITLES
        );
    }
}
