# ClueRoom Infra Enhancement Roadmap

> Purpose: define the post-MVP infrastructure enhancement path for ClueRoom.
>
> Scope: roadmap and operating principles only. This document does not implement Nginx, Docker, monitoring, agent, or database changes.

---

## 1. Current MVP Baseline

Current production direction:

```text
- single Lightsail production server
- Spring Boot API behind Nginx HTTPS
- MySQL and Redis through Docker Compose
- Blue-Green helper scripts for app deployment
- Prometheus and Grafana for basic metrics
- official scenario seed YAML stored outside public git
- S3 public assets for scenario/evidence/suspect images
```

Current operating priorities:

```text
1. keep API stable for Android MVP testing
2. preserve Blue-Green rollback path
3. avoid exposing scenario spoilers and secrets
4. avoid adding heavy services to the single production server too early
5. keep infra changes reviewable through PRs
```

---

## 2. Enhancement Principles

```text
1. Stabilize the MVP user flow before adding platform complexity.
2. Prefer small reviewable PRs over large infra rewrites.
3. Keep secrets and private scenario data outside public git.
4. Keep production mutations human-approved.
5. Treat monitoring alerts as operational tools, not proof by themselves.
6. Separate edge-level traffic defense from AI feature quota.
7. Use Android Kotlin wording for client integration docs.
8. Do not install n8n, Loki, exporters, workers, and new rate-limit systems all at once on the single MVP server.
```

---

## 3. Phase 0: Frontend E2E Connection Stability

Goal:

```text
Make the Android App playable end to end with the current backend MVP.
```

Work:

```text
- verify scenario list/detail flow
- verify opening synopsis display
- verify map/cover/evidence/suspect image URLs
- verify play session start
- verify evidence unlock flow
- verify interrogation flow
- verify final deduction flow
- verify fallback behavior when AI provider fails
```

Do not block this phase on:

```text
- n8n
- Loki
- node_exporter
- cAdvisor
- multi-server scale-out
- full LLMOps dashboard
```

---

## 4. Phase 1: Security And Traffic Defense

Goal:

```text
Reduce abuse risk before public or wider test exposure.
```

Work:

```text
- Nginx IP-based rate limit for basic bot traffic
- GeoIP / country-based bot traffic control PoC design
- request size limits
- HTTPS-only public API
- no direct Prometheus exposure
- no direct Grafana admin sharing
- no direct MySQL/Redis exposure
- secret file permission review
```

Reference: `docs/infra/RATE_LIMIT_POLICY.md`
Reference: `docs/infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md`
Reference: `docs/infra/GEOIP_BOT_TRAFFIC_POLICY.md`

AI cost defense must not rely only on Nginx IP rate limit.

Reason:

```text
- Android users can share a carrier NAT or Wi-Fi IP.
- attackers can rotate IPs.
- AI interrogation, scenario validation, and final deduction are cost-bearing features.
```

Required later:

```text
Redis-backed backend quota by userId, sessionId, scenarioId, and featureType.
```

Actual Nginx `limit_req` enforcement is deferred until after frontend E2E QA.
Before enforcement, INFRA-03A should use Nginx dry-run mode to observe rate-limit hits without returning `429`.

Recommended AI quota targets:

```text
- interrogation per session
- final deduction attempts per session
- scenario validation per user/time window
- total AI requests per user/day
```

---

## 5. Phase 2: Monitoring / Alert

Goal:

```text
Improve operational visibility without creating alert noise.
```

Current usable signals:

```text
- external /actuator/health
- Blue-Green status script
- Nginx config test
- Docker container status
- Prometheus health
- Grafana health
- bounded app/Nginx log tails
```

Blue-Green alert rule:

```text
app-blue or app-green target down is not critical by itself.
The standby slot can be intentionally stopped.
```

Critical checks should prefer:

```text
- external health failure
- active upstream health failure
- both app targets down
- Nginx cannot route to active upstream
```

Exporter requirements:

| Alert Area | Needed Component |
|---|---|
| host disk/cpu/memory | node_exporter |
| container CPU/RAM | cAdvisor |
| MySQL native status | mysqld_exporter or health bridge |
| Redis native status | redis_exporter or health bridge |

Until these are added, use:

```text
df -h
free -m
docker stats --no-stream
docker system df
app health checks
manual DB/Redis checks
```

Reference:

```text
docs/infra/GRAFANA_ALERT_POLICY.md
docs/infra/agent/MONITORING_AGENT_PLAN.md
docs/infra/agent/OPS_SNAPSHOT_SPEC.md
```

Actual Slack notification wiring is deferred to INFRA-09 after alert policy and metric names are confirmed.

---

## 6. Phase 3: LLMOps

Goal:

```text
Track AI quality, safety, latency, fallback, and cost.
```

Initial work:

```text
- collect AI request metadata
- track promptVersion
- track provider/model/featureType
- track latency and errorCode
- track fallbackUsed and fallbackReason
- optionally persist metadata to ai_call_logs after manual table creation
- track prompt validation failures
- track secret leak blocked count
```

