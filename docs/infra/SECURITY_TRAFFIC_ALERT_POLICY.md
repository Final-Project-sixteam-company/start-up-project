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

## 11. Notification Policy

Initial notification path:

```text
1. manual Grafana dashboard review
2. Discord/Slack manual summary
3. low-severity webhook notification
4. critical alert only after thresholds are proven
```

Do not send secrets, raw `.env`, raw user questions, AI answers, DB passwords, private keys, or Firebase JSON to Discord/Slack.

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
