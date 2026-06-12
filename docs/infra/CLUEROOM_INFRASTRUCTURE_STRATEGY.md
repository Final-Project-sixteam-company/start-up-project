# ClueRoom Infrastructure Strategy

> 문서 목적: ClueRoom 프로젝트의 인프라 구성, 선택 이유, 운영 범위, 확장 계획, PoC 계획을 별도 정본으로 관리한다.
> 기존 기획명 `CaseLab AI`는 레거시 명칭이며, 현재 서비스/도메인 기준 이름은 `ClueRoom`이다.
> MVP 이후 상세 고도화 순서와 scale-out PoC는 이 문서 하단의 통합 로드맵을 기준으로 한다.
> 보안/트래픽/알림 정책은 `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md`, 운영 명령어는 `docs/infra/OPS_RUNBOOK.md`를 기준으로 한다.
> Monitoring Agent, Infra Codex Agent, LLMOps Agent 운영 기준은 `docs/infra/agent/` 아래 문서를 기준으로 한다.

---

## 1. 현재 확정된 인프라 목표

### 1.0 고도화 문서 기준

현재 문서는 인프라 선택 이유와 운영 구조의 정본이다.
MVP 이후 세부 고도화 순서와 PoC 계획은 이 문서 하단으로 흡수했다.
보안/트래픽/알림 정책과 운영 절차, Agent/LLMOps 계획은 아래 문서로 분리한다.

```text
docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md
docs/infra/OPS_RUNBOOK.md
docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md
```

주요 고도화 항목:

```text
- Monitoring Agent
- Infra Codex Agent
- LLMOps Agent
- Nginx edge rate limit + Redis application-level AI quota
- Blue-Green alert false-positive control
- backup/restore hardening
- scale-out PoC
```

Rate Limit, GeoIP/Bot traffic, Grafana/Prometheus alert, Slack 알림 설계는 `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md`를 따른다.
Scale-out PoC의 결과는 `docs/infra/poc/POC-006-scaleout-manual-lb.md`, 실행 절차와 cleanup 기준은 `docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md`와 이 문서의 `Scale-Out PoC 정본` 절을 따른다.

### 1.1 실제 운영 MVP

ClueRoom의 실제 운영 MVP는 **저비용 Lightsail 3서버 역할 분리 구조**를 기준으로 한다.
초기 단일 서버 구조에서 출발했지만, 현재 운영 기준은 prod/app, data, ops를 분리한 external-data baseline이다.

```text
Android App
  ↓
https://api.clueroom.xyz
  ↓
Dynadot DNS
  ↓
AWS Lightsail prod server: clueroom-api-prod-01
  ├─ Nginx Reverse Proxy + HTTPS
  ├─ app-blue / app-green Blue-Green slots
  ├─ Prometheus / Grafana
  └─ Alloy log shipping
      ↓
AWS Lightsail data server: clueroom-data-01
  ├─ MySQL 8.4 source of truth
  ├─ Redis 8 source of truth
  ├─ local MySQL backup
  ├─ S3 DB backup upload
  └─ DATA_HEALTH / S3_BACKUP_HEALTH push
      ↓
AWS Lightsail ops server: clueroom-ops-01
  ├─ Loki
  ├─ n8n
  ├─ OPS_HEALTH push
  └─ Slack alert routing

AWS S3: scenario/image assets and private DB backup bucket
Firebase Cloud Messaging: push notification
```

### 1.2 MVP에서 반드시 포함할 것

```text
- Lightsail prod/data/ops 3서버 역할 분리
- Nginx Reverse Proxy
- HTTPS / Certbot / Let's Encrypt
- Docker Compose 기반 Spring Boot app-blue/app-green
- data server MySQL / Redis
- Dynadot DNS: api.clueroom.xyz
- AWS S3 이미지 저장소
- AWS S3 DB 백업 저장소
- Firebase Cloud Messaging 푸시 알림
- Secret 분리 구조
- deploy.sh Blue-Green 배포 스크립트
- GitHub Actions CI/CD
- data server MySQL 백업 / S3 업로드 / 복구 리허설
- 로그 / 장애 대응 Runbook
- Prometheus / Grafana 모니터링
- ops Loki / n8n / Slack alert routing
- Nginx rate limit / CN IPv4 block / manual blocklist
- Blue-Green 무중단 배포
- 인프라 의사결정 ADR
- 트래픽 증가 단계별 확장 계획과 scale-out PoC
```

### 1.3 MVP에서 실제 운영하지 않을 것

```text
- 운영용 멀티 인스턴스
- 운영용 Nginx Load Balancer 서버
- 운영용 RDS / ElastiCache / ALB / ASG
- 서버 측 Infra Codex 자동 운영
- Gemini 실패 시 Codex 실시간 fallback 자동화
```

위 항목은 현재 트래픽과 비용을 고려하면 과설계로 판단한다.
다만 학습/검증 목적의 PoC로는 별도 구성할 수 있다.
운영 전환이 아닌 PoC 계획은 이 문서의 `Scale-Out PoC 정본` 절에서 관리한다.
수동 Nginx load balancing과 Terraform app server scale-out은 단기 PoC로 검증하고, PoC 종료 후 리소스를 정리한다.

---

## 2. 왜 저비용 Lightsail 3서버로 운영하는가

### 2.1 현재 서비스 단계

ClueRoom은 현재 부트캠프 최종 프로젝트 MVP 단계다.

```text
- 실제 사용자는 팀원 / 튜터 / 소규모 피드백 사용자 중심
- 트래픽이 많지 않음
- 기능 구현과 시나리오 품질 검증이 더 중요함
- 운영 비용과 복잡도를 낮추는 것이 중요함
```

따라서 초기부터 관리형 로드밸런서, RDS, ElastiCache, ASG를 적용하는 것은 현재 규모 대비 과하다고 판단했다.
대신 단일 prod 서버에 모든 역할을 몰아넣는 구조는 벗어나, 장애 범위와 운영 책임을 prod/data/ops 3서버로 분리했다.

### 2.2 YAGNI 원칙 적용

YAGNI는 “You Aren't Gonna Need It”의 약자로, 아직 필요하지 않은 복잡한 기능이나 구조를 미리 만들지 않는다는 원칙이다.

현재 판단:

```text
현재 트래픽:
prod app 서버 1대의 Blue-Green 슬롯으로 충분

현재 핵심 리스크:
app 서버 수 부족보다 기능 완성도 / AI 응답 안정성 / 시나리오 품질 / 데이터 백업 안정성

현재 비용 전략:
관리형 고가용성보다 낮은 비용과 직접 운영 경험 우선
```

따라서 현재 운영은 Lightsail prod/data/ops 3서버를 기준으로 하고, 실제 사용자 수와 트래픽 지표가 증가할 때 app 서버 scale-out 또는 관리형 인프라로 단계적으로 확장한다.

### 2.3 현재 3서버 운영의 장점

