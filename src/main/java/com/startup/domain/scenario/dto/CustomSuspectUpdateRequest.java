package com.startup.domain.scenario.dto;

import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomSuspectUpdateRequest {

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
}
