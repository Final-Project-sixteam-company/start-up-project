package com.startup.domain.scenario.entity;

import com.startup.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "solution_evidences",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_solution_evidences_pair", columnNames = {"solution_id", "evidence_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SolutionEvidence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solution_id", nullable = false)
    private Solution solution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @Column(columnDefinition = "TEXT")
    private String reason;

    public SolutionEvidence(Solution solution, Evidence evidence, String reason) {
        this.solution = solution;
        this.evidence = evidence;
        this.reason = reason;
    }
}
