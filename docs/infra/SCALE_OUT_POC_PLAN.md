# ClueRoom Scale-out PoC Plan

> Status: INFRA-22/23/24 planning document.
> This document is for learning and rehearsal PoC only. It does not change production routing.

## 1. Purpose

ClueRoom production currently runs on one Lightsail server with Nginx, Docker Compose, Spring Boot, MySQL, Redis, Prometheus, and Grafana.

That is the correct MVP operating baseline.

This document defines optional post-MVP PoC paths for:

```text
- app/infra role separation
- manual Nginx load balancing
- server-level Blue-Green
- Blue/Green app groups
```

The goal is to prove expansion concepts and collect portfolio evidence without moving production `api.clueroom.xyz` too early.

## 2. Current MVP Baseline

```text
single Lightsail server
  ├─ Nginx HTTPS reverse proxy
  ├─ app-blue / app-green Spring Boot slots
  ├─ MySQL Docker container
  ├─ Redis Docker container
  ├─ Prometheus
  └─ Grafana
```

Current public route:

```text
Android App
  ↓
https://api.clueroom.xyz
  ↓
Dynadot DNS A record
  ↓
Lightsail static IP
  ↓
Nginx
  ↓
active app slot on localhost
```

Current Blue-Green scope:

```text
container slot level on one server
```

It is a deployment and rollback PoC, not high availability.

## 3. PoC Rules

```text
- Do not move production api.clueroom.xyz during PoC.
- Use poc-api.clueroom.xyz or a temporary test domain if public testing is needed.
- Keep production DB out of risky PoC changes.
- Prefer private IP communication between PoC servers.
- Keep secrets outside git.
- Destroy or stop paid PoC resources after evidence is collected.
```

PoC success is measured by repeatable checks and screenshots, not permanent operation.

## 4. Required Application Conditions

Before any multi-server app PoC, verify:

```text
- Spring Boot app is stateless between HTTP requests.
- uploaded/runtime images are stored in S3, not local app disk.
- session state is DB/Redis/JWT-based, not local memory.
- distributed locks use Redis if cross-instance locking is needed.
- all app instances can reach the same MySQL and Redis.
- final-deduction and AI request duplicate guards still work with multiple app instances.
- database schema changes are backward-compatible during mixed app versions.
```

If these are not true, scale-out can create inconsistent gameplay behavior.

## 5. PoC 1: Two-Server Role Separation

Goal:

```text
separate Spring Boot app runtime from infra/DB/monitoring host
```

Structure:

```text
infra-01
  ├─ Nginx
  ├─ MySQL
  ├─ Redis
  ├─ Prometheus
  └─ Grafana

app-01
  └─ Spring Boot App
```

Traffic:

```text
Client
  ↓
infra-01 Nginx
  ↓ private IP
app-01 Spring Boot
  ↓ private IP
infra-01 MySQL / Redis
```

What this proves:

```text
- app can run outside the DB/Redis host
- Nginx can proxy to another server
- environment variables can point to remote DB/Redis hosts
- private network security group / firewall rules are understood
```

Nginx upstream example:

```nginx
upstream clueroom_poc_backend {
    server 10.0.0.21:8080 max_fails=3 fail_timeout=10s;
}

server {
    server_name poc-api.clueroom.xyz;

    location / {
        proxy_pass http://clueroom_poc_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Validation:

```bash
curl -I https://poc-api.clueroom.xyz/actuator/health
curl -s https://poc-api.clueroom.xyz/api/scenarios
```

## 6. PoC 2: Manual Nginx Load Balancing

Goal:

```text
route traffic across two app servers using Nginx upstream
```

Structure:

```text
infra-01 Nginx
  ├─ app-01 Spring Boot
  └─ app-02 Spring Boot

shared MySQL
shared Redis
S3 for image assets
```

Nginx upstream example:

```nginx
upstream clueroom_poc_backend {
    least_conn;
    server 10.0.0.21:8080 max_fails=3 fail_timeout=10s;
    server 10.0.0.22:8080 max_fails=3 fail_timeout=10s;
}
```

Recommended response header for PoC:

```nginx
add_header X-ClueRoom-Upstream $upstream_addr always;
```

What this proves:

```text
- both app instances serve the same API
- Nginx can fail over when one app instance stops
- shared DB/Redis prevents per-instance state divergence
- S3 image URLs work regardless of app instance
```

Validation:

```bash
for i in $(seq 1 20); do
  curl -s -D - https://poc-api.clueroom.xyz/actuator/health -o /dev/null | grep -i X-ClueRoom-Upstream
