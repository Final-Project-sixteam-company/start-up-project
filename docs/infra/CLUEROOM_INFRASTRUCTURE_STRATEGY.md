# ClueRoom Infrastructure Strategy

> 문서 목적: ClueRoom 프로젝트의 인프라 구성, 선택 이유, 운영 범위, 확장 계획, PoC 계획을 별도 정본으로 관리한다.
> 기존 기획명 `CaseLab AI`는 레거시 명칭이며, 현재 서비스/도메인 기준 이름은 `ClueRoom`이다.
> MVP 이후 상세 고도화 순서는 `docs/infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md`를 기준으로 한다.
> Monitoring Agent, Infra Codex Agent, LLMOps Agent 운영 기준은 `docs/infra/agent/` 아래 문서를 기준으로 한다.

---

## 1. 현재 확정된 인프라 목표

### 1.0 고도화 문서 기준

현재 문서는 인프라 선택 이유와 운영 구조의 정본이다.
MVP 이후 세부 고도화 순서와 Agent/LLMOps 계획은 아래 문서로 분리한다.

```text
docs/infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md
docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md
docs/infra/agent/OPS_SNAPSHOT_SPEC.md
docs/infra/agent/MONITORING_AGENT_PLAN.md
docs/infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md
docs/infra/agent/LLMOPS_AGENT_PLAN.md
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

Rate Limit의 상세 정책과 적용 순서는 `docs/infra/RATE_LIMIT_POLICY.md`를 따른다.

### 1.1 실제 운영 MVP

ClueRoom의 실제 운영 MVP는 **단일 Lightsail 서버 기반**으로 유지한다.

```text
Android App
  ↓
https://api.clueroom.xyz
  ↓
Dynadot DNS
  ↓
AWS Lightsail 단일 서버
  ↓
Nginx Reverse Proxy + HTTPS
  ↓
Docker Compose
      ├─ Spring Boot App
      ├─ MySQL
      └─ Redis
  ↓
AWS S3
  ↓
Firebase Cloud Messaging
```

### 1.2 MVP에서 반드시 포함할 것

```text
- Lightsail 단일 서버
- Nginx Reverse Proxy
- HTTPS / Certbot / Let's Encrypt
- Docker Compose 기반 Spring Boot / MySQL / Redis
- Dynadot DNS: api.clueroom.xyz
- AWS S3 이미지 저장소
- Firebase Cloud Messaging 푸시 알림
- Secret 분리 구조
- deploy.sh Blue-Green 배포 스크립트
- GitHub Actions CI/CD
- MySQL 백업 스크립트
- 로그 / 장애 대응 Runbook
- Prometheus / Grafana 모니터링
- Blue-Green 무중단 배포 PoC
- 인프라 의사결정 ADR
- 트래픽 증가 단계별 확장 계획
```

### 1.3 MVP에서 실제 운영하지 않을 것

```text
- 운영용 멀티 인스턴스
- 운영용 DB 서버 분리
- 운영용 Redis 서버 분리
- 운영용 Nginx Load Balancer 서버
- 운영용 RDS / ElastiCache / ALB / ASG
```

위 항목은 현재 트래픽과 비용을 고려하면 과설계로 판단한다.
다만 학습/검증 목적의 PoC로는 별도 구성할 수 있다.

---

## 2. 왜 단일 인스턴스로 운영하는가

### 2.1 현재 서비스 단계

ClueRoom은 현재 부트캠프 최종 프로젝트 MVP 단계다.

```text
- 실제 사용자는 팀원 / 튜터 / 소규모 피드백 사용자 중심
- 트래픽이 많지 않음
- 기능 구현과 시나리오 품질 검증이 더 중요함
- 운영 비용과 복잡도를 낮추는 것이 중요함
```

따라서 초기부터 멀티 인스턴스, DB 분리, Redis 분리, 관리형 로드밸런서를 적용하는 것은 현재 규모 대비 과하다고 판단했다.

### 2.2 YAGNI 원칙 적용

YAGNI는 “You Aren't Gonna Need It”의 약자로, 아직 필요하지 않은 복잡한 기능이나 구조를 미리 만들지 않는다는 원칙이다.

현재 판단:

```text
현재 트래픽:
단일 서버로 충분

현재 핵심 리스크:
서버 수 부족보다 기능 완성도 / AI 응답 안정성 / 시나리오 품질

현재 비용 전략:
초기 스타트업처럼 낮은 비용과 단순 운영 우선
```

따라서 현재 운영은 단일 서버로 시작하고, 실제 사용자 수와 트래픽 지표가 증가할 때 단계적으로 확장한다.

### 2.3 단일 서버 운영의 장점

```text
- 비용이 낮고 예측 가능함
- 배포 구조가 단순함
- 장애 원인 파악이 쉬움
- 리눅스 / Docker / Nginx 운영 경험을 직접 쌓기 좋음
- 팀 프로젝트 기간 안에 안정적으로 운영 가능
```

### 2.4 단일 서버 운영의 한계

```text
- 서버 1대 장애 시 전체 서비스 중단
- App / DB / Redis가 같은 서버 자원을 공유
- 트래픽 증가 시 병목 발생 가능
- MySQL / Redis 장애가 곧 전체 장애로 이어질 수 있음
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