```text
- 비용이 낮고 예측 가능함
- prod, data, ops의 책임이 분리됨
- app Blue-Green 배포와 data source of truth가 분리됨
- S3 DB 백업과 restore rehearsal로 data server 장애 리스크를 낮춤
- Loki/n8n/Slack 알림이 prod runtime과 분리됨
- 리눅스 / Docker / Nginx 운영 경험을 직접 쌓기 좋음
- 팀 프로젝트 기간 안에 안정적으로 운영 가능
```

### 2.4 현재 3서버 운영의 한계

```text
- prod app 서버는 여전히 1대라 app 서버 장애 시 API 영향이 큼
- data 서버 MySQL/Redis 장애는 전체 앱 기능에 직접 영향
- ops 서버 장애 시 중앙 로그/알림이 약해짐
- 관리형 DB/Redis/ALB가 아니므로 운영자가 직접 복구해야 함
```

이 한계는 확장 계획 문서와 PoC로 보완한다.

---

## 3. 왜 AWS Lightsail을 선택했는가

### 3.1 선택 이유

초기 MVP 운영 서버로 AWS Lightsail을 선택했다.

```text
- 90일 무료 플랜 활용 가능
- 월 비용이 정액형이라 예측하기 쉬움
- EC2보다 시작 구조가 단순함
- Static IP, 방화벽, 인스턴스 관리가 간단함
- Ubuntu 서버에 직접 접속해 Linux / Docker / Nginx를 학습하기 좋음
- AWS 생태계 안에서 S3 / IAM / 추후 확장과 연결하기 쉬움
```

### 3.2 기존 경험과의 차별점

이전 프로젝트에서는 다음과 같은 AWS 관리형 인프라를 경험했다.

```text
- EC2
- RDS
- S3
- ElastiCache
- MSK / Kafka
- ALB
- ASG
- CloudFront
- GitHub Actions CI/CD
```

ClueRoom에서는 같은 관리형 연결을 반복하기보다, 직접 서버에 접속해 다음을 구성하는 데 집중한다.

```text
- Nginx Reverse Proxy 직접 설정
- Docker Compose 직접 운영
- MySQL / Redis 컨테이너 직접 운영
- Certbot으로 HTTPS 직접 설정
- Terraform으로 S3 리소스 코드화
- 수동 배포 → deploy.sh → CI/CD 순서로 자동화
```

### 3.3 왜 EC2/ECS가 아닌가

#### EC2

EC2는 더 자유롭고 확장성이 높지만, 초기 MVP에서는 다음 설정이 다시 필요하다.

```text
VPC
Subnet
Security Group
IAM Role
EBS
ALB
ASG
NAT
Route 53
```

이 구조는 이전 과제에서 이미 경험한 영역이며, 이번 프로젝트의 주 목표인 “리눅스 서버 운영 이해”와는 조금 다르다.

#### ECS

ECS는 컨테이너 오케스트레이션에 적합하지만, 이번 프로젝트의 학습 목표인 “Nginx 수동 로드밸런싱과 서버 역할 분리”를 직접 보기에는 VM 기반 구조가 더 직관적이다.

따라서 현재는 Lightsail을 선택하고, 추후 필요 시 EC2 / ECS / ALB / ASG 구조로 전환할 수 있도록 문서화한다.

---

## 4. 현재 운영 인프라 구성

현재 구조 다이어그램은 아래 Mermaid 파일로 보존한다.

```text
docs/infra/diagrams/current-production-request-flow.mmd
docs/infra/diagrams/observability-alert-flow.mmd
docs/infra/diagrams/backup-restore-flow.mmd
docs/infra/diagrams/scaleout-manual-lb-poc.mmd
```

발표/포트폴리오용 이미지 자산은 아래 경로에 둔다.
이 이미지는 공식 로고 원본이 아니라 운영 구조 설명을 위한 로고 스타일 시각 자료다. scale-out 이미지는 PoC 전 계획 이미지이므로 최신 정본은 `POC-006-scaleout-manual-lb.md`와 `scaleout-manual-lb-poc.mmd`를 우선한다.

```text
docs/infra/images/clueroom_current_production_architecture_logo_style.png
docs/infra/images/clueroom_current_production_architecture_logo_style.svg
docs/infra/images/clueroom_scaleout_manual_lb_architecture_logo_style.png
docs/infra/images/clueroom_scaleout_manual_lb_architecture_logo_style.svg
```

### 4.1 DNS

```text
Domain Registrar: Dynadot
Domain: clueroom.xyz
API Domain: api.clueroom.xyz
DNS Record:
  Type: A
  Host: api
  Target: Lightsail Static IP
```

현재 루트 도메인 `clueroom.xyz`는 사용하지 않는다.
API 서버는 `api.clueroom.xyz`로 분리한다.

```text
clueroom.xyz
→ 추후 랜딩 페이지 / 웹 프론트 / 소개 페이지

api.clueroom.xyz
→ Spring Boot API 서버
```

### 4.2 서버

현재 운영 서버는 Lightsail 3대다.

| 서버 | 역할 | 핵심 구성 |
|---|---|---|
| `clueroom-api-prod-01` | 운영 API / ingress / app runtime | Nginx, app-blue, app-green, Prometheus, Grafana, Alloy |
| `clueroom-data-01` | 운영 데이터 source of truth | MySQL, Redis, local backup, S3 upload, DATA_HEALTH, S3_BACKUP_HEALTH |
| `clueroom-ops-01` | 중앙 로그 / 알림 / 운영 자동화 | Loki, n8n, OPS_HEALTH, Slack alert router |

공통 기준:

```text
AWS Lightsail
Ubuntu
SSH user: ubuntu
```

prod 서버 주요 디렉터리:

```text
/opt/clueroom/app
/opt/clueroom/secrets
/opt/clueroom/backups
/opt/clueroom/logs
```

data 서버 주요 디렉터리:

```text
/opt/clueroom-data
/opt/clueroom-data/backups/mysql
/opt/clueroom-data/secrets
```

ops 서버 주요 디렉터리:

```text
/opt/clueroom-ops
```

### 4.3 Nginx

Nginx는 서버 OS에 직접 설치한다.

역할:

```text
- 80/443 외부 요청 수신
- HTTP → HTTPS 리다이렉트
- TLS 인증서 처리
- 내부 Blue-Green active upstream으로 Reverse Proxy
- public health check 외 actuator / bot scan path edge 차단
```

개념:

```text
외부 사용자
  ↓ HTTPS 443
Nginx
  ↓ clueroom_backend upstream
app-blue  127.0.0.1:8081
app-green 127.0.0.1:8082
```

Spring Boot는 직접 HTTPS를 처리하지 않는다.
SSL termination은 Nginx에서 수행한다.
현재 active slot은 `/etc/nginx/conf.d/clueroom-upstream.conf`와 health check 응답의 `X-ClueRoom-Upstream` 헤더로 확인한다.

운영 hardening 원칙:

