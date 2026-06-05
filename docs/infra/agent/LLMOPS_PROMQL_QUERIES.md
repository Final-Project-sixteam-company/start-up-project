# ClueRoom LLMOps PromQL Query Candidates

> Status: ST-67 query candidate document.
>
> Scope: Prometheus queries for AI call metrics emitted by the backend.

This document defines initial PromQL queries for Grafana panels and manual checks.

It does not create Grafana dashboards or alerts.

---

## 1. Metric Names

Spring/Micrometer names and Prometheus names:

| Micrometer | Prometheus | Meaning |
|---|---|---|
| `ai.requests` | `ai_requests_total` | AI feature call events, including provider/mock/fallback events |
| `ai.failures` | `ai_failures_total` | failed provider/client calls |
| `ai.fallbacks` | `ai_fallbacks_total` | fallback responses used |
| `ai.tokens` | `ai_tokens_total` | token usage when provider metadata exposes it |
| `ai.latency` | `ai_latency_seconds_*` | AI call latency timer series |

Expected low-cardinality labels:

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

Forbidden Prometheus label types:

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

Request-level IDs belong in structured logs or the optional `ai_call_logs` table, not Prometheus labels.

---

## 2. Manual Metric Presence Checks

Run from the production server because public `/actuator/prometheus` is blocked by Nginx hardening.

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_requests_total'
```

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_latency_seconds_count'
```

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_fallbacks_total'
```

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_tokens_total'
```

Notes:

```text
ai_tokens_total may be absent until the provider returns token usage metadata.
ai_fallbacks_total may be absent until fallback occurs.
```

---

## 3. Request Volume Panels

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

---

## 4. Failure And Fallback Panels

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
clamp_min(sum by (feature_type) (increase(ai_requests_total[5m])), 1)
```

Failure ratio by feature:

```promql
sum by (feature_type) (increase(ai_failures_total[5m]))
/
clamp_min(sum by (feature_type) (increase(ai_requests_total[5m])), 1)
```

Interpretation:

```text
Fallback may preserve demo UX, but it means the real provider or parser path did not complete.
Failure and fallback panels should be reviewed together.
```

---

## 5. Latency Panels

Default average latency by feature type:

```promql
sum by (feature_type) (rate(ai_latency_seconds_sum[5m]))
/
clamp_min(sum by (feature_type) (rate(ai_latency_seconds_count[5m])), 1)
```

Request count by feature type:

```promql
sum by (feature_type) (
  increase(ai_latency_seconds_count[5m])
)
```

p95 latency by feature type, only if Prometheus histogram buckets are enabled:

```promql
histogram_quantile(
  0.95,
  sum by (feature_type, le) (
    rate(ai_latency_seconds_bucket[5m])
  )
)
```

p50 latency by feature type, only if Prometheus histogram buckets are enabled:

```promql
histogram_quantile(
  0.50,
  sum by (feature_type, le) (
    rate(ai_latency_seconds_bucket[5m])
  )
)
```

Notes:

```text
ai_latency_seconds_bucket may not exist unless histogram buckets are enabled for this timer.
Use ai_latency_seconds_sum/count for the first dashboard if buckets are not present.
AI final deduction and scenario validation can naturally be slower than interrogation.
Do not use one global latency threshold for every feature.
```

---

## 6. Token Panels

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

Notes:

```text
Token metrics depend on provider metadata.
If the provider does not return usage, request/latency metrics can still work.
```

---

## 7. Initial Grafana Panel Candidates

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

Avoid showing:

```text
- sessionId
- scenarioId
- suspectId
- npcCode
- user question
- prompt text
- answer text
- solution text
- private scenario YAML
```

Grafana should show operational signals, not gameplay spoilers.

---

## 8. Alert Candidates

Do not create alerts before observing normal traffic.

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

## 9. Completion Criteria

```text
- PromQL candidates are documented.
- Queries avoid high-cardinality labels.
- Query notes distinguish provider attempts from fallback events.
- Latency interpretation separates interrogation, final deduction, and scenario validation.
- Token usage caveat is documented.
- No secret, prompt, answer, or solution text is included.
```
