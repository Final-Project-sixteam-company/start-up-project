# ClueRoom - Run and Deploy Guide

> 문서 목적: 로컬 실행, Docker Compose 실행, Android 연결, 운영 서버 배포 명령, 문제 해결을 간단히 정리한다.
> 상세 운영 명령어, Blue-Green rollback, 장애 대응은 `infra/OPS_RUNBOOK.md`를 기준으로 한다.
> 인프라 선택 이유, 확장 계획, PoC 계획, ADR 후보는 별도 문서 `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md`에서 관리한다.
> 운영 Agent / Monitoring / LLMOps / 고도화 로드맵은 `infra/agent/`와 `infra/CLUEROOM_INFRA_ENHANCEMENT_ROADMAP.md`를 참고한다.

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
| `AUTH_MOCK_FALLBACK_ENABLED` | JWT 전환기 token 없는 기존 API 요청을 `MOCK_USER_ID`로 허용할지 여부 |
| `AUTH_DEV_LOGIN_ENABLED` | `/api/auth/dev` 개발용 로그인 활성 여부. 운영 기본 `false` |
| `AUTH_REQUIRE_AUTHENTICATION` | 사용자별 API 인증 강제 여부. Android 전환 전 기본 `false` |
| `JWT_ISSUER` | JWT issuer. 운영 기본 `https://api.clueroom.xyz` |
| `JWT_SECRET` | 서버 전용 JWT HMAC secret. 레포/.env.example에는 실제 값 저장 금지 |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | access token 유효 시간 |
| `JWT_REFRESH_TOKEN_TTL_DAYS` | refresh token 유효 일수 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_IDS` | Google ID token `aud` 검증용 client id. 여러 개면 comma-separated |
| `KAKAO_APP_ID` | Kakao access token info `app_id` 검증용 앱 ID |
| `AWS_REGION` | S3 리전 |
| `AWS_ACCESS_KEY_ID` | 서버 전용 AWS access key |
| `AWS_SECRET_ACCESS_KEY` | 서버 전용 AWS secret key |
| `AWS_S3_BUCKET` | S3 bucket |
| `AWS_S3_PUBLIC_BASE_URL` | 이미지 asset key를 응답용 URL로 변환할 public base URL. 공란이면 이미지 URL은 `null` |
| `FCM_ENABLED` | FCM 활성 여부 |
| `FCM_PROJECT_ID` | Firebase project ID |
| `FCM_SERVICE_ACCOUNT_PATH` | 서버 내부 Firebase service account JSON 경로 |
| `SECRETS_HOST_DIR` | secret mount용 host 디렉터리 |
| `PROMETHEUS_HOST_PORT` | Prometheus localhost bind port |
| `GRAFANA_HOST_PORT` | Grafana localhost bind port |
| `GRAFANA_SERVER_DOMAIN` | Grafana public domain |
| `GRAFANA_SERVER_ROOT_URL` | Grafana public root URL |
| `GRAFANA_SERVER_ENFORCE_DOMAIN` | Grafana Host header domain enforcement |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 브라우저/WebView 테스트용 CORS |

### 3.2 Docker 내부 연결

Compose의 `app` 컨테이너는 기본적으로 내부 네트워크에서 아래 값을 사용한다.

```text
DB_HOST=mysql
DB_PORT=3306
REDIS_HOST=redis
REDIS_PORT=6379
```

컨테이너 내부에서 Spring이 읽는 최종 변수명은 여전히 `DB_HOST`와 `REDIS_HOST`다. 다만 Docker Compose가 그 값을 고를 때는 `APP_DB_HOST`와 `APP_REDIS_HOST`를 입력 변수로 사용한다.

이렇게 분리하는 이유는 기존 운영 `.env`의 `DB_HOST=localhost`, `REDIS_HOST=localhost`, `REDIS_PORT=16379` 같은 로컬 JVM 실행용 값이 app 컨테이너에 잘못 들어가는 것을 막기 위해서다.

Docker Compose로 앱 컨테이너를 실행할 때는 아래 APP_* 값을 기준으로 target을 정한다.

```properties
APP_DB_HOST=mysql
APP_DB_PORT=3306
APP_REDIS_HOST=redis
APP_REDIS_PORT=6379
```

Docker Compose interpolation은 쉘 환경변수가 `.env` 파일보다 우선될 수 있다. `APP_DB_HOST`, `APP_REDIS_HOST`, `APP_REDIS_PORT`를 쉘에 export해 둔 상태에서 compose를 실행하면 그 값이 컨테이너에 들어갈 수 있으므로, PoC 검증 전 현재 쉘 환경을 확인한다.

호스트에서 접속할 때는 아래 포트를 사용한다.

```text
MySQL: localhost:33306
Redis: localhost:16379
```

운영 cutover 이후 Blue-Green 배포 helper는 external-data mode를 기본으로 사용한다.

```text
prod app-blue/app-green
→ data server 172.26.1.185 MySQL/Redis

