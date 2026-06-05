# ClueRoom Rate Limit Policy

> Status: INFRA-02 policy design only.
> This document does not apply production Nginx `limit_req`, change Spring Boot code, or restart any server.

## 1. Purpose

ClueRoom needs rate limiting for three different risks.

```text
1. repeated bot traffic and scanner noise
2. excessive normal API traffic before it reaches Spring Boot
3. cost-bearing AI API abuse, double taps, retries, and repeated submissions
```

Actual production enforcement is deferred to `INFRA-03 Nginx Rate Limit PoC` after Android frontend E2E QA is complete.
The dry-run operating procedure is documented in `docs/infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md`.
Applying a strong edge rate limit during E2E can create false `429` failures and make frontend/backend debugging ambiguous.

## 2. Layer Separation

### Nginx IP-Based Rate Limit

Role:

```text
- first-line IP-based defense
- reduce repeated bot/scanner traffic
- prevent obviously excessive requests from reaching Spring Boot
- protect common public API paths from accidental or scripted request bursts
```

Limits:

```text
- Android users can share one carrier NAT or public Wi-Fi IP.
- A real attacker can rotate IPs.
- IP-based limits do not understand userId, sessionId, scenarioId, or AI feature state.
- AI cost defense cannot rely only on Nginx IP limits.
```

Nginx should be treated as edge throttling, not product-level quota.

### Backend Redis Rate Limit

Role:

```text
- userId / sessionId / scenarioId aware limits
- protect cost-bearing AI features
- prevent duplicate submissions and rapid repeat requests
- coordinate limits across app-blue/app-green during Blue-Green operation
```

Targets:

```text
- AI interrogation
- scenario validation
- final deduction submission
- FCM device token registration
- future presigned URL issuing or upload APIs
```

Redis-based limits should be implemented in backend code later. This policy document only defines the target shape.

## 3. API Policy Candidates

These values are initial candidates. Final values must be adjusted after frontend E2E, smoke tests, and observed traffic.

### General Read APIs

Examples:

```text
GET /api/scenarios
GET /api/scenarios/{scenarioId}
GET /api/play-sessions/{sessionId}/dashboard
GET /api/play-sessions/{sessionId}/locations
GET /api/play-sessions/{sessionId}/evidences
GET /api/play-sessions/{sessionId}/suspects
GET /api/play-sessions/{sessionId}/timeline
```

Candidate:

```text
Nginx: 5~10 req/s per IP, burst 20
Backend Redis: usually not needed for read-only MVP APIs
```

Notes:

```text
- Android screens can refresh several read APIs together.
- Too strict IP limits can break normal app navigation on carrier NAT.
- Start lenient and tune using Nginx access logs and app UX.
```

### AI Cost APIs

Examples:

```text
POST /api/play-sessions/{sessionId}/interrogations
POST /api/ai/scenarios/{scenarioId}/validate
POST /api/play-sessions/{sessionId}/final-deduction
```

Candidate:

```text
Nginx: 1 req/s per IP, burst 3~5
Backend Redis:
- userId-based AI request limit per minute
- sessionId-based interrogation limit
- scenarioId-based validation limit
- final-deduction duplicate submission guard per session
```

Notes:

```text
- These endpoints can trigger external AI provider cost.
- Nginx can reduce obvious bursts, but Redis must enforce product-aware limits.
- final-deduction already has state/lock behavior; Redis quota should complement, not replace it.
```

### FCM Device Token Registration

Example:

```text
POST /api/device-tokens
```

Candidate:

```text
Nginx: 1~2 req/s per IP, burst 5
Backend: upsert by userId + token hash
```

Notes:

```text
- Token registration can happen during app start or login refresh.
- Backend should make repeated same userId + token calls idempotent.
```

### File / Image APIs

Current:

```text
S3 public image GET is not served by ClueRoom Nginx.
```

Policy:

```text
- S3 public image GET is not an Nginx rate-limit target.
- If a presigned URL issuing API is added, apply stronger backend and edge limits.
- If upload APIs are added, manage rate limits together with body size and content validation.
```

