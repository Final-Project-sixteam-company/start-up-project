# ClueRoom LLMOps Agent Plan

> Purpose: define how ClueRoom can track AI feature quality, safety, latency, error, fallback, and cost signals.
>
> Status: planning document. This does not add metrics, dashboards, workers, or AI provider changes.

---

## 1. Why LLMOps Is Needed

ClueRoom depends on AI behavior for core gameplay.

The AI layer affects:

```text
- NPC interrogation quality
- final deduction scoring
- custom scenario validation
- fallback behavior during demo
- secret and spoiler safety
- latency and cost
```

General infrastructure monitoring is not enough because a request can be technically healthy but still fail the game design.

Examples:

```text
- AI answer succeeds but leaks hidden culprit information.
- AI answer succeeds but invents facts not present in scenario data.
- final deduction scoring returns a result but used fallback instead of DB VariantSolution.
- latency is high enough to break chat UX.
- token/cost usage spikes through repeated interrogation.
```

LLMOps should make these cases visible.

---

## 2. ClueRoom AI Feature List

Current and planned AI feature categories:

| Feature Type | API / Area | Risk |
|---|---|---|
| `INTERROGATION` | `POST /api/play-sessions/{sessionId}/interrogations` | secret leakage, hallucinated facts, latency |
| `FINAL_DEDUCTION` | `POST /api/play-sessions/{sessionId}/final-deduction` | wrong scoring, fallback misuse, missing solution data |
| `SCENARIO_VALIDATION` | `POST /api/ai/scenarios/{scenarioId}/validate` | validation false positive/negative |
| `PROMPT_POLICY_CHECK` | backend prompt policy and response policy checks | forbidden facts in prompt |
| `FALLBACK_RESPONSE` | AI provider failure path | demo continuity but quality degradation |

AI-related code should remain centered in `domain/ai`.

Reference policies:

```text
docs/AI_NPC_PROMPT_POLICY.md
docs/BACKEND_IMPLEMENTATION_GUIDE.md
AGENTS.md
```

---

## 3. AI Request Metadata To Collect

The system should collect metadata, not raw secrets.

Candidate fields:

```text
requestId
sessionId
scenarioId
featureType
npcCode
provider
model
promptVersion
latencyMs
success
errorCode
fallbackUsed
tokenInput
tokenOutput
estimatedCost
createdAt
```

Optional fields when safe:

```text
scenarioCode
variantId presence only, not solution text
policyKey
responsePolicyVersion
promptTemplateName
promptTemplateHash
```

Do not collect:

```text
- full prompt with hidden facts
- API keys
- private scenario YAML
- culpritCode in logs exposed to frontend or public dashboards
- full solution text in Monitoring Agent snapshots
- raw user PII
```

If prompt logging is needed for debugging, it must be separately gated, redacted, and limited to an approved private environment.

---

## 4. Prompt Version Management

Prompt behavior must be traceable.

Recommended version dimensions:

| Dimension | Example |
|---|---|
| prompt template name | `npc_interrogation_system` |
| prompt version | `v1.3.0` |
| response policy version | `v1` |
| scenario schema version | `seowolchae.v1` |
| model | `gpt-4o-mini` |
| provider | `openai` |

Each AI call should be attributable to a prompt version.

The version should change when:

```text
- system prompt wording changes
- answer length constraint changes
- prompt injection defense changes
- ResponsePolicyResolver contract changes
- final deduction rubric changes
- scenario validation criteria changes
```

Prompt version changes should be reviewed with leak-safety criteria.

---

## 5. Quality Evaluation Criteria

LLMOps should track both technical and gameplay quality.

| Category | Signal |
|---|---|
| correctness | answer follows public scenario facts |
| role consistency | NPC stays within public profile/alibi/policy |
| brevity | NPC answer remains within 1-2 sentences |
| safety | no culprit/solution leakage |
| grounding | no unsupported facts |
| usefulness | answer helps investigation without solving it directly |
| scoring reliability | final deduction uses DB solution data |
| resilience | fallback works when provider fails |
| latency | chat response time is acceptable |
| cost | request volume and token use stay bounded |

Evaluation can start manually.

