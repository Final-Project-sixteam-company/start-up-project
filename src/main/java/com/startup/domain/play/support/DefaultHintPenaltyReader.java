package com.startup.domain.play.support;

import com.startup.domain.ai.support.HintPenaltyReader;
import com.startup.domain.play.entity.UsedHint;
import com.startup.domain.play.repository.UsedHintRepository;
import com.startup.domain.scenario.repository.HintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
// UsedHint에 기록된 힌트 ID로 Hint.penaltyScore를 합산하여 반환한다.
// MockHintPenaltyReader의 실제 구현체.
public class DefaultHintPenaltyReader implements HintPenaltyReader {

    private final UsedHintRepository usedHintRepository;
    private final HintRepository hintRepository;

    @Override
    @Transactional(readOnly = true)
    public int getTotalPenalty(Long sessionId) {
        List<UsedHint> usedHints = usedHintRepository.findAllByPlaySessionId(sessionId);

        if (usedHints.isEmpty()) {
            return 0;
        }

        List<Long> hintIds = usedHints.stream()
                .map(UsedHint::getHintId)
                .toList();

        int totalPenalty = hintRepository.findAllById(hintIds)
                .stream()
                .mapToInt(hint -> hint.getPenaltyScore())
                .sum();

        log.debug("[HintPenaltyReader] sessionId={}, 사용 힌트 수={}, 총 패널티={}", sessionId, hintIds.size(), totalPenalty);
        return totalPenalty;
    }
}
