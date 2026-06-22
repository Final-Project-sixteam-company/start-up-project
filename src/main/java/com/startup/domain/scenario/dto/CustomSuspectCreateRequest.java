package com.startup.domain.scenario.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomSuspectCreateRequest {

    @NotBlank(message = "용의자 이름은 필수입니다.")
    private String name;

    private String role;
    private String characterType;
    private Boolean culpritEligible;
    private String relationToVictim;
    private String publicProfile;
    private String publicStatement;
    private String alibi;
    private String personalityPrompt;
    private JsonNode responsePolicyJson; // 응답 정책 (구조화된 JSON 객체 수용)
    private String portraitAssetKey;
    private Integer suspicionLevel; // 의심도
}
