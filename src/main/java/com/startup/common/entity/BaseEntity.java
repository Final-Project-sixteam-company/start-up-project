package com.startup.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreRemove;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
// 모든 JPA 엔티티가 공유하는 생성/수정/삭제 시각 필드다.
// soft delete를 쓰는 엔티티는 @SQLDelete와 @SQLRestriction을 함께 선언한다.
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PreRemove
    protected void preRemove() {
        // @SQLDelete 실행 시 현재 영속성 컨텍스트의 엔티티 상태도 삭제 상태로 맞춘다.
        this.deletedAt = LocalDateTime.now();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // AI 검증 우회 등을 막기 위해 자식 리소스 갱신 시 부모의 updatedAt을 강제로 최신화하는 메서드
    public void forceUpdateModifiedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