done
```

Stop one app server:

```bash
ssh app-01
docker compose stop app
```

Then verify:

```bash
curl -I https://poc-api.clueroom.xyz/actuator/health
curl -s https://poc-api.clueroom.xyz/api/scenarios
```

Expected:

```text
requests continue through app-02
```

## 7. PoC 3: Server-Level Blue-Green

Goal:

```text
switch traffic between two app servers instead of only app-blue/app-green containers on one host
```

Structure:

```text
infra-01
  ├─ Nginx
  ├─ MySQL
  ├─ Redis
  └─ Monitoring

app-blue-01
  └─ Spring Boot App

app-green-01
  └─ Spring Boot App
```

Nginx active upstream file idea:

```nginx
upstream clueroom_poc_backend {
    server 10.0.0.31:8080;
}
```

Switch target:

```nginx
upstream clueroom_poc_backend {
    server 10.0.0.32:8080;
}
```

Procedure:

```text
1. current active: app-blue-01
2. deploy new app version to app-green-01
3. check green health through private IP
4. switch Nginx upstream to green
5. nginx -t
6. reload Nginx
7. external health check
8. keep blue running for rollback window
```

Rollback:

```text
switch Nginx upstream back to app-blue-01 and reload
```

What this proves:

```text
- Blue-Green can be extended from container slots to server slots
- rollback remains a routing operation
- app hosts can be replaced independently
```

## 8. PoC 4: Blue/Green App Groups

Goal:

```text
model future multi-instance release groups
```

Structure:

```text
blue group:
  ├─ app-blue-01
  └─ app-blue-02

green group:
  ├─ app-green-01
  └─ app-green-02

infra-01 Nginx points to one group at a time
```

Blue upstream:

```nginx
upstream clueroom_blue_group {
    least_conn;
    server 10.0.0.41:8080;
    server 10.0.0.42:8080;
}
```

Green upstream:

```nginx
upstream clueroom_green_group {
    least_conn;
    server 10.0.0.51:8080;
    server 10.0.0.52:8080;
}
```

Active server block:

```nginx
location / {
    proxy_pass http://clueroom_blue_group;
}
```

Switch:

```nginx
location / {
    proxy_pass http://clueroom_green_group;
}
```

This is not required for MVP. It is useful for demonstrating how ClueRoom could evolve beyond one active app instance.

## 9. Shared Data And State

Shared components:

```text
MySQL
Redis
S3
external AI provider
FCM
```

Risks:

```text
- local files disappear between app servers
- local memory locks do not work cross-instance
- background jobs can run twice if not guarded
- schema changes can break old active app during rollback
```

Required mitigations:

```text
- keep runtime images in S3
- keep locks in Redis
- keep user/session/game state in DB/Redis
- make jobs idempotent or single-owner
- follow expand-contract DB migration policy later
```

## 10. Verification Checklist

For any scale-out PoC:

```text
- each server role is documented
- private IP routes are known
- only LB server exposes public 80/443 when possible
- app servers do not expose DB/Redis publicly
- /actuator/health works through public PoC domain
- /api/scenarios works through public PoC domain
- play-session creation works
- evidence/location image URLs still point to S3
- final deduction and interrogation do not fail because of instance split
- stopping one app instance does not break the LB PoC if multiple app instances are configured
- rollback path is documented
```

## 11. Cleanup

PoC resources should not be left running indefinitely.

Cleanup checklist:

```text
- stop or delete PoC app servers
- stop or delete PoC DB/Redis servers if created
- remove temporary DNS records
- remove temporary Nginx config
- remove temporary certificates if no longer needed
- confirm no extra paid Lightsail instances remain running
- preserve screenshots and notes in docs or project artifacts
```

Do not delete production resources during PoC cleanup.

## 12. Do Not Do

```text
- Do not move production api.clueroom.xyz to PoC LB without a separate rollout plan.
- Do not connect PoC apps to production DB for destructive tests.
- Do not expose MySQL or Redis publicly.
- Do not hardcode secrets in PoC scripts or docs.
- Do not leave paid PoC resources running after evidence is collected.
- Do not treat this PoC as production HA.
```

## 13. Completion Criteria

The scale-out PoC plan is complete when:

```text
- current MVP baseline is documented
- two-server role separation path is documented
- manual Nginx LB path is documented
- server-level Blue-Green path is documented
- Blue/Green group concept is documented
- app stateless requirements are listed
- validation and cleanup checklists are included
```
