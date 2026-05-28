package com.startup.domain.play.support;

import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import com.startup.domain.ai.support.SuspectReader;
import com.startup.domain.scenario.repository.SuspectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class DefaultSuspectReader implements SuspectReader {

    private final SuspectRepository suspectRepository;

    @Override
    public SuspectProfile findById(Long suspectId) {
        return suspectRepository.findById(suspectId)
                .map(suspect -> new SuspectProfile(
                        suspect.getId(),
                        suspect.getName(),
                        suspect.getRole(),
                        suspect.getRelationToVictim(),
                        suspect.getPublicProfile(),
                        suspect.getPublicStatement(),
                        suspect.getAlibi()
                ))
                .orElseThrow(() -> new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));
    }

    @Override
    public SuspectProfile findByIdAndScenarioId(Long suspectId, Long scenarioId) {
        return suspectRepository.findByIdAndScenarioId(suspectId, scenarioId)
                .map(suspect -> new SuspectProfile(
                        suspect.getId(),
                        suspect.getName(),
                        suspect.getRole(),
                        suspect.getRelationToVictim(),
                        suspect.getPublicProfile(),
                        suspect.getPublicStatement(),
                        suspect.getAlibi()
                ))
                .orElseThrow(() -> new AiException(AiErrorCode.INTERROGATION_SUSPECT_NOT_FOUND));
    }


}
