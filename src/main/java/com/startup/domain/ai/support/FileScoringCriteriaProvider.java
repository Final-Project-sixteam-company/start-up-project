package com.startup.domain.ai.support;

import com.startup.domain.ai.dto.ScoringCriteria;
import com.startup.domain.ai.error.AiErrorCode;
import com.startup.domain.ai.error.AiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FileScoringCriteriaProvider implements ScoringCriteriaProvider {

    private final Resource criteriaResource;
    private final JsonMapper jsonMapper;
    private ScoringCriteria criteria;

    public FileScoringCriteriaProvider(
            @Value("${caselab.scoring.criteria-path:classpath:scoring/demo_day_criteria.json}") Resource criteriaResource,
            JsonMapper jsonMapper) {
        this.criteriaResource = criteriaResource;
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    void init() {
        try (InputStream is = criteriaResource.getInputStream()) {
            criteria = jsonMapper.readValue(is, ScoringCriteria.class);
            log.info("채점 기준 로드 완료: scenarioId={}", criteria.scenarioId());
        } catch (IOException e) {
            log.error("채점 기준 파일 로드 실패", e);
        }
    }

    @Override
    public ScoringCriteria getByCriteria(Long scenarioId) {
        if (criteria == null || !criteria.scenarioId().equals(scenarioId)) {
            throw new AiException(AiErrorCode.SCORING_CRITERIA_NOT_FOUND);
        }
        return criteria;
    }
}
