# ClueRoom Nginx Rate Limit Dry-run Runbook

> Status: INFRA-03A runbook.
> This document is an operating procedure only. It does not apply Nginx changes by itself.

## 1. Purpose

ClueRoom should observe Nginx rate-limit behavior before enforcing real `429` responses.

Dry-run mode lets Nginx evaluate the configured limit and write rate-limit hit logs without rejecting requests.

```text
limit_req_dry_run on
→ rate-limit hits are logged
→ requests are not blocked
→ frontend E2E should not receive 429 from this PoC
```

This is the first production-safe step before deciding whether to enable real Nginx `limit_req` enforcement.

## 2. Background

Nginx IP-based rate limits are a first-line defense against repeated bot traffic and accidental request bursts.

They are not enough for AI cost control.

```text
- Android users can share one carrier NAT or public Wi-Fi IP.
- Attackers can rotate source IPs.
- AI interrogation, scenario validation, and final deduction need userId/sessionId/scenarioId-aware quotas.
```

Backend Redis-based limits are still required later for AI and stateful product limits.

## 3. Scope

Dry-run target:

```text
api.clueroom.xyz
location /
```

Excluded from this runbook:

```text
- real 429 enforcement
- AI endpoint-specific enforcement
- Redis user/session quota
- GeoIP/country blocking
- monitor.clueroom.xyz / Grafana
- actuator hardening locations
```

Do not add this dry-run snippet to `/actuator/prometheus` or other blocked actuator locations.

## 4. Pre-check

```bash
ssh clueroom
cd /opt/clueroom/app
```

Check the current serving path and Nginx syntax.

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
sudo nginx -t
```

Expected:

```text
- Blue-Green status prints active/standby information.
- /actuator/health returns 200.
- nginx -t reports syntax is ok and test is successful.
```

Stop if these are not true.

## 5. Backup Current Nginx Site Config

```bash
TS=$(date +%Y%m%d_%H%M%S)
sudo cp /etc/nginx/sites-available/clueroom-api /etc/nginx/sites-available/clueroom-api.before-rate-limit-dryrun-$TS
```

Verify the backup exists.

```bash
ls -al /etc/nginx/sites-available | grep before-rate-limit-dryrun
```

## 6. Create Rate Limit Zone File

`limit_req_zone` must live in Nginx `http` context, not inside a `server` or `location` block.

Ubuntu Nginx usually includes `/etc/nginx/conf.d/*.conf` inside the `http` context, so this runbook stores zones there.

```bash
sudo tee /etc/nginx/conf.d/clueroom-rate-limit-zones.conf > /dev/null << 'EOF'
# ClueRoom rate limit zones.
# These zones define request counters only.
# Enforcement is controlled in server/location blocks.

limit_req_zone $binary_remote_addr zone=clueroom_api_per_ip:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=clueroom_ai_per_ip:10m rate=1r/s;

limit_req_status 429;
limit_req_log_level warn;
EOF
```

Meaning:

```text
clueroom_api_per_ip
→ general API observation, 10 requests per second per IP

clueroom_ai_per_ip
→ future AI API candidate zone, not enforced by this runbook

limit_req_status 429
→ future enforcement status, not returned while dry-run is enabled

limit_req_log_level warn
→ write rate-limit events at warn level
```

## 7. Create Dry-run Snippet

```bash
sudo tee /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf > /dev/null << 'EOF'
# ClueRoom API rate limit dry-run.
# This logs rate limit hits but does not reject requests.
# Keep dry-run enabled during frontend E2E QA.

limit_req zone=clueroom_api_per_ip burst=30 nodelay;
limit_req_dry_run on;
EOF
```

The important line is:

```nginx
limit_req_dry_run on;
```

With dry-run enabled, Nginx should not return `429`.

## 8. Include Snippet In API Location

Edit the API site config.

```bash
sudo nano /etc/nginx/sites-available/clueroom-api
```

Add the snippet inside `location /`, before `proxy_pass`.

```nginx
location / {
    include /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf;

    proxy_pass http://clueroom_backend;
    proxy_http_version 1.1;

    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;

    proxy_connect_timeout 10s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    add_header X-ClueRoom-Upstream $upstream_addr always;
}
```

Do not add the snippet to:

```text
- location = /actuator/prometheus
- location ~* ^/actuator/(?!health$)
- bot-scan blocking locations
- monitor.clueroom.xyz
```

## 9. Apply

```bash
sudo nginx -t
```

Expected:

```text
syntax is ok
test is successful
```

If the test passes:

```bash
sudo systemctl reload nginx
```

## 10. Smoke Test

```bash
curl -I https://api.clueroom.xyz/actuator/health
curl https://api.clueroom.xyz/actuator/health
curl -I https://api.clueroom.xyz/swagger-ui/index.html
curl -I https://api.clueroom.xyz/actuator/prometheus
```

Expected:

```text
/actuator/health
→ 200

/swagger-ui/index.html
→ 200

/actuator/prometheus
→ 403
```

Run a repeated request smoke.

```bash
for i in $(seq 1 40); do
  curl -s -o /dev/null -w "%{http_code} " https://api.clueroom.xyz/actuator/health
done
echo
```

Expected:

```text
200 200 200 ...
```

`429` should not appear in dry-run mode.

If `429` appears, treat it as a failed dry-run setup and roll back.

## 11. Dry-run Log Check

```bash
sudo grep -i "limiting requests" /var/log/nginx/error.log | tail -n 30
```

Or:

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

No dry-run log does not necessarily mean failure.

```text
- The test traffic may not exceed the configured rate.
- The main success criterion is that requests are not blocked and the API remains healthy.
```

## 12. Rollback

Find the latest backup.

```bash
ls -al /etc/nginx/sites-available | grep before-rate-limit-dryrun
```

Restore the site config.

```bash
sudo cp /etc/nginx/sites-available/clueroom-api.before-rate-limit-dryrun-YYYYMMDD_HHMMSS /etc/nginx/sites-available/clueroom-api
```

Remove the dry-run files.

```bash
sudo rm -f /etc/nginx/conf.d/clueroom-rate-limit-zones.conf
sudo rm -f /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf
```

Validate and reload.

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

## 13. Completion Criteria

```text
- Nginx zone file created.
- Dry-run snippet created.
- api.clueroom.xyz location / includes the dry-run snippet.
- nginx -t succeeds.
- nginx reload succeeds.
- health returns 200.
- Swagger returns 200.
- /actuator/prometheus remains blocked.
- repeated request smoke returns no 429.
- rollback path is verified.
```

## 14. Criteria Before Real Enforcement

Do not disable dry-run until all of the following are true:

```text
- frontend E2E is complete
- Android normal navigation request bursts are observed
- Swagger/team QA paths are checked
- OPTIONS preflight behavior is checked
- 429 frontend UX is defined
- AI cost API protection is planned with Redis user/session/scenario quotas
```

Potential future enforcement change:

```nginx
limit_req_dry_run off;
```

or removing the dry-run line from the snippet.

## 15. Do Not Do

```text
- Do not apply real 429 enforcement during frontend E2E.
- Do not add this snippet to monitor.clueroom.xyz.
- Do not apply AI endpoint limits from this runbook.
- Do not treat Nginx IP limits as AI cost protection.
- Do not commit server secrets, Nginx private keys, or production config backups.
- Do not run destructive Docker, Git, Terraform, or SQL commands for this PoC.
```
