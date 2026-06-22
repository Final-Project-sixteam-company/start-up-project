package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Evidence;
import com.startup.domain.scenario.enums.EvidenceImportance;
import com.startup.domain.scenario.enums.EvidenceType;
import com.startup.domain.scenario.enums.EvidenceUnlockType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomEvidenceResponse {
    private Long evidenceId;
    private Long scenarioId;
    private String title;
    private String description;
    private String oneLine;
    private Long locationId;
    private String locationName;
    private EvidenceType evidenceType;
    private EvidenceImportance importance;
    private String imageUrl;
    private String imageAssetKey;
    private String thumbnailAssetKey;
    private JsonNode tagsJson;
    private String unlockPhase;
    private Boolean isInitialPublic;
    private EvidenceUnlockType unlockType;
    private JsonNode unlockConditionJson;
    private Integer unlockAfterMinutes;
    private Integer sortOrder;
    private List<RelatedSuspectDto> relatedSuspects;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class RelatedSuspectDto {
        private Long suspectId;
        private String name;
    }

    public static CustomEvidenceResponse from(
            Evidence evidence, 
            String locationName, 
            List<RelatedSuspectDto> relatedSuspects, 
            JsonMapper jsonMapper) {
            
        JsonNode parsedTagsJson = null;
        if (evidence.getTagsJson() != null && !evidence.getTagsJson().isEmpty()) {
            try {
                parsedTagsJson = jsonMapper.readTree(evidence.getTagsJson());
            } catch (Exception e) {}
        }

        JsonNode parsedUnlockConditionJson = null;
        if (evidence.getUnlockConditionJson() != null && !evidence.getUnlockConditionJson().isEmpty()) {
            try {
                parsedUnlockConditionJson = jsonMapper.readTree(evidence.getUnlockConditionJson());
            } catch (Exception e) {}
        }

        return CustomEvidenceResponse.builder()
                .evidenceId(evidence.getId())
                .scenarioId(evidence.getScenarioId())
                .title(evidence.getTitle())
                .description(evidence.getDescription())
                .oneLine(evidence.getOneLine())
                .locationId(evidence.getLocationId())
                .locationName(locationName)
                .evidenceType(evidence.getEvidenceType())
                .importance(evidence.getImportance())
                .imageUrl(evidence.getImageUrl())
                .imageAssetKey(evidence.getImageAssetKey())
                .thumbnailAssetKey(evidence.getThumbnailAssetKey())
                .tagsJson(parsedTagsJson)
                .unlockPhase(evidence.getUnlockPhase())
                .isInitialPublic(evidence.getIsInitialPublic())
                .unlockType(evidence.getUnlockType())
                .unlockConditionJson(parsedUnlockConditionJson)
                .unlockAfterMinutes(evidence.getUnlockAfterMinutes())
                .sortOrder(evidence.getSortOrder())
                .relatedSuspects(relatedSuspects)
                .build();
    }
}
