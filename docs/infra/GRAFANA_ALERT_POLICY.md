# ClueRoom Grafana Alert Policy

> Status: INFRA-08 policy design only.
> This document does not create Grafana alerts, change Docker Compose, add exporters, configure Slack, or modify any production server.

## 1. Purpose

ClueRoom needs a clear alert policy before enabling Slack notifications.
The goal is to separate signals that are reliable today from signals that need additional exporters or health bridges.

Current monitoring path:

```text
Spring Boot Actuator
→ Prometheus
→ Grafana
→ future Slack Alert
```

Slack notification wiring is deferred to `INFRA-09 Slack Alert Channel PoC`.

Current Prometheus scrape targets are expected to include:

```text
- job_name: clueroom-app        target: app:8080
- job_name: clueroom-app-blue   target: app-blue:8080
- job_name: clueroom-app-green  target: app-green:8080
- job_name: prometheus          target: prometheus:9090
```

In the production Blue-Green flow, `app-blue` or `app-green` can be intentionally stopped when it is the standby slot.
Therefore, a raw `up == 0` alert for one app target can create false positives.

## 2. Alert Design Principles

```text
1. Prefer external user-facing health first.
2. Do not treat Blue-Green standby slot down as an incident by itself.
3. Prefer active upstream health over individual app-blue/app-green target status.
4. Alert only on metrics that are actually collected.
5. Keep exporter-dependent alerts in a "future" category.
6. Alerts are for human verification, not automatic remediation.
7. Never commit Slack Webhook URLs or notification secrets to the repository.
```

Severity should start conservative. MVP alerts should reduce blind spots without waking the team for expected Blue-Green states.

## 3. Alerts That Can Be Designed Now

### 3.1 External API Health Down

Target:

```text
https://api.clueroom.xyz/actuator/health
```

Purpose:

```text
Check whether the production API is reachable from the user's point of view.
This is the most important alert because it is independent of which Blue-Green slot is active.
```

Recommended severity:

```text
CRITICAL
```

Candidate condition:

```text
Health check fails 2~3 consecutive times.
```

Current limitation:

```text
Prometheus can scrape internal targets, but it cannot check an external HTTPS URL unless an external check source exists.
```

Implementation candidates:

```text
- Grafana synthetic monitoring
- blackbox_exporter
- small Spring/infra health bridge
```

Until one of these is added, external health must be verified through runbook commands or an external uptime check.

### 3.2 HTTP 5xx Increase

Target metric candidate:

```text
http_server_requests_seconds_count
```

Purpose:

```text
Detect increasing Spring Boot 5xx responses.
```

Recommended severity:

```text
WARNING or CRITICAL depending on rate and duration
```

Candidate condition:

```text
5xx response rate stays above a threshold for 5 minutes.
```

Notes:

```text
- Confirm the exact metric name and labels in Grafana Explore before creating the alert.
- Spring Boot / Micrometer naming can vary by version and registry configuration.
- Separate a short incident spike from sustained failure.
```

### 3.3 HTTP Latency Increase

Target metric candidate:

```text
http_server_requests_seconds
```

Purpose:

```text
Detect abnormal API latency before users report slow screens.
```

Recommended severity:

```text
WARNING
```

Candidate condition:

```text
p95 latency stays above 2~3 seconds for non-AI API routes.
```

Notes:

```text
- AI interrogation and final deduction can legitimately take longer.
- Alert rules should separate general APIs from AI-cost APIs where possible.
- Confirm histogram/summary availability before using p95 expressions.
```

### 3.4 JVM Memory Increase

Target metric candidates:

```text
jvm_memory_used_bytes
jvm_memory_max_bytes
```

Purpose:

```text
Detect Spring Boot JVM memory pressure.
```

Recommended severity:

```text
WARNING
```

Candidate condition:

```text
JVM used/max ratio stays high for a sustained period.
```

Notes:

```text
- This is JVM memory, not total host RAM.
- Host RAM alerting requires node_exporter or another host-level exporter.
- Interpret JVM memory together with restart count, GC behavior, and recent deployment.
```

### 3.5 Prometheus Scrape Failure

Target metric:

```text
up
```

Purpose:

```text
Detect when Prometheus cannot scrape a target.
```

Safe policy:

```text
- prometheus target failure is important.
- active upstream app failure is important.
- both app-blue and app-green down is critical.
- standby app-blue/app-green down can be normal.
```

Do not create a naive CRITICAL alert for:

```promql
up{job=~"clueroom-app-blue|clueroom-app-green"} == 0
```

This can page the team when the standby slot is intentionally stopped after deployment.

## 4. Alerts Not Safe To Create Yet

The following areas need additional exporters or explicit health bridge endpoints.
Do not present them as fully reliable Grafana alerts until those components exist.

### 4.1 Host Disk Low

Current issue:

```text
Spring Boot actuator disk metrics are app-process oriented or limited.
They are not a substitute for full host disk monitoring.
```

Current health bridge interpretation:

```text
For Ops Snapshot Agent v3, disk source of truth is heartbeat disk_max_percent:
- prod: SERVER_HEALTH disk_max_percent
- data: DATA_HEALTH disk_max_percent
- ops: OPS_HEALTH disk_max_percent

Raw snapshot text percentages are fallback only.
Do not page on a disk value parsed from arbitrary "%" text.
```

Needed later:

```text
node_exporter
```

Manual checks:

```bash
df -h
docker system df
```

