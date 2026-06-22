package com.startup.domain.example.entity;

import com.startup.common.entity.BaseEntity;
import com.startup.domain.example.enums.ExampleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "examples")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE examples SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
// CRUD, enum 상태, soft delete 사용 방식을 보여주는 팀 컨벤션 예제 엔티티다.
public class Example extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExampleStatus status;

    @Builder
    private Example(String title, String content, ExampleStatus status) {
        // 생성 API에서 status를 생략하면 임시 저장 상태로 시작한다.
        this.title = title;
        this.content = content;
        this.status = status == null ? ExampleStatus.DRAFT : status;
    }

    public void update(String title, String content, ExampleStatus status) {
        this.title = title;
        this.content = content;
        this.status = status;
    }

    public void changeStatus(ExampleStatus status) {
        this.status = status;
    }
}
