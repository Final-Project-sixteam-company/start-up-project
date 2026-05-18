package com.startup.domain.ai.service;

import com.startup.domain.ai.enums.QuestionType;

public interface AiInterrogationService {

    String generateResponse(Long sessionId, Long suspectId, String question,
                            QuestionType questionType, Long presentedEvidenceId);
}
