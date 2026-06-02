# ClueRoom Ops Snapshot Spec

> Purpose: define a secret-safe server status snapshot format for Monitoring Agent, Infra Agent, and human incident review.
>
> Status: planning document. The snapshot script described here is not implemented yet.

---

## 1. Snapshot Purpose

An Ops Snapshot is a bounded text report that summarizes production health without exposing secrets.

It is used for:

```text
- human incident triage
- Infra Agent read-only diagnosis
- Monitoring Agent analysis
- deploy failure review
- post-incident summary drafting
```

It is not used for:

```text
- exporting secrets
- dumping database rows
- replacing the runbook
- making autonomous production changes
```

The snapshot should be safe to paste into an AI tool or team chat after a human review.

---

## 2. Collection Targets

The snapshot should collect these categories.

| Category | Purpose |
|---|---|
| timestamp | identify when the snapshot was taken |
| host | identify server and environment |
| git/deploy state | understand deployed branch/commit |
| Blue-Green status | determine active/standby slot |
| external health | verify public API health |
| Docker status | inspect running containers |
| memory | detect host pressure |
| disk | detect disk pressure |
| Docker disk | detect image/container/log pressure |
| Nginx config | verify reverse proxy config syntax |
| Prometheus health | verify metrics stack health |
| Grafana health | verify dashboard stack health |
| app log tail | inspect recent application errors |
| Nginx error tail | inspect recent proxy errors |
| deploy log tail | inspect recent deployment failure context |

The snapshot should prefer summaries over raw dumps.

---

## 3. Collection Prohibitions

The snapshot must not include the following.

```text
.env contents
DB password
AWS access key
AWS secret key
OpenAI API key
Gemini API key
Firebase service account JSON
SSH private key
private scenario spoiler YAML
database dump contents
full request/response bodies containing user data
signed URLs containing credentials
```

Do not run these commands in a snapshot script.

```bash
cat /opt/clueroom/app/.env
cat /opt/clueroom/secrets/env.d/*.env
cat /opt/clueroom/secrets/firebase-service-account.json
printenv
docker inspect
mysqldump
cat /opt/clueroom/secrets/scenarios/*.yaml
```

If a command can reveal secrets in normal output, exclude it or redact it before the snapshot is emitted.

---

## 4. Output Format

Recommended top-level format:

```text
# ClueRoom Ops Snapshot

snapshotVersion:
capturedAt:
host:
environment:
operator:

## Summary
...

## Health
...

## Blue-Green
...

## Runtime
...

## Resources
...

## Monitoring
...

## Logs
...

## Risk Notes
...
```

Machine-readable JSON can be added later, but the MVP format should stay readable for humans.

---

## 5. Command Set

### 5.1 Timestamp And Host

```bash
date -Is
hostname
whoami
pwd
```

### 5.2 Git And Deploy State

```bash
cd /opt/clueroom/app
git status --short
git log --oneline -5
```

If the deployed app directory is not a git checkout in a future deployment model, replace this with release metadata.

### 5.3 Blue-Green Status

```bash
/opt/clueroom/bg-status.sh
```

The snapshot must preserve which slot is active and which slot is standby.

Important Blue-Green interpretation:

```text
app-blue down or app-green down can be normal if that slot is standby.
Critical status depends on active upstream health and external health.
```

### 5.4 External Health

```bash
curl -I --max-time 10 https://api.clueroom.xyz/actuator/health
curl -s --max-time 10 https://api.clueroom.xyz/actuator/health
```

External health is more important than individual standby target status.

### 5.5 Docker Status

```bash
cd /opt/clueroom/app
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker compose ps
```

Avoid `docker inspect` because it can include environment variables and secrets.

### 5.6 Memory

```bash
free -m
```

### 5.7 Disk

```bash
df -h
docker system df
```

Note:

```text
Accurate host disk/cpu/memory alerting requires node_exporter.
This snapshot is a manual MVP substitute, not a full metrics solution.
```

### 5.8 Container Resource Snapshot

```bash
docker stats --no-stream
```

Note:

```text
Accurate container CPU/RAM alerting requires cAdvisor.
docker stats is useful for manual diagnosis but should not be treated as a complete alerting layer.
```

### 5.9 Nginx Syntax

```bash
sudo nginx -t
```

### 5.10 Prometheus Health

```bash
curl -s --max-time 10 http://localhost:9090/-/healthy
```

Prometheus target down interpretation must account for Blue-Green standby.

### 5.11 Grafana Health

```bash
curl -s --max-time 10 http://localhost:3000/api/health
```

### 5.12 App Logs

Identify the active Blue-Green slot first, then capture only the active app service log.

```bash
/opt/clueroom/bg-status.sh
```

Use one of these commands based on the active slot.

```bash
# If active slot is app-blue:
/opt/clueroom/bg-compose logs --tail=120 app-blue
# If active slot is app-green:
/opt/clueroom/bg-compose logs --tail=120 app-green
```

