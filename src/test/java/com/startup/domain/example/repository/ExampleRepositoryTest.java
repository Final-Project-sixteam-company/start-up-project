package com.startup.domain.example.repository;

import com.startup.domain.example.entity.Example;
import com.startup.domain.example.enums.ExampleStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
// Example 엔티티의 @SQLDelete/@SQLRestriction 동작을 실제 Repository 호출로 검증한다.
class ExampleRepositoryTest {

    @Autowired
    private ExampleRepository exampleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void deleteAppliesSoftDeleteRestriction() {
        Example example = exampleRepository.save(Example.builder()
                .title("soft-delete-title")
                .content("soft-delete-content")
                .status(ExampleStatus.ACTIVE)
                .build());

        Long exampleId = example.getId();

        exampleRepository.delete(example);
        exampleRepository.flush();
        entityManager.clear();

        assertThat(exampleRepository.findById(exampleId)).isEmpty();
        assertThat(exampleRepository.existsByTitle("soft-delete-title")).isFalse();

        Example recreated = exampleRepository.save(Example.builder()
                .title("soft-delete-title")
                .content("recreated-content")
                .status(ExampleStatus.DRAFT)
                .build());

        assertThat(recreated.getId()).isNotNull();
    }
}
