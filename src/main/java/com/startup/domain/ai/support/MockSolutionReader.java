package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.SolutionInfo;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockSolutionReader implements SolutionReader {

    @Override
    public SolutionInfo findByScenarioId(Long scenarioId) {
        if (scenarioId != 1L) {
            throw new AiException(AiErrorCode.SOLUTION_NOT_FOUND);
        }

        return new SolutionInfo(
                1L,
                "회사 자금 유용 사실이 데모데이에서 공개될 것을 막기 위해",
                "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 하고, 에피펜을 숨겨 응급처치를 막았다.",
                "피해자의 휴대폰으로 사망 이후 메시지를 보내 생존한 것처럼 위장했다.",
                "범인은 CFO 박재민입니다. 그는 회사 자금 유용 사실이 데모데이 발표에서 공개될 위기에 놓이자, "
                        + "피해자의 견과류 알레르기를 이용해 아몬드라떼를 마시게 했습니다. "
                        + "또한 피해자의 에피펜을 미리 빼돌려 응급처치를 막았고, "
                        + "사망 이후 피해자의 휴대폰으로 단체 채팅방에 메시지를 보내 생존한 것처럼 위장했습니다.",
                List.of(1L, 3L, 5L, 6L, 7L, 8L, 10L, 14L)
        );
    }
}
