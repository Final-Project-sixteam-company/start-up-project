# ClueRoom 보안 / 트래픽 / 알림 정책

## 1. 목적

이 문서는 traffic defense, rate limiting, GeoIP/bot 처리, Grafana alert 설계의 정본 정책이다.
상세 운영 명령어는 `OPS_RUNBOOK.md`에 둔다.
인프라 구조와 scale-out 근거는 `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`에 둔다.

흡수한 이전 문서:

| 이전 문서 | 이 문서에 흡수한 내용 |
|---|---|
| `RATE_LIMIT_POLICY.md` | rate limit 원칙, API 정책 후보, Redis/Nginx 계층 분리, rollout 순서 |
| `GEOIP_BOT_TRAFFIC_POLICY.md` | bot/GeoIP 해석, 차단 옵션, 운영 적용 기준 |
| `GRAFANA_ALERT_POLICY.md` | alert 원칙, 안전한 alert 후보, Blue-Green 오탐 방지 규칙, notification policy |

## 2. 현재 Baseline

```text
- Nginx가 api.clueroom.xyz의 public ingress다.
- Spring Boot app은 Blue-Green slot 뒤에서 실행된다.
- Prometheus/Grafana로 metrics와 dashboard를 본다.
- ops Loki/Alloy는 read-only log와 heartbeat 점검에 사용한다.
- n8n은 Grafana alert를 Slack으로 routing한다.
- Nginx API per-IP rate limit은 enforcement 상태다.
- Nginx CN IPv4 block은 적용된 상태다.
- 좁은 범위의 abusive IP block을 위한 manual blocklist snippet이 준비되어 있다.
- AI endpoint는 직접 LLM 비용을 만들 수 있으므로 추가 주의가 필요하다.
- Frontend E2E와 demo 안정성이 공격적인 차단보다 우선한다.
```

새 enforcement layer를 여러 개 한 번에 추가하지 않는다.
새 규칙이나 threshold 변경은 observe first, dry-run, evidence 안정화 후 enforce 순서를 따른다.
현재 API rate-limit과 CN block baseline은 이 단계를 이미 통과한 운영 상태다.

## 3. 계층 분리

### Nginx IP 기반 Rate Limit

Nginx는 IP 기준의 거친 요청 제한에 사용한다.

적합한 용도:

```text
- 광범위한 API abuse
- AI endpoint burst protection
- FCM token registration spam
- 명확한 bot/crawler burst 제어
```

Nginx는 user/account/business 판단을 하면 안 된다.
Nginx는 IP/path/method만 보므로 거친 제한 계층으로 유지한다.

### Backend Redis Rate Limit 계층

Backend Redis rate limiting은 user/session/scenario aware 규칙에 사용한다.

적합한 용도:

```text
- user별 AI call limit
- session별 interrogation/final-deduction pacing
- device별 FCM registration pacing
- authenticated identity가 필요한 abuse control
```

Backend rate limit은 Android가 처리할 수 있는 API error response를 반환해야 한다.

### Cloudflare / WAF 계층

DNS/proxy 구성이 활성화되어 있다면 Cloudflare WAF를 coarse bot/country/path rule에 사용할 수 있다.
넓은 L7 제어와 빠른 rollback에는 Cloudflare가 적합하지만, app-specific cost control을 Cloudflare에만 의존하면 안 된다.

## 4. API Rate Limit 후보

| API group | 초기 정책 방향 |
|---|---|
| General read APIs | 정상 앱 사용에 마찰이 없을 정도로 충분히 높게 설정 |
| `POST /api/play-sessions/{sessionId}/interrogations` | AI 비용 때문에 더 엄격하게 설정 |
| `POST /api/play-sessions/{sessionId}/final-deduction` | AI 비용과 state transition 때문에 더 엄격하게 설정 |
| `POST /api/ai/scenarios/{scenarioId}/validate` | 일반 사용자 노출 API 중 가장 엄격하게 설정 |
| FCM token registration | 중간 수준 제한, 이후 device/user aware로 확장 |
| image/static URLs | app Nginx만으로 처리하지 말고 CDN/S3 control 우선 |
| health/swagger/preflight | deploy, QA, CORS, monitoring을 깨지 않게 보호 |

현재 Nginx enforcement baseline:

