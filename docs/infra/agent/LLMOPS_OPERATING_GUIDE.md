# ClueRoom LLMOps 운영 가이드

> 목적: ClueRoom AI 기능의 telemetry, safety review, smoke check, PromQL query, LLMOps agent analysis 기준을 정의한다.
>
> 상태: LLMOps 운영 정본 문서다. 이전 LLMOps agent plan, PromQL query 후보, smoke runbook을 흡수했다. 이 문서 자체가 dashboard, worker, provider 변경을 배포하지는 않는다.

---

## 1. LLMOps가 필요한 이유

ClueRoom은 핵심 gameplay가 AI 동작에 의존한다.

AI layer가 영향을 주는 영역:

```text
- NPC interrogation quality
- final deduction scoring
- custom scenario validation
- demo 중 fallback behavior
- secret and spoiler safety
- latency and cost
```

일반 인프라 모니터링만으로는 부족하다.
요청이 기술적으로 성공해도 game design 관점에서는 실패할 수 있기 때문이다.

예시:

```text
- AI answer는 성공했지만 hidden culprit information을 누설했다.
- AI answer는 성공했지만 scenario data에 없는 사실을 만들었다.
- final deduction scoring은 결과를 반환했지만 DB VariantSolution이 아니라 fallback을 사용했다.
- latency가 높아서 chat UX를 깨뜨렸다.
- 반복 심문으로 token/cost 사용량이 급증했다.
```

LLMOps는 이런 상태를 보이게 만드는 작업이다.

---

## 2. ClueRoom AI 기능 목록

현재 및 계획된 AI feature category:

| Feature Type | API / Area | Risk 위험 |
|---|---|---|
| `INTERROGATION` | `POST /api/play-sessions/{sessionId}/interrogations` | secret leakage, hallucinated facts, latency 위험 |
| `FINAL_DEDUCTION` | `POST /api/play-sessions/{sessionId}/final-deduction` | wrong scoring, fallback misuse, missing solution data 위험 |
| `SCENARIO_VALIDATION` | `POST /api/ai/scenarios/{scenarioId}/validate` | validation false positive/negative 위험 |
| `PROMPT_POLICY_CHECK` | backend prompt policy and response policy checks | prompt에 forbidden facts가 들어갈 위험 |
| `FALLBACK_RESPONSE` | AI provider failure path | demo continuity는 지키지만 quality degradation 위험 |

AI 관련 코드는 `domain/ai` 중심으로 유지한다.

참조 정책:

```text
docs/AI_NPC_PROMPT_POLICY.md
docs/BACKEND_IMPLEMENTATION_GUIDE.md
AGENTS.md
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md section 13
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md section 14
```

---

## 3. 수집할 AI Request Metadata

시스템은 raw secret이 아니라 metadata를 수집해야 한다.

후보 field:

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

안전할 때만 선택적으로 사용할 수 있는 field:

```text
scenarioCode
variantId presence only, not solution text
policyKey
responsePolicyVersion
promptTemplateName
promptTemplateHash
```

수집하지 말 것:

```text
- hidden fact가 포함된 full prompt
- API keys
- private scenario YAML
- frontend 또는 public dashboard에 노출되는 culpritCode
- Monitoring Agent snapshot의 full solution text
- raw user PII
```

Debug를 위해 prompt logging이 필요하다면 별도 gate, redaction, 승인된 private environment 제한이 필요하다.

---

## 4. Prompt Version 관리

Prompt 동작은 추적 가능해야 한다.

권장 version dimension:

| Dimension 기준 | Example 예시 |
|---|---|
| prompt template name 이름 | `npc_interrogation_system` |
| prompt version 버전 | `v1.3.0` |
| response policy version 버전 | `v1` |
| scenario schema version 버전 | `seowolchae.v1` |
| model 모델 | `deepseek-v4-flash` |
| provider 제공자 | `deepseek` |

각 AI call은 prompt version에 귀속될 수 있어야 한다.

Version을 바꿔야 하는 경우:

```text
- system prompt wording changes
- answer length constraint changes
- prompt injection defense changes
- ResponsePolicyResolver contract changes
- final deduction rubric changes
- scenario validation criteria changes
```

Prompt version 변경은 leak-safety 기준으로 review해야 한다.

---

## 5. 품질 평가 기준

LLMOps는 기술 품질과 gameplay 품질을 모두 추적해야 한다.

| Category 분류 | Signal 신호 |
|---|---|
| correctness | answer가 public scenario fact를 따름 |
| role consistency | NPC가 public profile/alibi/policy 안에 머묾 |
| brevity | NPC answer가 1~2문장 제한 유지 |
| safety | culprit/solution leakage 없음 |
| grounding | unsupported fact 없음 |
| usefulness | 사건을 직접 풀지 않으면서 investigation에 도움 |
| scoring reliability | final deduction이 DB solution data 사용 |
| resilience | provider 실패 시 fallback 동작 |
| latency | chat response time이 수용 가능 |
| cost | request volume과 token use가 bounded |

평가는 수동으로 시작할 수 있다.

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

## 6. Secret Leakage 방지

이것은 LLMOps에서 가장 높은 우선순위의 safety rule이다.

AI NPC prompt에는 아래가 포함되면 안 된다.

```text
- culpritCode
- activeVariant
- full solution text
- hidden solution timeline
- unreleased suspect secret
- backend-only scoring criteria
```

AI NPC prompt에는 prompt-safe data만 포함할 수 있다.

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

Leak prevention metric은 아래를 구분해야 한다.

```text
- provider call 전에 prompt validation으로 차단됨
- provider call 후 model response가 reject됨
- safety failure 때문에 fallback 사용
```

참조:

```text
docs/AI_NPC_PROMPT_POLICY.md
```

---

## 7. NPC Knowledge Boundary 검증

각 NPC는 자신의 knowledge boundary 안에서만 답해야 한다.

검증 항목:

```text
1. Prompt가 public 또는 unlocked information만 포함했는가?
2. Prompt가 selected response policy만 포함했는가?
3. Generated answer가 unsupported fact를 추가했는가?
4. Answer가 policy support 없이 direct culprit knowledge를 암시했는가?
5. Answer가 sentence limit을 초과했는가?
6. Answer가 다른 suspect의 hidden state를 reveal했는가?
```

Agent는 failure를 분류해야 한다.

| Failure Type | 의미 |
|---|---|
| `PROMPT_CONTAINS_FORBIDDEN_FACT` | backend prompt assembly가 forbidden secret을 포함함 |
| `MODEL_HALLUCINATED_FACT` | model answer가 unsupported detail을 만들어냄 |
| `NPC_BOUNDARY_VIOLATION` | answer가 NPC가 몰라야 할 지식을 사용함 |
| `ANSWER_TOO_LONG` | 1~2문장 rule 위반 |
| `POLICY_MISMATCH` | selected response policy를 따르지 않음 |
| `INJECTION_RISK` | user question이 prompt override를 시도함 |

Prompt construction failure는 model style failure보다 더 심각하게 취급한다.

---

## 8. Fallback 추적

Fallback은 demo resilience에 필요하지만 반드시 보여야 한다.

추적할 항목:

```text
- provider unavailable
- timeout
- response validation failed
- prompt validation blocked
- quota/rate limit hit
- manual/mock fallback used
```

Fallback metric은 아래를 구분해야 한다.

```text
fallbackUsed=true
fallbackReason
provider
model
featureType
promptVersion
```

Fallback answer는 demo continuity를 위한 것이며, 품질이 낮을 수 있다.
Fallback이 반복되면 성공률만 보고 정상으로 판단하면 안 된다.

---

## 9. Latency, Error, Cost 추적

AI call은 일반 API보다 비용과 지연 변동성이 크다.

기본 측정 항목:

```text
- request count
- success count
- failure count
- fallback count
- latencyMs
- prompt tokens
- completion tokens
- total tokens
- estimated cost if reliable
```