```text
- 외부에서 /actuator/health만 health check 용도로 허용
- /actuator/prometheus는 외부 공개하지 않음
- /actuator/env, /actuator/beans 등 민감 actuator endpoint는 Nginx에서 차단
- /.env, /.git, wp-admin, phpmyadmin 등 봇 스캔 경로는 Nginx에서 upstream 전 차단
- Rate Limit은 Nginx IP 기반 방어와 Redis 기반 user/session quota를 분리해 설계
```

### 4.4 Docker Compose

prod 서버는 Docker Compose로 app-blue/app-green만 운영 traffic에 연결한다.
MySQL/Redis는 prod 서버 local container가 아니라 data 서버가 source of truth다.

현재 운영 Blue-Green 구조는 external-data overlay를 포함한다.
`app-blue` / `app-green`은 local MySQL/Redis가 아니라 data 서버 `172.26.1.185`의 MySQL/Redis를 source of truth로 사용한다.
prod local MySQL/Redis는 운영 DB가 아니며 rollback/local-data copy 용도 또는 stop-only 정리 대상이다.

```text
Docker Compose + Blue-Green + external-data overlay
  ├─ app-blue  : 127.0.0.1:8081
  ├─ app-green : 127.0.0.1:8082
  ├─ external MySQL : 172.26.1.185
  ├─ external Redis : 172.26.1.185
  └─ local mysql/redis : not source of truth
```

기존 단일 app 컨테이너(`start-up-app`)는 legacy 경로로 보고 운영 traffic 대상에서 제외한다.
운영 traffic은 Blue-Green 슬롯인 `app-blue` 또는 `app-green` 중 Nginx active upstream으로 지정된 슬롯만 받는다.
배포 후 rollback 확인이 끝나면 standby 슬롯은 stop 상태로 둘 수 있지만, rollback 가능성을 위해 무분별하게 삭제하지 않는다.

Prometheus / Grafana는 actuator metric 확인용으로 구성했다.
Prometheus는 외부에 직접 공개하지 않고 Grafana datasource가 Docker 내부 URL(`http://prometheus:9090`)로 조회한다. Grafana는 팀원이 운영 메트릭을 함께 볼 수 있도록 Nginx HTTPS reverse proxy 뒤에서 `https://monitor.clueroom.xyz`로 공개한다.
Alert 정책은 외부 health와 active upstream을 우선하고, standby app-blue/app-green down은 오탐 가능성이 있으므로 단독 CRITICAL로 보지 않는다.

운영 원칙:

```text
- 3000/9090 포트는 Lightsail 방화벽에 직접 열지 않음
- docker-compose.yml의 Prometheus/Grafana host binding은 127.0.0.1 유지
- Grafana는 팀원별 Viewer 계정 발급
- Grafana admin 계정 공유 금지
- 서버 로그 접근은 인프라 담당자 중심으로 제한
- ops Loki/Alloy는 운영 snapshot과 log shipping 확인 대상이다.
- prod API 서버에 Loki/n8n 같은 추가 런타임을 한 번에 올리지 않는다.
```

2GB 서버에서 Prometheus / Grafana를 상시 운영할지는 메모리 사용량을 기준으로 판단한다.

### 4.5 데이터베이스

현재 운영 DB는 data 서버의 MySQL 8.4다.
prod app-blue/app-green은 아래 private endpoint를 바라본다.

```text
DB_HOST=172.26.1.185
DB_PORT=3306
```

운영 보안 원칙:

```text
- MySQL 포트는 public internet에 열지 않음
- prod app 서버에서만 private network로 접근
- source of truth 백업은 data 서버에서 수행
- prod local MySQL 백업은 운영 DB 백업으로 간주하지 않음
- S3 백업과 restore rehearsal을 운영 백업 신뢰 기준으로 둠
```

### 4.6 Redis

현재 운영 Redis는 data 서버의 Redis 8이다.
prod app-blue/app-green은 아래 private endpoint를 바라본다.

```text
REDIS_HOST=172.26.1.185
REDIS_PORT=6379
```

Redis는 AI rate limit, lock, cache, 임시 상태에 활용할 수 있다.
scale-out을 위해 Redis는 app local memory나 app local container에 묶지 않는다.

### 4.7 S3

이미지 저장소는 AWS S3를 사용한다.

선택 이유:

```text
- AWS 생태계와 관리 편의성
- 튜터 피드백 반영
- 초기 비용 차이가 크지 않음
- Spring Boot 연동 자료가 많음
- 추후 서버 확장 시 파일 저장소 공유 문제 해결
```

저장 대상:

```text
- 시나리오 썸네일
- 증거 이미지
- 용의자 이미지
- 현장 이미지
```

원칙:

```text
- 서버 로컬 디스크에 이미지 저장하지 않음
- DB에는 image_url 또는 object_key만 저장
- S3 Public Access Block 유지
- 필요 시 Presigned URL 방식 사용
```

S3 버킷은 Terraform으로 생성했다.

```text
Terraform managed:
- S3 bucket
- Public Access Block
- Versioning
- SSE-S3 encryption
- CORS
- Lifecycle rule: incomplete multipart upload 7일 abort
```

현재 이미지 asset bucket lifecycle는 이미지 object를 30일 뒤 삭제하는 정책이 아니다.
이미지 object 보관 기간은 서비스/콘텐츠 정책에 맞춰 별도 결정하며, DB 백업 retention과 섞어 설명하지 않는다.

### 4.8 FCM

Android 푸시 알림은 Firebase Cloud Messaging을 사용한다.

역할:

```text
Android App
  - FCM registration token 발급
  - 백엔드에 token 등록
  - 푸시 수신

Spring Boot Backend
  - 사용자별 device token 저장
  - Firebase Admin SDK 또는 HTTP v1 API로 푸시 발송

Firebase Cloud Messaging
  - Android 기기로 메시지 전달
```

초기 알림 후보:

```text
- 새 증거 해금 알림
- AI 시나리오 검증 완료 알림
- 리뷰 / 북마크 관련 알림
- 운영 공지
```

---

## 5. 보안 원칙

### 5.1 외부 공개 포트

Lightsail 방화벽에서 외부에 공개하는 포트:

```text
22  SSH
80  HTTP
443 HTTPS
```

외부 공개하지 않는 포트:

```text
8080 Spring Boot local/single app
8081 Spring Boot app-blue
8082 Spring Boot app-green
3306 MySQL
6379 Redis
```

Spring Boot는 Nginx 뒤에서만 접근한다.

SSH는 Fail2Ban `sshd` jail로 반복 실패 접속을 차단한다.
팀원별 제한 계정은 SSH key / username / host 입력을 여러 번 틀리면 일시 ban될 수 있으므로, 접속 실패가 반복되면 재시도하기 전에 인프라 담당자에게 public IP와 함께 확인을 요청한다.

### 5.2 Secret 관리

GitHub에 커밋하지 않는 값:

```text
.env
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
OPENAI_API_KEY
DB_PASSWORD
Firebase service account JSON
PEM 형식 SSH 인증키
Let's Encrypt privkey file
```

운영 runtime secret은 서버의 `.env`와 `/opt/clueroom/secrets/env.d`에 둔다.

