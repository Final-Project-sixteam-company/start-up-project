package com.startup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @CreatedDate, @LastModifiedDate 같은 JPA Auditing 필드를 전역으로 활성화한다.
@ConfigurationPropertiesScan
@EnableJpaAuditing
@SpringBootApplication
public class StartUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(StartUpApplication.class, args);
    }

}
