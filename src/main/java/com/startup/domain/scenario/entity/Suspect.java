package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "suspects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 용의자 공개 프로필. AI 심문 시 공개 정보만 전달한다.
// 범인 여부, 핵심 비밀은 Solution/SuspectSecret으로 관리한다.
public class Suspect extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(length = 100)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String role;

    @Column(name = "character_type", length = 50)
    private String characterType;

    @Column(name = "culprit_eligible", nullable = false)
    private Boolean culpritEligible = true;

    @Column(name = "relation_to_victim", length = 255)
    private String relationToVictim;

    @Column(name = "public_profile", columnDefinition = "TEXT")
    private String publicProfile; //공개 프로필

    @Column(name = "public_statement", columnDefinition = "TEXT")
    private String publicStatement; //공개 진술

    @Column(columnDefinition = "TEXT")
    private String alibi;

    @Column(name = "personality_prompt", columnDefinition = "TEXT")
    private String personalityPrompt; //ai 성격 프롬프트

    @Column(name = "response_policy_json", columnDefinition = "TEXT")
    private String responsePolicyJson; //응답 정책 json

    @Column(name = "portrait_asset_key", length = 500)
    private String portraitAssetKey;

    @Column(name = "suspicion_level", nullable = false)
    private Integer suspicionLevel = 0; //의심도

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder; //표시 순서

    @Builder
    private Suspect(Long scenarioId, String code, String name, String role, String characterType,
                    Boolean culpritEligible, String relationToVictim,
                    String publicProfile, String publicStatement, String alibi,
                    String personalityPrompt, String responsePolicyJson, String portraitAssetKey,
                    Integer suspicionLevel, Integer sortOrder) {
        this.scenarioId = scenarioId;
        this.code = code;
        this.name = name;
        this.role = role;
        this.characterType = characterType;
        this.culpritEligible = culpritEligible != null ? culpritEligible : true;
        this.relationToVictim = relationToVictim;
        this.publicProfile = publicProfile;
        this.publicStatement = publicStatement;
        this.alibi = alibi;
        this.personalityPrompt = personalityPrompt;
        this.responsePolicyJson = responsePolicyJson;
        this.portraitAssetKey = portraitAssetKey;
        this.suspicionLevel = suspicionLevel != null ? suspicionLevel : 0;
        this.sortOrder = sortOrder;
    }
}
