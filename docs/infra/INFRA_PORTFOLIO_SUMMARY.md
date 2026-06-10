# ClueRoom 인프라 포트폴리오 요약

> 목적: ClueRoom 인프라 작업을 발표, 포트폴리오, 면접 리뷰용으로 요약한다.
> 운영 명령어는 `OPS_RUNBOOK.md`에 두고, 구조와 로드맵은 `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`에 둔다.
> 이 문서는 secret, 원본 로그, DB dump, private IP credential, private scenario data를 의도적으로 포함하지 않는다.

---

## 1. 한 줄 요약

ClueRoom은 저비용 AWS Lightsail MVP 구조로 운영하지만, Blue-Green 앱 배포, 분리된 data server, S3 DB 백업과 복구 rehearsal, Loki/Grafana/n8n/Slack 관측, Nginx rate limit과 CN IP 차단, AI 호출 LLMOps telemetry 같은 운영 안정장치를 갖춘 구조다.

---

## 2. 제약 조건

```text
- 부트캠프 최종 프로젝트라 MVP 비용 제약이 있다.
- Android + Spring Boot 서비스가 api.clueroom.xyz로 외부에 노출된다.
- AI 심문이 핵심 기능이므로 latency, failure, fallback, token cost를 볼 수 있어야 한다.
- 관리형 인프라의 완성도보다 팀 데모 안정성과 rollback 가능성이 더 중요하다.
- 인프라 작업은 managed service만 쓰는 것이 아니라 Linux/Docker/Nginx 운영 경험을 보여줘야 한다.
```

목표는 가장 큰 AWS stack을 쓰는 것이 아니었다.
작은 startup-style MVP에서 필요한 최소한의 현실적인 운영 모델을 만드는 것이 목표였다.

---

## 3. 현재 운영 구조

| Server | 역할 | 주요 구성요소 |
|---|---|---|
| `clueroom-api-prod-01` | API ingress와 app runtime | Nginx, app-blue, app-green, Prometheus, Grafana, Alloy |
| `clueroom-data-01` | 데이터 source of truth | MySQL, Redis, local backup, S3 upload, DATA_HEALTH, S3_BACKUP_HEALTH |
| `clueroom-ops-01` | 로그와 alert 자동화 | Loki, n8n, OPS_HEALTH, Slack alert router |

현재 요청 흐름:

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

관련 다이어그램:

```text
docs/infra/diagrams/current-production-request-flow.mmd
docs/infra/diagrams/observability-alert-flow.mmd
docs/infra/diagrams/backup-restore-flow.mmd
docs/infra/diagrams/planned-scaleout-manual-lb.mmd
```

---

## 4. 구현된 내용

### Blue-Green 앱 배포

```text
app-blue  -> 127.0.0.1:8081
app-green -> 127.0.0.1:8082
Nginx upstream이 active slot을 제어한다.
```

운영적으로 중요한 이유:

```text
- 새 버전을 standby slot에 먼저 배포한다.
- health check 통과 후에만 Nginx upstream을 전환한다.
- 문제가 생기면 upstream을 이전 slot으로 되돌려 rollback한다.
- 일반적인 애플리케이션 배포에서 전체 downtime을 줄인다.
```

### External Data Cutover 적용

이전 구조:

```text
prod server: Nginx + app + MySQL + Redis
```

현재 구조:

```text
prod server: Nginx + app-blue/app-green
data server: MySQL + Redis
```

효과:

```text
- app container가 stateless에 가까워졌다.
- app scale-out이 가능해졌다.
- 데이터 소유권이 명확해졌다.
- prod local MySQL/Redis는 source of truth가 아니다.
```

### S3 DB 백업과 복구 Rehearsal

백업 경로:

```text
data MySQL
  -> /opt/clueroom-data/backup-mysql.sh
  -> local .sql.gz
  -> /opt/clueroom-data/upload-mysql-backup-s3.sh
  -> private S3 backup bucket + .sha256 sidecar
  -> S3_BACKUP_HEALTH to Loki/Grafana
```

복구 rehearsal 검증 항목:

```text
- S3에서 .sql.gz와 .sha256 다운로드
- sha256 비교
- gzip -t 실행
- temporary mysql:8.4 container에 restore
- startup database와 핵심 table count 확인
- temporary container와 다운로드 파일 제거
```

### 관측과 알림

