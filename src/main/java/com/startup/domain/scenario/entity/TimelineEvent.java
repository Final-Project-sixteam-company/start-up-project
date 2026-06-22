package com.startup.domain.scenario.entity;

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
@Table(name = "timeline_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "related_suspect_id")
    private Long relatedSuspectId;

    @Column(name = "related_evidence_id")
    private Long relatedEvidenceId;

    @Column(name = "event_time", nullable = false, length = 50)
    private String eventTime;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "is_true_event", nullable = false)
    private Boolean isTrueEvent;

    @Column(nullable = false, length = 30)
    private String visibility;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private TimelineEvent(Long scenarioId,
                          Long relatedSuspectId,
                          Long relatedEvidenceId,
                          String eventTime,
                          Integer eventOrder,
                          String title,
                          String description,
                          String eventType,
                          Boolean isTrueEvent,
                          String visibility) {
        this.scenarioId = scenarioId;
        this.relatedSuspectId = relatedSuspectId;
        this.relatedEvidenceId = relatedEvidenceId;
        this.eventTime = eventTime;
        this.eventOrder = eventOrder;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.isTrueEvent = isTrueEvent;
        this.visibility = visibility;
    }
}