prod local MySQL/Redis
→ rollback/비교용으로 일시 유지
```

즉시 하지 말 것:

```bash
docker compose stop mysql redis
docker compose down -v
docker volume rm ...
```

App 컨테이너가 외부 data 서버를 바라보려면 `APP_DB_HOST`와 `APP_REDIS_HOST`를 data 서버 private IP 또는 DNS로 바꾸고 external-data override를 함께 사용한다.

```bash
docker compose -f docker-compose.yml -f docker-compose.external-data.yml config
docker compose -f docker-compose.yml -f docker-compose.external-data.yml up -d --build app
```

Blue-Green slot으로 검증할 때는 Blue-Green 전용 override를 같이 사용한다.

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.external-data.yml \
  -f docker-compose.bluegreen.yml \
  -f docker-compose.bluegreen.external-data.yml \
  config
```

`/opt/clueroom/deploy.sh`와 `/opt/clueroom/bg-compose`도 같은 compose 파일 순서를 사용한다.

```text
docker-compose.yml
docker-compose.external-data.yml
docker-compose.bluegreen.yml
docker-compose.bluegreen.external-data.yml
```

`docker-compose.external-data.yml`과 `docker-compose.bluegreen.external-data.yml`은 로컬 `mysql/redis` healthcheck 의존성을 제거한다. `app-blue` / `app-green`은 external-data mode에서 `mysql` / `redis`에 `depends_on`하지 않아야 한다. `prometheus`도 legacy `app` service에 `depends_on`하지 않아야 하며, `grafana -> prometheus` 의존성은 유지 가능하다.

운영 Blue-Green에서 `APP_DB_HOST` / `APP_REDIS_HOST`는 compose interpolation 단계에서 필요하다. 따라서 `/opt/clueroom/app/.env` 또는 배포 명령을 실행하는 쉘 환경에 넣어야 하며, service `env_file`로만 추가되는 secret env 파일에만 두면 `DB_HOST` / `REDIS_HOST` 값이 바뀌지 않을 수 있다.

### 3.3 Auth/JWT 1단계 로컬 테스트

1단계 auth는 기존 API 호환을 위해 전역 인증 강제를 아직 켜지 않는다. Bearer token이 있으면 SecurityContext 사용자로 처리하고, token이 없으면 `AUTH_MOCK_FALLBACK_ENABLED=true`일 때 `MOCK_USER_ID`로 fallback한다.

로컬에서 개발용 로그인 플로우를 확인할 때만 아래 값을 `.env`에 둔다.

```properties
AUTH_DEV_LOGIN_ENABLED=true
JWT_SECRET=<32자 이상 로컬 테스트용 난수>
```

검증:

```bash
curl -s -X POST http://localhost:8080/api/auth/dev \
  -H 'Content-Type: application/json' \
  -d '{"email":"dev@example.com","nickname":"Dev User","deviceId":"android-emulator"}'
```

