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
- Heavy AI endpoints need extra care because they can create direct LLM cost.
- Frontend E2E and demo stability are higher priority than aggressive blocking.
```

Do not add multiple new enforcement layers at once.
The default rollout principle is observe first, then dry-run, then enforce only after evidence is stable.

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

Initial Nginx dry-run groups:

```text
General API:
- /api/scenarios
- /api/play-sessions read paths

AI cost APIs:
- /api/play-sessions/*/interrogations
- /api/play-sessions/*/final-deduction
- /api/ai/scenarios/*/validate

Device/FCM:
- FCM token registration path when implemented
```

Initial numeric candidates:

```text
General read APIs:
- Nginx: 5~10 req/s per IP, burst 20
- Backend Redis: usually not needed for read-only MVP APIs

AI cost APIs:
- Nginx: 1 req/s per IP, burst 3~5
- Backend Redis: user/session/scenario-aware quota

FCM token registration:
- Nginx: 1~2 req/s per IP, burst 5
- Backend: upsert by userId + token hash

Health / Swagger / Preflight:
- Do not apply aggressive limits.
- Keep health checks and CORS preflight safe during deploy, QA, and monitoring.
```

Documentation-only Nginx shape:

```nginx
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=ai_per_ip:10m rate=1r/s;
limit_req_zone $binary_remote_addr zone=device_token_per_ip:10m rate=2r/s;

location /api/ {
    limit_req zone=api_per_ip burst=20 nodelay;
    proxy_pass http://clueroom_backend;
}

location ~ ^/api/(play-sessions/.*/interrogations|ai/scenarios/.*/validate|play-sessions/.*/final-deduction) {
    limit_req zone=ai_per_ip burst=5;
    proxy_pass http://clueroom_backend;
}

location = /api/device-tokens {
    limit_req zone=device_token_per_ip burst=5 nodelay;
    proxy_pass http://clueroom_backend;
}
```

Backend Redis rate-limit key candidates:

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

1. Add zone/snippet in dry-run mode only.
2. Validate Nginx syntax.
3. Reload Nginx.
4. Run normal Android/API smoke.
5. Inspect access/error logs for dry-run hits.
6. Tune thresholds.
7. Only then consider enforcement.

Do not enforce during frontend E2E QA or before demo without explicit approval.

Production enforcement requires:

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
Blocking requires evidence and rollback path.

### Blocking Options

| Option | Use when | Notes |
|---|---|---|
| Cloudflare WAF custom rules | Cloudflare proxy is active and quick rollback is needed | preferred broad control |
| Nginx GeoIP2 | country-based logic must live near ingress | adds module/config complexity |
| ipset / nftables | severe L3/L4 abuse | higher operational risk, avoid early |

### Production Enforcement Criteria

Block only if most of these are true:

```text
- traffic is clearly abusive or automated
- normal team/QA traffic is not affected
- blocking scope is narrow
- rollback is one command/config revert
- evidence is captured without secrets
- infra lead approves
```

Do not create permanent country blocking based on a single log sample.

### GeoIP / Bot PoC Appendix

Read-only log inspection commands:

```bash
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo awk '$9 ~ /^4/ {print $1, $7, $9}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -30
sudo grep -E '(\.env|\.git|wp-admin|wp-login|phpmyadmin|pma|vendor|server-status)' /var/log/nginx/access.log | tail -n 80
sudo awk -F\" '{print $6}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo tail -n 100 /var/log/nginx/error.log
```

Nginx GeoIP2 conceptual shape, not production-ready:

```nginx
geoip2 /path/to/GeoLite2-Country.mmdb {
    $geoip2_country_code country iso_code;
}

map $geoip2_country_code $blocked_country {
    default 0;
    CN 1;
}

server {
    if ($blocked_country) {
        return 403;
    }
}
```

GeoIP2 PoC should use a test host, test server block, or `poc-api.clueroom.xyz` first.
Rollback means removing the map/location rule, running `nginx -t`, reloading Nginx, and verifying `/actuator/health`.

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

## 9. Alerts Not Safe Yet

Do not make critical alerts yet for these without better exporters or validated bridges:

```text
- host disk low from incomplete source
- host CPU/RAM high without stable exporter
- MySQL down without a reliable health bridge
- Redis down without a reliable health bridge
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

Prometheus-exported Micrometer names use underscore form, so Java metric names `ai.failures`, `ai.latency`, and `ai.fallbacks` are expected as `ai_failures_total`, `ai_latency_seconds_*`, and `ai_fallbacks_total`.
If the bucket series does not exist, do not create a p95 alert until histogram publishing is verified.

## 11. Notification Policy

Initial notification path:

```text
1. manual Grafana dashboard review
2. Slack manual summary
3. low-severity webhook notification
4. critical alert only after thresholds are proven
```

Candidate channels:

```text
#clueroom-alerts
#clueroom-infra
```

Do not send secrets, raw `.env`, raw user questions, AI answers, DB passwords, private keys, or Firebase JSON to Slack.

## 12. Do Not Do

```text
- Do not enable Nginx enforcement without dry-run evidence.
- Do not block a country based on one suspicious IP.
- Do not page on standby Blue-Green target down by itself.
- Do not expose Prometheus directly to the public internet.
- Do not paste raw access logs containing user input into public PRs.
- Do not add Loki, n8n, exporters, workers, and new rate-limit enforcement all at once on the app server.
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
