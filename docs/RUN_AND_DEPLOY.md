# ClueRoom - Run and Deploy Guide

> 문서 목적: 로컬 실행, Docker Compose 실행, Android 연결, 운영 서버 배포 명령, 문제 해결을 간단히 정리한다.  
> 인프라 선택 이유, 확장 계획, PoC 계획, ADR 후보는 별도 문서 `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`에서 관리한다.

---

## 0. 이름 기준

```text
현재 서비스명: ClueRoom
도메인: clueroom.xyz
운영 API: https://api.clueroom.xyz
기존 기획명: CaseLab AI
```

레포 내부의 일부 파일명, Docker 이름, DB 이름에는 기존 기획명 또는 `start-up/startup` 레거시 명칭이 남아 있을 수 있다.  
작동 중인 설정은 무리하게 일괄 변경하지 않고, 운영 도메인과 문서에서 ClueRoom 기준을 사용한다.

---

## 1. 로컬 빠른 실행

프로젝트 루트에서 실행한다.

### 1.1 Docker 전체 실행

```bash
bash scripts/compose-up.sh
```

종료:

```bash
bash scripts/compose-down.sh
```

### 1.2 Gradle Task 실행

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

`-v`는 MySQL/Redis 볼륨을 삭제하므로 운영 서버에서는 신중하게 사용한다.

---

## 2. 로컬 접속 정보

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

| 변수 | 설명 |
|---|---|
| `SERVER_PORT` | 앱 포트 |
| `SPRING_PROFILES_ACTIVE` | local / docker / prod |
| `DB_HOST` | DB host |
| `DB_PORT` | DB port |
| `DB_NAME` | DB 이름 |
| `DB_USERNAME` | DB 사용자 |
| `DB_PASSWORD` | DB 비밀번호 |
| `MYSQL_HOST_PORT` | Docker MySQL host port |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |
| `REDIS_HOST_PORT` | Docker Redis host port |
| `SPRING_AI_MODEL_CHAT` | AI Provider 활성 여부 |
| `OPENAI_API_KEY` | 서버 전용 OpenAI API Key |
| `AWS_REGION` | S3 리전 |
| `AWS_ACCESS_KEY_ID` | 서버 전용 AWS access key |
| `AWS_SECRET_ACCESS_KEY` | 서버 전용 AWS secret key |
| `AWS_S3_BUCKET` | S3 bucket |
| `AWS_S3_PUBLIC_BASE_URL` | 이미지 URL base 또는 공란 |
| `FCM_ENABLED` | FCM 활성 여부 |
| `FCM_PROJECT_ID` | Firebase project ID |
| `FCM_SERVICE_ACCOUNT_PATH` | 서버 내부 Firebase service account JSON 경로 |
| `SECRETS_HOST_DIR` | secret mount용 host 디렉터리 |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 브라우저/WebView 테스트용 CORS |

### 3.2 Docker 내부 연결

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
Android 앱 안에 OpenAI API Key, AWS Key, Firebase service account를 넣지 않는다.

### 4.1 Android Emulator

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

PC와 휴대폰이 같은 Wi-Fi에 있어야 한다.

```text
http://192.168.x.x:8080
```

Windows 방화벽이 8080 포트를 막으면 실제 기기에서 접속되지 않는다.

### 4.3 운영 환경

```text
https://api.clueroom.xyz
```

Base URL에는 `/api`를 넣지 않는다.  
실제 API 경로는 `/api/...` prefix를 포함한다.

### 4.4 CORS

Native Android 앱의 Retrofit, OkHttp는 브라우저가 아니므로 CORS 제한을 받지 않는다.  
Android WebView, 웹 프론트, API 테스트 페이지에는 CORS 설정이 필요할 수 있다.

---

## 5. Spring AI / OpenAI

Spring AI는 서버 기능이다.  
Android 앱은 백엔드 API만 호출한다.

### 5.1 AI 비활성 기본값

