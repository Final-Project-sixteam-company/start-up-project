# ClueRoom Monitoring Agent Plan

> Purpose: define how ClueRoom can introduce an AI-assisted monitoring agent without weakening production safety.
>
> Status: planning document. This does not add n8n, Loki, exporters, webhooks, or runtime workers.

---

## 1. Monitoring Agent Purpose

The Monitoring Agent helps humans interpret operational signals.

It should answer questions like:

```text
- Is the API currently healthy?
- Is this a deploy problem, app problem, Nginx problem, DB problem, or resource problem?
- Is a Prometheus target alert likely real or a Blue-Green standby false positive?
- What safe read-only check should be run next?
- What action requires human approval?
```

The agent must not:

```text
- mutate production state
- execute rollback automatically
- read or print secrets
- treat standby slot shutdown as an incident by itself
- replace human deploy ownership
```

The first production value is not automation. It is faster and more consistent incident interpretation.

---

## 2. Input Data

The primary input is a human-reviewed Ops Snapshot.

Reference:

```text
docs/infra/agent/OPS_SNAPSHOT_SPEC.md
```

Recommended input categories:

```text
- snapshot metadata
- external API health
- Blue-Green active/standby status
- Docker container status
- host memory summary
- heartbeat disk summary from SERVER_HEALTH / DATA_HEALTH / OPS_HEALTH
- Docker disk summary
- Nginx syntax result
- Prometheus health
- Grafana health
- bounded app log tail
- bounded Nginx error log tail
- recent GitHub Actions CD run summary
```

Forbidden input:

```text
- .env values
- API keys
- DB passwords
- Firebase service account JSON
- SSH private keys
- private scenario spoiler YAML
- DB dump contents
- full user request bodies
```

The agent should also accept a minimal manual input when no snapshot script exists yet.

```text
externalHealth:
blueGreenStatus:
dockerPs:
nginxTest:
recentErrorSummary:
```

---

## 3. Output JSON Format

Monitoring Agent output should be structured enough for Discord/Slack or a dashboard.

```json
{
  "severity": "OK",
  "status": "external_api_healthy",
  "summary": "External API health is UP. app-green is stopped as standby, which is normal.",
  "evidence": [
    "External /actuator/health returned 200",
    "Blue-Green active slot is app-blue",
    "Nginx syntax is OK"
  ],
  "likelyCause": null,
  "falsePositiveNotes": [
    "app-green target down is expected because standby can be stopped"
  ],
  "resourceSignals": {
    "prodDisk": {
      "value": 19,
      "source": "SERVER_HEALTH.disk_max_percent",
      "confidence": "high"
    },
    "prodMemory": {
      "availableMb": 412,
      "source": "snapshot.free.available",
      "notificationClass": "report"
    }
  },
  "notificationPolicy": {
    "immediateSlack": false,
    "recommendedChannel": "24h_report",
    "roleSplit": "Grafana Alert handles event notifications; Ops Snapshot Agent handles periodic status reports."
  },
  "safeReadOnlyChecks": [
    "Run /opt/clueroom/bg-status.sh",
    "Run curl -I https://api.clueroom.xyz/actuator/health"
  ],
  "humanApprovedActions": [],
  "rollbackRequired": false,
  "confidence": "high"
}
```

Required fields:

| Field | Meaning |
|---|---|
| `severity` | `OK`, `INFO`, `WARNING`, or `CRITICAL` |
| `status` | short machine-readable status |
| `summary` | human-readable status |
| `evidence` | observed facts only |
| `likelyCause` | inference, or `null` |
| `falsePositiveNotes` | alert interpretation caveats |
| `resourceSignals` | parsed resource facts with source and confidence |
| `notificationPolicy` | whether the result should page immediately or be reported |
| `safeReadOnlyChecks` | commands or checks that do not mutate state |
| `humanApprovedActions` | candidate remediation requiring approval |
| `rollbackRequired` | whether rollback should be considered |
| `confidence` | `low`, `medium`, or `high` |

---

## 4. Severity Classification

| Severity | Meaning | Example |
|---|---|---|
| `OK` | healthy, no action needed | external API health UP |
| `INFO` | expected or low-impact event | standby slot stopped after deploy |
| `WARNING` | possible issue, service still usable | disk approaching threshold, one non-active target down |
| `CRITICAL` | user-facing outage or high data risk | external health fail, active upstream down, DB unavailable |

Severity rules:

