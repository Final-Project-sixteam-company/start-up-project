# ClueRoom GeoIP / Bot Traffic Policy

> Status: INFRA-06 policy and PoC design only.
> This document does not apply country blocking, install GeoIP modules, change DNS, modify Nginx, or restart production servers.

## 0. Decision Summary

ClueRoom MVP enhancement work does not immediately adopt Cloudflare WAF.
The current production path is already stabilized around Dynadot DNS, Nginx Reverse Proxy, and Certbot HTTPS.
Adding Cloudflare Proxy changes DNS, SSL mode, real client IP headers, origin protection, cache behavior, and WAF policy at the same time.

Therefore, the short-term INFRA-06 PoC priority is Nginx GeoIP2.

```text
MVP / learning PoC priority:
→ Nginx GeoIP2

Long-term production candidate:
→ Cloudflare WAF, if ClueRoom later needs CDN, managed WAF, challenge-based bot control, or centralized edge policy

Not recommended for current production:
→ ipset / iptables / nftables country CIDR blocking
```

Nginx GeoIP2 is also not applied directly to production `api.clueroom.xyz`.
It should be validated on a test server, a separate test server block, or `poc-api.clueroom.xyz` before any production decision.

## 1. Purpose

ClueRoom is currently operated for Korean MVP users first.
If overseas bot traffic, vulnerability scans, or country/ASN-based abnormal traffic increases,
the team needs a documented way to evaluate blocking options before changing production traffic controls.

This policy intentionally avoids immediate country-wide blocking on `api.clueroom.xyz`.

```text
Goal:
- observe current Nginx access patterns
- compare country/ASN blocking options
- separate safe PoC from production enforcement
- define when country-based controls are justified
```

## 2. Background

Current risk:

```text
- public API domains are routinely scanned by bots
- common paths such as /.env, /.git, wp-admin, phpmyadmin can be probed
- overseas cloud/VPN/proxy traffic can bypass simple IP blocking
- country-wide blocking can affect legitimate users or reviewers
```

Existing first-line protection already applied:

```text
- Nginx blocks /.env, /.git, wp-admin, phpmyadmin and similar scan paths
- Nginx blocks /actuator/prometheus externally
- Nginx blocks sensitive actuator endpoints except /actuator/health
- Fail2Ban sshd jail protects repeated SSH login failures
- legacy start-up-app is not used as production traffic path
```

Rate Limit is tracked separately in `docs/infra/RATE_LIMIT_POLICY.md`.
GeoIP/country blocking must not replace endpoint hardening or user/session-aware backend quota.

## 3. Current Defense State

```text
Lightsail firewall:
- public: 22, 80, 443
- not public: MySQL, Redis, app-blue/app-green ports

Nginx:
- /actuator/health public
- /actuator/prometheus blocked externally
- sensitive actuator endpoints blocked externally
- common bot scan paths blocked before Spring Boot

Fail2Ban:
- sshd jail enabled
- repeated SSH auth failures can ban source IPs

Rate Limit:
- policy documented
- actual Nginx limit_req rollout deferred until frontend E2E completes
```

## 4. Read-Only Log Inspection

The commands below are read-only and are safe for investigation.
They do not change firewall, Nginx, or app state.

Top source IPs:

```bash
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
```

Recent 4xx request sources and paths:

```bash
sudo awk '$9 ~ /^4/ {print $1, $7, $9}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -30
```

Common bot scan paths:

```bash
sudo grep -E '(\.env|\.git|wp-admin|wp-login|phpmyadmin|pma|vendor|server-status)' /var/log/nginx/access.log | tail -n 80
```

Top user agents:

```bash
sudo awk -F\" '{print $6}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
```

Recent Nginx errors:

```bash
sudo tail -n 100 /var/log/nginx/error.log
```

Check whether hardening paths are being blocked:

```bash
sudo grep -E '(\.env|\.git|wp-admin|wp-login|phpmyadmin|pma)' /var/log/nginx/access.log | tail -n 50
```

Expected interpretation:

```text
403 or 404 for bot scan paths
→ Nginx hardening is handling the request before Spring Boot.

large repeated 4xx from one source
→ candidate for Fail2Ban, rate limit, WAF challenge, or temporary IP-level control.

wide distributed scans from many networks
→ country/ASN controls may be considered only after impact review.
```

