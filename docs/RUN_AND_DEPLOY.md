# CaseLab AI - Run and Deploy Guide

> 문서 목적: 로컬 실행, Docker, Android 연결, 환경변수, 인프라/배포 운영 기준을 한 곳에서 관리한다.  
> 이 문서는 기존 실행/Android 연동/인프라 운영 문서의 핵심 내용을 통합한다.

---

## 0. 정본 범위

이 문서가 관리하는 내용:

```text
로컬 실행
Docker Compose 실행
MySQL / Redis / Prometheus / Grafana 포트
Android Emulator / 실제 기기 연결 주소
환경변수
Spring AI / OpenAI 키 관리
초기 운영 인프라 구조
배포 단계 로드맵
기본 장애 대응
```

이 문서가 관리하지 않는 내용:

| 내용 | 정본 문서 |
|---|---|
| 제품 범위, MVP 우선순위 | `CaseLab_AI_PRD.md` |
| API Request/Response | `CaseLab_AI_API_Spec.md` |
| Android 화면별 API 매핑 | `ANDROID_SCREEN_API_MAPPING.md` |
| 백엔드 구현 규칙 | `BACKEND_IMPLEMENTATION_GUIDE.md` |
| AI 프롬프트 정책 | `AI_NPC_PROMPT_POLICY.md` |

---

## 1. 빠른 실행

프로젝트 루트에서 실행한다.

### 1.1 Docker 전체 실행

```bash
bash scripts/compose-up.sh
```

종료:

```bash
bash scripts/compose-down.sh
```

### 1.2 Gradle Task로 실행

```bash
./gradlew composeUp
./gradlew composeDown
./gradlew composePs
./gradlew composeLogs
```

Windows PowerShell:

```powershell
.\gradlew.bat composeUp
.\gradlew.bat composeDown
```

IntelliJ에서는 Gradle 창에서 `Tasks > docker > composeUp`을 실행할 수 있다.

### 1.3 Docker Compose 직접 실행

`dockerfile`은 `build/libs/*.jar`를 복사하므로 먼저 `bootJar`가 필요하다.

```bash
./gradlew bootJar
docker compose up -d --build
```

종료:

```bash
docker compose down
```

DB/Redis 볼륨까지 초기화:

```bash
docker compose down -v
```

---

## 2. 로컬 접속 정보

Docker Compose 실행 후 기본 접속 정보:

| 대상 | 주소 |
|---|---|
| App | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| MySQL | `localhost:33306` |
| Redis | `localhost:16379` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Docker 내부 MySQL | `mysql:3306 / startup` |
| Docker 내부 Redis | `redis:6379` |

Grafana 로컬 기본 계정은 `.env.example` 기준 `admin / admin`이다.  
외부 공개 환경에서는 반드시 변경한다.

---

## 3. 환경변수

로컬 설정은 `.env.example`을 복사해서 `.env`로 관리한다.

```bash
cp .env.example .env
```

실제 `.env`는 커밋하지 않는다.

### 3.1 주요 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 호스트에 열 앱 포트 |
| `DB_HOST` | `localhost` | 로컬 실행 시 DB 호스트 |
| `DB_PORT` | `3306` | 로컬 DB 포트 |
| `DB_NAME` | `startup` | DB 이름 |
| `DB_USERNAME` | `root` | DB 사용자 |
| `DB_PASSWORD` | `12345678` | 로컬 개발용 DB 비밀번호 |
| `MYSQL_HOST_PORT` | `33306` | Docker MySQL 호스트 포트 |
| `REDIS_HOST` | `localhost` | 로컬 Redis 호스트 |
| `REDIS_PORT` | `16379` | 로컬 Redis 포트 |
| `REDIS_HOST_PORT` | `16379` | Docker Redis 호스트 포트 |
| `SPRING_AI_MODEL_CHAT` | `none` | AI Provider 비활성/활성 |
| `OPENAI_API_KEY` | empty | 서버에서만 사용하는 OpenAI API Key |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | OpenAI chat model |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | local patterns | 브라우저/WebView 테스트용 CORS |

### 3.2 Docker 내부 환경

Compose의 `app` 컨테이너는 내부 네트워크에서 아래 값을 사용한다.

