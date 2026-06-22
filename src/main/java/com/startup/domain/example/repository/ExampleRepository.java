package com.startup.domain.example.repository;

import com.startup.domain.example.entity.Example;
import com.startup.domain.example.enums.ExampleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 예제에서는 Spring Data JPA 메서드 네이밍으로 단순 조회와 중복 검사를 표현한다.
public interface ExampleRepository extends JpaRepository<Example, Long> {

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);

    List<Example> findAllByOrderByCreatedAtDesc();

    List<Example> findAllByStatusOrderByCreatedAtDesc(ExampleStatus status);
}
