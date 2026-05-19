package com.startup.domain.ai.support;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.startup.domain.ai.dto.ResponsePolicyResult;
import com.startup.domain.ai.entity.SuspectResponsePolicy;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResponsePolicyResolver {

    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private final SuspectResponsePolicyRepository policyRepository;
    private final JsonMapper jsonMapper;

    public ResponsePolicyResult resolve(Long suspectId,
                                        List<Long> unlockedEvidenceIds,
                                        Long presentedEvidenceId) {
        List<SuspectResponsePolicy> policies = policyRepository.findAllBySuspectId(suspectId);

        return policies.stream()
                .filter(policy -> !"DEFAULT".equals(policy.getConditionKey()))
                .filter(policy -> matchesCondition(policy, unlockedEvidenceIds, presentedEvidenceId))
                .max(Comparator.comparingInt(SuspectResponsePolicy::getPriority))
                .map(this::toResult)
                .orElseGet(() -> findDefaultPolicy(policies));
    }

    private ResponsePolicyResult findDefaultPolicy(List<SuspectResponsePolicy> policies) {
        return policies.stream()
                .filter(policy -> "DEFAULT".equals(policy.getConditionKey()))
                .findFirst()
                .map(this::toResult)
                .orElse(ResponsePolicyResult.hardcodedFallback());
    }

    private boolean matchesCondition(SuspectResponsePolicy policy,
                                     List<Long> unlockedEvidenceIds,
                                     Long presentedEvidenceId) {
        List<Long> required = parseLongList(policy.getRequiredEvidenceIds());
        if (!required.isEmpty() && !unlockedEvidenceIds.containsAll(required)) {
            return false;
        }

        List<Long> excluded = parseLongList(policy.getExcludedEvidenceIds());
        if (!excluded.isEmpty() && excluded.stream().anyMatch(unlockedEvidenceIds::contains)) {
            return false;
        }

        if (policy.getPresentedEvidenceId() != null) {
            if (!Objects.equals(policy.getPresentedEvidenceId(), presentedEvidenceId)) {
                return false;
            }
        }

        return true;
    }

    private ResponsePolicyResult toResult(SuspectResponsePolicy policy) {
        return new ResponsePolicyResult(
                policy.getConditionKey(),
                policy.getPolicyText(),
                parseStringList(policy.getAllowedFacts()),
                parseStringList(policy.getForbiddenFacts()),
                policy.getTone()
        );
    }

    private List<Long> parseLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, LONG_LIST_TYPE);
        } catch (Exception e) {
            log.warn("JSON 파싱 실패 (Long list): {}", json, e);
            return List.of();
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return jsonMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("JSON 파싱 실패 (String list): {}", json, e);
            return List.of();
        }
    }
}