```text
- External API health failure is CRITICAL unless proven to be a transient test issue.
- Active upstream health failure is CRITICAL.
- app-blue or app-green down alone is not CRITICAL.
- Both app slots down is CRITICAL.
- Nginx syntax failure after config edit is WARNING or CRITICAL depending on reload status.
- Disk severity must come from heartbeat `disk_max_percent` when available.
- Raw snapshot text percentage parsing is fallback only and must cite low/medium confidence.
- Disk full or DB unavailable is CRITICAL when it affects runtime.
- Prod available-memory WARNING alone is report-grade unless paired with OOM, restart loop, active health failure, or user-facing impact.
```

Resource parser source-of-truth:

```text
prod disk: SERVER_HEALTH disk_max_percent
data disk: DATA_HEALTH disk_max_percent
ops disk: OPS_HEALTH disk_max_percent
prod memory: available memory from snapshot/free output
```

Do not infer disk pressure from arbitrary percentages in raw snapshot text.
Known false-positive sources include curl progress, HTTP percentages, Docker CPU/memory percentages, log prose, and unrelated threshold text.

---

## 5. Failure Type Classification

Use stable categories so later dashboards can aggregate incidents.

| Type | Meaning |
|---|---|
| `EXTERNAL_HEALTH_FAIL` | public API health endpoint fails |
| `ACTIVE_UPSTREAM_FAIL` | active Blue-Green slot is unhealthy |
| `STANDBY_TARGET_DOWN_EXPECTED` | standby app target is intentionally stopped |
| `BOTH_APP_TARGETS_DOWN` | both app-blue and app-green unavailable |
| `NGINX_CONFIG_ERROR` | `nginx -t` fails |
| `DB_HEALTH_RISK` | MySQL unreachable or app reports DB failure |
| `REDIS_HEALTH_RISK` | Redis unreachable or app reports Redis failure |
| `HOST_RESOURCE_RISK` | disk, memory, or CPU pressure |
| `CONTAINER_RESOURCE_RISK` | container CPU/RAM pressure |
| `DEPLOY_SCRIPT_FAIL` | deploy script failed or left inconsistent state |
| `AI_COST_RISK` | unusual AI feature request volume or quota pressure |
| `OBSERVABILITY_GAP` | alert needs missing exporter or missing health bridge |

For AI cost risk, do not rely only on Nginx IP rate limit.

```text
Nginx Rate Limit is edge-level protection.
AI cost defense requires Redis-backed application-level quota by userId, sessionId, scenarioId, and featureType.
```

---

## 6. Execution Location Options

### Option A. Manual / Local PoC

```text
Operator collects an Ops Snapshot.
Operator reviews it for secrets.
Operator sends it to Gemini Flash or another approved model.
Agent returns structured analysis.
```

This is the recommended Phase 1.

Why:

```text
- no new production service
- no new runtime secret on the server
- lowest blast radius
- easiest to review prompt quality
```

### Option B. n8n Workflow

```text
n8n receives an Ops Snapshot or webhook payload.
n8n calls Gemini API.
n8n sends a Discord/Slack summary.
```

Use only after infra server separation is available.

Do not add n8n to the current single production app server as a permanent service during MVP.

### Option C. Spring Boot Worker

```text
Spring Boot scheduled worker collects or receives status.
Worker calls an AI provider.
Worker writes alert summary or sends webhook.
```

This needs additional design because the current backend dependency direction is centered on Spring AI OpenAI.
If Gemini is used, provider integration, quota, and secret handling must be reviewed first.

### Option D. Separate Python Worker

```text
Python process reads snapshot data.
Python process calls Gemini API.
Python process sends Discord/Slack alert.
```

This is flexible but introduces server resource and secret-management risk.

Recommended secret path for a future worker:

```text
/opt/clueroom/secrets/env.d/monitoring.env
```

The file must never be committed or printed.

---

## 7. Gemini Flash Selection Rationale

Gemini Flash is a candidate for Monitoring Agent analysis because:

```text
- low latency for summary and classification
- lower cost for frequent operational summaries
- adequate reasoning for bounded Ops Snapshot analysis
- suitable for JSON-format incident summaries
```

The model is not used for autonomous remediation.

The agent output must be treated as an analysis suggestion, not as final authority.

---

## 8. Discord / Slack Alert PoC

Phase 1 can be manual.

```text
Operator runs or assembles a snapshot.
Operator asks the model to classify it.
Operator manually posts the result in Discord/Slack.
```

Phase 2 can use a webhook, but still with human-reviewed input.

Notification policy:

```text
CRITICAL
→ Slack immediate notification is allowed.

WARNING / INFO
→ manual execution summary or 24h report is recommended.

Event-style alerting
→ Grafana Alert owns this.

Periodic state reporting
→ Ops Snapshot Agent owns this.
```

