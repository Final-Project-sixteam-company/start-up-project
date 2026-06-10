# ClueRoom Infra Portfolio Summary

> Purpose: summarize the ClueRoom infrastructure work for presentation, portfolio, and interview review.
> Operational commands remain in `OPS_RUNBOOK.md`; architecture and roadmap remain in `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`.
> This document intentionally avoids secrets, raw logs, DB dumps, private IP credentials, and private scenario data.

---

## 1. One-Line Summary

ClueRoom runs on a low-cost AWS Lightsail MVP architecture, but it includes production-oriented safeguards: Blue-Green app deployment, externalized data server, S3 DB backup and restore rehearsal, Loki/Grafana/n8n/Slack observability, Nginx rate limiting and CN IP blocking, and LLMOps telemetry for AI calls.

---

## 2. Constraints

```text
- Bootcamp final project with MVP cost constraints
- Android + Spring Boot service exposed through api.clueroom.xyz
- AI interrogation is a core feature, so latency, failure, fallback, and token cost need visibility
- Team demo stability and rollback are more important than managed-infra completeness
- The infra work should show real Linux/Docker/Nginx operations, not only managed services
```

The goal was not to use the largest AWS stack. The goal was to build the minimum realistic operating model for a small startup-style MVP.

---

## 3. Current Production Shape

| Server | Role | Main Components |
|---|---|---|
| `clueroom-api-prod-01` | API ingress and app runtime | Nginx, app-blue, app-green, Prometheus, Grafana, Alloy |
| `clueroom-data-01` | Data source of truth | MySQL, Redis, local backup, S3 upload, DATA_HEALTH, S3_BACKUP_HEALTH |
| `clueroom-ops-01` | Logs and alert automation | Loki, n8n, OPS_HEALTH, Slack alert router |

Current request flow:

```text
Android App
  ↓
https://api.clueroom.xyz
  ↓
prod Nginx: TLS / rate limit / CN block / manual blocklist
  ↓
active app slot: app-blue or app-green
  ↓
data server: MySQL / Redis
```

Relevant diagrams:

```text
docs/infra/diagrams/current-production-request-flow.mmd
docs/infra/diagrams/observability-alert-flow.mmd
docs/infra/diagrams/backup-restore-flow.mmd
docs/infra/diagrams/planned-scaleout-manual-lb.mmd
```

---

## 4. What Was Implemented

### Blue-Green App Deployment

```text
app-blue  -> 127.0.0.1:8081
app-green -> 127.0.0.1:8082
Nginx upstream controls the active slot.
```

Why it matters:

```text
- deploy new version to standby slot first
- switch Nginx only after health passes
- rollback by switching upstream back
- avoid full downtime for normal application deploys
```

### External Data Cutover

Before:

```text
prod server: Nginx + app + MySQL + Redis
```

After:

```text
prod server: Nginx + app-blue/app-green
data server: MySQL + Redis
```

Effect:

```text
- app containers are closer to stateless
- app scale-out becomes possible
- data ownership is explicit
- prod local MySQL/Redis is not source of truth
```

### S3 DB Backup And Restore Rehearsal

Backup path:

```text
data MySQL
  -> /opt/clueroom-data/backup-mysql.sh
  -> local .sql.gz
  -> /opt/clueroom-data/upload-mysql-backup-s3.sh
  -> private S3 backup bucket + .sha256 sidecar
  -> S3_BACKUP_HEALTH to Loki/Grafana
```

Restore rehearsal verified:

```text
- download .sql.gz and .sha256 from S3
- compare sha256
- run gzip -t
- restore into temporary mysql:8.4 container
- check startup database and key table counts
- remove temporary container and downloaded files
```

### Observability And Alerting

```text
prod Nginx/App logs -> Alloy -> ops Loki
data/ops/prod health heartbeats -> Loki
Prometheus/Grafana -> dashboards and alerts
Grafana alert -> n8n -> Slack
Gemini analysis -> optional helper message
```

Important rule:

```text
The deterministic Slack alert is sent first.
Gemini failure must not block the basic alert.
Codex is not a real-time automatic fallback; it is used for manual handoff and deep analysis.
```

### Traffic Defense

Current baseline:

```text
Nginx API per-IP rate limit enforced
limit_req_dry_run off
limit_req_status 429
CN IPv4 block applied
manual blocklist snippet available
sensitive path scanning blocked with 403
403/429 alerts routed through Grafana/n8n/Slack
```

This is MVP defense, not a full WAF replacement.

### LLMOps

AI call observability focuses on derived metadata, not raw prompt/answer storage.

```text
AI_CALL: feature, provider, model, latency, tokens, status
AI_CALL_CONTEXT: prompt block token estimates and template hash
```

Safety rule:

```text
Do not log raw prompt, raw answer, user question text, sessionId, scenarioId, suspectId, or npcCode in AI_CALL_CONTEXT.
```

---

## 5. Not Yet Operating As Baseline

These items are intentionally not described as current baseline:

```text
1. Server-side Infra Codex automatic production operation
2. Codex as real-time fallback when Gemini alert analysis fails
3. Terraform-created app server scale-out with manual Nginx load balancing
```

The scale-out work is a short-lived PoC. After validation, extra app server resources and static IPs should be removed unless the team explicitly decides to keep them.

Baseline remains:

```text
prod server 1
+ data server 1
+ ops server 1
```

---

## 6. Interview Q&A Short Answers

### Why Lightsail?

Because this project targets a low-cost MVP. Lightsail keeps cost and operations understandable while still allowing real Linux, Docker, Nginx, backup, and monitoring work.

### Why not RDS?

RDS is a valid future option, but the project needed direct operating experience and lower cost. Risk is reduced with a separated data server, S3 backup, and restore rehearsal.

### Why Blue-Green?

It lets the team deploy to a standby slot, health check it, switch Nginx upstream, and rollback quickly if the new version fails.

### Why split MySQL/Redis to a data server?

App servers cannot scale safely if each app server owns local DB/Redis. Externalizing data makes app-blue/app-green and future app servers share the same source of truth.

### Does Gemini failure break alerting?

No. n8n sends the basic deterministic Slack alert first. Gemini only adds optional analysis.

### Is Codex automatically operating infra?

No. Codex is not attached as a real-time production operator. It is used for repo work, document/runbook improvement, and manual handoff/deep analysis.

### Is scale-out live?

No. The baseline is still one prod app server, one data server, and one ops server. Terraform scale-out with manual Nginx load balancing is a PoC and should be cleaned up after validation.

---

## 7. Presentation Script

### 30 Seconds

ClueRoom started as a low-cost Lightsail MVP, but we added production-oriented safeguards step by step. The prod server runs Nginx and Blue-Green Spring Boot slots, data is separated into a dedicated MySQL/Redis server, and logs/alerts go to an ops server with Loki, Grafana, n8n, and Slack. We also added S3 database backups with restore rehearsal, Nginx rate limiting and CN IP blocking, and LLMOps telemetry for AI call cost and latency. The remaining scale-out work is a short-lived Terraform/manual Nginx PoC, not the normal baseline.

### 1 Minute

The infrastructure goal was to build a realistic MVP operations model under cost constraints. Instead of jumping directly to RDS, ALB, or Kubernetes, we used Lightsail and built the operational controls ourselves.

The prod server handles Nginx ingress and Blue-Green app deployment. The data server owns MySQL and Redis as source of truth. The ops server handles centralized logs and alert automation through Loki, Grafana, n8n, and Slack.

For reliability, DB backup runs on the data server, uploads to a private S3 backup bucket with sha256 sidecars, and is verified through restore rehearsal in a temporary MySQL container. For traffic defense, Nginx rate limiting, CN IPv4 block, manual blocklist, and 403/429 alerts are in place. For AI operations, AI_CALL and AI_CALL_CONTEXT metadata give visibility into latency, token cost, fallback, and prompt block estimates without storing raw prompts or answers.

### 3 Minutes

The core infrastructure decision was to keep the system inexpensive but operationally credible. We avoided prematurely moving to heavy managed infrastructure, but we still built the controls that matter for a real MVP: deploy rollback, data ownership, backup verification, monitoring, alerting, and traffic defense.

The prod server runs Nginx and two Spring Boot slots, app-blue and app-green. Deployment goes to standby first, then Nginx switches upstream after health checks. This gives rollback without relying on Kubernetes or ALB.

The data server owns MySQL and Redis. This was necessary because app scale-out is not safe if DB or Redis lives inside each app server. The app containers now point to external data endpoints, which makes the current app runtime closer to stateless.

The ops server centralizes logs and alert routing. Prod logs are shipped through Alloy to Loki, data and ops health checks are pushed as heartbeat logs, and Grafana alerts go through n8n to Slack. Gemini may add analysis, but basic alerting does not depend on Gemini. Codex is also not a real-time production fallback; it remains a manual analysis and handoff tool.

Backup is not treated as complete just because a dump file exists. The data server creates a gzip dump, uploads it to S3 with a sha256 sidecar, pushes S3 backup health, and the team rehearses restore by downloading the backup and importing it into a temporary MySQL container.

Finally, traffic defense is handled at Nginx with rate limit enforcement, CN IPv4 block, manual blocklist, and sensitive path blocking. The remaining scale-out work is a short PoC using Terraform to create an extra app server and manually attach it to Nginx upstream. The default baseline remains prod 1, data 1, ops 1.

---

## 8. Do Not Overclaim

```text
- Do not claim Kubernetes, ECS, ALB, or ASG production operation.
- Do not claim MySQL HA or replication.
- Do not claim Codex automatic production fallback.
- Do not claim scale-out is the normal baseline.
- Do not claim WAF-grade complete security.
```

Use this phrasing instead:

```text
ClueRoom uses a low-cost MVP infrastructure with practical production safeguards and a clear path to scale-out.
```