```text
/opt/clueroom/app/.env
/opt/clueroom/secrets/env.d/ai.env
/opt/clueroom/secrets/env.d/portone.env
/opt/clueroom/secrets/env.d/oauth.env
/opt/clueroom/secrets/firebase-service-account.json
```

GitHub Actions Secrets에는 배포 접속에 필요한 값만 둔다.

```text
LIGHTSAIL_HOST
LIGHTSAIL_USER
LIGHTSAIL_SSH_KEY
```

AI/PortOne/OAuth/Firebase/DB secret은 GitHub Actions 로그에 노출될 이유가 없으므로 서버 secret으로 관리한다.

### 5.3 이미지 보안

S3는 Public Access Block을 유지한다.
이미지 조회는 public URL 직접 노출보다 Presigned URL 또는 백엔드 제어 방식으로 확장한다.

---

## 6. 배포 전략

### 6.1 현재 서버 배포

현재 운영 배포는 GitHub Actions CD를 기본 경로로 사용한다. CD workflow는 운영 서버에 SSH 접속한 뒤 `/opt/clueroom/deploy.sh`를 실행한다.

```bash
/opt/clueroom/deploy.sh
```

서버에서 직접 실행해야 하는 경우에도 동일한 스크립트를 사용한다.

```bash
ssh clueroom
/opt/clueroom/deploy.sh
/opt/clueroom/bg-status.sh
```

배포 스크립트는 현재 Nginx active upstream을 읽고 반대편 Blue/Green 컨테이너를 target으로 선택한다. target 컨테이너 health check가 성공한 뒤에만 Nginx upstream을 전환하고, 실패하면 기존 upstream으로 rollback한다.

### 6.2 deploy.sh

레포에는 서버 배치용 원본 스크립트를 둔다.

```text
scripts/deploy-bluegreen.sh
scripts/bg-compose.sh
scripts/bg-status.sh
scripts/stop-standby.sh
scripts/rollback-bluegreen.sh
scripts/backup-mysql.sh
docker-compose.bluegreen.yml
```

서버 배치 위치:

```text
/opt/clueroom/deploy.sh
/opt/clueroom/bg-compose
/opt/clueroom/bg-status.sh
/opt/clueroom/stop-standby.sh
/opt/clueroom/rollback-bluegreen.sh
/opt/clueroom/backup-mysql.sh
```

이전 active service는 rollback용으로 남긴다. 정상 확인 후에는 사람이 blue/green을 직접 판단해 중지하지 않고 `/opt/clueroom/stop-standby.sh`가 active가 아닌 slot만 자동 중지한다.

### 6.3 CI/CD

#### CI

```text
PR 또는 develop push
→ GitHub Actions
→ ./gradlew clean test
→ ./gradlew bootJar
```

CI는 서버를 건드리지 않으므로 먼저 적용할 수 있다.

#### CD

CD는 `workflow_dispatch` 수동 실행으로 시작한다.

```text
GitHub Actions
  ↓ SSH
Lightsail Server
  ↓
deploy.sh 실행
  ↓
bg-status.sh로 active/standby 확인
  ↓
정상 확인 후 stop-standby.sh 또는 문제 시 rollback-bluegreen.sh
```

CD workflow에는 서버 접속 secret만 둔다. runtime secret은 서버의 `.env`와 `/opt/clueroom/secrets`에서 읽는다. 이후 안정화되면 develop merge 시 자동 배포로 전환할 수 있다.

---

## 7. Blue-Green / Canary 전략

### 7.1 용어 구분

#### Blue-Green

```text
Blue = 현재 운영 버전
Green = 새 배포 버전

검증 후 트래픽을 Blue 100%에서 Green 100%로 전환
```

#### Canary

```text
새 버전에 트래픽을 일부만 보내며 점진적으로 확대

예:
90% Blue / 10% Green
70% Blue / 30% Green
0% Blue / 100% Green
```

### 7.2 ClueRoom에서의 적용 방식

MVP 운영은 prod/data/ops 3서버 baseline을 유지한다.
운영 배포 전환에는 prod 서버 내부의 app-blue/app-green Blue-Green을 사용한다.
이는 app 배포 rollback을 위한 구조이며, app 서버 자체의 고가용성이나 멀티 서버 load balancing은 아니다.

현재 Blue-Green 구조:

```text
prod server + external data server
  ├─ Nginx
  ├─ app-blue  : 8081
  ├─ app-green : 8082
  ├─ external MySQL : 172.26.1.185
  ├─ external Redis : 172.26.1.185
  └─ prod local MySQL/Redis : not source of truth
```

이 구조는 완전한 멀티 서버 고가용성은 아니지만, Nginx upstream 전환, external-data cutover, app deploy rollback 절차를 검증하기에는 충분하다.

전환 방식:

```text
active upstream 8081 -> target app-green/8082
active upstream 8082 -> target app-blue/8081
```

target health check가 성공한 뒤에만 Nginx upstream을 바꾸고, 이전 active service는 rollback용으로 유지한다.

운영 명령어는 helper script를 기준으로 한다.

```text
/opt/clueroom/bg-status.sh
→ 현재 active / standby 확인

/opt/clueroom/stop-standby.sh
→ active가 아닌 slot만 자동 중지

/opt/clueroom/rollback-bluegreen.sh
→ 이전 slot으로 rollback
```

### 7.3 인증샷 포인트

```text
- docker ps: app-blue / app-green 동시 실행
- Nginx upstream 설정
- Blue 응답 확인
- Green 응답 확인
- Nginx 전환 후 Green 100% 응답 확인
- Blue 중지 후에도 Green 정상 응답 확인
```

### 7.4 주의사항

Blue-Green은 같은 DB를 공유하므로 스키마 변경이 위험하다.

원칙:

```text
- 기존 버전과 신규 버전이 모두 동작 가능한 DB 변경부터 적용
- 컬럼 삭제 같은 파괴적 변경은 마지막 단계에서 수행
- 필요 시 expand and contract migration 전략 사용
```

---

## 8. 트래픽 증가에 따른 확장 계획

현재는 구체적인 사용자 수보다 “운영 징후”를 기준으로 확장한다.
아래 수치는 문서화를 위한 기준이며 실제 운영 지표에 따라 조정한다.

### Phase 1. MVP 운영

대상:

```text
팀원 / 튜터 / 소규모 피드백 사용자
DAU 0~100
```

구성:

```text
Lightsail prod/data/ops 3대
prod Nginx
prod Docker Compose app-blue/app-green
data MySQL / Redis
ops Loki / n8n
S3
FCM
```

확장하지 않는 이유:

```text
- 현재 트래픽이 낮음
- 비용 최소화
- app 서버 수평 확장보다 운영 안정성/관측/백업 우선
```

### Phase 2. 운영 안정화

대상:

```text
DAU 100~500
소규모 외부 테스트
```

완료 또는 고도화 대상:

```text
- deploy.sh
- GitHub Actions CI/CD
- data server MySQL 백업 스크립트
- S3 DB 백업 업로드
- restore rehearsal
- 로그 Runbook
- Prometheus / Grafana
- Loki / n8n / Slack alert routing
- S3 이미지 업로드 안정화
- FCM 발송 로그
- Redis rate limit / lock
```