## 5. Log Interpretation Caution

Top source IPs in Nginx access logs are not automatically attackers.

The current ClueRoom production server can include traffic from:

```text
- Codex-based QA requests
- Android/frontend E2E test requests
- team member manual testing
- infra lead curl / Swagger checks
- GitHub Actions or external validation checks
```

Do not block an IP only because it appears frequently in access logs.

Bot-like traffic should be judged by multiple signals:

```text
- repeated scan paths such as /.env, /.git, wp-admin, phpmyadmin
- abnormal repeated requests in a short time window
- high 4xx ratio
- suspicious User-Agent patterns
- overlap with known QA/test time windows
- whether the IP only probes scan paths without normal API flow
```

This caution is important during frontend E2E and Codex QA because test automation can temporarily look noisy.

## 6. Blocking Options

### 6.1 Cloudflare WAF Custom Rules

Description:

```text
Place Cloudflare in front of api.clueroom.xyz and define custom WAF rules by country, ASN, continent, path, rate, or threat characteristics.
```

Pros:

```text
- country / ASN / continent conditions can be used
- challenge, block, skip, and rate controls can be combined
- bot traffic can be filtered before it reaches Lightsail
- server CPU, Nginx, and Spring Boot receive less unwanted traffic
- operational controls are centralized outside the production VM
```

Cons:

```text
- current DNS/proxy flow uses Dynadot DNS, so Cloudflare proxy introduction is a larger change
- DNS, TLS, proxy mode, real client IP headers, and cache behavior must be reviewed
- feature availability and rule syntax can vary by Cloudflare plan
- misconfigured proxy/WAF rules can break normal Android/API traffic
```

Recommendation:

```text
Long-term production candidate only if ClueRoom later needs CDN, managed WAF, challenge-based bot control, or centralized edge policy.
Not selected for the current short-term PoC because it changes DNS, proxy, SSL, and client IP handling scope.
If evaluated later, use a PoC domain first and start with challenge/log mode where possible.
```

### 6.2 Nginx GeoIP2

Description:

```text
Install GeoIP2 support on the Nginx host and use a GeoIP database such as MaxMind GeoLite2 Country to map client IP to country code.
```

Pros:

```text
- works inside the current Nginx reverse proxy structure
- no Cloudflare migration is required
- useful learning PoC for edge-level request classification
- can combine country decisions with path-specific rules
```

Cons:

```text
- GeoIP2 module installation is required
- GeoLite2/GeoIP2 database account, license, download, and update process must be managed
- stale GeoIP DB can make decisions inaccurate
- production server performs the classification work
- rollback and update automation must be documented
```

Recommendation:

```text
Preferred short-term PoC for this project.
It works within the current Nginx reverse proxy structure and does not require DNS/proxy migration.
Use a test host, test server block, or poc-api.clueroom.xyz first.
Use cautiously for always-on production enforcement.
```

### 6.3 ipset / iptables / nftables Country CIDR Blocking

Description:

```text
Load country-based CIDR ranges into kernel-level firewall sets and drop traffic before it reaches Nginx.
```

Pros:

```text
- traffic can be dropped before Nginx
- can be efficient for known bad CIDR sets
- independent of app and Nginx path logic
```

Cons:

```text
- country CIDR lists change and require updates
- normal users, reviewers, VPN users, or cloud traffic can be blocked by mistake
- wrong rules can lock out legitimate access or complicate incident response
- operational rollback must be extremely clear
- heavy-handed for current MVP scale
```

Recommendation:

```text
Do not use for current production MVP.
Keep as investigation/comparison only unless there is a clear abuse incident and rollback plan.
```

## 7. Production Enforcement Criteria

Do not apply country or ASN blocking until all conditions below are satisfied.

```text
1. Nginx access logs show repeated bot/scanner traffic from a country, ASN, or provider.
2. Existing Nginx hardening and Fail2Ban are not enough.
3. Rate Limit policy has been considered and is insufficient for the observed pattern.
4. Expected normal user impact is low.
5. The rule can start as log/challenge/rate limit before hard block where possible.
6. Rollback steps are documented and tested.
7. The team knows how to identify false positives quickly.
```

