# ClueRoom Security / Traffic / Alert Policy

## 1. Purpose

This document is the canonical policy for traffic defense, rate limiting, GeoIP/bot handling, and Grafana alert design.
Detailed operational commands remain in `OPS_RUNBOOK.md`.
Infrastructure structure and scale-out rationale remain in `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`.

Absorbed source documents:

| Previous document | Absorbed here |
|---|---|
| `RATE_LIMIT_POLICY.md` | rate limit principles, API policy candidates, Redis/Nginx layer split, rollout order |
| `GEOIP_BOT_TRAFFIC_POLICY.md` | bot/GeoIP interpretation, blocking options, production enforcement criteria |
| `GRAFANA_ALERT_POLICY.md` | alert principles, safe alert candidates, Blue-Green false-positive rules, notification policy |

## 2. Current Baseline

```text
- Nginx is the public ingress for api.clueroom.xyz.
- Spring Boot app runs behind Blue-Green slots.
- Prometheus/Grafana exist for metrics and dashboarding.
- ops Loki/Alloy can be used for read-only log and heartbeat inspection.
- n8n routes Grafana alerts to Slack.
- Nginx API per-IP rate limit is enforced.
- Nginx CN IPv4 block is applied.
- Manual blocklist snippet is available for narrow abusive IP blocks.
- Heavy AI endpoints need extra care because they can create direct LLM cost.
- Frontend E2E and demo stability are higher priority than aggressive blocking.
```

Do not add multiple new enforcement layers at once.
For new or changed rules, the rollout principle remains observe first, then dry-run, then enforce only after evidence is stable.
The current API rate-limit and CN block baseline has already passed that gate and is the operating state.

## 3. Layer Separation

### Nginx IP-Based Rate Limit

Use Nginx for coarse request throttling by IP.

Good for:

```text
- broad API abuse
- AI endpoint burst protection
- FCM token registration spam
- obvious bot/crawler burst control
```

Nginx must not make user/account/business decisions.
It only sees IP/path/method and should remain coarse.

### Backend Redis Rate Limit

Use backend Redis rate limiting for user/session/scenario-aware rules.

Good for:

```text
- per user AI call limit
- per session interrogation/final-deduction pacing
- per device FCM registration pacing
- abuse control that needs authenticated identity
```

Backend rate limit should return API error responses that Android can handle.

### Cloudflare / WAF

Cloudflare WAF can be used for coarse bot/country/path rules if DNS/proxy setup is active.
Prefer Cloudflare for broad L7 controls and quick rollback, but do not depend on it for app-specific cost controls.

## 4. API Rate Limit Candidates

| API group | Initial policy direction |
|---|---|
| General read APIs | high enough to avoid normal app friction |
| `POST /api/play-sessions/{sessionId}/interrogations` | stricter due AI cost |
| `POST /api/play-sessions/{sessionId}/final-deduction` | stricter due AI cost and state transition |
| `POST /api/ai/scenarios/{scenarioId}/validate` | strictest among normal user-visible APIs |
| FCM token registration | medium strictness, device/user aware later |
| image/static URLs | prefer CDN/S3 controls, not app Nginx only |
| health/swagger/preflight | do not break deploy, QA, CORS, or monitoring |

Current Nginx enforcement baseline:

```text
limit_req_zone clueroom_api_per_ip: 20r/s
burst=60 nodelay
limit_req_dry_run off
limit_req_status 429
limit_conn per IP: 30
```

Operational interpretation:

```text
- 429 means Nginx rate limit, not a permanent ban.
- 403 means CN block, manual blocklist, sensitive path block, or another explicit deny rule.
- 403/429 alerts are warning-level signals unless normal user impact is confirmed.
- Health/swagger/preflight must remain safe during deploy, QA, and monitoring.
```

Reference Nginx shape:

```nginx
limit_req_zone $binary_remote_addr zone=clueroom_api_per_ip:10m rate=20r/s;
limit_conn_zone $binary_remote_addr zone=clueroom_conn_per_ip:10m;

location /api/ {
    limit_req zone=clueroom_api_per_ip burst=60 nodelay;
    limit_req_dry_run off;
    limit_req_status 429;
    limit_conn clueroom_conn_per_ip 30;
    proxy_pass http://clueroom_backend;
}
```

Backend Redis rate-limit key candidates remain useful for future user/session-aware quotas:

```text
rate:ai:interrogation:user:{userId}:session:{sessionId}
rate:ai:validation:user:{userId}:scenario:{scenarioId}
rate:final-deduction:session:{sessionId}
rate:device-token:user:{userId}:token:{tokenHash}
```

429 response shape candidate:

```json
{
  "success": false,
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
    "retryAfterSeconds": 30
  }
}
```

`Retry-After` header can be considered for API consumers.
The response must not include secret, scenario solution, prompt, AI provider details, or private state.

## 5. Rate Limit Rollout

1. Back up current Nginx site and snippet config.
2. Add or change zone/snippet in dry-run mode first when changing thresholds or new endpoint groups.
2. Validate Nginx syntax.
3. Reload Nginx.
4. Run normal Android/API smoke.
5. Inspect access/error logs for dry-run hits.
6. Tune thresholds.
7. Only then switch the changed rule to enforcement.

Do not expand enforcement during frontend E2E QA or before demo without explicit approval.

New production enforcement or threshold change requires:

```text
- no normal Android smoke failures
- no health/swagger/preflight breakage
- reviewed dry-run hit sample
- rollback command ready
- threshold documented in this policy or OPS_RUNBOOK
```

## 6. Bot / GeoIP Policy

### Interpretation Rules

Do not treat unknown IP/country traffic as an incident by itself.
Correlate with:

```text
- request rate
- path pattern
- response status
- user-agent
- AI endpoint hits
- DB/app resource impact
- repeated 4xx/5xx
```

Read-only inspection is allowed.
Current CN IPv4 block is part of the operating baseline.
New country blocks, manual IP blocks, or wider deny rules still require evidence and rollback path.

### Blocking Options

| Option | Use when | Notes |
|---|---|---|
| Cloudflare WAF custom rules | Cloudflare proxy is active and quick rollback is needed | preferred broad control |
| Nginx geo map | country/CIDR block must live near ingress | current CN IPv4 block uses an aggregated map |
| ipset / nftables | severe L3/L4 abuse | higher operational risk, avoid early |

### Production Enforcement Criteria

For new blocks beyond the current CN baseline, block only if most of these are true:

```text
- traffic is clearly abusive or automated
- normal team/QA traffic is not affected
- blocking scope is narrow
- rollback is one command/config revert
- evidence is captured without secrets
- infra lead approves
```

Do not create additional permanent country blocking based on a single log sample.

### GeoIP / Bot PoC Appendix

Read-only log inspection commands:

```bash
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo awk '$9 ~ /^4/ {print $1, $7, $9}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -30
sudo grep -E '(\.env|\.git|wp-admin|wp-login|phpmyadmin|pma|vendor|server-status)' /var/log/nginx/access.log | tail -n 80
sudo awk -F\" '{print $6}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo tail -n 100 /var/log/nginx/error.log
```

Current CN block shape:

```nginx
geo $clueroom_is_cn_ip {
    default 0;
    include /etc/nginx/geoip/cn-aggregated.map;
}

if ($clueroom_is_cn_ip) {
    return 403;
}
```

Rollback means removing the include/deny rule, running `nginx -t`, reloading Nginx, and verifying `/actuator/health`.

## 7. Grafana Alert Principles

Alerts should be actionable.
A dashboard panel can be noisy; an alert must require action.

Design principles:

```text
- Prefer external user-impact signals over internal noise.
- Prefer active Blue-Green slot health over standby slot health.
- Avoid alerts that require missing exporters to be accurate.
- Do not page on expected deploy transitions.
- Start with manual review or low-severity notifications before critical alerts.
```

## 8. Safe Alert Candidates

### External API Health Down

Candidate signal:

```text
https://api.clueroom.xyz/actuator/health unavailable or unhealthy
```

Use as high priority because it reflects user-visible API availability.

### HTTP 5xx Increase

Candidate signal:

```text
5xx rate increases over baseline
```

Use only with a time window to avoid single transient failures.

### HTTP Latency Increase

Candidate signal:

```text
p95/p99 API latency crosses threshold for sustained period
```

Tune after real traffic exists.

### JVM Memory Increase

Candidate signal:

```text
JVM memory pressure sustained over threshold
```

Use warning first, not critical, until baseline is known.

### Prometheus Scrape Failure

Current Prometheus jobs:

```text
clueroom-app
clueroom-app-blue
clueroom-app-green
prometheus
```

Do not alert critically on one standby Blue-Green target down.

### Data / Backup / Ops Health