### 4.2 Host CPU / RAM High

Current issue:

```text
Prometheus does not have host-level CPU/RAM metrics without node_exporter.
```

Needed later:

```text
node_exporter
cAdvisor for container-level CPU/RAM
```

Manual checks:

```bash
free -m
top
docker stats --no-stream
```

### 4.3 MySQL Down

Current issue:

```text
Prometheus cannot accurately alert on MySQL native health without a MySQL exporter or health bridge.
```

Candidates:

```text
- mysqld_exporter
- Spring Boot health indicator
- separate health bridge
```

Manual checks:

```bash
cd /opt/clueroom/app
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
docker compose ps mysql
docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysqladmin ping -uroot --silent && echo "MySQL OK"
```

Do not commit DB passwords or full connection strings in alert docs or screenshots.

### 4.4 Redis Down

Current issue:

```text
Prometheus cannot accurately alert on Redis native health without Redis exporter or health bridge.
```

Candidates:

```text
- redis_exporter
- Spring Boot health indicator
- separate health bridge
```

Manual checks:

```bash
cd /opt/clueroom/app
docker compose ps redis
docker compose exec redis redis-cli ping
```

## 5. Blue-Green Alert Rules

ClueRoom uses an app-blue/app-green Blue-Green deployment model.
After deployment and rollback verification, the standby slot can be stopped to save memory.

Therefore:

```text
app-blue target down != incident by itself
app-green target down != incident by itself
standby target down == may be normal
active upstream unhealthy == incident candidate
external API health down == incident candidate
```

Incident verification order:

```text
1. Check external /actuator/health.
2. Check X-ClueRoom-Upstream to identify active slot.
3. Check active slot health/logs.
4. Treat standby slot state as reference only.
```

Runbook commands:

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
/opt/clueroom/bg-compose ps
```

Active app log check:

```bash
/opt/clueroom/bg-compose logs --tail=120 app-blue
/opt/clueroom/bg-compose logs --tail=120 app-green
```

Pick the active service based on `bg-status.sh` or `X-ClueRoom-Upstream`.

## 6. Slack Notification Policy

Slack integration is planned for `INFRA-09`.
This document only defines the message policy.

Role split:

```text
Grafana Alert
→ event notification and immediate incident alerting

Ops Snapshot Agent
→ periodic status report, manual snapshot summary, and false-positive-aware interpretation
```

Notification severity policy:

```text
CRITICAL
→ Slack immediate notification is allowed.

WARNING / INFO
→ manual execution summary or 24h report is preferred.

Prod memory WARNING from available-memory alone
→ report-grade unless paired with health failure, OOM/restart evidence, or user-facing impact.
```

Candidate channels:

```text
#clueroom-alerts
#clueroom-infra
```

Message fields:

```text
- severity
- summary
- affected service
- current active upstream
- first detected time
- recommended runbook
- safe next commands
```

Example:

```text
[CRITICAL] ClueRoom API health check failed

Source: Grafana Alert
Role: event notification

Service: api.clueroom.xyz
Active slot: app-blue / 8081
Symptom: /actuator/health failed 3 times
Recommended:
1. /opt/clueroom/bg-status.sh
2. /opt/clueroom/bg-compose logs --tail=120 app-blue
3. Consider /opt/clueroom/rollback-bluegreen.sh if active slot is unhealthy
```

Secret policy:

```text
SLACK_WEBHOOK_URL must not be committed to GitHub.
```

Candidate storage:

```text
- n8n credential
- /opt/clueroom/secrets/env.d/monitoring.env
- GitHub Actions Secret, only if Actions sends the alert directly
```

Do not paste Slack webhook URLs into PR comments, screenshots, GitHub issues, or runbook examples.

## 7. Rollout Order

```text
1. Write alert policy document.
2. Confirm actual metric names in Grafana Explore.
3. Choose external health check method:
   - blackbox_exporter
   - Grafana synthetic monitoring
   - Spring health bridge
4. Choose Slack webhook management method.
5. Test WARNING-level alerts first as non-paging or low-noise reports.
6. Test CRITICAL alerts during a quiet window.
7. Start with human-verification alerts only.
```

Do not combine alert rollout with rate limit rollout, exporter rollout, or production deploy troubleshooting in one change.

## 8. Completion Criteria

```text
- GRAFANA_ALERT_POLICY.md exists.
- Current possible alerts and exporter-dependent alerts are separated.
- Blue-Green standby target false-positive prevention is documented.
- Slack notification policy is included.
- Slack webhook secret storage candidates are documented.
- Actual Grafana Alert creation is deferred to INFRA-09 or a separate ticket.
```

## 9. Do Not Do

```text
- Do not modify feature code.
- Do not modify docker-compose.yml as part of INFRA-08.
- Do not change production server settings.
- Do not commit Slack webhook URLs.
- Do not add .env or secret files.
- Do not treat standby app-blue/app-green down as a CRITICAL incident.
- Do not create host CPU/RAM/disk, MySQL, or Redis alerts without exporter or health bridge support.
```

## 10. Review Checklist

```text
- app-blue/app-green standby down false-positive is handled.
- Disk/MySQL/Redis alerts clearly mention exporter or health bridge requirements.
- Slack, not Discord, is the notification target.
- Slack webhook secret policy is present.
- No actual server, Docker, Grafana, or Slack settings are changed.
- Frontend E2E is not affected by this document-only work.
```