```text
DB_HOST=mysql
DB_PORT=3306
REDIS_HOST=redis
REDIS_PORT=6379
```

호스트에서 접속할 때는 아래 포트를 사용한다.

```text
MySQL: localhost:33306
Redis: localhost:16379
```

---

## 4. Android 앱 연결

Android 앱은 Spring Boot를 REST API 서버로 호출한다.  
Android 앱 안에 OpenAI API Key를 넣지 않는다.

### 4.1 Android Emulator

Emulator에서 PC의 `localhost:8080`에 접근할 때는 `localhost`가 아니라 아래 주소를 사용한다.

```text
http://10.0.2.2:8080
```

Retrofit 예시:

```kotlin
Retrofit.Builder()
    .baseUrl("http://10.0.2.2:8080/")
    .build()
```

Retrofit `baseUrl`은 마지막 `/`가 필요하다.

### 4.2 실제 기기

실제 기기 테스트는 PC와 휴대폰이 같은 Wi-Fi에 있어야 한다.

```text
http://192.168.x.x:8080
```

Windows 방화벽이 8080 포트를 막으면 실제 기기에서 접속되지 않는다.

### 4.3 운영 환경

운영 API Base URL:

```text
https://api.caselab.ai
```

Base URL에는 `/api`를 넣지 않는다.  
실제 API 경로는 `/api/...` prefix를 포함한다.

### 4.4 Android HTTP 허용

로컬 개발에서 `http://`를 쓰면 Android 9 이상에서 cleartext 설정이 필요할 수 있다.

개발용으로만 `AndroidManifest.xml`에 아래 설정을 둘 수 있다.

```xml
<application
    android:usesCleartextTraffic="true">
</application>
```

운영 배포에서는 HTTPS를 사용한다.

### 4.5 CORS

Native Android 앱의 Retrofit, OkHttp는 브라우저가 아니므로 CORS 제한을 받지 않는다.

다만 Android WebView, 웹 프론트, API 테스트 페이지를 위해 서버에는 로컬 개발용 CORS 설정을 둘 수 있다.

```properties
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:[*],http://127.0.0.1:[*],http://10.0.2.2:[*],http://192.168.*.*:[*]
```

---

## 5. Spring AI / OpenAI

Spring AI는 서버 기능이다.  
Android 앱은 일반 API처럼 백엔드 엔드포인트를 호출하고, 백엔드가 AI Provider와 통신한 뒤 JSON 응답을 내려준다.

### 5.1 기본값

로컬 기본값은 AI Provider를 끈 상태다.

```properties
SPRING_AI_MODEL_CHAT=none
OPENAI_API_KEY=
```

이 상태에서는 앱이 API Key 없이 부팅되어야 한다.

### 5.2 OpenAI 활성화

OpenAI를 사용할 때만 아래 값을 설정한다.

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_CHAT_TEMPERATURE=0.4
```

API Key는 `.env`, 서버 secret, GitHub Actions secret에만 둔다.  
Android 앱, Git 저장소, 문서 예시에 실제 키를 남기지 않는다.

---

## 6. Docker 구성

현재 `docker-compose.yml`의 주요 서비스:

| 서비스 | 역할 | 호스트 포트 |
|---|---|---|
| `mysql` | MySQL 8.4 | `33306` |
| `redis` | Redis 8 | `16379` |
| `app` | Spring Boot App | `8080` |
| `prometheus` | Actuator metric 수집 | `9090` |
| `grafana` | 로컬 모니터링 UI | `3000` |

컨테이너 데이터는 named volume에 저장된다.

```text
start-up-mysql-data
start-up-redis-data
start-up-prometheus-data
start-up-grafana-data
```

볼륨 삭제가 필요한 경우에만 `docker compose down -v`를 사용한다.

---

## 7. 로컬 문제 해결

### 7.1 `bootJar` 실패

증상:

```text
BUILD FAILED
Compilation failed
```

확인:

```bash
./gradlew test
./gradlew bootJar --stacktrace
```

원인 후보:

```text
Java 컴파일 오류
테스트 실패
Gradle 의존성 다운로드 실패
환경변수 누락
```

### 7.2 Docker가 실행 중이 아님

확인:

```bash
docker version
docker compose version
```

Docker Desktop이 꺼져 있으면 먼저 실행한다.

### 7.3 포트 충돌

확인:

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :33306
netstat -ano | findstr :16379
```