Current Loki heartbeat signals:

```text
DATA_HEALTH
S3_BACKUP_HEALTH
OPS_HEALTH
SERVER_HEALTH
```

Safe alert examples:

```text
- DATA_HEALTH reports MySQL or Redis failure
- S3_BACKUP_HEALTH reports S3 upload failure, missing state, or stale upload
- OPS_HEALTH reports Loki/n8n critical failure
- heartbeat missing for the expected window
```

S3 backup alerts should distinguish:

```text
backup failed
S3 upload failed
S3 backup heartbeat missing
restore rehearsal not recently verified
```

### Nginx 403 / 429

Current alert interpretation:

```text
403 increase
→ usually block rules working
→ check normal-user impact and repeated source IPs

429 increase
→ rate limit working
→ check normal-user impact and endpoint distribution
```

403/429 should start as warning. Escalate only if normal user traffic is affected or API availability drops.

## 9. Alerts Not Safe Yet

Do not make critical alerts yet for these without better exporters or validated bridges:

```text
- host disk low from incomplete source
- host CPU/RAM high without stable exporter
- one standby Blue-Green app target down
```

Ops Snapshot disk judgement uses `SERVER_HEALTH`, `DATA_HEALTH`, `OPS_HEALTH` `disk_max_percent` as the source of truth.
Raw snapshot text percentages are fallback only.

## 10. Blue-Green Alert Rules

Safe policy:

```text
- active upstream app failure is important.
- both app-blue and app-green down is critical.
- standby app-blue/app-green down can be normal after deploy.
- external /actuator/health is more important than one scrape target.
```

Avoid naive critical alert:

```promql
up{job=~"clueroom-app-blue|clueroom-app-green"} == 0
```

Better checks combine active slot information, external health, and both-target-down conditions.

### Alert Threshold Appendix

These are candidate thresholds only. Confirm actual Prometheus metric names in Grafana Explore before creating alerts.

| Alert | Candidate metric/expression | Window | Threshold | Severity |
|---|---|---:|---:|---|
| AI failures spike | `sum(increase(ai_failures_total[5m]))` | 5m | `>= 23` | WARNING first |
| AI p95 latency high | `histogram_quantile(0.95, sum(rate(ai_latency_seconds_bucket[5m])) by (le, feature_type))` | 5m | `> 23s` | WARNING first |
| AI fallback spike | `sum(increase(ai_fallbacks_total[5m]))` | 5m | tune after baseline | WARNING first |
| Both Blue-Green targets down | `up{job="clueroom-app-blue"} == 0 and up{job="clueroom-app-green"} == 0` | 1~3m | true | CRITICAL candidate |
| Prometheus scrape down | `up{job="prometheus"} == 0` | 1~3m | true | CRITICAL candidate |
| Nginx 403 blocked request | Loki access log query matching HTTP 403 | 5~10m | tune after baseline | WARNING |
| Nginx 429 rate limit | Loki access log query matching HTTP 429 | 5~10m | tune after baseline | WARNING |
| S3 backup failed/stale | Loki `{job="s3-backup-health", instance="clueroom-data-01"}` status failure/stale | 10m | failure present | CRITICAL |
| S3 backup heartbeat missing | Loki S3_BACKUP_HEALTH count | 10m | below 1 | CRITICAL |

Prometheus-exported Micrometer names use underscore form, so Java metric names `ai.failures`, `ai.latency`, and `ai.fallbacks` are expected as `ai_failures_total`, `ai_latency_seconds_*`, and `ai_fallbacks_total`.
If the bucket series does not exist, do not create a p95 alert until histogram publishing is verified.

## 11. Notification Policy

Current notification path:

```text
1. Grafana alert fires
2. n8n receives alert webhook
3. n8n sends deterministic Slack alert first
4. Gemini may add optional analysis
5. Codex is used for manual handoff/deep analysis, not real-time automatic fallback
```

### Current n8n Workflow Inventory

This table summarizes the active n8n workflow exports reviewed on 2026-06-10.
Do not commit the raw workflow JSON exports because they can contain webhook paths, credential references, Slack channel IDs, URLs, or prompt bodies.