```text
prod Nginx/App logs -> Alloy -> ops Loki
data/ops/prod health heartbeats -> Loki
Prometheus/Grafana -> dashboards and alerts
Grafana alert -> n8n -> Slack
Gemini analysis -> optional helper message
```

중요한 운영 규칙:

```text
기본 deterministic Slack alert를 먼저 보낸다.
Gemini 실패가 기본 alert를 막으면 안 된다.
Codex는 real-time automatic fallback이 아니라 manual handoff와 deep analysis 용도다.
```

현재 n8n workflow:

```text
ClueRoom - Grafana Alert Router v8 Budgeted Gemini 3.5
ClueRoom - Ops Snapshot Agent v5 Lite Daily Budget
ClueRoom - LLMOps Light Monitor v4 Budgeted Gemini 3.5
ClueRoom - Infra Codex Handoff Report v1
ClueRoom - LLMOps Codex Handoff Report v2
```

### 트래픽 방어

현재 baseline:

```text
Nginx API per-IP rate limit enforced
limit_req_dry_run off
limit_req_status 429
CN IPv4 block applied
manual blocklist snippet available
sensitive path scanning blocked with 403
403/429 alerts routed through Grafana/n8n/Slack
```

이것은 MVP 수준의 방어이며, 완전한 WAF 대체가 아니다.

### LLMOps 관측

AI call 관측은 raw prompt/answer 저장이 아니라 파생 metadata 중심이다.

```text
AI_CALL: feature, provider, model, latency, tokens, status
AI_CALL_CONTEXT: prompt block token estimates and template hash
```

안전 규칙:

```text
AI_CALL_CONTEXT에는 raw prompt, raw answer, user question text, sessionId, scenarioId, suspectId, npcCode를 기록하지 않는다.
```

---

## 5. 현재 운영 baseline이 아닌 것

아래 항목은 의도적으로 현재 운영 baseline으로 설명하지 않는다.

```text
1. Server-side Infra Codex automatic production operation
2. Codex as real-time fallback when Gemini alert analysis fails
3. Terraform-created app server scale-out with manual Nginx load balancing
```

Scale-out 작업은 단기 PoC다.
검증 후 팀이 명시적으로 유지하기로 결정하지 않는 한, 추가 app server resource와 static IP는 제거해야 한다.

운영 baseline은 그대로 아래 구조다.

```text
prod server 1
+ data server 1
+ ops server 1
```

---

## 6. 면접 Q&A 짧은 답변

### 왜 Lightsail을 선택했나?

이 프로젝트는 저비용 MVP를 목표로 한다.
Lightsail은 비용과 운영 복잡도를 낮게 유지하면서도 실제 Linux, Docker, Nginx, backup, monitoring 운영 경험을 만들 수 있다.

### 왜 RDS를 쓰지 않았나?

RDS는 향후 선택지로 유효하다.
다만 이번 프로젝트에서는 직접 운영 경험과 낮은 비용이 중요했고, data server 분리, S3 백업, restore rehearsal로 위험을 줄였다.

### 왜 Blue-Green인가?

Standby slot에 먼저 배포하고 health check 후 Nginx upstream을 전환할 수 있다.
새 버전에 문제가 생기면 upstream을 되돌려 빠르게 rollback할 수 있다.

### 왜 MySQL/Redis를 data server로 분리했나?

각 app server가 local DB/Redis를 소유하면 app scale-out이 안전하지 않다.
데이터를 외부화하면 app-blue/app-green과 향후 app server가 동일한 source of truth를 바라볼 수 있다.

### Gemini 실패가 알림을 깨뜨리나?

아니다.
n8n은 기본 deterministic Slack alert를 먼저 보내고, Gemini는 선택 분석만 추가한다.

### Codex가 인프라를 자동 운영하나?

아니다.
Codex는 real-time production operator로 붙어 있지 않다.
Repo 작업, 문서/runbook 개선, manual handoff/deep analysis 용도다.

### Scale-out이 운영 중인가?

아니다.
Baseline은 여전히 prod app server 1대, data server 1대, ops server 1대다.
Terraform scale-out과 manual Nginx load balancing은 PoC이며 검증 후 정리해야 한다.

---

## 7. 발표 스크립트

### 30초

