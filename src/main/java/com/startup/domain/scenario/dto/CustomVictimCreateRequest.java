package com.startup.domain.scenario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CustomVictimCreateRequest {

    @NotBlank(message = "피해자 이름은 필수입니다.")
    private String name;

    private Integer age;
    private String role;
    private String description;
    private String causeOfDeath;
    private String foundCondition;
    private Long foundLocationId;
}