### Phase 3. 단일 서버 Blue-Green

대상:

```text
배포 중단 시간을 줄이고 싶을 때
배포 검증 경험을 남기고 싶을 때
```

구성:

```text
app-blue
app-green
Nginx upstream
```

목적:

```text
- 무중단 배포 PoC
- 포트폴리오 인증샷
- Nginx upstream 학습
- 운영 배포 rollback 절차 검증
```

### Phase 4. DB / Redis 분리

대상:

```text
완료됨: external-data cutover
향후 app server scale-out의 선행 조건
```

현재 구조:

```text
prod app-blue/app-green
  ↓
data server MySQL / Redis
```

검증 결과:

```text
- app-blue/app-green의 DB_HOST/REDIS_HOST가 data server를 바라봄
- external-data compose override가 Blue-Green helper에 반영됨
- prod local MySQL/Redis는 source of truth가 아님
- data server 백업/S3 업로드/restore rehearsal까지 연결됨
```

### Phase 5. App 서버 수평 확장

대상:

```text
DAU 3,000~10,000 이상
API 응답 지연 증가
App 서버 CPU/메모리 병목
특정 이벤트/광고로 트래픽 급증
또는 포트폴리오/학습 목적의 단기 PoC
```

현재 상태:

```text
완료된 PoC / 운영 기준 구조 아님
2026-06-12 Terraform 기반 app node scale-out + 수동 Nginx load balancing 검증 성공.
local active 127.0.0.1:8081, app01 172.26.6.201:8080, app02 172.26.2.166:8080 equal mode upstream 확인.
60회 요청 분산 결과 21 / 19 / 20 확인.
PoC 종료 후 임시 app node 리소스와 scaleout key/port 리소스는 정리한다.
기본 운영 구조는 prod 1대 + data 1대 + ops 1대다.
```

구성:

```text
prod Nginx / prod control server
  ├─ local active Blue-Green slot
  ├─ app01
  └─ app02

data server MySQL/Redis
ops Loki/Grafana/n8n
S3 backup/storage
```

필수 조건:

```text
- App 서버 stateless
- 파일 저장 S3
- 세션 상태 JWT / DB / Redis
- Lock은 Redis 기반
- DB/Redis는 공유 인프라
```

### Phase 6. 관리형 인프라 전환

대상:

```text
지속적인 고트래픽
운영 안정성 요구 증가
장애 허용성 필요
팀 규모 확장
```

전환 후보:

```text
- ALB
- ASG
- RDS Multi-AZ
- ElastiCache
- CloudFront
- ECS
- CloudWatch 알림
```

단, ALB/RDS/ElastiCache/MSK 등은 기존 프로젝트에서 이미 경험한 관리형 인프라이므로, ClueRoom에서는 필요 시점에 전환하는 것으로 문서화한다.

---

## 9. 멀티 인스턴스 PoC 결과

### 9.1 목적

실제 운영 기준 구조 전환이 아니라, 수동 Nginx 로드밸런싱과 DB/Redis 분리 구조가 app node scale-out에 충분한지 검증하기 위한 학습/포트폴리오용 PoC다.

```text
운영 기준 구조 전환 ❌
학습/검증/인증샷 ⭕
2026-06-12 PoC 성공 ⭕
```

### 9.2 2026-06-12 실제 검증 구조

```text
api.clueroom.xyz
  ↓
prod Nginx / prod control server
  ├─ local active Blue-Green slot: 127.0.0.1:8081
  ├─ app01: 172.26.6.201:8080
  └─ app02: 172.26.2.166:8080
        ↓
data server MySQL/Redis
        ↓
ops Loki/Grafana/n8n
```

이 PoC는 별도 `poc-api` 도메인이나 별도 LB 서버를 만들지 않았다. Terraform으로 임시 app01/app02를 만들고, prod 서버에서 jar/env/secrets/scenarios를 동기화한 뒤 기존 prod Nginx upstream에 canary/equal 모드로 붙여 검증했다.

정본:

```text
docs/infra/poc/POC-006-scaleout-manual-lb.md
docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md
docs/infra/diagrams/scaleout-manual-lb-poc.mmd
```

### 9.3 검증 결과

```text
Terraform으로 app01/app02 생성
prod inventory로 app node 관리
prod에서 jar/env/secrets/scenarios 동기화
app01/app02 Spring Boot 기동
data 서버 MySQL/Redis 연결
app node Alloy -> ops Loki 수집
Nginx upstream에 local active + app01 + app02 추가
equal mode 60회 요청에서 21 / 19 / 20 분산 확인
```

### 9.4 검증한 upstream

```nginx
upstream clueroom_backend {
    server 127.0.0.1:8081 max_fails=3 fail_timeout=10s weight=1;
    server 172.26.6.201:8080 max_fails=3 fail_timeout=10s weight=1;
    server 172.26.2.166:8080 max_fails=3 fail_timeout=10s weight=1;
}
```

### 9.5 PoC 인증샷과 증거

```text
- Terraform app01/app02 생성 결과
- prod inventory app01.env/app02.env
- app01/app02 actuator health
- app01/app02 secret/scenario set 여부
- ops Loki docker-app 로그 조회
- Nginx upstream 설정
- api.clueroom.xyz health check의 X-ClueRoom-Upstream
- equal mode 60회 분산 결과 21 / 19 / 20
- rollback 후 local active upstream 복구 확인
```

### 9.6 PoC 종료

PoC는 성공으로 보되, 추가 app node를 기본 운영 기준 구조로 계속 운영하지 않는다.

```text
1. 인증샷 촬영
2. 설정 문서화
3. Nginx upstream을 local active only로 원복
4. app node runtime reset
5. Terraform destroy로 임시 app node, scaleout key pair, port resource 정리
```

---

## 10. 스크린샷 가이드

### 10.1 필수 스크린샷

```text
- Lightsail 서버 Running 상태
- 운영 서버 Static IP/public IP 연결 상태
- Lightsail 방화벽 22 / 80 / 443
- docker ps: app / mysql / redis
- Nginx 설정 파일
- Certbot 인증서 목록
- https://api.clueroom.xyz/swagger-ui/index.html
- https://api.clueroom.xyz/actuator/health
- Terraform state list / output
- S3 버킷 설정: Public Block / Versioning / Encryption / CORS / Lifecycle
```

### 10.2 후속 스크린샷

```text
- deploy.sh 성공 결과
- GitHub Actions CI 성공
- GitHub Actions CD 성공
- MySQL 백업 스크립트 실행 결과
- FCM 테스트 푸시 수신 화면
- Blue-Green PoC
- 멀티 인스턴스 PoC
```

### 10.3 스크린샷 금지 항목

```text
- .env 전체
- AWS_SECRET_ACCESS_KEY
- OPENAI_API_KEY
- Firebase service account JSON
- PEM 형식 SSH 인증키
- DB_PASSWORD
- Let's Encrypt privkey.pem
```

