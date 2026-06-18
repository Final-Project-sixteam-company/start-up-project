package com.startup.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// QueryDSL의 JPAQueryFactory를 스프링 빈으로 등록한다.
// Repository에서 QueryDSL을 사용할 때 직접 EntityManager를 주입받지 않아도 된다.
@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
