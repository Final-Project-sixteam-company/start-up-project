# ClueRoom LLMOps Operating Guide

> Purpose: define the canonical operating guide for ClueRoom AI feature telemetry, safety review, smoke checks, PromQL queries, and LLMOps agent analysis.
>
> Status: canonical LLMOps operating guide. It absorbs the previous LLMOps agent plan, PromQL query candidates, and smoke runbook. It does not itself deploy dashboards, workers, or provider changes.

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
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md section 13
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md section 14
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
| model | `deepseek-v4-flash` |
| provider | `deepseek` |

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

`scenarioId`, `sessionId`, `suspectId`, and `npcCode` are intentionally excluded from Prometheus labels.
They are request-level fields for structured logs and optional `ai_call_logs` rows only.

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



---

## 13. LLMOps Smoke And Rollout Runbook

This section absorbs `LLMOPS_SMOKE_RUNBOOK.md`.
Use it after a backend deploy that changes AI telemetry, prompt context logging, or LLMOps persistence.

### 13.1 Runtime Signals

Expected runtime signals:

```text
AI_CALL structured log
AI_CALL_CONTEXT structured log
ai_requests_total
ai_failures_total
ai_fallbacks_total
ai_tokens_total
ai_latency_seconds
```

Optional DB persistence:

```text
ai_call_logs
```

Default DB state:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Metric/log telemetry can work after deploy while DB insert remains disabled.
Enable DB persistence only after the table exists and rollback is clear.

### 13.2 Smoke Safety Rules

Do not log, store, paste, or put into dashboard labels:

```text
raw system prompt
raw user prompt
raw user question
raw AI answer
final deduction free text
solution text
culprit data
private scenario YAML
provider API keys
```

Prometheus labels must stay low-cardinality:

```text
feature_type
provider
model
prompt_version
success
fallback_used
error_code
token_type
```

Request-level IDs such as `sessionId`, `scenarioId`, `suspectId`, and `npcCode` may be used in structured logs or optional DB rows, but they must not become Prometheus labels.
`AI_CALL_CONTEXT` should remain a prompt-shape/cost signal and must not include raw prompt, raw answer, user question body, or direct gameplay identifiers.

### 13.3 Preconditions

```bash
ssh clueroom
cd /opt/clueroom/app

/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
```

Expected:

```text
API health is 200.
Active Blue-Green slot is known.
```

Do not use public `/actuator/prometheus`; production Nginx hardening blocks it externally.
Query Prometheus locally from the server or through the ops path.

### 13.4 Generate One AI Call

Preferred low-risk options:

```text
1. Use an existing test play session and send one interrogation request.
2. Run scenario validation for a non-production/custom scenario if available.
3. In local/mock mode, start the app and trigger a mock interrogation path.
```

Avoid final deduction smoke on an active demo session unless the team explicitly wants to complete that session.

### 13.5 Verify Structured Logs

Check active app logs through the Blue-Green helper.

```bash
/opt/clueroom/bg-compose logs --tail=300 app-blue | grep 'AI_CALL'
/opt/clueroom/bg-compose logs --tail=300 app-green | grep 'AI_CALL'
```

Expected `AI_CALL` shape:

```text
AI_CALL featureType=INTERROGATION provider=... model=... promptVersion=... scenarioId=... sessionId=... suspectId=... npcCode=... latencyMs=... success=... errorCode=... fallbackUsed=... promptTokens=... completionTokens=... totalTokens=...
```

Expected `AI_CALL_CONTEXT` shape:

```text
AI_CALL_CONTEXT featureType=INTERROGATION provider=... model=... promptVersion=... systemRuleTokens=... policyContextTokens=... npcProfileTokens=... evidenceContextTokens=... historyTokens=... questionTokens=... promptCharLength=... historyTurns=... includedEvidenceCount=... templateHash=...
```

Expected safety:

```text
- no prompt body
- no answer body
- no user free text
- no solution text
- no culprit data
- no API key
- no sessionId/scenarioId/suspectId/npcCode in AI_CALL_CONTEXT
```

### 13.6 Verify Prometheus Metrics

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_requests_total'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_latency_seconds_count'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_fallbacks_total'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_tokens_total'
```

Expected:

```text
ai_requests_total exists after at least one AI call.
ai_latency_seconds_count exists after at least one AI provider/mock/fallback event.
ai_tokens_total may be absent or empty if the provider does not return token usage metadata.
ai_fallbacks_total may be absent or zero if fallback was not used.
```

### 13.7 Optional DB Persistence

Only do this after deciding to store detailed metadata in MySQL.
The table DDL is maintained in section 10.1 of this guide.

Enable DB logging only after the table exists:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=true
```

For production Blue-Green, put the value in the runtime env source loaded by the helpers, preferably:

```text
/opt/clueroom/secrets/env.d/ai.env
```

Confirm the active/running slot has the value:

```bash
docker exec start-up-app-blue printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
docker exec start-up-app-green printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
```

If one slot is intentionally stopped, check only the active/running slot from `/opt/clueroom/bg-status.sh`.

After enabling DB logging and generating one AI call, query metadata only:

```sql
SELECT
  id,
  feature_type,
  provider,
  model,
  prompt_version,
  scenario_id,
  play_session_id,
  suspect_id,
  npc_code,
  latency_ms,
  success,
  error_code,
  fallback_used,
  prompt_tokens,
  completion_tokens,
  total_tokens,
  created_at
FROM ai_call_logs
ORDER BY id DESC
LIMIT 10;
```

Expected:

```text
Rows exist after AI calls.
No prompt body, answer body, user free text, solution text, culprit data, or API key columns exist.
```

Disable DB persistence first if it creates noise or the table is not ready:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Do not drop `ai_call_logs` during an incident.
Disable logging first, then archive/drop later only after confirming no analysis needs it.