### Health / Swagger / Preflight

Health:

```text
GET /actuator/health
```

Policy:

```text
- Do not apply aggressive limits.
- Blue-Green scripts, uptime checks, and manual incident response depend on health checks.
```

Swagger:

```text
/swagger-ui/**
/v3/api-docs/**
```

Policy:

```text
- Keep available during team development and MVP testing.
- For stronger production hardening, consider Basic Auth or IP restriction.
```

OPTIONS preflight:

```text
OPTIONS /*
```

Policy:

```text
- Do not apply strong limits before frontend E2E validation.
- CORS preflight failures can look like API failures in Android/web clients.
- If throttled later, verify the actual frontend networking stack first.
```

## 4. Nginx Example

The following is documentation-only. Do not apply this to production before `INFRA-03`.

`http {}` level example:

```nginx
limit_req_zone $binary_remote_addr zone=api_per_ip:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=ai_per_ip:10m rate=1r/s;
limit_req_zone $binary_remote_addr zone=device_token_per_ip:10m rate=2r/s;
```

Endpoint examples:

```nginx
# General API
location /api/ {
    limit_req zone=api_per_ip burst=20 nodelay;
    proxy_pass http://clueroom_backend;
}

# AI heavy endpoints
location ~ ^/api/(play-sessions/.*/interrogations|ai/scenarios/.*/validate|play-sessions/.*/final-deduction) {
    limit_req zone=ai_per_ip burst=5;
    proxy_pass http://clueroom_backend;
}

# Device token registration
location = /api/device-tokens {
    limit_req zone=device_token_per_ip burst=5 nodelay;
    proxy_pass http://clueroom_backend;
}
```

Before applying:

```text
1. Re-check exact API paths in docs/CaseLab_AI_API_Spec.md.
2. Verify Android frontend request bursts during normal navigation.
3. Verify Swagger/API docs paths if they stay open.
4. Verify OPTIONS preflight behavior.
5. Test 429 behavior in a staging or controlled production window.
```

## 5. Redis Rate Limit Design

Candidate keys:

```text
rate:ai:interrogation:user:{userId}:session:{sessionId}
rate:ai:validation:user:{userId}:scenario:{scenarioId}
rate:final-deduction:session:{sessionId}
rate:device-token:user:{userId}:token:{tokenHash}
```

Candidate dimensions:

```text
userId
sessionId
scenarioId
featureType
tokenHash
time window
```

Example error response shape:

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

Notes:

```text
- The final backend code should align with the existing ErrorResponse / BusinessException style.
- Retry-After HTTP header can be considered for API consumers.
- Error response must not include secret, scenario solution, prompt, or provider details.
```

## 6. Rollout Order

```text
1. Finalize this policy document.
2. Wait for frontend E2E QA completion.
3. Apply Nginx general API limit_req dry-run using `docs/infra/RATE_LIMIT_DRY_RUN_RUNBOOK.md`.
4. Observe dry-run logs without returning 429.
5. After E2E, decide whether to run a controlled real-enforcement PoC.
6. Verify 429 responses and Android UX only in the controlled enforcement step.
7. Design backend Redis user/session/scenario limits.
8. Connect rate-limit events to AI cost, latency, and fallback metrics.
```

`INFRA-03` should only start after frontend E2E no longer depends on unrestricted request bursts.

## 7. Do Not Do

```text
- Do not apply strong production rate limits during frontend E2E.
- Do not rely only on IP limits for AI cost control.
- Do not rate-limit S3 public image GET through ClueRoom Nginx.
- Do not break OPTIONS preflight with aggressive limits.
- Do not add backend feature code as part of INFRA-02.
- Do not include server secrets, API keys, or private scenario data in rate-limit logs or docs.
```

## 8. Review Checklist

```text
- Nginx IP limit and Redis application quota are clearly separated.
- Android carrier NAT / public Wi-Fi risk is acknowledged.
- AI cost endpoints are classified separately.
- Frontend E2E completion is a gate before production application.
- OPTIONS preflight caution is documented.
- S3 image traffic is excluded from ClueRoom Nginx limits.
```