---

## 11. ADR 후보

### ADR-001. 초기 인프라를 Lightsail 저비용 구조로 선택한 이유

핵심:

```text
현재 MVP 트래픽과 비용을 고려해 Lightsail 기반 직접 운영 구조 선택
YAGNI 원칙 적용
prod/data/ops 역할 분리로 운영 안정성 보완
추후 확장 계획 문서화
```

### ADR-002. Nginx를 Reverse Proxy로 사용한 이유

핵심:

```text
Spring Boot 앞단에서 80/443 처리
HTTPS termination
추후 upstream / Blue-Green / Load Balancing 확장 가능
```

### ADR-003. DB/Redis를 data server로 분리한 이유

핵심:

```text
app 서버 scale-out의 선행 조건
data source of truth 명확화
prod app 배포와 데이터 저장소 책임 분리
RDS/ElastiCache 전환 전 저비용 운영 경험 확보
```

### ADR-004. 파일 저장소를 S3로 선택한 이유

핵심:

```text
서버 로컬 파일 저장 방지
멀티 인스턴스 확장 대비
AWS 생태계 관리 편의성
```

### ADR-005. Terraform은 S3부터 적용한 이유

핵심:

```text
이미 수동 생성한 Lightsail import 복잡도 회피
신규 리소스인 S3부터 IaC 적용
```

### ADR-006. prod 내부 Blue-Green을 고가용성 구조가 아닌 배포 안정화로 보는 이유

핵심:

```text
현재 운영 배포 전환에는 사용
prod 서버 장애 고가용성은 제공하지 않음
무중단 배포와 rollback 절차 검증 목적
```

### ADR-007. 멀티 인스턴스를 운영 적용하지 않고 확장 계획/PoC로 둔 이유

핵심:

```text
현재 규모 대비 과설계
비용과 운영 복잡도 증가
대신 확장 시나리오와 PoC 인증샷으로 설계 역량 보완
```

---

## 12. 발표용 요약

```text
ClueRoom은 초기 MVP 단계이지만 운영 안정성과 포트폴리오 검증을 위해 Lightsail 3서버 역할 분리 구조로 운영한다.

prod 서버는 Nginx Reverse Proxy로 HTTPS, rate limit, CN IPv4 block, manual blocklist, 내부 Blue-Green upstream(app-blue 8081 / app-green 8082) 프록시를 처리하고, api.clueroom.xyz 도메인으로 외부 접근을 제공한다.

data 서버는 MySQL/Redis source of truth를 담당하며, prod app은 external-data overlay로 data 서버를 바라본다. prod local MySQL/Redis는 운영 DB가 아니다.

ops 서버는 Loki/n8n/Slack alert routing을 담당한다. Gemini 분석은 보조이며, 기본 alert는 AI 분석 성공 여부와 무관하게 먼저 전송한다.

파일 저장은 서버 로컬이 아니라 S3로 분리했고, DB 백업도 data 서버 local backup + private S3 backup + restore rehearsal까지 검증했다.

아직 운영 기본값이 아닌 것은 서버 측 Infra Codex 자동 운영, Gemini 실패 시 Codex 실시간 fallback, Terraform 기반 수동 load balancing scale-out PoC다.
```

---

## 13. 통합 인프라 고도화 로드맵

이 절은 기존 `CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md`의 단계 계획을 흡수한 것이다.
운영 명령은 `OPS_RUNBOOK.md`, 보안/트래픽/알림 정책은 `SECURITY_TRAFFIC_ALERT_POLICY.md`를 따른다.

### Phase 0. Frontend E2E Connection Stability

목표:

```text
- Android가 실제 운영 API와 안정적으로 연결된다.
- 이미지 URL, active session 복구, timeline, evidence/suspect detail이 더미 없이 동작한다.
- PR/문서상 미구현 API를 프론트가 호출하지 않는다.
```

### Phase 1. Security And Traffic Defense

목표:

```text
- public ingress는 Nginx로 제한한다.
- secret은 팀원별 제한 계정/ACL로 관리한다.
- rate limit은 dry-run 검증 후 enforce 상태로 운영한다.
- CN IPv4 block과 manual blocklist는 운영 Nginx edge에서 관리한다.
- 새로운 block/rate-limit 변경은 증거와 rollback 기준 없이 적용하지 않는다.
```

정본:

```text
docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md
docs/infra/OPS_RUNBOOK.md
```

### Phase 2. Monitoring / Alert

목표:

```text
- external health, active upstream, 5xx, latency, JVM metric 중심으로 본다.
- standby Blue-Green target down은 단독 CRITICAL로 보지 않는다.
- DATA_HEALTH, S3_BACKUP_HEALTH, OPS_HEALTH, SERVER_HEALTH를 Loki/Grafana에서 본다.
- Grafana alert는 n8n을 통해 Slack으로 보내고, Gemini 분석은 보조로만 사용한다.
```

### Phase 3. LLMOps

목표:

```text
- AI_CALL 로그와 Prometheus metric으로 provider, model, latency, token, fallback을 본다.
- AI_CALL_CONTEXT 로그로 prompt block estimate와 templateHash를 확인한다.
- prompt/answer/user question 원문은 운영 로그에 저장하지 않는다.
- Loki/Prometheus 기반 smoke를 먼저 사용하고, DB persistence는 필요 시 flag로 켠다.
```

### Phase 4. Agent Introduction

목표:

```text
- agent는 read-only diagnosis부터 시작한다.
- 운영 변경은 승인 정책과 rollback을 먼저 둔다.
- snapshot에는 secret과 raw env를 넣지 않는다.
- 서버 측 Infra Codex 자동 운영과 Gemini 실패 시 Codex 실시간 fallback은 아직 운영 기본값이 아니다.
```

정본:

```text
docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md
docs/infra/agent/LLMOPS_OPERATING_GUIDE.md
```

### Phase 5. Backup / Restore Hardening

목표:

```text
- data server backup script와 restore rehearsal 절차를 운영 문서화한다.
- S3 backup storage는 private bucket, encryption, lifecycle, least-privilege IAM을 기준으로 운영한다.
- 운영 restore는 rehearsal 성공 후에만 진행한다.
```

### Phase 6. Scale-Out PoC / Server-Level Blue-Green

목표:

```text
- 2026-06-12 PoC로 검증 완료. 현재 운영 기준 구조로 전환하지 않는다.
- app은 stateless여야 한다.
- DB/Redis는 shared external data source여야 한다.
- S3를 파일 저장소로 사용한다.
- app server 2대 이상일 때 lock/session/cache는 local memory에 두지 않는다.
- 검증된 방식은 Terraform app node 생성 + prod 제어형 sync/start/check + 수동 Nginx upstream apply/rollback이다.
- PoC 종료 후 임시 app node 리소스와 scaleout key/port 리소스를 정리한다.
```

### Phase 7. Registry / Commit SHA / DB Migration

목표:

```text
- image tag 또는 commit SHA 기반 배포 추적을 강화한다.
- DB migration은 expand-and-contract 원칙을 따른다.
- 파괴적 migration은 마지막 단계에서만 수행한다.
```

