package com.startup.domain.ai.service;

import com.startup.common.auth.MockUserProvider;
import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.ai.dto.EvidenceInfo;
import com.startup.domain.ai.dto.InterrogationLogResponse;
import com.startup.domain.ai.dto.InterrogationLogResponse.PresentedEvidenceDto;
import com.startup.domain.ai.dto.SuspectProfile;
import com.startup.domain.ai.entity.InterrogationLog;
import com.startup.domain.ai.repository.InterrogationLogRepository;
import com.startup.domain.ai.support.EvidenceReader;
import com.startup.domain.ai.support.PlaySessionReader;
import com.startup.domain.ai.support.SuspectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterrogationLogQueryService {

    private final MockUserProvider mockUserProvider;
    private final PlaySessionReader playSessionReader;
    private final InterrogationLogRepository interrogationLogRepository;
    private final SuspectReader suspectReader;
    private final EvidenceReader evidenceReader;

    @Transactional(readOnly = true)
    public List<InterrogationLogResponse> list(Long sessionId, Long suspectId) {
        validateSessionOwner(sessionId);

        List<InterrogationLog> logs = (suspectId == null)
                ? interrogationLogRepository.findByPlaySessionIdOrderByCreatedAtAsc(sessionId)
                : interrogationLogRepository.findByPlaySessionIdAndSuspectIdOrderByCreatedAtAsc(sessionId, suspectId);

        if (logs.isEmpty()) {
            return List.of();
        }

        Map<Long, SuspectProfile> suspectMap = buildSuspectMap(logs);
        Map<Long, EvidenceInfo> evidenceMap = buildEvidenceMap(logs);

        return logs.stream()
                .map(log -> toResponse(log, suspectMap, evidenceMap))
                .toList();
    }

    private void validateSessionOwner(Long sessionId) {
        Long currentUserId = mockUserProvider.currentUserId();
        Long ownerUserId = playSessionReader.getOwnerUserId(sessionId);
        if (!Objects.equals(currentUserId, ownerUserId)) {
            throw new BusinessException(CommonErrorCode.ACCESS_DENIED, "Interrogation log access is denied.");
        }
    }

    private Map<Long, SuspectProfile> buildSuspectMap(List<InterrogationLog> logs) {
        Set<Long> suspectIds = logs.stream()
                .map(InterrogationLog::getSuspectId)
                .collect(Collectors.toSet());

        Map<Long, SuspectProfile> map = new HashMap<>();
        for (Long id : suspectIds) {
            map.put(id, suspectReader.findById(id));
        }
        return map;
    }

    private Map<Long, EvidenceInfo> buildEvidenceMap(List<InterrogationLog> logs) {
        Set<Long> evidenceIds = logs.stream()
                .map(InterrogationLog::getPresentedEvidenceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, EvidenceInfo> map = new HashMap<>();
        for (Long id : evidenceIds) {
            map.put(id, evidenceReader.findById(id));
        }
        return map;
    }

    private InterrogationLogResponse toResponse(InterrogationLog log,
                                                Map<Long, SuspectProfile> suspectMap,
                                                Map<Long, EvidenceInfo> evidenceMap) {
        SuspectProfile suspect = suspectMap.get(log.getSuspectId());

        PresentedEvidenceDto presentedEvidence = null;
        if (log.getPresentedEvidenceId() != null) {
            EvidenceInfo evidence = evidenceMap.get(log.getPresentedEvidenceId());
            presentedEvidence = new PresentedEvidenceDto(
                    log.getPresentedEvidenceId(),
                    evidence != null ? evidence.title() : null
            );
        }

        return new InterrogationLogResponse(
                log.getId(),
                log.getSuspectId(),
                suspect.name(),
                log.getQuestionType(),
                log.getQuestion(),
                log.getAnswer(),
                presentedEvidence,
                log.getCreatedAt()
        );
    }
}
