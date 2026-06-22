package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CustomEvidenceUpdateRequest {

    private String title;
    private String description;
    private String oneLine;
    private Long locationId;
    private EvidenceType evidenceType;
    private EvidenceImportance importance;
    private String imageUrl;
    private String imageAssetKey;
    private String thumbnailAssetKey;
    private String tagsJson;
    private String unlockPhase;
    private Boolean isInitialPublic;
    private EvidenceUnlockType unlockType;
    private String unlockConditionJson;
    private Integer unlockAfterMinutes;
    private Integer sortOrder;
    
    // (Ownership 검증 대상) 증거와 관련된 용의자 ID 리스트
    private List<Long> relatedSuspectIds;
}