| Workflow | Trigger | Monitors / Inputs | Deterministic Slack Output | Optional AI / Handoff | Failure Budget |
|---|---|---|---|---|---|
| `ClueRoom - Grafana Alert Router v8 Budgeted Gemini 3.5` | Grafana POST webhook | Grafana alert payload, related Loki logs for Nginx 5xx or app ERROR/Exception | Basic alert first: status, max severity, firing/resolved counts, alert summary, immediate next checks | `gemini-3.5-flash` adds short Korean analysis after the basic alert | Gemini only for firing alerts, daily Gemini limit 3, one retry after 70s, basic alert is never blocked by Gemini |
| `ClueRoom - Ops Snapshot Agent v5 Lite Daily Budget` | Manual plus scheduled every 24h | `/opt/clueroom/ops-snapshot.sh`, `DATA_HEALTH`, `SERVER_HEALTH` | Basic ops status first: prod/data health, Nginx syntax signal, disk/memory summary, recent error pattern count | `gemini-2.5-flash-lite` adds Korean ops analysis after the basic status | Daily Gemini limit 1, one retry after 70s, basic status is never blocked by Gemini |
| `ClueRoom - LLMOps Light Monitor v4 Budgeted Gemini 3.5` | Manual plus scheduled every 1h | Loki `AI_CALL` logs for the recent 60m window, limit 500 | Basic LLMOps summary first: count, success/failure/fallback, latency, token total, top feature/prompt groups | `gemini-3.5-flash` adds short LLMOps analysis after the basic summary | Daily Gemini limit 2, one retry after 70s, basic summary is never blocked by Gemini |
| `ClueRoom - Infra Codex Handoff Report v1` | Manual plus scheduled every 24h | Ops snapshot, `DATA_HEALTH`, `SERVER_HEALTH`, `OPS_HEALTH`, recent Nginx 5xx, app ERROR/Exception, recent `AI_CALL` | Slack handoff report for human/Codex review | Codex handoff report only; no autonomous production action | Slack report only; agent action remains human-approved |
| `ClueRoom - LLMOps Codex Handoff Report v2` | Manual plus scheduled every 24h | Loki `AI_CALL` logs for recent 24h, limit 5000 | Slack LLMOps handoff: totals, failure/fallback rate, latency, tokens, top prompt buckets, sample failures | Codex handoff report only; no autonomous prompt or runtime change | Slack report only; prompt/backend changes require PR review |

### Workflow Alert Contents

The current alert/reporting split is:

```text
Grafana Alert Router
→ event notification
→ critical firing alerts may mention the channel
→ includes basic runbook checks before any Gemini analysis

Ops Snapshot Agent
→ periodic state report
→ checks prod/data health, heartbeat bridge, Nginx syntax, disk/memory, and recent error patterns
→ WARNING/INFO output is report-grade unless paired with user-facing impact

LLMOps Light Monitor
→ hourly AI_CALL summary
→ watches failure count, fallback count, latency, token total, and feature/prompt grouping
→ CRITICAL when failure >= 3, fallback >= 3, or max latency >= 15s in the 60m window
→ WARNING when failure/fallback exists, max latency >= 8s, average latency >= 5s, or total tokens >= 30000

Infra Codex Handoff
→ daily infra review input
→ combines health heartbeats, recent Nginx 5xx, app errors, and AI_CALL samples

LLMOps Codex Handoff
→ daily LLMOps review input
→ summarizes 24h AI_CALL cost, latency, failure, fallback, and promptVersion candidates
```

Gemini analysis is advisory only.
If Gemini fails, times out, returns an unusable response, or exceeds daily budget, the deterministic Slack message must still be sent.

Candidate channels:

```text
#clueroom-alerts
#clueroom-infra
```

Do not send secrets, raw `.env`, raw user questions, AI answers, DB passwords, private keys, or Firebase JSON to Slack.

## 12. Do Not Do

```text
- Do not expand Nginx enforcement without dry-run evidence.
- Do not block an additional country based on one suspicious IP.
- Do not page on standby Blue-Green target down by itself.
- Do not expose Prometheus directly to the public internet.
- Do not paste raw access logs containing user input into public PRs.
- Do not move Loki/n8n back onto the prod app server.
- Do not use rate limiting to hide backend 5xx bugs.
```

## 13. Review Checklist

Before changing traffic/security/alert policy:

```text
- Is this observe, dry-run, or enforcement?
- What normal user flow could be affected?
- What is the rollback?
- Is there evidence without secrets?
- Does Android E2E still pass?
- Does actuator health still work?
- Does Swagger/preflight still work if it must remain available?
- Is the active Blue-Green slot distinguished from standby?
```