```text
limit_req_zone clueroom_api_per_ip: 20r/s
burst=60 nodelay
limit_req_dry_run off
limit_req_status 429
limit_conn per IP: 30
```

운영 해석:

```text
- 429는 Nginx rate limit이며, 영구 차단이 아니다.
- 403은 CN block, manual blocklist, sensitive path block 또는 명시적인 deny rule이다.
- 403/429 alert는 정상 사용자 영향이 확인되기 전까지 warning-level signal로 본다.
- Health/swagger/preflight는 deploy, QA, monitoring 중에도 안전해야 한다.
```

참고 Nginx 형태:

```nginx
limit_req_zone $binary_remote_addr zone=clueroom_api_per_ip:10m rate=20r/s;
limit_conn_zone $binary_remote_addr zone=clueroom_conn_per_ip:10m;

location /api/ {
    limit_req zone=clueroom_api_per_ip burst=60 nodelay;
    limit_req_dry_run off;
    limit_req_status 429;
    limit_conn clueroom_conn_per_ip 30;
    proxy_pass http://clueroom_backend;
}
```

향후 Backend Redis rate-limit key 후보:

```text
rate:ai:interrogation:user:{userId}:session:{sessionId}
rate:ai:validation:user:{userId}:scenario:{scenarioId}
rate:final-deduction:session:{sessionId}
rate:device-token:user:{userId}:token:{tokenHash}
```

429 response shape 후보:

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

API consumer를 위해 `Retry-After` header를 검토할 수 있다.
응답에는 secret, scenario solution, prompt, AI provider detail, private state를 포함하지 않는다.

## 5. Rate Limit Rollout 절차

1. 현재 Nginx site와 snippet config를 백업한다.
2. threshold 변경이나 새 endpoint group 추가 시 먼저 dry-run mode로 zone/snippet을 추가하거나 변경한다.
3. Nginx syntax를 검증한다.
4. Nginx를 reload한다.
5. 정상 Android/API smoke를 실행한다.
6. Access/error log에서 dry-run hit를 확인한다.
7. Threshold를 조정한다.
8. 그 다음 변경한 rule을 enforcement로 전환한다.

Frontend E2E QA 중이거나 demo 전에는 명시적 승인 없이 enforcement를 확대하지 않는다.

새로운 production enforcement 또는 threshold 변경에는 아래 조건이 필요하다.

```text
- 정상 Android smoke 실패 없음
- health/swagger/preflight 깨짐 없음
- dry-run hit sample 리뷰 완료
- rollback command 준비 완료
- threshold가 이 policy 또는 OPS_RUNBOOK에 문서화됨
```

## 6. Bot / GeoIP 정책

### 해석 규칙

알 수 없는 IP/country traffic 자체만으로 incident로 보지 않는다.
아래 항목과 함께 판단한다.

```text
- request rate
- path pattern
- response status
- user-agent
- AI endpoint hits
- DB/app resource impact
- repeated 4xx/5xx
```

Read-only 점검은 허용된다.
현재 CN IPv4 block은 운영 baseline이다.
새 country block, manual IP block, 더 넓은 deny rule은 여전히 증거와 rollback path가 필요하다.

### 차단 옵션

| Option | 사용 조건 | 비고 |
|---|---|---|
| Cloudflare WAF custom rules | Cloudflare proxy가 활성화되어 있고 빠른 rollback이 필요할 때 | 넓은 범위 제어에 적합 |
| Nginx geo map | country/CIDR block을 ingress 근처에서 처리해야 할 때 | 현재 CN IPv4 block은 aggregated map 사용 |
| ipset / nftables | 심각한 L3/L4 abuse | 운영 위험이 높으므로 초기에는 피한다 |

### 운영 적용 기준

현재 CN baseline 외의 새 차단은 대부분의 조건을 만족할 때만 적용한다.

```text
- traffic이 명확히 abusive 또는 automated다.
- 정상 팀/QA traffic이 영향받지 않는다.
- blocking scope가 좁다.
- rollback이 한 command/config revert로 가능하다.
- secret 없는 evidence가 있다.
- infra lead가 승인한다.
```

단일 log sample만 보고 추가 permanent country block을 만들지 않는다.

### GeoIP / Bot PoC 부록

Read-only log 점검 명령:

```bash
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo awk '$9 ~ /^4/ {print $1, $7, $9}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -30
sudo grep -E '(\.env|\.git|wp-admin|wp-login|phpmyadmin|pma|vendor|server-status)' /var/log/nginx/access.log | tail -n 80
sudo awk -F\" '{print $6}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head -20
sudo tail -n 100 /var/log/nginx/error.log
```

현재 CN block 형태:

```nginx
geo $clueroom_is_cn_ip {
    default 0;
    include /etc/nginx/geoip/cn-aggregated.map;
}

if ($clueroom_is_cn_ip) {
    return 403;
}
```

Rollback은 include/deny rule을 제거하고, `nginx -t`를 실행한 뒤 Nginx reload와 `/actuator/health` 확인을 수행하는 것이다.

## 7. Grafana Alert 원칙

Alert는 실행 가능해야 한다.
Dashboard panel은 noisy할 수 있지만, alert는 action이 필요한 경우에만 울려야 한다.

설계 원칙:

```text
- 내부 noise보다 외부 사용자 영향 signal을 우선한다.
- Standby slot health보다 active Blue-Green slot health를 우선한다.
- exporter가 부족하면 정확해질 수 없는 alert는 피한다.
- 예상 가능한 deploy transition을 page하지 않는다.
- Critical alert 전에 manual review 또는 low-severity notification으로 시작한다.
```

## 8. 안전한 Alert 후보

### External API Health Down 감지

후보 signal:

```text
https://api.clueroom.xyz/actuator/health unavailable or unhealthy
```

사용자에게 보이는 API availability를 반영하므로 높은 우선순위로 본다.

### HTTP 5xx 증가

후보 signal:

```text
5xx rate increases over baseline
```

단일 transient failure를 피하기 위해 time window와 함께 사용한다.

### HTTP Latency 증가

후보 signal:

```text
p95/p99 API latency crosses threshold for sustained period
```

실제 traffic baseline이 생긴 뒤 조정한다.

### JVM Memory 증가

후보 signal:

```text
JVM memory pressure sustained over threshold
```

Baseline이 명확해지기 전까지 critical이 아니라 warning부터 사용한다.

### Prometheus Scrape 실패

현재 Prometheus jobs:

```text
clueroom-app
clueroom-app-blue
clueroom-app-green
prometheus
```

Standby Blue-Green target 하나가 down인 것만으로 critical alert를 만들지 않는다.

### Data / Backup / Ops Health 감지

현재 Loki heartbeat signals:

```text
DATA_HEALTH
S3_BACKUP_HEALTH
OPS_HEALTH
SERVER_HEALTH
```

안전한 alert 예시:

```text
- DATA_HEALTH가 MySQL 또는 Redis failure를 보고한다.
- S3_BACKUP_HEALTH가 S3 upload failure, missing state, stale upload를 보고한다.
- OPS_HEALTH가 Loki/n8n critical failure를 보고한다.
- 기대 window 안에 heartbeat가 없다.
```

S3 backup alert는 아래를 구분해야 한다.

```text
backup failed
S3 upload failed
S3 backup heartbeat missing
restore rehearsal not recently verified
```

### Nginx 403 / 429 해석

현재 alert 해석:

```text
403 increase
→ 보통 block rule이 동작 중이라는 의미다.
→ 정상 사용자 영향과 반복 source IP를 확인한다.

429 increase
→ rate limit이 동작 중이라는 의미다.
→ 정상 사용자 영향과 endpoint 분포를 확인한다.
```

403/429는 warning에서 시작한다.
정상 사용자 traffic이 영향받거나 API availability가 떨어질 때만 escalation한다.

## 9. 아직 안전하지 않은 Alert

더 나은 exporter 또는 검증된 bridge 없이 아래 항목을 critical alert로 만들지 않는다.

```text
- 불완전한 source 기반 host disk low
- 안정적인 exporter 없는 host CPU/RAM high
- standby Blue-Green app target 하나 down
```

Ops Snapshot의 disk 판단은 `SERVER_HEALTH`, `DATA_HEALTH`, `OPS_HEALTH`의 `disk_max_percent`를 source of truth로 사용한다.
Raw snapshot text의 percentage는 fallback으로만 사용한다.

## 10. Blue-Green Alert 규칙

안전한 정책:

```text
- active upstream app failure는 중요하다.
- app-blue와 app-green이 모두 down이면 critical이다.
- standby app-blue/app-green down은 deploy 후 정상일 수 있다.
- 외부 /actuator/health가 단일 scrape target보다 중요하다.
```

피해야 할 naive critical alert:

```promql
up{job=~"clueroom-app-blue|clueroom-app-green"} == 0
```

더 나은 check는 active slot 정보, external health, both-target-down 조건을 함께 본다.

### Alert Threshold 부록

아래는 후보 threshold다.
Alert를 만들기 전에 Grafana Explore에서 실제 Prometheus metric name을 확인한다.

| Alert | 후보 metric/expression | Window | Threshold | Severity |
|---|---|---:|---:|---|
| AI failures spike 감지 | `sum(increase(ai_failures_total[5m]))` | 5m | `>= 23` | WARNING first |
| AI p95 latency high 감지 | `histogram_quantile(0.95, sum(rate(ai_latency_seconds_bucket[5m])) by (le, feature_type))` | 5m | `> 23s` | WARNING first |
| AI fallback spike 감지 | `sum(increase(ai_fallbacks_total[5m]))` | 5m | baseline 이후 조정 | WARNING first |
| Blue-Green 양쪽 target down | `up{job="clueroom-app-blue"} == 0 and up{job="clueroom-app-green"} == 0` | 1~3m | true | CRITICAL candidate |
| Prometheus scrape down 감지 | `up{job="prometheus"} == 0` | 1~3m | true | CRITICAL candidate |
| Nginx 403 blocked request 감지 | HTTP 403에 매칭되는 Loki access log query | 5~10m | baseline 이후 조정 | WARNING |
| Nginx 429 rate limit 감지 | HTTP 429에 매칭되는 Loki access log query | 5~10m | baseline 이후 조정 | WARNING |
| S3 backup failed/stale 감지 | Loki `{job="s3-backup-health", instance="clueroom-data-01"}` status failure/stale | 10m | failure present | CRITICAL |
| S3 backup heartbeat missing 감지 | Loki S3_BACKUP_HEALTH count | 10m | below 1 | CRITICAL |

Prometheus로 export된 Micrometer 이름은 underscore 형태를 사용한다.
따라서 Java metric 이름 `ai.failures`, `ai.latency`, `ai.fallbacks`는 `ai_failures_total`, `ai_latency_seconds_*`, `ai_fallbacks_total`로 예상한다.
Bucket series가 없으면 histogram publishing을 확인하기 전까지 p95 alert를 만들지 않는다.

## 11. Notification Policy 알림 정책

현재 notification path:

```text
1. Grafana alert fires
2. n8n receives alert webhook
3. n8n sends deterministic Slack alert first
4. Gemini may add optional analysis
5. Codex is used for manual handoff/deep analysis, not real-time automatic fallback
```

### 현재 n8n Workflow Inventory

이 표는 2026-06-10에 확인한 active n8n workflow export를 요약한다.
Raw workflow JSON export는 webhook path, credential reference, Slack channel ID, URL, prompt body를 포함할 수 있으므로 commit하지 않는다.

