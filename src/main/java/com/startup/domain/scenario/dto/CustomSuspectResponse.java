package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Suspect;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomSuspectResponse {
    private Long suspectId;
    private Long scenarioId;
    private String name;
    private String role;
    private String characterType;
    private Boolean culpritEligible;
    private String relationToVictim;
    private String publicProfile;
    private String publicStatement;
    private String alibi;
    private String personalityPrompt;
    private JsonNode responsePolicyJson;
    private String portraitAssetKey;
    private Integer suspicionLevel;
    private Integer sortOrder;

    public static CustomSuspectResponse from(Suspect suspect, JsonMapper jsonMapper) {
        if (suspect == null) return null;
        
        JsonNode policyNode = null;
        if (suspect.getResponsePolicyJson() != null && !suspect.getResponsePolicyJson().isEmpty()) {
            try {
                policyNode = jsonMapper.readTree(suspect.getResponsePolicyJson());
            } catch (Exception e) {
                // 파싱 실패 시 무시하거나 빈 객체 처리
            }
        }

        return CustomSuspectResponse.builder()
                .suspectId(suspect.getId())
                .scenarioId(suspect.getScenarioId())
                .name(suspect.getName())
                .role(suspect.getRole())
                .characterType(suspect.getCharacterType())
                .culpritEligible(suspect.getCulpritEligible())
                .relationToVictim(suspect.getRelationToVictim())
                .publicProfile(suspect.getPublicProfile())
                .publicStatement(suspect.getPublicStatement())
                .alibi(suspect.getAlibi())
                .personalityPrompt(suspect.getPersonalityPrompt())
                .responsePolicyJson(policyNode)
                .portraitAssetKey(suspect.getPortraitAssetKey())
                .suspicionLevel(suspect.getSuspicionLevel())
                .sortOrder(suspect.getSortOrder())
                .build();
    }
}