### Do Not Do

```text
- prod app 서버에 n8n, Loki, workers를 다시 올리지 않는다.
- standby Blue-Green target down을 바로 장애로 단정하지 않는다.
- 기존 rate limit/CN block을 근거 없이 변경하지 않는다.
- data server MySQL/Redis와 S3 백업 상태를 확인하지 않고 app 서버만 무작정 늘리지 않는다.
- private seed, DB dump, secret snapshot을 public 문서나 PR에 올리지 않는다.
```

### PoC Result Summary

이 표는 외부 infra AI 문서의 PoC 결과를 정본 문서로 흡수한 것이다.

| PoC | 상태 | 정본 반영 위치 |
|---|---|---|
| POC-001 external-data cutover | 완료 | prod app-blue/app-green이 data server MySQL/Redis를 바라보며, prod local MySQL/Redis는 source of truth가 아님 |
| POC-002 S3 backup / restore rehearsal | 완료 | data server local backup, S3 upload, sha256 sidecar, S3_BACKUP_HEALTH, 임시 MySQL restore rehearsal |
| POC-003 observability / alert pipeline | 완료 | prod Alloy, ops Loki, Grafana dashboard/alert, n8n Slack routing, DATA_HEALTH/SERVER_HEALTH/OPS_HEALTH |
| POC-004 Nginx rate-limit / IP block | 완료 | API per-IP rate limit enforced, dry-run off, CN IPv4 block, manual blocklist, 403/429 warning alert |
| POC-005 LLMOps observability | 완료 | AI_CALL, token/latency/fallback visibility, AI_CALL_CONTEXT prompt block estimate/templateHash, raw prompt/answer/user question 미저장 |
| POC-006 Terraform app node scale-out + 수동 Nginx LB | 완료 / 운영 기준 구조 아님 | app01/app02 생성, prod sync/start/check, app node Alloy 로그 수집, Nginx equal mode 21/19/20 분산 확인. 정본: [poc/POC-006-scaleout-manual-lb.md](poc/POC-006-scaleout-manual-lb.md), [runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md](runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md) |

---

## 14. Scale-Out PoC 정본

이 절은 기존 `SCALE_OUT_POC_PLAN.md`의 핵심 내용을 흡수한 것이다.
PoC는 운영 적용이 아니라 학습/검증/인증샷 목적이다.
2026-06-12 기준 POC-006은 성공으로 판정했다.
상세 결과는 `docs/infra/poc/POC-006-scaleout-manual-lb.md`, 재실행/rollback 절차는 `docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md`를 따른다.

### 2026-06-12 POC-006 결과

```text
Terraform으로 app01/app02 생성
prod inventory로 app node 관리
prod에서 jar/env/secrets/scenarios 동기화
app01/app02 Spring Boot 기동
data 서버 MySQL/Redis 연결
app node Alloy -> ops Loki 수집
Nginx upstream에 local active + app01 + app02 추가
equal mode 60회 요청에서 21 / 19 / 20 분산 확인
```

이 결과는 app layer scale-out 검증이다.
DB/Redis HA, managed load balancer, managed autoscaling 완료를 의미하지 않는다.

### PoC 규칙

```text
- 운영 기준 구조 전환과 PoC 검증을 구분한다.
- prod Nginx upstream에 붙일 때는 health, config backup, rollback 절차를 먼저 확보한다.
- local active upstream은 반드시 127.0.0.1:8081 또는 127.0.0.1:8082여야 한다.
- 127.0.0.1:80 또는 bare 127.0.0.1 upstream이 보이면 즉시 rollback한다.
- PoC 종료 후 임시 app node 리소스와 scaleout key/port 리소스를 정리한다.
- secret 값, Firebase JSON, DB password, JWT 값, API key는 공개 문서와 git에 넣지 않는다.
```

### 즉시 적용 가능한 LLMOps 경로

Scale-out 전에 LLMOps는 별도 DB migration보다 구조화 로그와 Prometheus/Loki smoke를 먼저 사용한다.

```text
AI call -> app log AI_CALL -> Alloy/Loki -> query/smoke
AI call context -> app log AI_CALL_CONTEXT -> Alloy/Loki -> privacy/query smoke
```

DB persistence가 필요하면 feature flag로 켜고, 운영 DB 부하와 privacy를 먼저 검토한다.

### 애플리케이션 필수 조건

Scale-out PoC 전에 애플리케이션은 아래 조건을 만족해야 한다.

```text
- app instance가 stateless
- file/image storage가 S3
- DB/Redis가 app local container에 묶이지 않음
- session state가 DB/Redis/JWT 등 공유 가능한 저장소 기준
- lock이 local memory가 아닌 Redis/DB 등 공유 저장소 기준
- actuator health가 instance별로 확인 가능
```

### PoC 1. 두 서버 역할 분리

목적:

```text
app server와 data server 분리를 검증한다.
```

구조:

```text
app server
  - Nginx
  - app-blue/app-green

data server
  - MySQL
  - Redis
```

현재 external-data cutover 구조가 이 PoC의 운영 기준 구조에 가깝다.

### PoC 2. 수동 Nginx Load Balancing

목적:

```text
Nginx upstream으로 app-01/app-02를 수동 분산하고 한쪽 중지 시 다른 쪽이 응답하는지 확인한다.
```

구조:

```text
prod Nginx / prod control server
  -> local active Blue-Green slot
  -> app01
  -> app02
  -> shared data server MySQL/Redis
```

2026-06-12 실제 equal mode upstream:

```nginx
upstream clueroom_backend {
    server 127.0.0.1:8081 max_fails=3 fail_timeout=10s weight=1;
    server 172.26.6.201:8080 max_fails=3 fail_timeout=10s weight=1;
    server 172.26.2.166:8080 max_fails=3 fail_timeout=10s weight=1;
}
```

검증:

```bash
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 60 scaleout
```

2026-06-12 결과:

```text
21 / 19 / 20
```

