package com.startup.domain.scenario.service;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import com.startup.domain.scenario.dto.*;
import com.startup.domain.scenario.entity.*;
import com.startup.domain.ai.entity.SuspectResponsePolicy;
import com.startup.domain.ai.repository.SuspectResponsePolicyRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.startup.domain.scenario.enums.ScenarioStatus;
import com.startup.domain.scenario.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioLocationRepository locationRepository;
    private final ScenarioAccessService scenarioAccessService;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSuspectRepository evidenceSuspectRepository;
    private final HintRepository hintRepository;
    private final SolutionRepository solutionRepository;
    private final SuspectResponsePolicyRepository suspectResponsePolicyRepository;
    private final EvidenceUnlockRuleRepository evidenceUnlockRuleRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public CustomLocationCreateResponse createLocation(Long userId, Long scenarioId, CustomLocationCreateRequest request) {
        // 작성자 본인인지 확인
        scenarioAccessService.validateEditable(userId, scenarioId);

        // 동시에 장소를 추가하더라도 Race Condition 차단
        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 이미 발행된(PUBLISHED) 시나리오에는 더 이상 장소 추가 불가
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // DB Lock이 걸려있으므로 여러 트랜잭션이 중복된 숫자를 가져갈 수 없음
        Integer maxSortOrder = locationRepository.findMaxSortOrderByScenarioId(scenarioId);
        int nextSortOrder = maxSortOrder + 1;

        // 엔티티 생성
        ScenarioLocation location = ScenarioLocation.builder()
                .scenarioId(scenarioId)
                .name(request.getName())
                .description(request.getDescription())
                .floor(request.getFloor())
                .mapX(request.getMapX())
                .mapY(request.getMapY())
                .imageAssetKey(request.getImageAssetKey())
                .sortOrder(nextSortOrder)
                .build();

        ScenarioLocation savedLocation = locationRepository.save(location);

        // 부모 시나리오의 updatedAt 강제 갱신 (AI 검증 결과 우회 차단)
        scenario.forceUpdateModifiedAt();

        // 생성된 자식 ID만 DTO로 매핑하여 반환
        return new CustomLocationCreateResponse(savedLocation.getId());
    }

    // 상단에 private final VictimRepository victimRepository; 추가 필요

    @Transactional
    public CustomVictimCreateResponse createOrUpdateVictim(Long userId, Long scenarioId, CustomVictimCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 상태 방어 (발행된 시나리오는 수정 불가)
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // 입력받은 발견 장소가 현재 시나리오 소속인지 검사
        if (request.getFoundLocationId() != null) {
            ScenarioLocation location = locationRepository.findById(request.getFoundLocationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다."));
            if (!location.getScenarioId().equals(scenarioId)) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "다른 시나리오의 장소를 피해자 발견 위치로 지정할 수 없습니다.");
            }
        }

        // UPSERT 분기 (이미 있으면 덮어쓰기, 없으면 새로 생성)
        Victim savedVictim;
        Optional<Victim> existingVictim = victimRepository.findByScenarioId(scenarioId);

        if (existingVictim.isPresent()) {
            Victim victim = existingVictim.get();
            victim.updateInfo(
                    request.getFoundLocationId(), request.getName(), request.getAge(),
                    request.getRole(), request.getDescription(), request.getCauseOfDeath(),
                    request.getFoundCondition()
            );
            savedVictim = victim;
        } else {
            Victim newVictim = Victim.builder()
                    .scenarioId(scenarioId)
                    .foundLocationId(request.getFoundLocationId())
                    .name(request.getName())
                    .age(request.getAge())
                    .role(request.getRole())
                    .description(request.getDescription())
                    .causeOfDeath(request.getCauseOfDeath())
                    .foundCondition(request.getFoundCondition())
                    .build();
            savedVictim = victimRepository.save(newVictim);
        }

        // 부모 시나리오의 updatedAt 강제 갱신 -> AI 논리 검증 우회 전면 차단
        scenario.forceUpdateModifiedAt();

        return new CustomVictimCreateResponse(savedVictim.getId());
    }

    @Transactional
    public CustomSuspectCreateResponse createSuspect(Long userId, Long scenarioId, CustomSuspectCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 발행된 시나리오는 수정 불가
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        Integer maxSortOrder = suspectRepository.findMaxSortOrderByScenarioId(scenarioId);
        int nextSortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 1;

        String generatedCode = "SUSPECT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 엔티티 생성 및 저장
        Suspect newSuspect = Suspect.builder()
                .scenarioId(scenarioId)
                .code(generatedCode)
                .name(request.getName())
                .role(request.getRole())
                .characterType(request.getCharacterType())
                .culpritEligible(request.getCulpritEligible() != null ? request.getCulpritEligible() : true)
                .relationToVictim(request.getRelationToVictim())
                .publicProfile(request.getPublicProfile())
                .publicStatement(request.getPublicStatement())
                .alibi(request.getAlibi())
                .personalityPrompt(request.getPersonalityPrompt())
                .responsePolicyJson(request.getResponsePolicyJson() != null ? request.getResponsePolicyJson().toString() : null)
                .portraitAssetKey(request.getPortraitAssetKey())
                .suspicionLevel(request.getSuspicionLevel() != null ? request.getSuspicionLevel() : 0) // 제공 안되면 0
                .sortOrder(nextSortOrder)
                .build();

        Suspect savedSuspect = suspectRepository.save(newSuspect);

        if (request.getResponsePolicyJson() != null) {
            JsonNode policyNode = request.getResponsePolicyJson();
            if (policyNode.isArray()) {
                for (JsonNode node : policyNode) {
                    validatePolicyEvidenceIds(scenarioId, node);
                    saveSuspectResponsePolicy(savedSuspect.getId(), node);
                }
            } else {
                validatePolicyEvidenceIds(scenarioId, policyNode);
                saveSuspectResponsePolicy(savedSuspect.getId(), policyNode);
            }
        }

        // 부모 시나리오의 updatedAt 강제 갱신
        scenario.forceUpdateModifiedAt();

        return new CustomSuspectCreateResponse(savedSuspect.getId());
    }


    @Transactional
    public CustomEvidenceCreateResponse createEvidence(Long userId, Long scenarioId, CustomEvidenceCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        // 발행된 시나리오 수정 금지
        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // 넘겨받은 장소 ID가 다른 시나리오의 장소가 아닌지 검증
        if (request.getLocationId() != null) {
            ScenarioLocation location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다."));
            if (!location.getScenarioId().equals(scenarioId)) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "다른 시나리오의 장소를 증거 위치로 지정할 수 없습니다.");
            }
        }

        // 관련 용의자로 1, 2, 3번을 넘겼을 때, 셋 중 하나라도 다른 시나리오 용의자라면 해킹 시도로 간주하고 튕겨냄
        if (request.getRelatedSuspectIds() != null && !request.getRelatedSuspectIds().isEmpty()) {
            List<Long> uniqueSuspectIds = request.getRelatedSuspectIds().stream().distinct().toList();
            int validCount = suspectRepository.findAllByIdInAndScenarioId(uniqueSuspectIds, scenarioId).size();
            if (validCount != uniqueSuspectIds.size()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "현재 시나리오에 소속되지 않은 용의자가 포함되어 있습니다.");
            }
        }

        Integer maxSortOrder = evidenceRepository.findMaxSortOrderByScenarioId(scenarioId);
        int nextSortOrder = (maxSortOrder == null ? 0 : maxSortOrder) + 1;

        String generatedCode = "EVIDENCE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Evidence evidence = Evidence.builder()
                .scenarioId(scenarioId)
                .code(generatedCode)
                .locationId(request.getLocationId())
                .title(request.getTitle())
                .description(request.getDescription())
                .oneLine(request.getOneLine())
                .evidenceType(request.getEvidenceType())
                .importance(request.getImportance())
                .imageUrl(request.getImageUrl())
                .imageAssetKey(request.getImageAssetKey())
                .thumbnailAssetKey(request.getThumbnailAssetKey())
                .tagsJson(request.getTagsJson())
                .unlockPhase(request.getUnlockPhase())
                .isInitialPublic(request.getIsInitialPublic())
                .unlockType(request.getUnlockType())
                .unlockConditionJson(request.getUnlockConditionJson())
                .unlockAfterMinutes(request.getUnlockAfterMinutes())
                .sortOrder(nextSortOrder)
                .build();

        Evidence savedEvidence = evidenceRepository.save(evidence);

        // 관련 용의자 매핑 정보 다중 저장 (EvidenceSuspect)
        if (request.getRelatedSuspectIds() != null && !request.getRelatedSuspectIds().isEmpty()) {
            List<EvidenceSuspect> evidenceSuspects = request.getRelatedSuspectIds().stream()
                    .distinct() // 중복 입력 방어
                    .map(suspectId -> EvidenceSuspect.builder()
                            .evidenceId(savedEvidence.getId())
                            .suspectId(suspectId)
                            .relationType(com.startup.domain.scenario.enums.RelationType.RELATED) // 기본 연관 관계
                            .build())
                    .toList();
            evidenceSuspectRepository.saveAll(evidenceSuspects);
        }

        // 언락 조건 규칙 보존 (NONE이 아니면 저장)
        if (request.getUnlockType() != null && request.getUnlockType() != com.startup.domain.scenario.enums.EvidenceUnlockType.NONE) {
            String processedConditionJson = request.getUnlockConditionJson();
            if (request.getUnlockType() == com.startup.domain.scenario.enums.EvidenceUnlockType.EVIDENCE_PRESENTED) {
                processedConditionJson = validateAndTranslateEvidencePresentedCondition(scenarioId, processedConditionJson);
            }

            EvidenceUnlockRule rule = EvidenceUnlockRule.builder()
                    .scenarioId(scenarioId)
                    .evidenceId(savedEvidence.getId())
                    .evidenceCode(savedEvidence.getCode()) // 생성된 증거 코드 사용
                    .unlockType(request.getUnlockType().name())
                    .requiredPhase(request.getUnlockPhase())
                    .conditionJson(processedConditionJson)
                    .sortOrder(nextSortOrder)
                    .build();
            evidenceUnlockRuleRepository.save(rule);
        }

        // 부모 시나리오 강제 갱신 -> 검증 우회 전면 방어
        scenario.forceUpdateModifiedAt();

        return new CustomEvidenceCreateResponse(savedEvidence.getId());
    }

    private String validateAndTranslateEvidencePresentedCondition(Long scenarioId, String conditionJson) {
        if (conditionJson == null || conditionJson.isBlank()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "EVIDENCE_PRESENTED 조건은 필수입니다.");
        }
        try {
            ObjectNode root = (ObjectNode) jsonMapper.readTree(conditionJson);
            
            Long presentedEvidenceId = root.has("requiredPresentedEvidenceId") ? root.get("requiredPresentedEvidenceId").asLong() : 
                                       (root.has("evidenceId") ? root.get("evidenceId").asLong() : null);
            if (presentedEvidenceId == null) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "제시 대상 증거 ID(requiredPresentedEvidenceId)가 누락되었습니다.");
            }
            Evidence presented = evidenceRepository.findById(presentedEvidenceId)
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "제시 대상 증거를 찾을 수 없습니다."));
            if (!scenarioId.equals(presented.getScenarioId())) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "제시 대상 증거가 현재 시나리오 소속이 아닙니다.");
            }
            root.put("requiredPresentedEvidenceCode", presented.getCode());
            
            if (root.has("requiredCharacterId") && !root.get("requiredCharacterId").isNull()) {
                Long characterId = root.get("requiredCharacterId").asLong();
                Suspect suspect = suspectRepository.findById(characterId)
                        .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "대화 대상 용의자를 찾을 수 없습니다."));
                if (!scenarioId.equals(suspect.getScenarioId())) {
                    throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "대화 대상 용의자가 현재 시나리오 소속이 아닙니다.");
                }
                root.put("requiredCharacterCode", suspect.getCode());
            }

            if (root.has("requiredEvidenceIds") && !root.get("requiredEvidenceIds").isNull()) {
                List<String> codes = new ArrayList<>();
                for (JsonNode idNode : root.get("requiredEvidenceIds")) {
                    Long prerequisiteId = idNode.asLong();
                    Evidence prerequisite = evidenceRepository.findById(prerequisiteId)
                            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "선행 해금 증거를 찾을 수 없습니다."));
                    if (!scenarioId.equals(prerequisite.getScenarioId())) {
                        throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "선행 해금 증거가 현재 시나리오 소속이 아닙니다.");
                    }
                    codes.add(prerequisite.getCode());
                }
                ArrayNode codesNode = jsonMapper.createArrayNode();
                codes.forEach(codesNode::add);
                root.set("requiredEvidenceCodes", codesNode);
            }

            return jsonMapper.writeValueAsString(root);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "unlockConditionJson 파싱 실패: " + e.getMessage());
        }
    }

    @Transactional
    public CustomHintCreateResponse createHint(Long userId, Long scenarioId, CustomHintCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        Integer maxHintLevel = hintRepository.findMaxHintLevelByScenarioId(scenarioId);
        int nextHintLevel = maxHintLevel + 1;

        Hint hint = Hint.builder()
                .scenarioId(scenarioId)
                .hintLevel(nextHintLevel)
                .content(request.getContent())
                .unlockAfterMinutes(request.getUnlockAfterMinutes())
                .penaltyScore(request.getPenaltyScore())
                .build();

        Hint savedHint = hintRepository.save(hint);

        scenario.forceUpdateModifiedAt();

        return new CustomHintCreateResponse(savedHint.getId());
    }

    @Transactional
    public CustomSolutionCreateResponse createOrUpdateSolution(Long userId, Long scenarioId, CustomSolutionCreateRequest request) {
        scenarioAccessService.validateEditable(userId, scenarioId);

        Scenario scenario = scenarioRepository.findByIdForUpdate(scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND, "시나리오를 찾을 수 없습니다."));

        if (scenario.getStatus() == ScenarioStatus.PUBLISHED) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이미 발행된 시나리오는 수정할 수 없습니다.");
        }

        // 진범 용의자가 이 시나리오에 소속되어 있는지 검증
        Suspect culprit = suspectRepository.findByIdAndScenarioId(request.getCulpritSuspectId(), scenarioId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "해당 용의자는 이 시나리오 소속이 아닙니다."));
        
        if (!culprit.getCulpritEligible()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "이 용의자는 범인으로 지목될 수 없습니다.");
        }

        String keyEvidenceStr = "";
        if (request.getKeyEvidenceIds() != null && !request.getKeyEvidenceIds().isEmpty()) {
            long validCount = evidenceRepository.countByIdInAndScenarioId(request.getKeyEvidenceIds(), scenarioId);
            if (validCount != request.getKeyEvidenceIds().size()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "일부 증거가 존재하지 않거나 이 시나리오 소속이 아닙니다.");
            }
            keyEvidenceStr = String.join(",", request.getKeyEvidenceIds().stream().map(String::valueOf).toList());
        }

        // UPSERT 분기
        Solution savedSolution;
        Optional<Solution> existingSolution = solutionRepository.findByScenarioId(scenarioId);

        if (existingSolution.isPresent()) {
            Solution solution = existingSolution.get();
            solution.updateInfo(
                    request.getCulpritSuspectId(),
                    request.getMotive(),
                    request.getMethod(),
                    request.getCoverUp(),
                    request.getFullExplanation(),
                    keyEvidenceStr
            );
            savedSolution = solution;
        } else {
            Solution newSolution = Solution.builder()
                    .scenarioId(scenarioId)
                    .culpritSuspectId(request.getCulpritSuspectId())
                    .motive(request.getMotive())
                    .method(request.getMethod())
                    .coverUp(request.getCoverUp())
                    .fullExplanation(request.getFullExplanation())
                    .keyEvidenceIds(keyEvidenceStr)
                    .build();
            savedSolution = solutionRepository.save(newSolution);
        }

        // 부모 시나리오 updatedAt 갱신
        scenario.forceUpdateModifiedAt();

        return new CustomSolutionCreateResponse(savedSolution.getId());
    }

    private void saveSuspectResponsePolicy(Long suspectId, JsonNode node) {
        String basePolicy = node.has("policyText") && !node.get("policyText").isNull() ? node.get("policyText").asText() : "";
        StringBuilder policyBuilder = new StringBuilder(basePolicy);

        Integer maxSentences = node.has("maxSentences") && !node.get("maxSentences").isNull() ? node.get("maxSentences").asInt() : null;
        if (maxSentences != null) {
            if (policyBuilder.length() > 0) policyBuilder.append(" ");
            policyBuilder.append("답변은 최대 ").append(maxSentences).append("문장으로 제한한다.");
        }

        Boolean allowExternalFacts = node.has("allowExternalFacts") && !node.get("allowExternalFacts").isNull() ? node.get("allowExternalFacts").asBoolean() : null;
        if (allowExternalFacts != null && !allowExternalFacts) {
            if (policyBuilder.length() > 0) policyBuilder.append(" ");
            policyBuilder.append("설정에 없는 외부 사실을 임의로 지어내지 않는다.");
        }

        String defaultStance = node.has("defaultStance") && !node.get("defaultStance").isNull() ? node.get("defaultStance").asText() : null;
        if (defaultStance != null && basePolicy.isEmpty()) {
            if (policyBuilder.length() > 0) policyBuilder.append(" ");
            policyBuilder.append("기본 태도: ").append(defaultStance).append(".");
        }

        String finalPolicyText = policyBuilder.length() > 0 ? policyBuilder.toString().trim() : "기본 응답";
        String tone = node.has("tone") && !node.get("tone").isNull() ? node.get("tone").asText() : defaultStance;

        SuspectResponsePolicy policy = SuspectResponsePolicy.builder()
                .suspectId(suspectId)
                .conditionKey(node.has("conditionKey") ? node.get("conditionKey").asText() : "DEFAULT")
                .userIntent(node.has("userIntent") && !node.get("userIntent").isNull() ? node.get("userIntent").asText() : null)
                .requiredEvidenceIds(node.has("requiredEvidenceIds") && !node.get("requiredEvidenceIds").isNull() ? node.get("requiredEvidenceIds").toString() : null)
                .excludedEvidenceIds(node.has("excludedEvidenceIds") && !node.get("excludedEvidenceIds").isNull() ? node.get("excludedEvidenceIds").toString() : null)
                .presentedEvidenceId(node.has("presentedEvidenceId") && !node.get("presentedEvidenceId").isNull() ? node.get("presentedEvidenceId").asLong() : null)
                .policyText(finalPolicyText)
                .allowedFacts(node.has("allowedFacts") && !node.get("allowedFacts").isNull() ? node.get("allowedFacts").toString() : null)
                .forbiddenFacts(node.has("forbiddenFacts") && !node.get("forbiddenFacts").isNull() ? node.get("forbiddenFacts").toString() : null)
                .tone(tone)
                .priority(node.has("priority") && !node.get("priority").isNull() ? node.get("priority").asInt() : 0)
                .build();
        suspectResponsePolicyRepository.save(policy);
    }

    private void validatePolicyEvidenceIds(Long scenarioId, JsonNode node) {
        validateEvidenceArray(scenarioId, node.get("requiredEvidenceIds"));
        validateEvidenceArray(scenarioId, node.get("excludedEvidenceIds"));

        String conditionKey = node.has("conditionKey") && !node.get("conditionKey").isNull() ? node.get("conditionKey").asText() : "DEFAULT";

        boolean hasGates = false;

        JsonNode required = node.get("requiredEvidenceIds");
        if (required != null && !required.isNull() && required.isArray() && !required.isEmpty()) {
            hasGates = true;
        }

        JsonNode excluded = node.get("excludedEvidenceIds");
        if (excluded != null && !excluded.isNull() && excluded.isArray() && !excluded.isEmpty()) {
            hasGates = true;
        }

        JsonNode presented = node.get("presentedEvidenceId");
        if (presented != null && !presented.isNull()) {
            hasGates = true;
            if (!presented.isNumber()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "제시된 증거 ID는 숫자여야 합니다.");
            }
            long validCount = evidenceRepository.countByIdInAndScenarioId(java.util.List.of(presented.asLong()), scenarioId);
            if (validCount != 1) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "정책에 포함된 증거가 존재하지 않거나 이 시나리오 소속이 아닙니다.");
            }
        }

        if ("DEFAULT".equals(conditionKey) && hasGates) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "DEFAULT 상태인 정책에는 증거 조건(해금/제시 등)을 설정할 수 없습니다. 별도의 conditionKey를 지정해주세요.");
        }
    }

    private void validateEvidenceArray(Long scenarioId, JsonNode arrayNode) {
        if (arrayNode == null || arrayNode.isNull()) {
            return;
        }
        if (!arrayNode.isArray()) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "정책 증거 ID 목록은 JSON 배열 형태여야 합니다.");
        }

        List<Long> evidenceIds = new ArrayList<>();
        for (JsonNode element : arrayNode) {
            if (!element.isNumber()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "정책 증거 ID는 숫자여야 합니다.");
            }
            evidenceIds.add(element.asLong());
        }

        if (!evidenceIds.isEmpty()) {
            long validCount = evidenceRepository.countByIdInAndScenarioId(evidenceIds, scenarioId);
            if (validCount != evidenceIds.size()) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "정책에 포함된 증거가 존재하지 않거나 이 시나리오 소속이 아닙니다.");
            }
        }
    }
}