운영에서는 `JWT_SECRET`을 `/opt/clueroom/secrets/env.d/oauth.env` 같은 서버 secret env로만 주입한다.

Android OAuth 로그인은 앱이 provider SDK로 받은 token을 백엔드에 전달하고, 백엔드는 provider 검증 후 ClueRoom JWT를 발급한다.

Google:

```bash
curl -s -X POST http://localhost:8080/api/auth/oauth \
  -H 'Content-Type: application/json' \
  -d '{"provider":"GOOGLE","idToken":"<google-id-token>","deviceId":"android"}'
```

Kakao:

```bash
curl -s -X POST http://localhost:8080/api/auth/oauth \
  -H 'Content-Type: application/json' \
  -d '{"provider":"KAKAO","accessToken":"<kakao-access-token>","deviceId":"android"}'
```

운영에서는 `GOOGLE_CLIENT_ID` 또는 `GOOGLE_CLIENT_IDS`, `KAKAO_APP_ID`를 secret env로 주입한다. Google은 ID token의 `aud`, Kakao는 access token info의 `app_id`를 서버 설정값과 비교한다.

보호 API 전환은 Android가 access token 저장과 `Authorization: Bearer <accessToken>` 첨부를 완료한 뒤 진행한다.

```properties
AUTH_REQUIRE_AUTHENTICATION=true
```

전환 후 token 없이 401이 되어야 하는 대표 경로:

```text
/api/play-sessions/**
/api/device-tokens/**
/api/scenarios write 계열
/api/ai/scenarios/{scenarioId}/validate
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
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=deepseek-v4-flash
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

운영 서버의 Spring Boot App은 Blue-Green overlay로 `app-blue:8081` 또는 `app-green:8082`가 active가 된다. 현재 active slot은 `/opt/clueroom/bg-status.sh` 또는 health check 응답의 `X-ClueRoom-Upstream` 헤더로 확인한다.

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

## 8. 운영 서버 배포

현재 운영 서버:

```text
https://api.clueroom.xyz
```

기본 배포는 GitHub Actions CD의 수동 실행(`workflow_dispatch`)을 사용한다.

```text
GitHub
→ Actions
→ Backend CD
→ Run workflow
→ develop
```

CD는 운영 서버에 SSH 접속해 아래 스크립트를 실행한다.

```bash
/opt/clueroom/deploy.sh
```

서버에서 직접 실행해야 할 때도 같은 스크립트를 사용한다.

```bash
ssh clueroom
/opt/clueroom/deploy.sh
/opt/clueroom/bg-status.sh
curl -f https://api.clueroom.xyz/actuator/health
```

주의:

```text
git pull origin develop
= 서버의 소스코드만 최신화

