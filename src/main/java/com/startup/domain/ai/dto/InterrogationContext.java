package com.startup.domain.ai.dto;

import java.util.List;

public record InterrogationContext(
        SuspectProfile suspect,
        List<EvidenceInfo> revealedEvidences,
        EvidenceInfo presentedEvidence,
        ResponsePolicyResult policy,
        List<ChatTurn> history
) {
}