분석 축:

```text
featureType
provider
model
promptVersion
success
fallbackUsed
errorCode
```

Prometheus label에 넣으면 안 되는 high-cardinality 값:

```text
sessionId
scenarioId
suspectId
npcCode
user question
raw prompt
raw answer
```

Request-level detail은 structured log나 optional DB row로 관리하고, Prometheus label로 넣지 않는다.

---

## 10. Prometheus Metric 후보

현재 또는 후보 metric:

```text
ai_requests_total
ai_failures_total
ai_fallbacks_total
ai_tokens_total
ai_latency_seconds
```

권장 label:

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

해석:

```text
ai_requests_total은 provider, mock, fallback event를 포함할 수 있다.
Provider attempt view는 fallback_used="false"로 filter하는 것이 좋다.
Fallback view는 ai_fallbacks_total을 우선 사용한다.
```

피해야 할 high-cardinality label:

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

Request-level detail은 Prometheus label이 아니라 structured log 또는 future DB row로 관리한다.

현재 structured log field:

```text
AI_CALL featureType provider model promptVersion scenarioId sessionId suspectId npcCode latencyMs success errorCode fallbackUsed promptTokens completionTokens totalTokens
```

이 log에는 raw prompt text, raw AI answer text, solution text, culprit data, API key, private scenario YAML이 절대 포함되면 안 된다.

## 10.1 Optional DB Persistence 선택 DB 저장

Phase 2에서는 같은 metadata를 `ai_call_logs`에 저장할 수 있다.

이 경로는 기본 비활성화다.

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Target database에 table이 존재한 뒤에만 활성화한다.

```text
AI_LLMOPS_DB_LOGGING_ENABLED=true
```

DB writer는 best-effort다.

```text
- LLMOps logging 실패 때문에 AI gameplay가 실패하면 안 된다.
- Missing table 또는 DB insert failure는 한 번 log한 뒤 non-blocking telemetry failure로 취급한다.
- Prompt text, AI answer text, solution text, culprit data, API key, private scenario YAML은 저장하지 않는다.
```

Manual MySQL DDL 후보:

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

장기 운영 전에 retention을 결정해야 한다.

초기 retention 후보:

```text
- detailed ai_call_logs row는 30~90일 보관
- aggregated dashboard data는 필요 시 더 오래 보관
- prompt와 answer body가 없으므로 ai_call_logs를 replay source로 사용하지 않는다.
```

Backend safety/quota 기능이 생긴 뒤의 future metric 후보:

```text
ai_prompt_validation_failures_total
ai_secret_leak_blocked_total
ai_response_validation_failures_total
ai_quota_rejections_total
```

---

## 11. Grafana LLMOps Dashboard 후보

초기 dashboard panel:

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

Scenario-specific panel 시나리오별 panel:

```text
interrogation count per scenario
fallback count per scenario
final deduction success/error count
missing scoring data indicators
```

Grafana에는 spoiler text나 private solution content를 표시하지 않는다.
Grafana는 operational metrics용이지 secret scenario inspection용이 아니다.

---

## 12. LLMOps Agent 역할

LLMOps Agent는 metadata와 safety signal을 분석해야 한다.

허용 작업:

```text
- AI failure/fallback trend 요약
- failure rate가 증가한 prompt version 식별
- 반복 prompt validation failure flag
- 가능한 NPC knowledge boundary violation flag
- safe manual review sample 제안
- LLMOps dashboard 개선안 draft
```

금지 작업:

```text
- API key 읽기 또는 출력
- culprit 또는 solution text 노출
- production prompt 자율 변경
- ResponsePolicyResolver 우회
- final deduction scoring criteria override
```

권장 실행 경로:

```text
Phase 1: metadata와 log 기반 manual analysis
Phase 2: dashboard-based trend review
Phase 3: monitoring stack 성숙 후 scheduled summary
Phase 4: human approval 기반 automated alert suggestion
```

Agent는 style 또는 latency 문제보다 secret-safety regression을 더 높은 우선순위로 취급해야 한다.