실제 앱 반영
= /opt/clueroom/deploy.sh 또는 GitHub Actions CD 필요
```

CD 성공 후 바로 standby를 끄지 말고 `/opt/clueroom/bg-status.sh`로 상태를 확인한다. 정상이고 잠시 확인 후 문제가 없으면 `/opt/clueroom/stop-standby.sh`로 active가 아닌 slot만 중지한다. 문제가 있으면 `/opt/clueroom/rollback-bluegreen.sh`를 사용한다.

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
/etc/nginx/conf.d/clueroom-upstream.conf
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

## 10. Blue-Green 배포

운영 서버의 `/opt/clueroom/deploy.sh`는 Blue-Green 배포를 수행한다. 일반 배포는 GitHub Actions CD로 실행하고, 서버에서 직접 실행하는 방식은 비상/확인용으로 사용한다.

external-data cutover 이후 `/opt/clueroom/deploy.sh`와 `/opt/clueroom/bg-compose`는 기본적으로 아래 compose 조합을 사용한다.

```text
docker-compose.yml
docker-compose.external-data.yml
docker-compose.bluegreen.yml
docker-compose.bluegreen.external-data.yml
```

운영 read-only snapshot은 현재 external-data 구조를 기준으로 확인한다.

```bash
cp /opt/clueroom/app/scripts/ops-snapshot.sh /opt/clueroom/ops-snapshot.sh
chmod +x /opt/clueroom/ops-snapshot.sh
bash -n /opt/clueroom/ops-snapshot.sh
/opt/clueroom/ops-snapshot.sh | tee /tmp/clueroom-ops-snapshot.txt
```

snapshot v3는 app-blue/app-green 컨테이너의 `DB_HOST` / `REDIS_HOST`가 data 서버 `172.26.1.185`를 보는지, data MySQL/Redis TCP 연결이 가능한지, ops Loki ready와 `start-up-alloy` 실행 여부를 확인한다. prod local MySQL/Redis는 source of truth가 아니라 rollback/local-data copy로만 표시한다.

레포 원본 스크립트를 서버 실행 위치로 배치한다.

```bash
ssh clueroom
cd /opt/clueroom/app
git pull origin develop
sudo cp scripts/deploy-bluegreen.sh /opt/clueroom/deploy.sh
sudo cp scripts/bg-compose.sh /opt/clueroom/bg-compose
sudo cp scripts/backup-mysql.sh /opt/clueroom/backup-mysql.sh
sudo cp scripts/bg-status.sh /opt/clueroom/bg-status.sh
sudo cp scripts/stop-standby.sh /opt/clueroom/stop-standby.sh
sudo cp scripts/rollback-bluegreen.sh /opt/clueroom/rollback-bluegreen.sh
sudo chmod +x \
  /opt/clueroom/deploy.sh \
  /opt/clueroom/bg-compose \
  /opt/clueroom/backup-mysql.sh \
  /opt/clueroom/bg-status.sh \
  /opt/clueroom/stop-standby.sh \
  /opt/clueroom/rollback-bluegreen.sh
```

배포 실행:

```bash
ssh clueroom
/opt/clueroom/deploy.sh
```

동작 흐름:

```text
1. /etc/nginx/conf.d/clueroom-upstream.conf에서 현재 active port 확인
2. develop 최신화
3. ./gradlew clean bootJar
4. 현재 active 반대편 slot을 target으로 선택
5. target app-blue 또는 app-green 재빌드/실행
6. target health check
7. Nginx upstream 전환
8. 외부 health check
9. 실패 시 기존 upstream으로 rollback
10. 성공 시 이전 active slot은 rollback용으로 유지
```

현재 active / standby 확인:

```bash
/opt/clueroom/bg-status.sh
```

배포 성공 후 standby 정리:

```bash
/opt/clueroom/stop-standby.sh
```

`stop-standby.sh`는 active slot을 자동 감지하고 active가 아닌 slot만 중지한다. 사람이 `app-blue` 또는 `app-green`을 직접 판단해 stop하지 않는다.

새 배포에 문제가 있을 때 이전 slot으로 rollback:

```bash
/opt/clueroom/rollback-bluegreen.sh
```

주의사항:

- `rollback-bluegreen.sh`는 반대편 slot container가 기존에 존재할 때만 rollback한다.
- rollback script는 target slot을 새로 build하지 않는다.
- blue/green을 사람이 직접 판단해 stop하지 말고 helper script를 사용한다.

수동 확인이 필요한 경우:

```bash
curl -I https://api.clueroom.xyz/actuator/health
cat /etc/nginx/conf.d/clueroom-upstream.conf
/opt/clueroom/bg-compose ps
```

현재 active가 아닌 서비스를 확인하지 않고 양쪽을 모두 중지하지 않는다.

레포 원본:

```text
scripts/deploy-bluegreen.sh
scripts/bg-compose.sh
scripts/bg-status.sh
scripts/stop-standby.sh
scripts/rollback-bluegreen.sh
docker-compose.bluegreen.yml
```

---

## 11. CD workflow 실행

GitHub Actions CD는 수동 실행(`workflow_dispatch`)으로 운영 서버에 접속해 `/opt/clueroom/deploy.sh`를 실행한다.

필요한 GitHub Actions Secrets 이름:

```text
LIGHTSAIL_HOST
LIGHTSAIL_USER
LIGHTSAIL_SSH_KEY
```

runtime secret인 AI/PortOne/OAuth/Firebase/DB 값은 GitHub Actions Secrets에 넣지 않고 서버 `.env`와 `/opt/clueroom/secrets`에서 관리한다.

CD 성공 후 운영 서버에서 확인한다.

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
```