Examples that may justify PoC:

```text
- repeated vulnerability scans across many paths
- high request volume from non-target regions causing app/Nginx load
- repeated 4xx/5xx-triggering bot activity after path hardening
- clear abuse source cluster in Nginx access logs
```

Examples that do not justify immediate country block:

```text
- one-off scan attempts
- isolated 404s for common bot paths
- normal API traffic from unknown IPs
- lack of confirmed source country/ASN
```

## 8. PoC Scenarios

### 8.1 Cloudflare WAF PoC

Recommended scope:

```text
test-api.clueroom.xyz or poc-api.clueroom.xyz
```

Steps:

```text
1. Create a test DNS/proxy route.
2. Confirm origin health through Cloudflare.
3. Create a WAF custom rule by country/ASN/path.
4. Start with managed challenge or log mode where available.
5. Verify normal API requests, Swagger, Android test requests, and health checks.
6. Document rollback and disable rule process.
```

Success criteria:

```text
- test domain remains reachable for expected clients
- unwanted condition receives challenge/block response
- origin Nginx logs show reduced unwanted requests
- rule can be disabled quickly
```

### 8.2 Nginx GeoIP2 PoC

Recommended scope:

```text
separate PoC server, test server block, or non-production route
```

Steps:

```text
1. Install GeoIP2 module in a PoC environment.
2. Configure GeoLite2 Country DB path.
3. Map client country code to an Nginx variable.
4. Return 403 for one test country or test condition.
5. Verify Nginx config with nginx -t.
6. Verify rollback by removing or disabling the map/location rule.
```

Example shape, not production-ready:

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

Notes:

```text
Nginx if usage must be reviewed carefully before production use.
This snippet is conceptual and should not be copied directly into production.
```

### 8.3 ipset / nftables PoC

Recommended scope:

```text
local lab or disposable test server only
```

Steps:

```text
1. Use a small test CIDR list, not a full country list.
2. Apply rule to a test service.
3. Confirm allowed and blocked traffic.
4. Confirm explicit rollback command.
5. Document list source and update process.
```

Production use requires a stronger change approval than document-only infra tasks.

## 9. Rollback Principles

Cloudflare:

```text
- disable WAF custom rule
- switch rule from block to log/challenge
- pause proxy only if DNS/TLS impact is understood
```

Nginx GeoIP2:

```text
- revert Nginx config
- nginx -t
- systemctl reload nginx
- verify /actuator/health
```

ipset / nftables:

```text
- remove specific rule or set
- verify SSH remains available
- verify HTTPS API health
- persist only after rollback is proven
```

Minimum post-rollback checks:

```bash
curl -I https://api.clueroom.xyz/actuator/health
/opt/clueroom/bg-status.sh
sudo tail -n 50 /var/log/nginx/error.log
```

## 10. Do Not Do

```text
- Do not apply broad China IP or country-wide blocking directly to api.clueroom.xyz during MVP.
- Do not add large CIDR deny lists manually and leave them unmanaged.
- Do not apply WAF block rules without normal user testing.
- Do not expose IP reputation API keys, WAF tokens, MaxMind license keys, or Cloudflare tokens in Slack, PRs, or docs.
- Do not use GeoIP blocking as a replacement for endpoint hardening, rate limit, and app-level authorization.
- Do not block OPTIONS/preflight without frontend E2E verification.
```

## 11. Completion Criteria

```text
- GEOIP_BOT_TRAFFIC_POLICY.md exists.
- Cloudflare WAF, Nginx GeoIP2, and ipset/nftables options are compared.
- The reason for not applying immediate production country blocking is explicit.
- Read-only log inspection commands are documented.
- PoC conditions and rollback principles are documented.
- No production server setting is changed.
- No secret file or token is added.
```

## 12. Review Checklist

```text
- Does the document avoid immediate broad country blocking?
- Does it separate observation, PoC, and production enforcement?
- Does it mention normal user false-positive risk?
- Does it include rollback paths?
- Does it avoid committing API keys, WAF tokens, MaxMind keys, or Cloudflare credentials?
- Does it keep Cloudflare WAF, Nginx GeoIP2, and kernel firewall options separate?
```