ClueRoom은 저비용 Lightsail MVP로 시작했지만, 운영에 필요한 안전장치를 단계적으로 추가했습니다. Prod server는 Nginx와 Blue-Green Spring Boot slot을 운영하고, 데이터는 전용 MySQL/Redis data server로 분리했습니다. 로그와 알림은 Loki, Grafana, n8n, Slack이 있는 ops server로 보냅니다. 또한 S3 DB 백업과 restore rehearsal, Nginx rate limiting과 CN IP block, AI call 비용/지연 관측을 위한 LLMOps telemetry를 적용했습니다. 남은 scale-out은 Terraform/manual Nginx PoC이며, 기본 운영 baseline은 아닙니다.

### 1분

인프라 목표는 비용 제약 안에서 현실적인 MVP 운영 모델을 만드는 것이었습니다. RDS, ALB, Kubernetes로 바로 뛰기보다 Lightsail을 사용하고 운영 제어를 직접 구성했습니다.

Prod server는 Nginx ingress와 Blue-Green app 배포를 담당합니다. Data server는 MySQL과 Redis source of truth를 담당합니다. Ops server는 Loki, Grafana, n8n, Slack을 통해 중앙 로그와 alert 자동화를 담당합니다.

신뢰성을 위해 DB 백업은 data server에서 실행하고, sha256 sidecar와 함께 private S3 backup bucket에 업로드하며, temporary MySQL container에서 restore rehearsal로 검증합니다. 트래픽 방어는 Nginx rate limiting, CN IPv4 block, manual blocklist, 403/429 alert를 적용했습니다. AI 운영은 raw prompt/answer를 저장하지 않고 AI_CALL과 AI_CALL_CONTEXT metadata로 latency, token cost, fallback, prompt block estimate를 관측합니다.

### 3분

핵심 인프라 결정은 시스템을 저렴하게 유지하되 운영적으로 신뢰 가능한 구조를 만드는 것이었습니다. 무거운 managed infrastructure로 성급히 이동하지 않았지만, 실제 MVP에서 중요한 deploy rollback, data ownership, backup verification, monitoring, alerting, traffic defense는 직접 구축했습니다.

Prod server는 Nginx와 두 개의 Spring Boot slot인 app-blue/app-green을 실행합니다. 배포는 standby slot에 먼저 들어가고, health check를 통과하면 Nginx upstream을 전환합니다. Kubernetes나 ALB 없이도 rollback 가능한 구조를 만들기 위한 선택입니다.

Data server는 MySQL과 Redis를 소유합니다. DB나 Redis가 각 app server 안에 있으면 scale-out이 안전하지 않기 때문에 필요했던 분리입니다. 현재 app container는 external data endpoint를 바라보며, app runtime은 stateless에 가까워졌습니다.

Ops server는 로그와 alert routing을 중앙화합니다. Prod 로그는 Alloy를 통해 Loki로 전달하고, data/ops health check는 heartbeat log로 push됩니다. Grafana alert는 n8n을 거쳐 Slack으로 갑니다. Gemini는 분석을 추가할 수 있지만 기본 alert는 Gemini에 의존하지 않습니다. Codex도 real-time production fallback이 아니라 manual analysis와 handoff 도구입니다.

백업은 dump 파일 생성만으로 완료로 보지 않습니다. Data server가 gzip dump를 만들고, sha256 sidecar와 함께 S3에 업로드하고, S3 backup health를 push하며, 팀은 백업을 다운로드해 temporary MySQL container에 import하는 방식으로 restore rehearsal을 수행합니다.

마지막으로 트래픽 방어는 Nginx rate limit enforcement, CN IPv4 block, manual blocklist, sensitive path blocking으로 처리합니다. 남은 scale-out 작업은 Terraform으로 추가 app server를 만들고 Nginx upstream에 수동 연결하는 짧은 PoC입니다. 기본 baseline은 prod 1대, data 1대, ops 1대입니다.

---

## 8. 과장해서 말하지 말 것

```text
- Kubernetes, ECS, ALB, ASG를 운영 중이라고 말하지 않는다.
- MySQL HA나 replication을 운영 중이라고 말하지 않는다.
- Codex automatic production fallback을 운영 중이라고 말하지 않는다.
- scale-out이 일반 운영 baseline이라고 말하지 않는다.
- WAF 수준의 완전한 보안을 갖췄다고 말하지 않는다.
```

대신 아래처럼 설명한다.

```text
ClueRoom은 실용적인 운영 안전장치와 명확한 scale-out 경로를 갖춘 저비용 MVP 인프라를 사용한다.
```