해결:

```text
충돌 프로세스 종료
.env에서 SERVER_PORT / MYSQL_HOST_PORT / REDIS_HOST_PORT 변경
```

### 7.4 MySQL이 healthy가 되지 않음

확인:

```bash
docker compose logs mysql
docker compose ps
```

해결 후보:

```text
DB_PASSWORD 확인
기존 volume의 root password와 현재 .env 값 불일치 여부 확인
필요 시 docker compose down -v 후 재실행
```

### 7.5 앱 컨테이너 DB 연결 실패

Docker 내부에서는 DB 호스트가 `localhost`가 아니라 `mysql`이어야 한다.

Compose의 app 환경변수:

```text
DB_HOST=mysql
DB_PORT=3306
```

### 7.6 Redis 연결 실패

확인:

```bash
docker compose logs redis
docker compose logs app
```

확인할 값:

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

Docker 내부 app은 `redis:6379`를 사용하고, 호스트는 `localhost:16379`를 사용한다.

### 7.7 Spring AI 부팅 실패

AI Provider를 켰는데 API Key가 없으면 부팅 또는 호출이 실패할 수 있다.

로컬에서 AI 없이 실행하려면:

```properties
SPRING_AI_MODEL_CHAT=none
OPENAI_API_KEY=
```

OpenAI를 사용할 때는:

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=실제_키
```

### 7.8 Grafana에 데이터가 안 보임

확인:

```bash
docker compose ps
docker compose logs prometheus
docker compose logs grafana
```

Prometheus가 `app`의 Actuator Prometheus endpoint를 읽을 수 있어야 한다.

---

## 8. 1차 운영 인프라 구조

초기 운영은 저비용 단일 서버 구조로 시작한다.

```text
Android App
  ↓ HTTPS
api.caselab.ai
  ↓
Cloudflare DNS
  ↓
AWS Lightsail Ubuntu Server
  ↓
Nginx Reverse Proxy
  ↓
Docker Compose
  ├─ Spring Boot App
  ├─ MySQL
  └─ Redis
  ↓
Object Storage
  └─ Cloudflare R2 또는 S3
