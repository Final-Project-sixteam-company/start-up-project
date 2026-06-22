package com.startup.domain.ai.dto;

import java.util.List;

public record InterrogationContext(
        Long scenarioId,
        SuspectProfile suspect,
        List<EvidenceInfo> revealedEvidences,
        EvidenceInfo presentedEvidence,
        ResponsePolicyResult policy,
        List<ChatTurn> history
) {
    public InterrogationContext(SuspectProfile suspect,
                                List<EvidenceInfo> revealedEvidences,
                                EvidenceInfo presentedEvidence,
                                ResponsePolicyResult policy,
                                List<ChatTurn> history) {
        this(null, suspect, revealedEvidences, presentedEvidence, policy, history);
    }
}
