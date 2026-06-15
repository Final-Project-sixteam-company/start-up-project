package com.startup.domain.scenario.support;

import com.startup.common.error.BusinessException;
import com.startup.common.error.CommonErrorCode;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConditionJsonParser {

    private final JsonMapper jsonMapper;

    public ConditionJsonParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public boolean hasRequiredCharacterCode(String conditionJson, String characterCode) {
        if (conditionJson == null || conditionJson.isBlank()) return false;
        try {
            JsonNode root = jsonMapper.readTree(conditionJson);
            if (root.has("requiredCharacterCode") && !root.get("requiredCharacterCode").isNull()) {
                return characterCode.equals(root.get("requiredCharacterCode").asText());
            }
        } catch (Exception e) {
            // 파싱 실패 시 혹시 모를 의존성을 위해 안전하게 true 반환 (fail-closed)
            return true;
        }
        return false;
    }

    public boolean hasRequiredPresentedEvidenceCodeOrEvidenceCodes(String conditionJson, String evidenceCode) {
        if (conditionJson == null || conditionJson.isBlank()) return false;
        try {
            JsonNode root = jsonMapper.readTree(conditionJson);
            if (root.has("requiredPresentedEvidenceCode") && !root.get("requiredPresentedEvidenceCode").isNull()) {
                if (evidenceCode.equals(root.get("requiredPresentedEvidenceCode").asText())) {
                    return true;
                }
            }
            if (root.has("requiredEvidenceCodes") && !root.get("requiredEvidenceCodes").isNull()) {
                JsonNode reqCodes = root.get("requiredEvidenceCodes");
                if (reqCodes.isArray()) {
                    for (JsonNode node : reqCodes) {
                        if (evidenceCode.equals(node.asText())) {
                            return true;
                        }
                    }
                } else if (reqCodes.isTextual()) {
                    if (evidenceCode.equals(reqCodes.asText())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            return true; // fail-closed
        }
        return false;
    }

    public boolean hasEvidenceIdInArrayOrString(String idsJson, Long evidenceId) {
        if (idsJson == null || idsJson.isBlank()) return false;
        try {
            JsonNode node = jsonMapper.readTree(idsJson);
            if (node.isArray()) {
                for (JsonNode idNode : node) {
                    if (evidenceId.equals(idNode.asLong())) return true;
                }
            } else if (node.isNumber() && evidenceId.equals(node.asLong())) {
                return true;
            } else if (node.isTextual() && evidenceId.toString().equals(node.asText())) {
                return true;
            }
        } catch (Exception e) {
            return true; // fail-closed
        }
        return false;
    }
}