---

## 13. LLMOps Smoke And Rollout Runbook 절차

이 섹션은 `LLMOPS_SMOKE_RUNBOOK.md`를 흡수한다.
AI telemetry, prompt-context logging, LLMOps persistence를 변경하는 backend deploy 후 사용한다.

### 13.1 Runtime Signals 런타임 신호

기대 runtime signal:

```text
AI_CALL structured log
AI_CALL_CONTEXT prompt-shape/cost log
ai_requests_total
ai_failures_total
ai_fallbacks_total
ai_tokens_total
ai_latency_seconds
```

`AI_CALL_CONTEXT`는 심문 AI 호출 직전의 prompt 구성 비용을 보는 best-effort 로그다.
원문 prompt, 원문 answer, 사용자 질문 전문, direct gameplay identifier를 남기지 않고 token/length/count/hash 같은 파생값만 남긴다.

Optional DB persistence 선택 DB 저장:

```text
ai_call_logs
```

기본 DB 상태:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Metric/log telemetry는 deploy 후 동작할 수 있으며 DB insert는 비활성 상태로 남을 수 있다.
DB persistence는 table이 존재하고 rollback이 명확한 뒤에만 활성화한다.

### 13.2 Smoke Safety Rules 안전 규칙

아래 항목은 log, store, paste, dashboard label에 넣지 않는다.

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

Prometheus label은 low-cardinality를 유지해야 한다.

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

`sessionId`, `scenarioId`, `suspectId`, `npcCode` 같은 request-level ID는 structured log나 optional DB row에 사용할 수 있지만 Prometheus label로 만들면 안 된다.
`AI_CALL_CONTEXT`는 prompt-shape/cost signal이므로 raw prompt, raw answer, user question body, `sessionId`, `scenarioId`, `suspectId`, `npcCode`를 포함하면 안 된다.

### 13.3 Preconditions 사전 조건

```bash
ssh clueroom
cd /opt/clueroom/app

/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
```

기대 상태:

```text
API health is 200.
Active Blue-Green slot is known.
```

Public `/actuator/prometheus`는 사용하지 않는다. Production Nginx hardening이 외부 접근을 차단한다.
Prometheus는 server local 또는 ops path에서 query한다.

### 13.4 Generate One AI Call AI 호출 1회 생성

선호하는 low-risk option:

```text
1. 기존 test play session에서 interrogation request 1회 전송
2. 가능한 경우 non-production/custom scenario에 대한 scenario validation 실행
3. local/mock mode에서 app 시작 후 mock interrogation path trigger
```

팀이 해당 session을 완료하려는 것이 아니라면 active demo session에서 final deduction smoke를 피한다.

### 13.5 Verify Structured Logs 구조화 로그 확인

Active app log는 Blue-Green helper로 확인한다.

```bash
ACTIVE_SERVICE="$(/opt/clueroom/bg-status.sh | awk -F': ' '/Active service/{print $2}')"

/opt/clueroom/bg-compose logs --tail=500 "$ACTIVE_SERVICE" | grep 'AI_CALL featureType=' | tail -n 10
/opt/clueroom/bg-compose logs --tail=500 "$ACTIVE_SERVICE" | grep 'AI_CALL_CONTEXT featureType=' | tail -n 10
```

기대 `AI_CALL` 형태:

```text
AI_CALL featureType=INTERROGATION provider=... model=... promptVersion=... scenarioId=... sessionId=... suspectId=... npcCode=... latencyMs=... success=... errorCode=... fallbackUsed=... promptTokens=... completionTokens=... totalTokens=...
```

기대 `AI_CALL_CONTEXT` 형태:

```text
AI_CALL_CONTEXT featureType=INTERROGATION provider=... model=... promptVersion=... systemRuleTokens=... policyContextTokens=... npcProfileTokens=... evidenceContextTokens=... historyTokens=... questionTokens=... promptCharLength=... historyTurns=... includedEvidenceCount=... templateHash=...
```

