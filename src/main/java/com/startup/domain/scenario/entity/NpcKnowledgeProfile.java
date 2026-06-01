package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "npc_knowledge_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_npc_knowledge_profiles_suspect", columnNames = {"suspect_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// AI 프롬프트에 넣을 수 있는 prompt-safe NPC 지식 경계만 저장한다. 정답/Variant Truth는 여기에 넣지 않는다.
public class NpcKnowledgeProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "suspect_id", nullable = false)
    private Long suspectId;

    @Column(name = "character_code", nullable = false, length = 100)
    private String characterCode;

    @Column(name = "public_alibi", columnDefinition = "TEXT")
    private String publicAlibi;

    @Column(name = "self_role", columnDefinition = "TEXT")
    private String selfRole;

    @Column(name = "direct_knowledge_json", columnDefinition = "TEXT")
    private String directKnowledgeJson;

    @Column(name = "inferred_knowledge_json", columnDefinition = "TEXT")
    private String inferredKnowledgeJson;

    @Column(name = "forbidden_knowledge_json", columnDefinition = "TEXT")
    private String forbiddenKnowledgeJson;

    @Column(name = "stage_policies_json", columnDefinition = "TEXT")
    private String stagePoliciesJson;

    @Builder
    private NpcKnowledgeProfile(Long scenarioId, Long suspectId, String characterCode,
                                String publicAlibi, String selfRole,
                                String directKnowledgeJson, String inferredKnowledgeJson,
                                String forbiddenKnowledgeJson, String stagePoliciesJson) {
        this.scenarioId = scenarioId;
        this.suspectId = suspectId;
        this.characterCode = characterCode;
        this.publicAlibi = publicAlibi;
        this.selfRole = selfRole;
        this.directKnowledgeJson = directKnowledgeJson;
        this.inferredKnowledgeJson = inferredKnowledgeJson;
        this.forbiddenKnowledgeJson = forbiddenKnowledgeJson;
        this.stagePoliciesJson = stagePoliciesJson;
    }
}