Plain `docker compose logs --tail=120 app` is only for legacy single-app or local compose mode, not production Blue-Green mode.

Do not capture unbounded logs.

### 5.13 Nginx Error Logs

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

### 5.14 Recent Deploy Logs

```bash
ls -lt /opt/clueroom/logs | head
tail -n 120 /opt/clueroom/logs/deploy.log
```

If the deploy log filename is timestamped, the operator should tail the latest deploy log.

---

## 6. Normal Example

Example summary:

```text
# ClueRoom Ops Snapshot

snapshotVersion: 1
capturedAt: 2026-06-02T21:30:00+09:00
host: clueroom-api-prod-01
environment: production
operator: human-reviewed

## Summary
External API health is UP. Blue-Green active slot is app-blue. app-green is stopped as standby.

## Health
GET https://api.clueroom.xyz/actuator/health: 200 OK

## Blue-Green
active: app-blue
standby: app-green
standbyStatus: stopped

## Runtime
mysql: running
redis: running
app-blue: running
app-green: stopped
nginx: config ok

## Resources
disk: normal
memory: normal
dockerDisk: normal

## Monitoring
prometheus: healthy
grafana: healthy

## Logs
No recent critical app or nginx errors in bounded tails.

## Risk Notes
No immediate action required.
```

In this example, `app-green` stopped is not an incident because it is the standby slot.

---

## 7. Incident Example

Example summary:

```text
# ClueRoom Ops Snapshot

snapshotVersion: 1
capturedAt: 2026-06-02T22:10:00+09:00
host: clueroom-api-prod-01
environment: production
operator: human-reviewed

## Summary
External API health is failing. Blue-Green status cannot confirm a healthy active upstream.

## Health
GET https://api.clueroom.xyz/actuator/health: timeout

## Blue-Green
active: unknown
standby: unknown

## Runtime
mysql: running
redis: running
app-blue: restarting
app-green: stopped
nginx: config ok

## Resources
disk: 92% on root
memory: low free memory
dockerDisk: high image/cache usage

## Monitoring
prometheus: healthy
grafana: healthy

## Logs
app-blue tail shows repeated DB connection timeout.
nginx error tail shows upstream timeout.

## Risk Notes
Potential API outage. Check active upstream and DB connection path before rollback.
```

This example should be treated as incident-level because external health is failing.

---

## 8. Agent Analysis Prompt Example

Use a prompt like this when sending a human-reviewed snapshot to an AI agent.

```text
You are analyzing a ClueRoom production ops snapshot.

Rules:
- Do not ask for or reveal secrets.
- Do not recommend destructive commands.
- In Blue-Green deployment, app-blue or app-green down is not critical by itself. Determine active upstream and external health first.
- Distinguish confirmed evidence from inference.
- Return severity, likely cause, immediate read-only checks, and any change that requires human approval.

Output:
1. severity: OK | INFO | WARNING | CRITICAL
2. evidence summary
3. likely cause
4. safe read-only next checks
5. human-approved remediation candidates
6. rollback note if applicable
```

---

## 9. Future `/opt/clueroom/ops-snapshot.sh` Design

The future script should:

```text
1. run only bounded read-only commands
2. avoid printing secrets
3. redact known sensitive patterns
4. include command exit statuses
5. include timestamps
6. mark each section as PASS/WARN/FAIL/UNKNOWN
7. save output to a timestamped local file
8. avoid uploading output automatically in MVP
```

Candidate path:

```text
/opt/clueroom/ops-snapshot.sh
```

Candidate output path:

```text
/opt/clueroom/logs/ops-snapshot-YYYYMMDD_HHMMSS.txt
```

The script should not be added to production automatically until reviewed through PR and tested on staging or a maintenance window.

---

## 10. Alerting Limitations

The current MVP monitoring stack is not enough for every alert type.

| Alert Type | MVP Status | Needed For Accurate Alert |
|---|---|---|
| external API health | possible | existing HTTPS health check |
| active app slot health | possible | Blue-Green status + health endpoint |
| app-blue/app-green target down | noisy alone | combine with active upstream or both-target failure |
| host disk/cpu/memory | manual only | node_exporter |
| container CPU/RAM | manual only | cAdvisor |
| MySQL health | manual or app health | mysqld_exporter or health bridge |
| Redis health | manual or app health | redis_exporter or health bridge |

Do not create critical alerts for `app-blue target down` or `app-green target down` alone.

---

## 11. Secret-Safe Review Checklist

Before sharing an Ops Snapshot:

```text
[ ] no .env values
[ ] no API keys
[ ] no Firebase JSON
[ ] no SSH keys
[ ] no database dump content
[ ] no private scenario spoiler YAML
[ ] no signed URL credentials
[ ] logs are bounded
[ ] Blue-Green active/standby interpretation is included
[ ] external health result is included
```

