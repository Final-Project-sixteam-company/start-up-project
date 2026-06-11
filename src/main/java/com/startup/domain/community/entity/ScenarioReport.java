package com.startup.domain.community.entity;

import com.startup.domain.community.enums.ReportReason;
import com.startup.domain.community.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "scenario_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_scenario_reports_reporter_scenario",
                        columnNames = {"reporter_id", "scenario_id"}
                )
        },
        indexes = {
                @Index(name = "idx_scenario_reports_scenario", columnList = "scenario_id, status"),
                @Index(name = "idx_scenario_reports_reporter", columnList = "reporter_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ScenarioReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 255)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Builder
    private ScenarioReport(Long scenarioId, Long reporterId, ReportReason reason, String detail) {
        this.scenarioId = scenarioId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.detail = detail;
        this.status = ReportStatus.PENDING;
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }
}