Prometheus metric candidates:

```text
ai_requests_total
ai_failures_total
ai_latency_seconds
ai_fallbacks_total
ai_tokens_total
ai_prompt_validation_failures_total
ai_secret_leak_blocked_total
```

Safety priority:

```text
Secret leakage and NPC knowledge-boundary failures are higher priority than style or latency issues.
```

Reference:

```text
docs/infra/agent/LLMOPS_AGENT_PLAN.md
docs/AI_NPC_PROMPT_POLICY.md
```

---

## 7. Phase 4: Agent Introduction

Goal:

```text
Use AI agents to assist operations without granting unsafe production autonomy.
```

Recommended order:

```text
1. Infra Agent docs and prompt policy
2. Manual Ops Snapshot analysis
3. Monitoring Agent local PoC
4. Codex support for infra PRs and deployment analysis
5. Discord/Slack manual summary
6. n8n workflow after infra server separation
7. optional dedicated worker after need is proven
```

Current rule:

```text
Infra Codex Agent starts as IntelliJ Local Codex only.
Production server agent mode is not recommended early.
If introduced later, it starts with read-only commands only.
```

Reference:

```text
docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md
docs/infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md
docs/infra/agent/MONITORING_AGENT_PLAN.md
```

---

## 8. Phase 5: Backup / Restore Hardening

Goal:

```text
Make backups recoverable, not just created.
```

Work:

```text
- document backup schedule
- verify MySQL backup command
- rehearse restore on non-production environment
- separate backup storage from public app asset storage
- define retention policy
- define backup integrity checks
```

S3 backup rule:

```text
Do not upload DB backups to the public app asset bucket/prefix.
```

Reference:

```text
docs/infra/MYSQL_BACKUP_AND_RESTORE_POLICY.md
```

Recommended:

```text
- backup-only private S3 bucket
- or at minimum private backup prefix separate from public images
- separate IAM user/policy
- server-side encryption
- lifecycle policy
- restore rehearsal documentation
```

---

## 9. Phase 6: Scale-Out PoC / Server-Level Blue-Green

Goal:

```text
Evaluate whether ClueRoom needs multi-server deployment after MVP.
```

PoC candidates:

```text
- separate app server and infra/monitoring server
- split n8n/Loki from API server
- evaluate load balancer
- test server-level Blue-Green instead of only container slot Blue-Green
- evaluate managed DB option if traffic grows
```

Reference:

```text
docs/infra/SCALE_OUT_POC_PLAN.md
```

Do not start this before:

```text
- Android E2E flow is stable
- seed/import workflow is proven
- backup/restore is rehearsed
- current monitoring noise is controlled
```

---

## 10. Phase 7: Registry / Commit SHA / DB Migration

Goal:

```text
Make deployment artifacts and database schema changes traceable.
```

Work:

```text
- container image registry
- image tag by commit SHA
- deploy metadata endpoint or release file
- migration tool decision
- migration runbook
- rollback rule for schema changes
```

Migration caution:

```text
If production uses ddl-auto=validate, schema changes require migration SQL before deploy.
```

Rollback caution:

```text
Not every DB migration can be rolled back by switching app version.
Schema rollback must be planned separately.
```

---

## 11. Do Not Do

Do not do these during MVP stabilization:

```text
- install n8n, Loki, node_exporter, cAdvisor, MySQL exporter, Redis exporter, and workers all at once
- expose Prometheus publicly
- share Grafana admin account
- put production secrets in GitHub
- put private scenario spoiler YAML in public repo
- upload DB backups to public S3 asset bucket/prefix
- treat app-blue/app-green standby target down as critical by itself
- depend only on Nginx IP rate limit for AI cost control
- change Android docs to Flutter/Dio assumptions
- allow AI Agent autonomous production mutations
```

---

## 12. Priority Table

| Priority | Item | Phase | Reason |
|---|---|---|---|
| P0 | Android E2E API/image/AI flow stability | Phase 0 | demo/MVP usability |
| P0 | Secret and private scenario data boundary | Phase 0-1 | spoiler and credential safety |
| P1 | AI cost quota design | Phase 1 | prevent expensive abuse |
| P1 | Blue-Green alert false-positive control | Phase 2 | avoid noisy or wrong incident response |
| P1 | Backup/restore rehearsal | Phase 5 | data recovery confidence |
| P2 | LLMOps metadata and dashboard | Phase 3 | AI quality operations |
| P2 | Ops Snapshot and Monitoring Agent PoC | Phase 4 | better incident summaries |
| P3 | n8n/Loki automation | Phase 4 | useful after infra separation |
| P3 | node_exporter/cAdvisor/exporters | Phase 2 | better alerts after MVP stability |
| P3 | multi-server scale-out PoC | Phase 6 | traffic-driven, not MVP blocker |
| P3 | image registry and commit SHA deploy | Phase 7 | release traceability |