정상이고 잠시 확인 후 문제가 없으면 standby를 자동 정리한다.

```bash
/opt/clueroom/stop-standby.sh
```

새 배포에 문제가 있으면 이전 slot으로 rollback한다.

```bash
/opt/clueroom/rollback-bluegreen.sh
```

---

## 12. MySQL 백업

운영 서버 백업 스크립트:

```bash
/opt/clueroom/backup-mysql.sh
```

백업 위치:

```text
/opt/clueroom/backups/mysql
```

확인:

```bash
ls -lh /opt/clueroom/backups/mysql
```

레포 원본:

```text
scripts/backup-mysql.sh
```

백업 파일(`*.sql.gz`)은 Git에 커밋하지 않는다.

---

## 13. Prometheus / Grafana 접속

Prometheus와 Grafana의 원본 포트는 `docker-compose.yml`에서 `127.0.0.1`에만 바인딩한다. 운영 서버 방화벽에서 3000/9090을 직접 열지 않는다.

로컬:

```text
Prometheus: http://localhost:9090
Grafana: http://localhost:3000
```

운영 Grafana는 팀원이 함께 볼 수 있도록 Nginx HTTPS reverse proxy 뒤의 별도 도메인으로 공개한다.

```text
https://monitor.clueroom.xyz
```

운영 `.env` 예시:

```properties
GRAFANA_SERVER_DOMAIN=monitor.clueroom.xyz
GRAFANA_SERVER_ROOT_URL=https://monitor.clueroom.xyz
GRAFANA_SERVER_ENFORCE_DOMAIN=true
```

Prometheus는 외부에 직접 공개하지 않고 Grafana가 Docker 내부 URL로 읽는다.

```text
Grafana datasource URL: http://prometheus:9090
```

운영 원칙:

```text
Grafana: Nginx HTTPS reverse proxy로 팀원 공유
Prometheus: 외부 비공개
Grafana admin 계정: 공유 금지
팀원 계정: Viewer부터 시작
서버 로그: 인프라 담당자 중심으로 확인
```

Prometheus를 직접 확인해야 하는 운영자는 SSH 터널로 접근한다.

```bash
ssh -N -L 9090:localhost:9090 clueroom
```

터널 연결 후 로컬 브라우저에서 접속한다.

```text
http://localhost:9090
```

Blue-Green target:

```text
app:8080
app-blue:8080
app-green:8080
```

standby app을 중지하면 `app-blue` 또는 `app-green` target이 `DOWN`으로 보일 수 있다. 단일 서버 Blue-Green PoC에서는 active app과 외부 health check가 정상이라면 허용 가능한 상태다.

---

## 14. FCM token 등록 확인

Android 앱에서 FCM registration token을 발급받은 뒤 백엔드 token 등록 API 호출 여부를 확인한다.

운영 서버 secret 예시:

```properties
FCM_ENABLED=true
FCM_PROJECT_ID=clueroom
FCM_SERVICE_ACCOUNT_PATH=/opt/clueroom/secrets/firebase-service-account.json
SECRETS_HOST_DIR=/opt/clueroom/secrets
```

Firebase service account JSON은 서버 secret이며 Git에 커밋하지 않는다.

FCM 문제 확인:

```bash
cd /opt/clueroom/app
docker compose logs --tail=120 app
```

---

## 15. 로컬 문제 해결

### 15.1 Docker가 실행 중이 아님

```bash
docker version
docker compose version
```

Docker Desktop이 꺼져 있으면 먼저 실행한다.

### 15.2 포트 충돌

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

### 15.3 MySQL이 healthy가 되지 않음

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

