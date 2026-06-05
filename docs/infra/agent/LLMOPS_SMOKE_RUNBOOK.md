# ClueRoom LLMOps Smoke Runbook

> Status: ST-67 smoke and rollout guide.
>
> Scope: AI call structured logs, Prometheus metrics, and optional `ai_call_logs` DB persistence.

This runbook is for verifying the LLMOps metadata path after a backend deploy.

It must not expose prompt text, AI answer text, solution text, culprit data, API keys, or private scenario YAML.

---

## 1. What ST-67 Adds

Runtime signals:

```text
AI_CALL structured log
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

Default:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

This means metric/log telemetry works after deploy, but DB insert is disabled until a human creates the table and enables the flag.

---

## 2. Safety Rules

Do not log or store:

```text
- raw system prompt
- raw user prompt
- raw user question
- raw AI answer
- final deduction free text
- solution text
- culprit data
- private scenario YAML
- provider API keys
```

Request-level IDs such as `sessionId`, `scenarioId`, `suspectId`, and `npcCode` may appear in structured logs or DB rows, but they must not become Prometheus labels.

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

---

## 3. Smoke Preconditions

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

---

## 4. Generate One AI Call

Any one AI feature call can generate the initial smoke signal.

Preferred low-risk options:

```text
1. Use an existing test play session and send one interrogation request.
2. Run scenario validation for a non-production/custom scenario if available.
3. In local/mock mode, start the app and trigger a mock interrogation path.
```

Avoid final deduction smoke on an active demo session unless the team explicitly wants to complete that session.

---

## 5. Verify Structured Log

Check the active app log through the Blue-Green helper.

```bash
/opt/clueroom/bg-compose logs --tail=300 app-blue | grep 'AI_CALL'
/opt/clueroom/bg-compose logs --tail=300 app-green | grep 'AI_CALL'
```

Expected log shape:

```text
AI_CALL featureType=INTERROGATION provider=... model=... promptVersion=... scenarioId=... sessionId=... suspectId=... npcCode=... latencyMs=... success=... errorCode=... fallbackUsed=... promptTokens=... completionTokens=... totalTokens=...
```

Expected safety:

```text
- no prompt body
- no answer body
- no user free text
- no solution text
- no culprit data
- no API key
```

---

## 6. Verify Prometheus Metrics

Query Prometheus locally from the server.

```bash
curl -G 'http://localhost:9090/api/v1/query' \
  --data-urlencode 'query=ai_requests_total'
```

Useful queries:

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

Do not add `sessionId`, `scenarioId`, `suspectId`, `npcCode`, raw prompt, or raw answer as Prometheus labels.

---

## 7. Optional DB Persistence Setup

Only do this after deciding to store detailed metadata in MySQL.

Create the table first.

```bash
cd /opt/clueroom/app

DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
DB_NAME="$(grep -E '^DB_NAME=' .env | tail -n 1 | cut -d '=' -f2-)"
DB_NAME="${DB_NAME:-startup}"

docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql \
  mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" <<'SQL'
CREATE TABLE IF NOT EXISTS ai_call_logs (
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
SQL
```

Enable DB logging only after the table exists:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=true
```

Then redeploy or restart the active app through the normal Blue-Green deploy process so the new env value is loaded.

Confirm the value is actually present inside the running app container:

```bash
docker exec start-up-app-blue printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
docker exec start-up-app-green printenv | grep '^AI_LLMOPS_DB_LOGGING_ENABLED='
```

If one slot is intentionally stopped, check only the active/running slot from `/opt/clueroom/bg-status.sh`.

---

## 8. Verify DB Persistence

After enabling DB logging and generating one AI call:

```bash
cd /opt/clueroom/app

DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
DB_NAME="$(grep -E '^DB_NAME=' .env | tail -n 1 | cut -d '=' -f2-)"
DB_NAME="${DB_NAME:-startup}"

docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql \
  mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" <<'SQL'
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
SQL
```

Expected:

```text
Rows exist after AI calls.
No prompt body, answer body, user free text, solution text, culprit data, or API key columns exist.
```

---

## 9. Disable DB Persistence

If DB logging creates noise or the table is not ready:

```text
AI_LLMOPS_DB_LOGGING_ENABLED=false
```

Then redeploy or restart through the normal Blue-Green process.

Metrics and structured logs still work while DB persistence is disabled.

---

## 10. Rollback Notes

Code rollback:

```text
Use the normal Blue-Green rollback process.
```

DB table rollback:

```text
Do not drop ai_call_logs immediately during an incident.
Disable AI_LLMOPS_DB_LOGGING_ENABLED first.
Drop or archive the table later only after confirming no analysis needs it.
```

---

## 11. Completion Criteria

```text
- AI_CALL structured log appears after one AI feature call.
- ai_requests_total is visible from local Prometheus query.
- ai_latency_seconds_count is visible from local Prometheus query.
- ai_fallbacks_total is visible when fallback occurs.
- ai_tokens_total is visible only when provider token metadata exists.
- Optional ai_call_logs rows appear only after table creation and AI_LLMOPS_DB_LOGGING_ENABLED=true.
- No raw prompt, raw answer, solution text, culprit data, API key, or private scenario YAML is logged or stored.
```