```

초기에는 AWS ALB, RDS 같은 완전 관리형 리소스를 바로 붙이지 않는다.

초기 목표:

```text
Lightsail 단일 서버
Nginx 직접 설정
Docker Compose 직접 운영
MySQL/Redis 직접 운영
HTTPS 직접 설정
백업 직접 설정
```

---

## 9. 권장 서버 스펙 / 비용 기준

> 클라우드 가격은 리전과 정책에 따라 바뀔 수 있다. 실제 결제 전에는 AWS 공식 Lightsail Pricing 페이지를 다시 확인한다.

초기 운영은 Lightsail Linux/Unix 인스턴스를 기준으로 잡는다.

| 구분 | 권장 스펙 | 비용 기준 | 사용 시점 |
|---|---|---:|---|
| 최소 운영 | 2GB RAM, 2 vCPU, 60GB SSD | 약 `$12/month` | 1차 MVP, 단일 app + MySQL + Redis |
| 여유 운영 | 4GB RAM, 2 vCPU, 80GB SSD | 약 `$24/month` | Blue/Green, Grafana, 여유 메모리 필요 시 |

2GB에서 시작하고 아래 조건이 생기면 4GB로 올린다.

```text
Spring Boot + MySQL + Redis를 동시에 돌릴 때 메모리가 부족함
app-blue / app-green 컨테이너를 동시에 띄우고 싶음
Prometheus / Grafana까지 상시 운영하고 싶음
AI 호출 로그와 백업 작업이 늘어남
```

추가 비용 후보:

```text
도메인 구매 비용
Cloudflare R2 또는 S3 이미지 저장 비용
AI API 사용량 비용
Managed DB 또는 RDS 분리 비용
Load Balancer 도입 비용
스냅샷 / 백업 저장 비용
```

현재 프로젝트의 기본 전략:

```text
초기: Lightsail 2GB + MySQL Docker + Redis Docker + Nginx
개선: Lightsail 4GB + app-blue/app-green + Nginx upstream
운영 안정화: Managed DB 또는 RDS 분리
```

참고:

```text
AWS Lightsail Pricing: https://aws.amazon.com/lightsail/pricing/
```

---

## 10. 배포 단계 로드맵

### Phase 0. 로컬 개발

```text
Docker Compose 로컬 실행
Android Emulator base URL = http://10.0.2.2:8080
AI Provider 기본 off
```

### Phase 1. 초저비용 운영 MVP

```text
AWS Lightsail 단일 서버
Nginx reverse proxy
Docker Compose
MySQL / Redis 컨테이너
Cloudflare DNS
Let's Encrypt HTTPS
수동 백업
Firebase App Distribution 테스트 배포
```

### Phase 2. Blue/Green 직접 운영

```text
app-blue / app-green 컨테이너 분리
Nginx upstream 전환
무중단에 가까운 수동 배포
```

### Phase 3. DB 분리

```text
MySQL을 Managed DB 또는 RDS로 분리
Redis를 ElastiCache 또는 외부 Redis로 분리 검토
백업/복구 자동화
```

### Phase 4. 실제 확장 구조

```text
Load Balancer
App Server 분리
Managed DB
Managed Redis
Object Storage
CI/CD 고도화
모니터링/알림
```

---

## 11. 운영 서버 기본 세팅

초기 운영 서버는 Ubuntu 기준으로 잡는다.

### 10.1 필수 패키지

```bash
sudo apt update
sudo apt install -y git curl unzip vim htop
```

### 10.2 Docker 설치 확인

```bash
docker version
docker compose version
```

### 10.3 Nginx 설치

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
sudo systemctl start nginx
```

### 10.4 서버 디렉터리 예시

```text
/opt/caselab
 ├─ app
 ├─ logs
 ├─ backup
 └─ .env
```

`.env`에는 운영 secret을 둔다.  
운영 `.env`는 Git에 커밋하지 않는다.

---

## 12. Nginx Reverse Proxy

운영 API 도메인:

```text
https://api.caselab.ai
```

Nginx는 외부 HTTPS 요청을 내부 Spring Boot app으로 전달한다.

단일 app 예시:

```nginx
server {
    server_name api.caselab.ai;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

설정 확인:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 13. HTTPS와 DNS

### 12.1 Cloudflare DNS

`api.caselab.ai`가 운영 서버 Public IP를 가리키게 설정한다.

```text
Type: A
Name: api
Value: Lightsail Public IP
```

### 12.2 Let's Encrypt

Certbot을 사용해 HTTPS 인증서를 발급한다.

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.caselab.ai
```

갱신 확인:

```bash
sudo certbot renew --dry-run
```

---

## 14. 이미지 저장 전략

초기에는 이미지 파일을 서버 디스크에 직접 저장하지 않는 방향을 우선 검토한다.

권장 후보:

```text
Cloudflare R2
AWS S3
```

저장 대상:

```text
시나리오 썸네일
증거 이미지
용의자 이미지
현장 이미지
```

Android에는 이미지 URL만 내려준다.

---

## 15. 백업 전략

초기 MySQL이 Docker 컨테이너 안에 있으면 수동 백업부터 준비한다.

수동 백업 예시:

```bash
mkdir -p /opt/caselab/backup
docker exec start-up-mysql mysqldump -uroot -p"$DB_PASSWORD" startup > /opt/caselab/backup/startup_$(date +%Y%m%d_%H%M%S).sql
```

운영에서는 아래를 추가한다.

```text
백업 스크립트
cron 등록
백업 파일 보관 주기
복구 절차 테스트
Object Storage 업로드
```

---

## 16. 배포 방식

### 15.1 1단계: 수동 배포

```bash
cd /opt/caselab/app
git pull origin main
./gradlew bootJar
docker compose up -d --build
docker compose ps
```

### 15.2 2단계: 배포 스크립트

운영 서버에 `deploy.sh`를 두고 아래 작업을 묶는다.