### 15.4 앱 컨테이너 DB 연결 실패

Docker 내부에서는 DB host가 `localhost`가 아니라 `mysql`이어야 한다.

```text
DB_HOST=mysql
DB_PORT=3306
```

### 15.5 Redis 연결 실패

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

### 15.6 Spring AI 부팅 실패

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

## 16. 운영 장애 대응 Runbook

상세 절차와 자주 발생하는 문제별 대응은 `infra/OPS_RUNBOOK.md`를 기준으로 한다. 이 문서에는 현재 운영 구조 기준의 1차 확인 순서만 둔다.

### 16.1 API가 안 열릴 때

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
cat /etc/nginx/conf.d/clueroom-upstream.conf
docker ps
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8082/actuator/health
```

확인할 것:

```text
현재 active upstream이 app-blue인지 app-green인지
active slot의 health check 통과 여부
Nginx upstream 설정
DNS가 서버 IP를 가리키는지
HTTPS 인증서 상태
```

### 16.2 Spring Boot 컨테이너가 죽었을 때

```bash
docker logs --tail=250 start-up-app-blue
docker logs --tail=250 start-up-app-green
docker inspect start-up-app-blue --format='RestartCount={{.RestartCount}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}}'
docker inspect start-up-app-green --format='RestartCount={{.RestartCount}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}}'
```

원인 후보:

```text
환경변수 누락
DB 연결 실패
Redis 연결 실패
AI Provider 설정 오류
메모리 부족
```

새 배포 후 오류라면 먼저 rollback을 검토한다.

```bash
/opt/clueroom/rollback-bluegreen.sh
```

### 16.3 DB 연결 실패

```bash
cd /opt/clueroom/app
docker compose logs mysql
docker compose exec mysql mysql -uroot -p
```

### 16.4 Nginx 설정 오류

```bash
sudo nginx -t
sudo journalctl -u nginx --no-pager -n 100
sudo systemctl reload nginx
```

### 16.5 디스크 부족

```bash
df -h
docker system df
du -sh /opt/clueroom/backups/mysql
docker image prune
```

운영에서는 prune 전에 현재 사용 중인 이미지/볼륨을 확인한다.

---

## 17. 보안 체크리스트

- [ ] 운영 `.env`를 Git에 커밋하지 않는다.
- [ ] OpenAI API Key를 Android 앱에 넣지 않는다.
- [ ] AWS Access Key를 Android 앱에 넣지 않는다.
- [ ] FCM service account JSON을 Git 또는 Android 앱에 넣지 않는다.
- [ ] DB/Redis 포트는 외부에 직접 공개하지 않는다.
- [ ] Spring Boot 8080/8081/8082는 외부에 직접 공개하지 않는다.
- [ ] Nginx는 HTTPS로 외부 공개한다.
- [ ] 백업 파일 접근 권한을 제한한다.
- [ ] 서버 시간대는 `Asia/Seoul` 기준으로 맞춘다.
- [ ] 장애 대응을 위해 로그 위치를 팀원이 알고 있어야 한다.

---

## 18. 관련 문서

| 문서 | 내용 |
|---|---|
| `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md` | 인프라 선택 이유, 확장 계획, PoC 계획, ADR 후보 |
| `infra/OPS_RUNBOOK.md` | 운영 상태 확인, Blue-Green rollback, 백업, 장애 대응 |
| `infra/CLUEROOM_SECRET_INPUT_GUIDE.md` | 운영 secret 입력/ACL/검증 절차 |
| `CaseLab_AI_PRD.md` | 제품 범위, MVP 우선순위 |
| `CaseLab_AI_API_Spec.md` | API Request/Response |
| `ANDROID_SCREEN_API_MAPPING.md` | Android 화면별 API 매핑 |
| `BACKEND_IMPLEMENTATION_GUIDE.md` | 백엔드 구현 규칙 |
| `AI_NPC_PROMPT_POLICY.md` | AI 프롬프트 정책 |
