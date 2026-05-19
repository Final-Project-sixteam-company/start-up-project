package com.startup.domain.ai.support;

import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.enums.QuestionType;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InterrogationLogWriter {

    private final InterrogationLogRepository interrogationLogRepository;

    @Transactional
    public void save(Long playSessionId, Long suspectId, Long presentedEvidenceId,
                     QuestionType questionType, String question, String answer, String aiModel) {
        InterrogationLog logEntry = InterrogationLog.builder()
                .playSessionId(playSessionId)
                .suspectId(suspectId)
                .presentedEvidenceId(presentedEvidenceId)
                .questionType(questionType)
                .question(question)
                .answer(answer)
                .aiModel(aiModel)
                .build();

        interrogationLogRepository.save(logEntry);
    }
}
