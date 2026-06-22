package com.startup.domain.scenario.dto;

import com.startup.domain.scenario.entity.Victim;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CustomVictimResponse {
    private Long victimId;
    private Long scenarioId;
    private Long foundLocationId;
    private String name;
    private Integer age;
    private String role;
    private String description;
    private String causeOfDeath;
    private String foundCondition;
    private LocalDateTime createdAt;

    public static CustomVictimResponse from(Victim victim) {
        if (victim == null) return null;
        return CustomVictimResponse.builder()
                .victimId(victim.getId())
                .scenarioId(victim.getScenarioId())
                .foundLocationId(victim.getFoundLocationId())
                .name(victim.getName())
                .age(victim.getAge())
                .role(victim.getRole())
                .description(victim.getDescription())
                .causeOfDeath(victim.getCauseOfDeath())
                .foundCondition(victim.getFoundCondition())
                .createdAt(victim.getCreatedAt())
                .build();
    }
}
