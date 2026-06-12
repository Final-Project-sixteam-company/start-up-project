package com.startup.domain.community.entity;

import com.startup.domain.auth.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "scenario_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_scenario_reviews_user_scenario",
                        columnNames = {"user_id", "scenario_id"}
                )
        },
        indexes = {
                @Index(name = "idx_scenario_reviews_scenario", columnList = "scenario_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ScenarioReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_spoiler", nullable = false)
    private boolean isSpoiler;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private ScenarioReview(Long scenarioId, User user, int rating, String content, boolean isSpoiler) {
        this.scenarioId = scenarioId;
        this.user = user;
        this.rating = rating;
        this.content = content;
        this.isSpoiler = isSpoiler;
    }

    public void updateReview(Integer rating, String content, Boolean isSpoiler) {
        if (rating != null) this.rating = rating;
        if (content != null) this.content = content;
        if (isSpoiler != null) this.isSpoiler = isSpoiler;
    }
}