```text
Phase 1:
sample AI responses after deploy and review manually

Phase 2:
store metadata and fallback counters

Phase 3:
add automated prompt safety checks

Phase 4:
add dashboard and alert thresholds
```

---

## 6. Secret Leakage Prevention

This is the highest-priority LLMOps safety rule.

AI NPC prompts must not include:

```text
- culpritCode
- activeVariant
- full solution text
- hidden solution timeline
- unreleased suspect secret
- backend-only scoring criteria
```

AI NPC prompts may include only prompt-safe data:

```text
- NPC public profile
- public statement
- public alibi
- currently unlocked evidence
- user-presented evidence
- ResponsePolicyResolver-selected policy text
- allowed facts selected by backend policy
- user question
```

Leak prevention metrics should distinguish:

```text
- prompt validation blocked before provider call
- model response rejected after provider call
- fallback used because of safety failure
```

Reference:

```text
docs/AI_NPC_PROMPT_POLICY.md
```

---

## 7. NPC Knowledge Boundary Validation

Each NPC must answer within its knowledge boundary.

Validation should check:

```text
1. Did the prompt include only public or unlocked information?
2. Did the prompt include only the selected response policy?
3. Did the generated answer introduce unsupported facts?
4. Did the answer imply direct culprit knowledge without policy support?
5. Did the answer exceed the sentence limit?
6. Did the answer reveal facts from another suspect's hidden state?
```

The agent should classify failures.

| Failure Type | Meaning |
|---|---|
| `PROMPT_CONTAINS_FORBIDDEN_FACT` | backend prompt assembly included forbidden secret |
| `MODEL_HALLUCINATED_FACT` | model answer invented unsupported detail |
| `NPC_BOUNDARY_VIOLATION` | answer used knowledge NPC should not have |
| `ANSWER_TOO_LONG` | 1-2 sentence rule failed |
| `POLICY_MISMATCH` | answer did not follow selected response policy |
| `INJECTION_RISK` | user question attempted prompt override |

LLMOps should treat prompt construction failures as more severe than model style failures.

---

## 8. Fallback Tracking

Fallback is necessary for demo resilience, but it must be visible.

Track:

```text
fallbackUsed
fallbackReason
featureType
provider
model
errorCode
latencyMs before fallback
sessionId
scenarioId
createdAt
```

Candidate fallback reasons:

```text
PROVIDER_DISABLED
API_KEY_MISSING
PROVIDER_TIMEOUT
PROVIDER_ERROR
PROMPT_VALIDATION_FAILED
RESPONSE_VALIDATION_FAILED
SCORING_DATA_MISSING
```

Dashboard views should separate:

```text
- expected local fallback when AI disabled
- production provider failure fallback
- safety fallback
- missing scenario data fallback
```

Fallback should not silently hide official scenario seed problems.

---

## 9. Latency, Error, And Cost Tracking

Track latency by feature type.

```text
interrogation latency
final deduction latency
scenario validation latency
fallback latency
```

Track error rate by:

```text
provider
model
featureType
scenarioId
promptVersion
errorCode
```

Track cost pressure with request and token metadata.

```text
requests per user/session/scenario
requests per feature type
input tokens
output tokens
estimated cost
quota hit count
```

Rate limit principle:

```text
Nginx IP rate limit is edge-level protection.
AI cost defense requires Redis-backed application-level quota by userId, sessionId, scenarioId, and featureType.
```

This matters for Android because carrier NAT or shared Wi-Fi can make many users appear under one IP, while attackers can also rotate IPs.

---

## 10. Prometheus Metric Candidates

Phase 1 implementation records structured `AI_CALL` logs and the following Micrometer metrics.
Micrometer dot names are exposed to Prometheus with Prometheus naming conventions.

Implemented counters:

```text
Micrometer: ai.requests  -> Prometheus: ai_requests_total
Micrometer: ai.failures  -> Prometheus: ai_failures_total
Micrometer: ai.fallbacks -> Prometheus: ai_fallbacks_total
Micrometer: ai.tokens    -> Prometheus: ai_tokens_total
```

Implemented timer:

```text
Micrometer: ai.latency -> Prometheus: ai_latency_seconds
```

Implemented low-cardinality labels:

