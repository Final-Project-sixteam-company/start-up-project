package com.startup.domain.scenario.dto;

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
    private String responsePolicyJson; // 응답 정책
    private String portraitAssetKey;
    private Integer suspicionLevel; // 의심도
}