---

## 14. PromQL Query Appendix

This section absorbs `LLMOPS_PROMQL_QUERIES.md`.
It defines initial Prometheus queries for Grafana panels and manual checks.
It does not create dashboards or alerts.

### 14.1 Metric Names

| Micrometer | Prometheus | Meaning |
|---|---|---|
| `ai.requests` | `ai_requests_total` | AI feature call events, including provider/mock/fallback events |
| `ai.failures` | `ai_failures_total` | failed provider/client calls |
| `ai.fallbacks` | `ai_fallbacks_total` | fallback responses used |
| `ai.tokens` | `ai_tokens_total` | token usage when provider metadata exposes it |
| `ai.latency` | `ai_latency_seconds_*` | AI call latency timer series |

Forbidden Prometheus labels:

```text
sessionId
scenarioId
suspectId
npcCode
requestId
raw prompt
raw answer
user text
solution text
culprit data
```

### 14.2 Manual Metric Presence Checks

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_requests_total'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_latency_seconds_count'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_fallbacks_total'

curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_tokens_total'
```

### 14.3 Request Volume Panels

AI requests by feature type over 5 minutes:

```promql
sum by (feature_type) (
  increase(ai_requests_total[5m])
)
```

AI requests by provider and model:

```promql
sum by (provider, model) (
  increase(ai_requests_total[5m])
)
```

AI requests by prompt version:

```promql
sum by (feature_type, prompt_version) (
  increase(ai_requests_total[30m])
)
```

Provider attempts only, excluding fallback events:

```promql
sum by (feature_type, provider, model) (
  increase(ai_requests_total{fallback_used="false"}[5m])
)
```

### 14.4 Failure And Fallback Panels

Failure count by feature and error code:

```promql
sum by (feature_type, error_code) (
  increase(ai_failures_total[5m])
)
```

Fallback count by feature and error code:

```promql
sum by (feature_type, error_code) (
  increase(ai_fallbacks_total[5m])
)
```

Fallback ratio by feature:

```promql
sum by (feature_type) (increase(ai_fallbacks_total[5m]))
/
clamp_min(
  sum by (feature_type) (
    increase(ai_requests_total{fallback_used="false",provider!="mock"}[5m])
  ),
  1
)
```

Failure ratio by feature:

```promql
sum by (feature_type) (increase(ai_failures_total[5m]))
/
clamp_min(
  sum by (feature_type) (
    increase(ai_requests_total{fallback_used="false",provider!="mock"}[5m])
  ),
  1
)
```

Interpretation:

```text
Fallback may preserve demo UX, but it means the real provider or parser path did not complete.
Use provider attempts as the denominator for failure/fallback ratios.
Do not divide by all ai_requests_total because fallback events are also recorded as AI request events.
Mock-mode smoke calls are excluded from production-oriented ratio denominators.
```

### 14.5 Latency Panels

Average latency by feature type:

```promql
sum by (feature_type) (increase(ai_latency_seconds_sum[5m]))
/
clamp_min(sum by (feature_type) (increase(ai_latency_seconds_count[5m])), 0.001)
```

Request count by feature type:

```promql
sum by (feature_type) (
  increase(ai_latency_seconds_count[5m])
)
```

p95 latency by feature type, only if histogram buckets are enabled:

```promql
histogram_quantile(
  0.95,
  sum by (feature_type, le) (
    rate(ai_latency_seconds_bucket[5m])
  )
)
```

Notes:

```text
ai_latency_seconds_bucket may not exist unless histogram buckets are enabled.
Use ai_latency_seconds_sum/count for the first dashboard if buckets are absent.
Final deduction and scenario validation can naturally be slower than interrogation.
Do not use one global latency threshold for every feature.
```

### 14.6 Token Panels

Total token usage by feature and token type:

```promql
sum by (feature_type, token_type) (
  increase(ai_tokens_total[30m])
)
```

Total token usage by provider and model:

```promql
sum by (provider, model, token_type) (
  increase(ai_tokens_total[30m])
)
```

Prompt/completion split:

```promql
sum by (token_type) (
  increase(ai_tokens_total[30m])
)
```

### 14.7 Initial Dashboard And Alert Candidates

Recommended first dashboard:

```text
1. AI requests by feature type
2. AI requests by provider/model
3. AI fallback ratio by feature type
4. AI failure ratio by feature type
5. AI p95 latency by feature type
6. AI token usage by provider/model/token_type
7. AI prompt version distribution
```

Do not show gameplay spoilers, raw prompt text, raw answer text, user free text, or private scenario YAML.

Candidate warning rules after baseline observation:

```text
- fallback ratio > 20% for 5 minutes
- failure ratio > 10% for 5 minutes
- interrogation p95 latency > 8 seconds for 5 minutes
- final deduction p95 latency > 60 seconds for 5 minutes
- token usage spike above normal baseline
```

Alert thresholds must be tuned after real Android/AI traffic is observed.

---

## 15. LLMOps Completion Criteria

```text
- AI_CALL and AI_CALL_CONTEXT logs appear after one AI feature call.
- AI_CALL_CONTEXT contains only block-level estimates and metadata, not raw prompt/answer/user question text.
- ai_requests_total is visible from local Prometheus query.
- ai_latency_seconds_count is visible from local Prometheus query.
- ai_fallbacks_total is visible when fallback occurs.
- ai_tokens_total is visible only when provider token metadata exists.
- Optional ai_call_logs rows appear only after table creation and AI_LLMOPS_DB_LOGGING_ENABLED=true.
- PromQL queries avoid high-cardinality labels.
- Failure/fallback ratios use provider attempts as denominator.
- No raw prompt, raw answer, solution text, culprit data, API key, or private scenario YAML is logged or stored.
```
