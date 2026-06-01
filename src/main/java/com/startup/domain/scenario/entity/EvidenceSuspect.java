package com.startup.domain.scenario.entity;

import com.startup.domain.scenario.enums.RelationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "evidence_suspects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
// Evidence와 Suspect의 N:M 연결 엔티티. 특정 증거가 어떤 용의자와 관련되는지 표현한다.
public class EvidenceSuspect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evidence_id", nullable = false)
    private Long evidenceId;

    @Column(name = "suspect_id", nullable = false)
    private Long suspectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 50)
    private RelationType relationType;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private EvidenceSuspect(Long evidenceId, Long suspectId, RelationType relationType) {
        this.evidenceId = evidenceId;
        this.suspectId = suspectId;
        this.relationType = relationType != null ? relationType : RelationType.RELATED;
    }
}