기대 safety:

```text
- prompt body 없음
- answer body 없음
- user free text 없음
- solution text 없음
- culprit data 없음
- API key 없음
- AI_CALL_CONTEXT에는 sessionId/scenarioId/suspectId/npcCode 없음
```

`AI_CALL_CONTEXT`가 없고 `AI_CALL`만 보이면 prompt-context logging regression으로 본다.
단, `AI_CALL_CONTEXT logging failed but AI call will continue` warning만 있고 실제 AI 호출이 성공했다면 사용자 기능은 정상이며 telemetry 쪽을 별도 조사한다.

### 13.6 Verify Prometheus Metrics Prometheus 지표 확인

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

기대:

```text
AI call이 최소 1회 발생한 뒤 ai_requests_total이 존재한다.
AI provider/mock/fallback event가 최소 1회 발생한 뒤 ai_latency_seconds_count가 존재한다.
Provider가 token usage metadata를 반환하지 않으면 ai_tokens_total은 없거나 비어 있을 수 있다.
Fallback이 사용되지 않았으면 ai_fallbacks_total은 없거나 0일 수 있다.
```

### 13.7 Optional DB Persistence 선택 DB 저장

상세 metadata를 MySQL에 저장하기로 결정한 뒤에만 사용한다.
Table DDL은 이 가이드 section 10.1에서 관리한다.

Table이 존재한 뒤에만 DB logging을 활성화한다.

```text
AI_LLMOPS_DB_LOGGING_ENABLED=true
```

Production Blue-Green에서는 runtime env source에 값을 넣는다. 권장 위치:

```text
/opt/clueroom/secrets/env.d/ai.env
```

Active/running slot에 값이 있는지 확인한다.

```bash
docker exec start-up-app-blue printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
docker exec start-up-app-green printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
```

한 slot이 의도적으로 중지된 경우 `/opt/clueroom/bg-status.sh` 기준 active/running slot만 확인한다.

DB logging 활성화 후 AI call을 1회 발생시키고 metadata만 조회한다.

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

기대:

```text
AI call 이후 row가 존재한다.
Prompt body, answer body, user free text, solution text, culprit data, API key column이 없다.
```

Table이 준비되지 않았거나 noise가 생기면 먼저 DB persistence를 비활성화한다.

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

---

## 14. PromQL Query Appendix 쿼리 부록

이 섹션은 `LLMOPS_PROMQL_QUERIES.md`를 흡수한다.
Query는 dashboard/alert 후보이며, 실제 metric name은 Grafana Explore에서 확인한 뒤 사용한다.

### 14.1 Metric Names 지표 이름

Java/Micrometer name과 Prometheus export name은 다를 수 있다.
초기 후보:

| Concept 개념 | Java metric name | Prometheus candidate |
|---|---|---|
| requests | `ai.requests` | `ai_requests_total` |
| failures | `ai.failures` | `ai_failures_total` |
| fallbacks | `ai.fallbacks` | `ai_fallbacks_total` |
| tokens | `ai.tokens` | `ai_tokens_total` |
| latency | `ai.latency` | `ai_latency_seconds_*` |

Prometheus scrape 확인:

```bash
curl -s 'http://localhost:9090/api/v1/label/__name__/values' | jq -r '.data[]' | grep '^ai_'
```

Metric이 없으면 먼저 AI feature를 1회 호출한다.

### 14.2 Manual Metric Presence Checks 수동 지표 존재 확인

```promql
ai_requests_total
```

```promql
ai_failures_total
```

```promql
ai_fallbacks_total
```

```promql
ai_tokens_total
```

```promql
ai_latency_seconds_count
```

### 14.3 Request Volume Panels 요청량 패널

Feature별 request 증가량:

```promql
sum by (feature_type) (
  increase(ai_requests_total[5m])
)
```

Provider/model별 request 증가량:

```promql
sum by (provider, model) (
  increase(ai_requests_total[5m])
)
```

Prompt version별 request 증가량:

```promql
sum by (prompt_version) (
  increase(ai_requests_total[30m])
)
```

Provider attempt만 보고 싶으면 fallback event를 제외한다.

```promql
sum by (feature_type, provider, model) (
  increase(ai_requests_total{fallback_used="false"}[5m])
)
```

### 14.4 Failure And Fallback Panels 실패와 fallback 패널

Feature별 failure count:

```promql
sum by (feature_type) (
  increase(ai_failures_total[5m])
)
```

Feature별 fallback count:

```promql
sum by (feature_type) (
  increase(ai_fallbacks_total[5m])
)
```

Feature별 failure ratio:

```promql
sum by (feature_type) (increase(ai_failures_total[5m]))
/
clamp_min(
  sum by (feature_type) (
    increase(ai_requests_total{fallback_used="false", provider!="mock"}[5m])
  ),
  1
)
```

Feature별 fallback ratio:

```promql
sum by (feature_type) (increase(ai_fallbacks_total[5m]))
/
clamp_min(
  sum by (feature_type) (
    increase(ai_requests_total{fallback_used="false", provider!="mock"}[5m])
  ),
  1
)
```

Error code별 failure:

```promql
sum by (feature_type, error_code) (
  increase(ai_failures_total[30m])
)
```

Prompt version별 failure:

```promql
sum by (prompt_version, error_code) (
  increase(ai_failures_total[30m])
)
```

주의:

```text
Denominator가 0이면 Grafana panel에서 null/NaN 처리한다.
Mock/fallback event가 ai_requests_total에 포함되는지 실제 metric implementation을 확인한다.
```

### 14.5 Latency Panels 지연 패널

Latency count 확인:

```promql
sum by (feature_type) (
  increase(ai_latency_seconds_count[5m])
)
```

Average latency:

```promql
sum by (feature_type) (rate(ai_latency_seconds_sum[5m]))
/
sum by (feature_type) (rate(ai_latency_seconds_count[5m]))
```

p95 latency 후보:

```promql
histogram_quantile(
  0.95,
  sum by (le, feature_type) (
    rate(ai_latency_seconds_bucket[5m])
  )
)
```

p99 latency 후보:

```promql
histogram_quantile(
  0.99,
  sum by (le, feature_type) (
    rate(ai_latency_seconds_bucket[5m])
  )
)
```

주의:

```text
ai_latency_seconds_bucket은 histogram bucket이 활성화되어야 존재할 수 있다.
Bucket이 없으면 첫 dashboard는 ai_latency_seconds_sum/count를 사용한다.
Final deduction과 scenario validation은 interrogation보다 자연스럽게 느릴 수 있다.
모든 feature에 하나의 global latency threshold를 쓰지 않는다.
```

### 14.6 Token Panels 토큰 패널

Feature와 token type별 total token usage:

```promql
sum by (feature_type, token_type) (
  increase(ai_tokens_total[30m])
)
```

Provider/model별 total token usage:

```promql
sum by (provider, model, token_type) (
  increase(ai_tokens_total[30m])
)
```

Prompt/completion 분리:

```promql
sum by (token_type) (
  increase(ai_tokens_total[30m])
)
```

### 14.7 초기 Dashboard와 Alert 후보

권장 첫 dashboard:

```text
1. AI requests by feature type
2. AI requests by provider/model
3. AI fallback ratio by feature type
4. AI failure ratio by feature type
5. AI p95 latency by feature type
6. AI token usage by provider/model/token_type
7. AI prompt version distribution
```

Gameplay spoiler, raw prompt text, raw answer text, user free text, private scenario YAML을 표시하지 않는다.

Baseline 관측 후 warning rule 후보:

```text
- fallback ratio > 20% for 5 minutes
- failure ratio > 10% for 5 minutes
- interrogation p95 latency > 8 seconds for 5 minutes
- final deduction p95 latency > 60 seconds for 5 minutes
- token usage spike above normal baseline
```

Alert threshold는 실제 Android/AI traffic을 관측한 뒤 조정한다.

---