```text
AWS Lightsail
Ubuntu
Static IP
SSH user: ubuntu
```

서버 내부 주요 디렉터리:

```text
/opt/clueroom/app
/opt/clueroom/secrets
/opt/clueroom/backups
/opt/clueroom/logs
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

단일 서버에서 Docker Compose로 실행한다.

```text
Spring Boot App
MySQL
Redis
```

현재 구조:

```text
Docker Compose
  ├─ app
  ├─ mysql
  └─ redis
```

Blue-Green PoC 구조에서는 같은 MySQL/Redis를 공유하고 App 컨테이너만 분리한다.

```text
Docker Compose + Blue-Green overlay
  ├─ app-blue  : 127.0.0.1:8081
  ├─ app-green : 127.0.0.1:8082
  ├─ mysql
  └─ redis
```

기존 단일 app 컨테이너(`start-up-app`)는 legacy 경로로 보고 운영 traffic 대상에서 제외한다.
운영 traffic은 Blue-Green 슬롯인 `app-blue` 또는 `app-green` 중 Nginx active upstream으로 지정된 슬롯만 받는다.
배포 후 rollback 확인이 끝나면 standby 슬롯은 stop 상태로 둘 수 있지만, rollback 가능성을 위해 무분별하게 삭제하지 않는다.

Prometheus / Grafana는 actuator metric 확인용으로 구성했다.
Prometheus는 외부에 직접 공개하지 않고 Grafana datasource가 Docker 내부 URL(`http://prometheus:9090`)로 조회한다. Grafana는 팀원이 운영 메트릭을 함께 볼 수 있도록 Nginx HTTPS reverse proxy 뒤에서 `https://monitor.clueroom.xyz`로 공개한다.

운영 원칙:

```text
- 3000/9090 포트는 Lightsail 방화벽에 직접 열지 않음
- docker-compose.yml의 Prometheus/Grafana host binding은 127.0.0.1 유지
- Grafana는 팀원별 Viewer 계정 발급
- Grafana admin 계정 공유 금지
- 서버 로그 접근은 인프라 담당자 중심으로 제한
- 로그 공유가 필요해지면 Loki/Promtail 도입
```

2GB 서버에서 Prometheus / Grafana를 상시 운영할지는 메모리 사용량을 기준으로 판단한다.

### 4.5 데이터베이스

MVP에서는 MySQL을 Docker 컨테이너로 운영한다.

```text
MySQL Docker
Named Volume 사용
서버 외부에 3306 공개하지 않음
```

운영 보안 원칙:

```text
- MySQL 포트는 외부 방화벽에서 열지 않음
- App 컨테이너와 내부 네트워크로 연결
- 백업 스크립트 필수
```

### 4.6 Redis

MVP에서는 Redis를 Docker 컨테이너로 운영한다.

```text
Redis Docker
AI rate limit / lock / cache / 임시 상태에 활용 가능
서버 외부에 6379 공개하지 않음
```

멀티 인스턴스 확장 시 Redis는 반드시 분리해야 한다.

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
- Lifecycle rule
```

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
pem private key
Let's Encrypt private key
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

### 7.2 ClueRoom에서의 적용 계획

MVP 운영은 단일 Lightsail 서버를 유지한다. 운영 배포 전환에는 단일 서버 Blue-Green을 사용하지만, 실제 멀티 서버 고가용성은 아니며 같은 서버 안에서 무중단 전환과 rollback 절차를 검증하는 PoC 성격으로 본다.

단일 서버 PoC 구조:

```text
Lightsail 단일 서버
  ├─ Nginx
  ├─ app-blue  : 8081
  ├─ app-green : 8082
  ├─ MySQL
  └─ Redis
```

이 구조는 진짜 멀티 서버 고가용성은 아니지만, Nginx upstream 전환과 무중단 배포 개념을 검증하기에는 충분하다.

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
Lightsail 1대
Nginx
Docker Compose
App / MySQL / Redis
S3
FCM
```

확장하지 않는 이유:

```text
- 현재 트래픽이 낮음
- 비용 최소화
- 운영 단순성 우선
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
- MySQL 백업 스크립트
- 로그 Runbook
- Prometheus / Grafana
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
DAU 1,000~3,000 이상
DB 쿼리 병목
Redis 사용량 증가
App 서버 CPU/메모리와 DB 자원 경합 발생
```

확장 구조:

```text
App Server
  ↓
DB Server 또는 Managed DB
Redis Server 또는 Managed Redis
S3
```

선택지:

```text
PoC:
- 별도 Lightsail 서버에 MySQL Docker
- 별도 Lightsail 서버에 Redis Docker

실제 운영:
- RDS
- ElastiCache
```

### Phase 5. App 서버 수평 확장

대상:

```text
DAU 3,000~10,000 이상
API 응답 지연 증가
App 서버 CPU/메모리 병목
특정 이벤트/광고로 트래픽 급증
```

구성:

```text
Nginx Load Balancer Server
  ├─ app-server-01
  └─ app-server-02

Shared DB
Shared Redis
S3
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

## 9. 멀티 인스턴스 PoC 계획

### 9.1 목적

실제 운영 적용이 아니라, 수동 Nginx 로드밸런싱과 DB/Redis 분리 구조를 검증하기 위한 학습/포트폴리오용 PoC다.

```text
운영 적용 ❌
학습/검증/인증샷 ⭕
```

### 9.2 권장 PoC 구조

```text
poc-api.clueroom.xyz
  ↓
clueroom-poc-lb-01
  ↓
┌──────────────────────┐
↓                      ↓
clueroom-poc-app-01   clueroom-poc-app-02
  ↓                      ↓
  └──────────┬───────────┘
             ↓
      clueroom-poc-db-01
             ↓
      clueroom-poc-redis-01

S3는 동일하게 사용
```

서버 역할:

```text
clueroom-poc-lb-01
- Nginx Load Balancer
- 외부 80/443 공개

clueroom-poc-app-01
- Spring Boot App
- DB/Redis 없음

clueroom-poc-app-02
- Spring Boot App
- DB/Redis 없음

clueroom-poc-db-01
- MySQL Docker

clueroom-poc-redis-01
- Redis Docker
```

### 9.3 왜 5대인가

직접 Nginx 로드밸런싱을 하려면 LB 역할 서버가 필요하다.

```text
필수:
- LB 서버 1대
- App 서버 2대
- DB 서버 1대
- Redis 서버 1대
```

따라서 가장 깔끔한 PoC는 5대다.

### 9.4 4대 타협안

비용이나 시간이 부족하면 아래처럼 구성할 수 있다.

```text
server-01: Nginx LB + app-01
server-02: app-02
server-03: MySQL
server-04: Redis
```

단점:

```text
- LB와 app-01이 같은 서버라 역할 분리가 애매함
- app-01과 app-02가 대칭 구조가 아님
- server-01 장애 시 LB와 app-01이 같이 죽음
```

따라서 문서에는 PoC 비용 절감용 타협안이라고 명시한다.

### 9.5 PoC 인증샷

```text
- Lightsail 인스턴스 5대 목록
- 각 서버 역할 태그
- Nginx upstream 설정
- app-server-01 / app-server-02 docker ps
- db-server MySQL docker ps
- redis-server Redis docker ps
- poc-api.clueroom.xyz health check
- app-01 중지 후 app-02로 계속 응답 확인
```

### 9.6 PoC 종료

PoC는 계속 운영하지 않는다.

```text
1. 인증샷 촬영
2. 설정 문서화
3. 서버 삭제
4. Static IP 미사용 상태 방치 금지
```

---

## 10. 스크린샷 가이드

### 10.1 필수 스크린샷

```text
- Lightsail 서버 Running 상태
- Static IP 연결
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
- pem private key
- DB_PASSWORD
- Let's Encrypt privkey.pem
```

---

## 11. ADR 후보

### ADR-001. 초기 인프라를 Lightsail 단일 서버로 선택한 이유

핵심:

```text
현재 MVP 트래픽과 비용을 고려해 단일 서버 구조 선택
YAGNI 원칙 적용
추후 확장 계획 문서화
```

### ADR-002. Nginx를 Reverse Proxy로 사용한 이유

핵심:

```text
Spring Boot 앞단에서 80/443 처리
HTTPS termination
추후 upstream / Blue-Green / Load Balancing 확장 가능
```

### ADR-003. 초기 DB/Redis를 Docker Compose로 운영한 이유

핵심:

```text
비용 절감
운영 단순성
초기 MVP에서는 충분
추후 트래픽 증가 시 분리
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

### ADR-006. 단일 서버 Blue-Green을 고가용성 구조가 아닌 배포 PoC로 보는 이유

핵심:

```text
현재 운영 배포 전환에는 사용
단일 서버이므로 서버 장애 고가용성은 제공하지 않음
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
ClueRoom은 초기 MVP 단계이므로 단일 Lightsail 서버에 Docker Compose 기반으로 Spring Boot, MySQL, Redis를 배포했다.

Nginx를 Reverse Proxy로 사용해 HTTPS와 내부 Blue-Green upstream(app-blue 8081 / app-green 8082) 프록시를 처리했고, api.clueroom.xyz 도메인으로 외부 접근을 제공한다.

파일 저장은 서버 로컬이 아니라 S3로 분리해 추후 멀티 인스턴스 확장에 대비했다.

초기부터 멀티 인스턴스 / DB 분리 / Redis 분리를 적용하지 않은 이유는 현재 사용자 규모에서 과설계라고 판단했기 때문이다.

대신 트래픽 증가 단계별로 DB/Redis 분리, App 서버 수평 확장, Nginx Load Balancer, RDS/ElastiCache/ALB 전환 계획을 문서화했다.

또한 현재 단일 서버 Blue-Green으로 배포 전환과 rollback 절차를 검증하고, 별도 PoC 환경에서는 Nginx 수동 로드밸런싱과 멀티 인스턴스 확장 가능성을 검증할 수 있다.
```
