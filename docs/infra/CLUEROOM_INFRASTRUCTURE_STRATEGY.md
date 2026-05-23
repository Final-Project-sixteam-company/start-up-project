# ClueRoom Infrastructure Strategy - Ops Update

> 이 문서는 운영 서버에서 검증한 최신 인프라 상태를 PR 단위로 정리한 운영 보강본이다. 프로젝트 전체 인프라 전략 정본은 `../CLUEROOM_INFRASTRUCTURE_STRATEGY.md`를 함께 본다.

## 1. 최신 완료 상태

```text
- Lightsail 단일 서버 MVP 운영
- Docker Compose App/MySQL/Redis
- Nginx Reverse Proxy
- HTTPS / Certbot
- Dynadot DNS: api.clueroom.xyz
- Terraform S3
- FCM
- Secret 분리 구조
- deploy.sh
- MySQL 백업
- OPS Runbook
- GitHub Actions CI
- GitHub Actions CD
- Prometheus / Grafana
- Blue-Green 무중단 배포 PoC
```

## 2. 왜 단일 서버인가

ClueRoom은 현재 MVP 검증 단계이며, 팀원/튜터/소규모 피드백 사용자가 주요 대상이다. 이 단계에서는 멀티 서버 고가용성보다 기능 완성도, AI 응답 안정성, 시나리오 품질, 운영 비용 통제가 더 중요하다.

단일 서버는 장애 허용성이 낮지만 운영 구조가 단순하고 문제 원인을 빠르게 좁힐 수 있다. 트래픽이 실제로 늘어나면 DB/Redis 분리와 App 서버 수평 확장으로 단계적으로 전환한다.

## 3. 왜 Lightsail인가

Lightsail은 정액 비용과 단순한 서버 운영 모델이 장점이다. Ubuntu 서버에 직접 접속해 Docker Compose, Nginx, Certbot, 로그 확인, 배포 스크립트, 백업을 구성하기 쉬워 이번 프로젝트의 운영 학습 목표와 맞다.

EC2/ECS/ALB/RDS/ElastiCache는 확장성은 높지만 초기 MVP에는 설정과 비용 부담이 크다. 이 프로젝트에서는 필요 시점에 관리형 인프라로 전환할 수 있도록 문서화하고, 현재는 단일 Lightsail에서 운영한다.

## 4. 왜 DB/Redis를 처음부터 분리하지 않았는가

초기에는 App, MySQL, Redis를 같은 Lightsail 서버의 Docker Compose로 운영한다.

```text
장점:
- 비용 절감
- 네트워크 구성이 단순함
- 장애 확인 경로가 짧음
- 팀 프로젝트 기간 안에 운영 가능

한계:
- 서버 1대 장애 시 전체 서비스 중단
- App/DB/Redis가 CPU/메모리를 공유
- 트래픽 증가 시 DB/Redis 병목이 발생할 수 있음
```

DB/Redis 분리는 트래픽과 자원 경합이 실제로 보일 때 Phase 4에서 수행한다.

## 5. 왜 S3는 분리했는가

이미지는 서버 로컬 디스크에 저장하지 않고 S3에 저장한다. App 서버를 여러 대로 늘릴 때 로컬 파일 동기화 문제가 생기지 않도록 하기 위해서다.

S3는 Terraform으로 관리한다.

```text
- bucket
- Public Access Block
- Versioning
- SSE-S3 encryption
- CORS
- Lifecycle rule
```

## 6. 왜 GitHub Actions Secrets에 모든 runtime secret을 넣지 않았는가

GitHub Actions CD는 서버에 SSH로 접속해 `/opt/clueroom/deploy.sh`를 실행한다. 애플리케이션 runtime secret은 서버의 `.env`와 `/opt/clueroom/secrets/env.d`에 둔다.

GitHub Actions Secrets에는 배포 접속에 필요한 값만 둔다.

```text
LIGHTSAIL_HOST
LIGHTSAIL_USER
LIGHTSAIL_SSH_KEY
```

AI, PortOne, OAuth, Firebase, DB secret은 GitHub Actions가 알 필요가 없고, 서버에서만 읽는다. 이 구조는 CI/CD 로그에 runtime secret이 노출될 가능성을 줄인다.

## 7. Blue-Green PoC 범위

Blue-Green은 실제 멀티 서버 고가용성이 아니라 단일 Lightsail 서버 안에서 배포 전환 전략을 검증하는 PoC다.

```text
Nginx
  -> app-blue  : 127.0.0.1:8081
  -> app-green : 127.0.0.1:8082

공유:
- MySQL
- Redis
- /opt/clueroom/secrets
```

배포 스크립트는 현재 active upstream을 읽고 반대편 컨테이너를 빌드/기동한다. target health check가 성공한 뒤에만 Nginx upstream을 전환한다. 이전 active service는 rollback용으로 남긴다.

## 8. 모니터링

Prometheus는 actuator endpoint를 수집한다.

```text
/actuator/prometheus
```

Blue-Green 모드에서는 `app-blue:8080`, `app-green:8080`을 각각 target으로 둔다. standby 컨테이너를 중지한 경우 해당 target이 `DOWN`으로 보일 수 있으며, MVP PoC에서는 정상 범위로 본다.

Grafana/Prometheus는 외부에 직접 공개하지 않고 SSH tunnel로 접근한다.

```bash
ssh -N -L 3000:localhost:3000 -L 9090:localhost:9090 clueroom
```

## 9. 확장 계획

```text
Phase 1: 단일 Lightsail MVP
Phase 2: 운영 안정화(CI/CD, 백업, 모니터링, Rate Limit, Fail2Ban)
Phase 3: 단일 서버 Blue-Green PoC
Phase 4: DB/Redis 분리
Phase 5: Nginx LB + App 서버 2대
Phase 6: 필요 시 RDS/ElastiCache/ALB/ASG/ECS 전환
```

확장 판단 기준은 실제 운영 지표다.

```text
- App CPU/메모리 사용률
- DB connection/slow query
- Redis memory/latency
- 배포 중단 시간이 사용자에게 보이는지
- 장애 대응 시간이 길어지는지
```

## 10. 보안 원칙

```text
- .env는 커밋하지 않는다.
- /opt/clueroom/secrets는 커밋하지 않는다.
- Firebase service account JSON은 서버 secret으로만 둔다.
- pem/private key는 공유하지 않는다.
- DB 백업 파일은 Git에 넣지 않는다.
- GitHub Actions에는 배포 접속 secret만 둔다.
```