```text
git pull
bootJar
docker compose up -d --build
health check
old image prune
```

### 15.3 3단계: GitHub Actions

자동화 단계에서는 GitHub Actions secret으로 운영 서버 접속 정보를 관리한다.

관리 대상:

```text
SSH_HOST
SSH_USER
SSH_KEY
APP_ENV
OPENAI_API_KEY
```

초기에는 수동 배포로 흐름을 검증한 뒤 자동화한다.

---

## 17. Android 앱 배포

Android 앱 배포는 백엔드 서버 배포와 다르다.

### 16.1 개발 중

```text
Android Studio에서 직접 실행
Emulator base URL = http://10.0.2.2:8080
```

### 16.2 테스터 배포

```text
APK 직접 공유
Firebase App Distribution
```

### 16.3 실제 출시

```text
Google Play Console
운영 API Base URL = https://api.caselab.ai
```

---

## 18. 운영 보안 체크리스트

- [ ] 운영 `.env`를 Git에 커밋하지 않는다.
- [ ] OpenAI API Key를 Android 앱에 넣지 않는다.
- [ ] Grafana 기본 비밀번호를 변경한다.
- [ ] MySQL root password를 로컬 기본값 그대로 쓰지 않는다.
- [ ] 운영 서버 방화벽에서 필요한 포트만 연다.
- [ ] Nginx는 HTTPS로만 외부 공개한다.
- [ ] DB/Redis 포트는 외부에 직접 공개하지 않는다.
- [ ] 백업 파일에 접근 권한을 제한한다.
- [ ] 서버 시간대는 `Asia/Seoul` 기준으로 맞춘다.
- [ ] 장애 대응을 위해 로그 위치를 팀원이 알고 있어야 한다.

---

## 19. 로그와 모니터링

### 18.1 로컬 로그

```bash
docker compose logs app
docker compose logs mysql
docker compose logs redis
docker compose logs prometheus
docker compose logs grafana
```

실시간:

```bash
docker compose logs -f app
```

### 18.2 서버 상태 확인

```bash
docker compose ps
docker stats
df -h
free -m
```

### 18.3 이후 확장

```text
Prometheus / Grafana 운영 대시보드
애플리케이션 로그 수집
AI 비용 로깅
에러 알림
Slow query 확인
```

---

## 20. 장애 대응 Runbook

### 19.1 API가 안 열릴 때

```bash
docker compose ps
docker compose logs app
sudo nginx -t
sudo systemctl status nginx
```

확인할 것:

```text
앱 컨테이너 실행 여부
8080 포트 바인딩 여부
Nginx proxy_pass 대상
DNS가 서버 IP를 가리키는지
HTTPS 인증서 상태
```

### 19.2 Spring Boot 컨테이너가 죽었을 때

```bash
docker compose logs app --tail=200
docker compose restart app
```

원인 후보:

```text
환경변수 누락
DB 연결 실패
Redis 연결 실패
AI Provider 설정 오류
메모리 부족
```

### 19.3 DB 연결 실패

```bash
docker compose logs mysql
docker compose exec mysql mysql -uroot -p
```

확인할 것:

```text
DB_PASSWORD
DB_HOST
DB_PORT
컨테이너 healthcheck
볼륨 초기화 여부
```

### 19.4 Nginx 설정 오류

```bash
sudo nginx -t
sudo journalctl -u nginx --no-pager -n 100
```

설정 수정 후:

```bash
sudo systemctl reload nginx
```

### 19.5 디스크 부족

```bash
df -h
docker system df
docker image prune
```

운영에서는 prune 전에 현재 사용 중인 이미지/볼륨을 확인한다.

---

## 21. 발표용 요약

```text
Android 앱은 Firebase App Distribution 또는 Google Play로 배포하고,
백엔드는 api.caselab.ai 도메인 뒤에 Nginx + Docker Compose 구조로 운영한다.

초기에는 비용과 학습 효율을 위해 Lightsail 단일 서버에서
Spring Boot, MySQL, Redis를 함께 운영한다.

트래픽이나 운영 필요성이 생기면 DB/Redis를 Managed Service로 분리하고,
Blue/Green 또는 Load Balancer 구조로 확장한다.
```