| Workflow | Trigger | 모니터링 대상 / 입력 | Deterministic Slack Output | Optional AI / Handoff | Failure Budget |
|---|---|---|---|---|---|
| `ClueRoom - Grafana Alert Router v8 Budgeted Gemini 3.5` | Grafana POST webhook | Grafana alert payload, Nginx 5xx 또는 app ERROR/Exception 관련 Loki logs | 기본 alert 먼저 전송: status, max severity, firing/resolved count, alert summary, 즉시 확인할 항목 | `gemini-3.5-flash`가 기본 alert 이후 짧은 한국어 분석 추가 | firing alert에만 Gemini 사용, daily Gemini limit 3, 70초 후 1회 retry, basic alert는 Gemini에 막히지 않음 |
| `ClueRoom - Ops Snapshot Agent v5 Lite Daily Budget` | Manual + 24h schedule | `/opt/clueroom/ops-snapshot.sh`, `DATA_HEALTH`, `SERVER_HEALTH` | 기본 ops status 먼저 전송: prod/data health, Nginx syntax signal, disk/memory summary, recent error pattern count | `gemini-2.5-flash-lite`가 기본 status 이후 한국어 ops analysis 추가 | Daily Gemini limit 1, 70초 후 1회 retry, basic status는 Gemini에 막히지 않음 |
| `ClueRoom - LLMOps Light Monitor v4 Budgeted Gemini 3.5` | Manual + 1h schedule | 최근 60m window의 Loki `AI_CALL` logs, limit 500 | 기본 LLMOps summary 먼저 전송: count, success/failure/fallback, latency, token total, top feature/prompt groups | `gemini-3.5-flash`가 기본 summary 이후 짧은 LLMOps analysis 추가 | Daily Gemini limit 2, 70초 후 1회 retry, basic summary는 Gemini에 막히지 않음 |
| `ClueRoom - Infra Codex Handoff Report v1` | Manual + 24h schedule | Ops snapshot, `DATA_HEALTH`, `SERVER_HEALTH`, `OPS_HEALTH`, 최근 Nginx 5xx, app ERROR/Exception, 최근 `AI_CALL` | Human/Codex review용 Slack handoff report | Codex handoff report만 생성, autonomous production action 없음 | Slack report만 생성, agent action은 human-approved 상태 유지 |
| `ClueRoom - LLMOps Codex Handoff Report v2` | Manual + 24h schedule | 최근 24h Loki `AI_CALL` logs, limit 5000 | Slack LLMOps handoff: totals, failure/fallback rate, latency, tokens, top prompt buckets, sample failures | Codex handoff report만 생성, autonomous prompt/runtime change 없음 | Slack report만 생성, prompt/backend 변경은 PR review 필요 |

### Workflow Alert 내용

현재 alert/reporting 역할 분리는 아래와 같다.

```text
Grafana Alert Router
→ event notification
→ critical firing alert는 channel mention 가능
→ Gemini 분석 전에 기본 runbook check를 포함한다.

Ops Snapshot Agent
→ periodic state report
→ prod/data health, heartbeat bridge, Nginx syntax, disk/memory, recent error pattern을 확인한다.
→ WARNING/INFO output은 user-facing impact와 함께 나타나지 않는 한 report-grade다.

LLMOps Light Monitor
→ hourly AI_CALL summary
→ failure count, fallback count, latency, token total, feature/prompt grouping을 본다.
→ 60m window에서 failure >= 3, fallback >= 3, max latency >= 15s이면 CRITICAL
→ failure/fallback 존재, max latency >= 8s, average latency >= 5s, total tokens >= 30000이면 WARNING

Infra Codex Handoff
→ daily infra review input
→ health heartbeat, recent Nginx 5xx, app errors, AI_CALL samples를 결합한다.

LLMOps Codex Handoff
→ daily LLMOps review input
→ 24h AI_CALL cost, latency, failure, fallback, promptVersion candidate를 요약한다.
```

Gemini analysis는 보조 정보일 뿐이다.
Gemini가 실패하거나, timeout되거나, 사용할 수 없는 응답을 반환하거나, daily budget을 초과해도 deterministic Slack message는 반드시 전송되어야 한다.

후보 채널:

```text
#clueroom-alerts
#clueroom-infra
```

Slack에는 secret, raw `.env`, raw user question, AI answer, DB password, private key, Firebase JSON을 보내지 않는다.

## 12. 하지 말 것

```text
- dry-run evidence 없이 Nginx enforcement를 확대하지 않는다.
- 의심 IP 하나만 보고 추가 country block을 만들지 않는다.
- standby Blue-Green target down 자체만으로 page하지 않는다.
- Prometheus를 public internet에 직접 노출하지 않는다.
- user input이 포함된 raw access log를 public PR에 붙이지 않는다.
- Loki/n8n을 prod app server로 되돌리지 않는다.
- backend 5xx bug를 숨기기 위해 rate limiting을 사용하지 않는다.
```

## 13. Review Checklist 점검표

Traffic/security/alert policy를 바꾸기 전에 확인한다.

```text
- 이것은 observe, dry-run, enforcement 중 무엇인가?
- 어떤 정상 사용자 flow가 영향받을 수 있는가?
- rollback은 무엇인가?
- secret 없는 evidence가 있는가?
- Android E2E가 여전히 통과하는가?
- actuator health가 여전히 동작하는가?
- Swagger/preflight가 유지되어야 한다면 여전히 동작하는가?
- active Blue-Green slot과 standby를 구분했는가?
```