```properties
SPRING_AI_MODEL_CHAT=none
OPENAI_API_KEY=
```

이 상태에서는 API Key 없이도 앱이 부팅되어야 한다.

### 5.2 OpenAI 활성화

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_CHAT_TEMPERATURE=0.4
```

API Key는 `.env`, 서버 secret, GitHub Actions secret에만 둔다.

---

## 6. FCM 푸시 알림

Android 푸시 알림은 Firebase Cloud Messaging을 사용한다.

```text
Android App
  - FCM registration token 발급
  - 백엔드에 token 등록
  - 알림 수신

Spring Boot Backend
  - 사용자별 token 저장
  - Firebase Admin SDK 또는 HTTP v1 API로 발송
```

운영 서버 secret 예시:

```properties
FCM_ENABLED=true
FCM_PROJECT_ID=clueroom
FCM_SERVICE_ACCOUNT_PATH=/opt/clueroom/secrets/firebase-service-account.json
SECRETS_HOST_DIR=/opt/clueroom/secrets
```

FCM service account JSON은 Git에 커밋하지 않는다.

---

## 7. Docker 구성

현재 주요 서비스:

| 서비스 | 역할 | 호스트 포트 |
|---|---|---|
| `mysql` | MySQL | `33306` |
| `redis` | Redis | `16379` |
| `app` | Spring Boot App | `8080` |
| `prometheus` | metric 수집 | `9090` |
| `grafana` | 모니터링 UI | `3000` |

Prometheus / Grafana는 2GB 운영 서버에서는 상시 운영하지 않을 수 있다.

### 7.1 상태 확인

```bash
docker compose ps
docker ps
docker compose logs app
docker compose logs mysql
docker compose logs redis
```

### 7.2 컨테이너 리소스 확인

```bash
docker stats
```

---

## 8. 운영 서버 수동 배포

현재 운영 서버:

```text
https://api.clueroom.xyz
```

수동 배포 순서:

```bash
ssh clueroom
cd /opt/clueroom/app
git pull origin develop
./gradlew clean bootJar
docker compose up -d --build app
curl -f https://api.clueroom.xyz/actuator/health
```

주의:

```text
git pull origin develop
= 서버의 소스코드만 최신화

실제 앱 반영
= bootJar + docker compose up -d --build app 필요
```

---

## 9. 운영 서버 주요 명령어

### 9.1 Nginx

```bash
sudo nginx -t
sudo systemctl status nginx
sudo systemctl restart nginx
sudo systemctl reload nginx
```

Nginx 설정 파일:

```text
/etc/nginx/sites-available/clueroom-api
```

로그:

```bash
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### 9.2 Certbot / HTTPS

```bash
sudo certbot certificates
sudo certbot renew --dry-run
```

HTTPS 확인:

```bash
curl https://api.clueroom.xyz/actuator/health
curl -I http://api.clueroom.xyz/actuator/health
```

### 9.3 App / DB / Redis

```bash
cd /opt/clueroom/app
docker compose ps
docker compose logs -f app
docker compose logs mysql
docker compose logs redis
```

MySQL 접속:

```bash
docker exec -it start-up-mysql mysql -uroot -p
```

Redis 접속:

```bash
docker exec -it start-up-redis redis-cli
ping
```

---

## 10. deploy.sh 예정

수동 배포가 안정화되면 서버에 `deploy.sh`를 둔다.

예정 흐름:

```bash
#!/bin/bash
set -e

cd /opt/clueroom/app

git pull origin develop
./gradlew clean bootJar
docker compose up -d --build app

sleep 15
curl -f https://api.clueroom.xyz/actuator/health

echo "Deploy success"
```

이 스크립트가 안정화되면 GitHub Actions CD에서 SSH로 호출한다.

---

## 11. 백업 예정

초기 MySQL은 Docker 컨테이너 안에서 운영하므로 `mysqldump` 기반 백업을 준비한다.

예정 위치:

```text
/opt/clueroom/backups
```

예정 명령 형태:

```bash
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > /opt/clueroom/backups/startup_$(date +%Y%m%d_%H%M%S).sql
```

추후 작업:

```text
- backup.sh 작성
- cron 등록
- 최근 N일 보관
- 필요 시 S3 업로드
- 복구 절차 테스트
```

---

## 12. 로컬 문제 해결

### 12.1 Docker가 실행 중이 아님

```bash
docker version
docker compose version
```

Docker Desktop이 꺼져 있으면 먼저 실행한다.

### 12.2 포트 충돌

Windows PowerShell:

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

### 12.3 MySQL이 healthy가 되지 않음

```bash
docker compose logs mysql
docker compose ps
```

원인 후보:

```text
DB_PASSWORD 불일치
기존 volume의 root password와 현재 .env 값 불일치
필요 시 로컬에서만 docker compose down -v
```

### 12.4 앱 컨테이너 DB 연결 실패

Docker 내부에서는 DB host가 `localhost`가 아니라 `mysql`이어야 한다.

```text
DB_HOST=mysql
DB_PORT=3306
```

### 12.5 Redis 연결 실패

```bash
docker compose logs redis
docker compose logs app
```

확인:

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

### 12.6 Spring AI 부팅 실패

AI 없이 실행:

```properties
SPRING_AI_MODEL_CHAT=none
OPENAI_API_KEY=
```

AI 활성화:

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=실제_키
```

---

## 13. 운영 장애 대응 Runbook

### 13.1 API가 안 열릴 때

```bash
docker compose ps
docker compose logs app
sudo nginx -t
sudo systemctl status nginx
curl http://localhost:8080/actuator/health
curl https://api.clueroom.xyz/actuator/health
```

확인할 것:

```text
앱 컨테이너 실행 여부
8080 포트 바인딩 여부
Nginx proxy_pass 대상
DNS가 서버 IP를 가리키는지
HTTPS 인증서 상태
```

### 13.2 Spring Boot 컨테이너가 죽었을 때

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

### 13.3 DB 연결 실패

```bash
docker compose logs mysql
docker compose exec mysql mysql -uroot -p
```

### 13.4 Nginx 설정 오류

```bash
sudo nginx -t
sudo journalctl -u nginx --no-pager -n 100
sudo systemctl reload nginx
```

### 13.5 디스크 부족

```bash
df -h
docker system df
docker image prune
```

운영에서는 prune 전에 현재 사용 중인 이미지/볼륨을 확인한다.

---

## 14. 보안 체크리스트

- [ ] 운영 `.env`를 Git에 커밋하지 않는다.
- [ ] OpenAI API Key를 Android 앱에 넣지 않는다.
- [ ] AWS Access Key를 Android 앱에 넣지 않는다.
- [ ] FCM service account JSON을 Git 또는 Android 앱에 넣지 않는다.
- [ ] DB/Redis 포트는 외부에 직접 공개하지 않는다.
- [ ] Spring Boot 8080은 외부에 직접 공개하지 않는다.
- [ ] Nginx는 HTTPS로 외부 공개한다.
- [ ] 백업 파일 접근 권한을 제한한다.
- [ ] 서버 시간대는 `Asia/Seoul` 기준으로 맞춘다.
- [ ] 장애 대응을 위해 로그 위치를 팀원이 알고 있어야 한다.

---

## 15. 관련 문서

| 문서 | 내용 |
|---|---|
| `CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 인프라 선택 이유, 확장 계획, PoC 계획, ADR 후보 |
| `CaseLab_AI_PRD.md` | 제품 범위, MVP 우선순위 |
| `CaseLab_AI_API_Spec.md` | API Request/Response |
| `ANDROID_SCREEN_API_MAPPING.md` | Android 화면별 API 매핑 |
| `BACKEND_IMPLEMENTATION_GUIDE.md` | 백엔드 구현 규칙 |
| `AI_NPC_PROMPT_POLICY.md` | AI 프롬프트 정책 |