```text
feature_type
provider
model
prompt_version
success
fallback_used
error_code      only on failure/fallback counters
token_type      only on token counter
```

Interpretation:

```text
ai_requests_total includes provider, mock, and fallback events.
Provider attempt views should filter fallback_used="false".
Fallback views should prefer ai_fallbacks_total.
```

Avoid high-cardinality labels:

```text
scenarioId
sessionId
suspectId
npcCode
requestId
user text
raw prompt
raw answer
```

Use structured logs or future DB rows for request-level detail, not Prometheus labels.

Current structured log fields:

```text
AI_CALL featureType provider model promptVersion scenarioId sessionId suspectId npcCode latencyMs success errorCode fallbackUsed promptTokens completionTokens totalTokens
```

These logs must never include raw prompt text, raw AI answer text, solution text, culprit data, API keys, or private scenario YAML.

## 10.1 Optional DB Persistence

Phase 2 can persist the same metadata to `ai_call_logs`.

This path is disabled by default.

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Enable it only after the table exists in the target database:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=true
```

The DB writer is best-effort:

```text
- AI gameplay must not fail because LLMOps logging failed.
- Missing table or DB insert failure is logged once and then treated as a non-blocking telemetry failure.
- Prompt text, AI answer text, solution text, culprit data, API keys, and private scenario YAML are not stored.
```

Manual MySQL DDL candidate:

```sql
CREATE TABLE ai_call_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    feature_type VARCHAR(40) NOT NULL,
    provider VARCHAR(80) NOT NULL,
    model VARCHAR(120) NOT NULL,
    prompt_version VARCHAR(120) NOT NULL,
    scenario_id BIGINT NULL,
    play_session_id BIGINT NULL,
    suspect_id BIGINT NULL,
    npc_code VARCHAR(120) NULL,
    latency_ms BIGINT NOT NULL,
    success TINYINT(1) NOT NULL,
    error_code VARCHAR(80) NULL,
    fallback_used TINYINT(1) NOT NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    total_tokens INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ai_call_logs_created_at (created_at),
    INDEX idx_ai_call_logs_feature_created (feature_type, created_at),
    INDEX idx_ai_call_logs_session_created (play_session_id, created_at),
    INDEX idx_ai_call_logs_scenario_created (scenario_id, created_at),
    INDEX idx_ai_call_logs_success_created (success, created_at)
);
```

Retention should be decided before long-term production use.

Initial retention candidate:

```text
- keep detailed ai_call_logs rows for 30-90 days
- keep aggregated dashboard data longer if needed
- do not use ai_call_logs as a replay source because prompt and answer bodies are intentionally absent
```

Future metric candidates after backend safety/quota features exist:

```text
ai_prompt_validation_failures_total
ai_secret_leak_blocked_total
ai_response_validation_failures_total
ai_quota_rejections_total
```

---

## 11. Grafana LLMOps Dashboard Candidates

Initial dashboard panels:

```text
AI requests by feature type
AI failure rate
AI fallback rate
AI latency p50/p95/p99
AI prompt validation failures
AI response validation failures
AI secret leak blocked count
AI quota rejection count
Provider/model distribution
Estimated token usage
```

Scenario-specific panels:

```text
interrogation count per scenario
fallback count per scenario
final deduction success/error count
missing scoring data indicators
```

Do not show spoiler text or private solution content in Grafana.

Grafana is for operational metrics, not secret scenario inspection.

---

## 12. LLMOps Agent Role

The LLMOps Agent should analyze metadata and safety signals.

Allowed tasks:

```text
- summarize AI failure/fallback trends
- identify prompt version with increased failure rate
- flag repeated prompt validation failures
- flag possible NPC knowledge boundary violations
- suggest safe manual review samples
- draft LLMOps dashboard improvements
```

Not allowed:

```text
- read or print API keys
- expose culprit or solution text
- autonomously change prompts in production
- bypass ResponsePolicyResolver
- override final deduction scoring criteria
```

Recommended execution path:

```text
Phase 1: manual analysis from metadata and logs
Phase 2: dashboard-based trend review
Phase 3: scheduled summary after monitoring stack matures
Phase 4: automated alert suggestions with human approval
```

The agent should treat secret-safety regressions as higher priority than style or latency issues.