Rollback 확인:

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
```

### 향후 후보. 서버 단위 Blue-Green

이 항목은 2026-06-12 POC-006의 검증 범위가 아니다. 이번 PoC에서 검증한 것은 prod Nginx에 local active slot과 임시 app01/app02를 수동 upstream으로 붙이는 방식이다.

목적:

```text
서버 단위 blue/green 전환을 검증한다.
```

구조:

```text
blue app group
green app group
shared data server
Nginx upstream switch
```

Nginx active upstream 예시:

```nginx
upstream clueroom_backend {
    server <app-blue-private-ip>:8080;
}
```

전환 대상 예시:

```nginx
upstream clueroom_backend {
    server <app-green-private-ip>:8080;
}
```

절차:

```text
1. 현재 active: app-blue-01
2. 새 app 버전을 app-green-01에 배포
3. private IP로 green health 확인
4. Nginx upstream을 green으로 전환
5. nginx -t
6. Nginx reload
7. 외부 health check
8. rollback window 동안 blue 유지
```

Rollback:

```text
Nginx upstream을 app-blue-01로 되돌리고 reload
```

### 공유 데이터와 상태

공유되어야 하는 것:

```text
MySQL
Redis
S3
secret 배포 정책
AI provider 설정
monitoring/logging 경로
```

공유하면 안 되는 것:

```text
container local filesystem upload
local in-memory lock
local-only session state
수동 DB dump copy를 live sync처럼 사용하는 방식
```

### 검증 체크리스트

```text
- app-01/app-02 health가 각각 200인지
- 한 app을 중지해도 LB health가 유지되는지
- DB write 후 다른 app에서 read 가능한지
- Redis lock/cache가 공유되는지
- S3 image URL이 양쪽 app에서 동일하게 동작하는지
- AI call log와 fallback metric이 instance label로 구분되는지
- rollback route가 있는지
```

---

## 15. Backup / Restore Strategy

이 절은 기존 `MYSQL_BACKUP_AND_RESTORE_POLICY.md`의 정책 내용을 흡수한 것이다.
실행 명령은 `OPS_RUNBOOK.md`의 MySQL 백업/복구 장을 따른다.

### Current Baseline

```text
- 운영 DB는 MySQL이다.
- external data server가 source of truth다.
- data server backup script가 source of truth MySQL을 local .sql.gz로 백업한다.
- data server upload script가 .sql.gz와 .sha256 sidecar를 private S3 backup bucket에 업로드한다.
- S3_BACKUP_HEALTH가 Loki/Grafana alert로 관측된다.
- restore rehearsal은 S3에서 내려받은 백업을 임시 MySQL 컨테이너에 복구해 검증한다.
- prod local MySQL은 운영 DB가 아니며 rollback/local-data copy 용도 또는 stop-only 정리 대상이다.
- prod `/opt/clueroom/backup-mysql.sh`는 운영 source of truth 백업으로 간주하지 않는다.
```

즉 external-data cutover 이후 prod 서버의 `backup-mysql.sh` 결과만으로는 운영 DB 백업 완료로 보지 않는다.
운영 source of truth 백업 기준은 data 서버의 `/opt/clueroom-data/backup-mysql.sh`, `/opt/clueroom-data/upload-mysql-backup-s3.sh`, `/opt/clueroom-data/s3-backup-health-push.sh`다.

현재 data 서버 cron 기준:

```text
10 3 * * * /opt/clueroom-data/backup-mysql.sh
20 3 * * * /opt/clueroom-data/upload-mysql-backup-s3.sh
* * * * * /opt/clueroom-data/data-health-push.sh
*/5 * * * * /opt/clueroom-data/s3-backup-health-push.sh
```

### S3 Backup Principles

S3 백업 저장소는 아래 원칙을 따른다.

```text
- private bucket
- public access block
- encryption enabled
- least-privilege IAM
- lifecycle/retention policy
- checksum or object metadata for integrity check
- restore rehearsal before production restore
```

S3 object layout 후보:

```text
s3://clueroom-prod-db-backups-apne2-<random_suffix>/mysql/prod/daily/YYYY/MM/DD/startup_YYYYMMDD_HHMMSS.sql.gz
s3://clueroom-prod-db-backups-apne2-<random_suffix>/mysql/prod/daily/YYYY/MM/DD/startup_YYYYMMDD_HHMMSS.sql.gz.sha256
```

Do not use:

```text
s3://clueroom-assets-pudding-20260520/official/...
```

The `official/` prefix is for public scenario/image runtime assets, not database backups.

### Backup Upload IAM

전용 IAM principal을 사용한다.

```text
clueroom-prod-db-backup-uploader
```

Minimum permissions:

```text
s3:PutObject
s3:GetObject
s3:ListBucket on backup bucket/prefix
s3:AbortMultipartUpload
```

Terraform으로 `aws_iam_access_key`를 만들지 않는다.
access key secret은 Terraform state에 남을 수 있으므로 AWS Console에서 수동 생성하고 data server에만 저장한다.

```text
/opt/clueroom-data/secrets/aws-backup.env
```

기대 env key:

```text
AWS_ACCESS_KEY_ID: server secret file에 설정
AWS_SECRET_ACCESS_KEY: server secret file에 설정
AWS_DEFAULT_REGION: ap-northeast-2
S3_BACKUP_BUCKET: clueroom-prod-db-backups-apne2-<random_suffix>
S3_BACKUP_PREFIX: mysql/prod
```

Required file mode:

```bash
chmod 600 /opt/clueroom-data/secrets/aws-backup.env
```

### Backup Script Enhancement Candidate

external-data source of truth 백업 스크립트의 처리 순서는 아래를 기준으로 한다.

```text
1. data server MySQL 대상 dump 생성
2. gzip 압축
3. pipeline failure를 감지한다 (`set -o pipefail` 또는 명시적 dump status 검증)
4. gzip 파일이 non-empty이고 `gzip -t`를 통과하는지 확인
5. sha256sum 생성
6. private S3 backup bucket/prefix에 .sql.gz 업로드
7. checksum sidecar 업로드
8. S3 object가 존재하고 size가 0이 아닌지 확인
9. local retention 유지
10. S3 lifecycle로 remote retention 관리
```

Candidate S3 upload command:

```bash
BACKUP_DIR="$(dirname "$BACKUP_FILE")"
BACKUP_BASE="$(basename "$BACKUP_FILE")"
DATE_PATH="$(date +%Y/%m/%d)"

test -s "$BACKUP_FILE"
gzip -t "$BACKUP_FILE"
(cd "$BACKUP_DIR" && sha256sum "$BACKUP_BASE" > "$BACKUP_BASE.sha256")

aws s3 cp "$BACKUP_FILE" "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/${DATE_PATH}/${BACKUP_BASE}" \
  --only-show-errors \
  --server-side-encryption AES256

aws s3 cp "$BACKUP_FILE.sha256" "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/${DATE_PATH}/${BACKUP_BASE}.sha256" \
  --only-show-errors \
  --server-side-encryption AES256
```

### DB Backup Retention Candidate

```text
local: 7 days
S3 daily backup: 30 days
S3 weekly backup: 12 weeks
S3 monthly backup: 12 months
```

이 retention 후보는 DB backup object 전용이다.
이미지 asset bucket retention은 이 표의 대상이 아니며, 현재 Terraform lifecycle은 incomplete multipart upload 정리 용도다.
최종 retention은 비용과 개인정보 보존 정책을 함께 보고 조정한다.

### Restore Guardrails

```text
- 운영 restore 전 현재 DB를 다시 백업한다.
- 임시 MySQL 컨테이너 또는 rehearsal host에서 import를 먼저 검증한다.
- secret 값을 로그/문서에 남기지 않는다.
- production restore는 담당자 승인 후 수행한다.
- restore 후 actuator health, 핵심 API, smoke를 확인한다.
```

### Terraform Provisioning

S3 backup bucket과 IAM은 Terraform으로 관리하는 방향을 우선한다.
단, Terraform state에는 secret 값을 넣지 않는다.