## 15. LLMOps 완료 기준

```text
- AI feature call 1회 후 AI_CALL log가 나타난다.
- Local Prometheus query에서 ai_requests_total이 보인다.
- Local Prometheus query에서 ai_latency_seconds_count가 보인다.
- Fallback 발생 시 ai_fallbacks_total이 보인다.
- ai_tokens_total은 provider token metadata가 있을 때만 보일 수 있다.
- Optional ai_call_logs row는 table 생성과 AI_LLMOPS_DB_LOGGING_ENABLED=true 이후에만 나타난다.
- PromQL query는 high-cardinality label을 피한다.
- Failure/fallback ratio는 provider attempt를 denominator로 사용한다.
- Raw prompt, raw answer, solution text, culprit data, API key, private scenario YAML이 log 또는 DB에 저장되지 않는다.
```

---

## 16. 현재 n8n LLMOps Workflow

2026-06-10에 확인한 현재 n8n export에는 두 개의 LLMOps workflow가 있다.
Raw workflow JSON export는 webhook path, credential reference, Slack channel ID, API URL, prompt body를 포함할 수 있으므로 public documentation artifact로 commit하지 않는다.

| Workflow | Trigger | Source Signal | Output | AI Usage |
|---|---|---|---|---|
| `ClueRoom - LLMOps Light Monitor v4 Budgeted Gemini 3.5` | Manual + every 1h | 최근 60m window의 Loki `AI_CALL` logs, limit 500 | 기본 Slack LLMOps summary 먼저 전송 | `gemini-3.5-flash` optional analysis after basic summary |
| `ClueRoom - LLMOps Codex Handoff Report v2` | Manual + every 24h | 최근 24h Loki `AI_CALL` logs, limit 5000 | Human/Codex review용 Slack handoff report | Autonomous AI action 없음. Manual review용 report |

### 16.1 Hourly LLMOps Light Monitor

Hourly monitor는 `AI_CALL` structured log만 집계한다.
Raw prompt text, raw answer text, user free text, solution text, culprit data, API key, private scenario YAML을 읽으면 안 된다.

현재 aggregation fields:

```text
- call count
- success count
- failure count
- fallback count
- total token usage
- average latency
- max latency
- provider/model/promptVersion/featureType grouping
- metadata only slowest or failure/fallback samples
```

현재 severity rules:

```text
CRITICAL:
- 60m window failure count >= 3
- 60m window fallback count >= 3
- 60m window max latency >= 15000ms

WARNING:
- failure count >= 1
- fallback count >= 1
- max latency >= 8000ms
- average latency >= 5000ms
- total tokens >= 30000

INFO:
- 위 조건 없음
```

Slack output 순서:

```text
1. deterministic basic LLMOps summary
2. budget과 retry policy가 허용할 때만 Gemini analysis
```

Gemini policy:

```text
model: gemini-3.5-flash
daily limit: 2 calls
retry: one retry after 70s
failure behavior: Gemini failure 또는 budget skip이 basic Slack summary를 막으면 안 됨
```

### 16.2 Daily LLMOps Codex Handoff

Daily handoff는 report generator이며 autonomous remediation workflow가 아니다.
최근 24h `AI_CALL` data를 human review와 Codex-assisted PR 작업 후보로 요약한다.

현재 report contents:

```text
- total AI_CALL count
- success/failure/fallback totals
- failure rate
- fallback rate
- average and max latency
- total prompt/completion/overall token usage
- top featureType buckets by tokens
- top promptVersion buckets by tokens
- top promptVersion buckets by average latency
- bounded failure/fallback samples by metadata
```

허용 follow-up:

```text
- promptVersion cost/latency trend review
- dashboard change 제안
- backend telemetry improvement 제안
- PR review note draft
```

금지:

```text
- production prompt 직접 변경
- production model/provider setting 직접 변경
- raw prompt, answer, user question, solution text, culprit data, private scenario YAML 노출
- manual response sampling 없이 report를 gameplay quality 증거로 취급
```