Recommended message shape:

```text
[ClueRoom Ops Snapshot] WARNING - external health slow but UP

Role:
- Grafana Alert: event notifications
- Ops Snapshot Agent: periodic status reports

Evidence:
- /actuator/health returned 200 after 4.2s
- active slot app-blue running
- app-green stopped as standby
- prod disk 19% from SERVER_HEALTH.disk_max_percent
- prod memory available 412MB, report-grade warning

Next read-only checks:
- /opt/clueroom/bg-status.sh
- /opt/clueroom/bg-compose logs --tail=120 app-blue
```

Plain `docker compose logs --tail=120 app` is only for legacy single-app or local compose mode, not production Blue-Green mode.

Do not send secrets or raw `.env` output to Discord/Slack.

---

## 9. Loki / n8n Adoption Timing

Do not add Loki and n8n to the current production app server all at once.
Current ops-side Loki/Alloy can already be used as a read-only signal source in Ops Snapshot checks.
The caution here is about adding more runtime services to the app server, not about denying the existing ops Loki/Alloy path.

Recommended path:

```text
Phase 1: Manual / Local PoC with human-reviewed Ops Snapshot
Phase 2: Discord/Slack manual alert summary
Phase 3: infra server separation
Phase 4: n8n workflow for scheduled summaries or webhook payloads
Phase 5: expand ops Loki/Alloy log search and retention policy if team log sharing becomes necessary
Phase 6: dedicated worker only if n8n is not enough
```

Reason:

```text
- n8n adds another runtime service and secret store
- Loki/Alloy storage and retention policy must be controlled on the ops side
- current single server should preserve API stability for demo/MVP
```

---

## 10. False Positive Prevention Rules

### 10.1 Blue-Green Targets

Prometheus may scrape these jobs:

```text
clueroom-app
clueroom-app-blue
clueroom-app-green
```

Blue-Green operation can intentionally stop the standby slot.

Do not create critical alerts for:

```text
app-blue target down
app-green target down
```

by themselves.

Use these higher-signal checks:

```text
- external /actuator/health
- /opt/clueroom/bg-status.sh
- active Nginx upstream
- both app targets down
- active slot health fail
```

Critical alert candidates:

```text
- external health fail
- active upstream health fail
- app-blue and app-green both down
- Nginx cannot route to active upstream
```

### 10.2 Exporter Gaps

The MVP Prometheus setup does not fully cover every infra component.

| Signal | Current MVP | Needed For Accurate Alert |
|---|---|---|
| host disk/cpu/memory | manual commands | node_exporter |
| container CPU/RAM | `docker stats` manual check | cAdvisor |
| MySQL health | app health or manual check | mysqld_exporter or health bridge |
| Redis health | app health or manual check | redis_exporter or health bridge |

The Monitoring Agent should mark these as `OBSERVABILITY_GAP` rather than inventing certainty.

### 10.3 Snapshot Resource Parsing

Ops Snapshot Agent v3 must not treat every `%` in raw text as disk usage.

Disk parser order:

```text
1. SERVER_HEALTH disk_max_percent for prod
2. DATA_HEALTH disk_max_percent for data
3. OPS_HEALTH disk_max_percent for ops
4. fallback df output only when heartbeat is missing or stale
```

If fallback parsing is used, include:

```text
source=fallback.df
confidence=low or medium
falsePositiveNotes includes "heartbeat disk_max_percent missing/stale"
```

Prod memory parser:

```text
Use available memory when present.
WARNING memory alone is a report finding, not an hourly Slack alert.
Escalate only with active health failure, OOM/restart evidence, or user-facing impact.
```

### 10.4 AI Cost Defense

Nginx IP rate limit is useful but insufficient.

Reasons:

```text
- Android users can share one carrier NAT or Wi-Fi IP.
- attackers can rotate IPs.
- AI interrogation, scenario validation, and final deduction are cost-bearing features.
```

Recommended layered defense:

```text
1. Nginx IP rate limit for edge-level bot traffic.
2. Redis backend quota by userId, sessionId, scenarioId, and featureType.
3. Separate AI quota for interrogation, scenario validation, and final deduction.
4. Monitoring metric or log summary for quota hits and fallback usage.
```

### 10.5 Android Kotlin Context

Monitoring documents should refer to the client as:

```text
Android App
Android baseUrl
Android Repository
API client
```

Do not introduce Flutter/Dio assumptions into ClueRoom infrastructure docs.

