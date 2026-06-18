# ClueRoom - Run and Deploy Guide

> 문서 목적: 로컬 실행, Docker Compose 실행, Android 연결, 운영 서버 배포 명령, 문제 해결을 간단히 정리한다.
> 상세 운영 명령어, Blue-Green rollback, 장애 대응은 `infra/OPS_RUNBOOK.md`를 기준으로 한다.
> 인프라 선택 이유, 확장 계획, PoC 계획, ADR 후보는 별도 문서 `infra/CLUEROOM_INFRASTRUCTURE_STRATEGY.md`에서 관리한다.
> 보안/트래픽/알림 정책은 `infra/SECURITY_TRAFFIC_ALERT_POLICY.md`, 운영 Agent / Monitoring / LLMOps 기준은 `infra/agent/`를 참고한다.

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
| `OPENAI_BASE_URL` | OpenAI-compatible API base URL. DeepSeek 사용 시 `https://api.deepseek.com` |
| `OPENAI_CHAT_MODEL` | Chat model 이름. 기본 운영 후보는 `deepseek-v4-flash` |
| `OPENAI_CHAT_TEMPERATURE` | Chat temperature |
| `AI_LLMOPS_DB_LOGGING_ENABLED` | AI 호출 로그 DB 저장 활성화 여부 |
| `AUTH_MOCK_FALLBACK_ENABLED` | JWT 전환기 token 없는 기존 API 요청을 `MOCK_USER_ID`로 허용할지 여부 |
| `AUTH_DEV_LOGIN_ENABLED` | `/api/auth/dev` 개발용 로그인 활성 여부. 운영 기본 `false` |
| `AUTH_REQUIRE_AUTHENTICATION` | 사용자별 API 인증 강제 여부. Android/Web 전환 전 기본 `false` |
| `AUTH_ADMIN_SEED_ENABLED` | 운영 secret env에 지정한 admin 테스트 계정을 생성/승격할지 여부. 기본 `false` |
| `AUTH_ADMIN_SEED_EMAIL` | admin seed 대상 이메일. 실제 값은 서버 secret env에만 저장 |
| `AUTH_ADMIN_SEED_NICKNAME` | admin seed 신규 생성 시 nickname |
| `AUTH_QA_SEED_ENABLED` | 운영 secret env에 지정한 QA 전용 일반 계정을 생성할지 여부. 기본 `false` |
| `AUTH_QA_SEED_EMAIL` | QA seed 대상 이메일. 실제 값은 서버 secret env에만 저장 |
| `AUTH_QA_SEED_NICKNAME` | QA seed 신규 생성 시 nickname |
| `JWT_ISSUER` | JWT issuer. 운영 기본 `https://api.clueroom.xyz` |
| `JWT_SECRET` | 서버 전용 JWT HMAC secret. 레포/.env.example에는 실제 값 저장 금지 |
| `JWT_ACCESS_TOKEN_TTL_SECONDS` | access token 유효 시간 |
| `JWT_REFRESH_TOKEN_TTL_DAYS` | refresh token 유효 일수 |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_IDS` | Google ID token `aud` 검증용 client id. 여러 개면 comma-separated |
| `KAKAO_APP_ID` | Kakao access token info `app_id` 검증용 앱 ID |
| `KAKAO_REST_API_KEY` | Web Kakao authorization code를 access token으로 교환할 때 사용하는 REST API key |
| `KAKAO_CLIENT_SECRET` | Kakao client secret을 활성화한 경우에만 사용하는 선택값 |
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
| `CORS_ALLOWED_ORIGIN_PATTERNS` | 웹 프론트/브라우저/WebView CORS origin. 운영 웹 기본 origin은 `https://clueroom.xyz`, `https://www.clueroom.xyz` |

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
`AUTH_REQUIRE_AUTHENTICATION=true`, `AUTH_DEV_LOGIN_ENABLED=true`, `GOOGLE_CLIENT_ID(S)` 또는 `KAKAO_APP_ID`가 설정된 상태에서 `JWT_SECRET`이 비어 있거나 32자 미만이면 서버는 부팅 단계에서 실패한다. 인증 기능을 켜기 전에 secret env 반영 여부를 먼저 확인한다.

운영/스테이징에서 AI rate limit 검증용 admin 계정이 필요하면 secret env에만 아래 값을 둔다. 실제 이메일은 공개 문서, PR 본문, 코드에 기록하지 않는다.

```properties
AUTH_ADMIN_SEED_ENABLED=true
AUTH_ADMIN_SEED_EMAIL=<server-secret-admin-email>
AUTH_ADMIN_SEED_NICKNAME=ClueRoom Admin
```

`AUTH_ADMIN_SEED_ENABLED=true`인데 email이 비어 있거나 inactive user를 가리키면 앱 부팅이 실패한다. 정상 부팅 시 해당 email의 `users.role`은 `ADMIN`으로 보장된다. 이후 AI rate limit 정책은 `ADMIN` role을 bypass 대상으로 삼는다.

운영/스테이징에서 blind QA 격리용 일반 계정이 필요하면 secret env에만 아래 값을 둔다. 실제 이메일은 공개 문서, PR 본문, 코드에 기록하지 않는다.

```properties
AUTH_QA_SEED_ENABLED=true
AUTH_QA_SEED_EMAIL=<server-secret-qa-email>
AUTH_QA_SEED_NICKNAME=ClueRoom QA
```

`AUTH_QA_SEED_ENABLED=true`인데 email이 비어 있거나 inactive user를 가리키면 앱 부팅이 실패한다. 정상 부팅 시 해당 email의 `users.role`은 기존 role을 유지하며, 신규 생성 시에는 `USER`다. admin seed와 QA seed는 같은 email을 사용할 수 없다. 이후 Google/Kakao가 verified email을 제공하면 OAuth provider 계정은 이 QA 계정에 연결된다.

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

운영에서는 `GOOGLE_CLIENT_ID` 또는 `GOOGLE_CLIENT_IDS`, `KAKAO_APP_ID`를 secret env로 주입한다. Web Kakao code-flow를 쓰면 `KAKAO_REST_API_KEY`도 함께 주입한다. Google은 ID token의 `aud`, Kakao는 access token info의 `app_id`를 서버 설정값과 비교한다.
Android와 Web이 같은 백엔드를 쓰면 `GOOGLE_CLIENT_IDS`에 Android OAuth client id와 Web OAuth client id를 comma-separated로 모두 넣는다.
기존 계정 email 기반 linking은 provider가 verified email을 제공한 경우에만 수행한다. Google은 `email_verified`, Kakao는 `is_email_valid=true`와 `is_email_verified=true`를 기준으로 한다.

보호 API 전환은 Android와 Web이 모두 access token 저장과 `Authorization: Bearer <accessToken>` 첨부를 완료한 뒤 진행한다.

```properties
AUTH_REQUIRE_AUTHENTICATION=true
```

`AUTH_REQUIRE_AUTHENTICATION=true`에서는 `AUTH_MOCK_FALLBACK_ENABLED=true`가 남아 있어도 token 없는 요청에 `MOCK_USER_ID`를 부여하지 않는다. 공개 시나리오 조회는 anonymous 사용자로 처리하고, 작성자 전용 DRAFT/PRIVATE 시나리오는 노출하지 않는다.

전환 후 token 없이 401이 되어야 하는 대표 경로:

```text
/api/play-sessions/**
/api/device-tokens/**
/api/scenarios write 계열
/api/ai/scenarios/{scenarioId}/validate
/api/** 신규 endpoint 기본 인증
```

`/api/auth/refresh` 등 auth 공개 endpoint는 만료 access token이 `Authorization` 헤더에 남아 있어도 refresh body 검증까지 도달해야 한다. 클라이언트 interceptor가 refresh 요청에 기존 Bearer token을 자동 첨부할 수 있기 때문이다.
보호 API에 대한 브라우저/WebView CORS preflight `OPTIONS` 요청은 Bearer token 없이 통과해야 한다.

웹 프론트 운영 origin:

```properties
CORS_ALLOWED_ORIGIN_PATTERNS=https://clueroom.xyz,https://www.clueroom.xyz,http://localhost:[*],http://127.0.0.1:[*],http://10.0.2.2:[*],http://192.168.*.*:[*]
```

Native Android HTTP client는 CORS 대상이 아니므로 위 값은 웹/브라우저/WebView 호출만 제어한다.

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
# OPENAI_API_KEY는 설정하지 않는다.
```

이 상태에서는 API Key 없이도 앱이 부팅되어야 한다.

### 5.2 OpenAI 활성화

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_BASE_URL=https://api.deepseek.com
# OPENAI_API_KEY는 로컬 .env 또는 운영 서버 secret env에서 주입한다.
OPENAI_CHAT_MODEL=deepseek-v4-flash
OPENAI_CHAT_TEMPERATURE=0.4
```

API Key는 로컬 개발용 `.env` 또는 운영 서버 secret env에만 둔다.
GitHub Actions Secrets에는 AI/PortOne/OAuth/Firebase/DB 같은 runtime secret을 넣지 않는다. CD workflow는 운영 서버 SSH 접속 secret만 사용한다.

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
/opt/clueroom/bg-status.sh
/opt/clueroom/bg-compose ps
docker ps
/opt/clueroom/bg-compose logs --tail=120 app-blue
/opt/clueroom/bg-compose logs --tail=120 app-green
```

운영 source of truth MySQL/Redis는 data 서버에 있다. prod 서버의 compose `mysql`/`redis`는 local-data 또는 rollback copy 확인이 필요할 때만 본다.

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
/opt/clueroom/bg-compose ps
/opt/clueroom/bg-compose logs -f app-blue
/opt/clueroom/bg-compose logs -f app-green
```

운영 MySQL 접속은 data 서버에서 수행한다. `clueroom-data` SSH alias는 로컬 PC SSH config 기준이다.

```bash
ssh -t clueroom-data 'DATA_MYSQL_CONTAINER="$(docker ps --format '\''{{.Names}} {{.Image}}'\'' | awk '\''/mysql/ {print $1; exit}'\'')" && docker exec -it "$DATA_MYSQL_CONTAINER" mysql -uroot -p'
```

운영 Redis 접속도 data 서버 기준으로 확인한다.

```bash
ssh -t clueroom-data 'DATA_REDIS_CONTAINER="$(docker ps --format '\''{{.Names}} {{.Image}}'\'' | awk '\''/redis/ {print $1; exit}'\'')" && docker exec -it "$DATA_REDIS_CONTAINER" redis-cli ping'
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

Ops Snapshot v3는 운영 합의 라벨이다.
현재 `ops-snapshot.sh` 출력에 `snapshotVersion: v3` 필드가 직접 찍히는 구조는 아니다.
스크립트는 app-blue/app-green 컨테이너의 `DB_HOST` / `REDIS_HOST`가 data 서버 `172.26.1.185`를 보는지, data MySQL/Redis TCP 연결이 가능한지, ops Loki ready와 `start-up-alloy` 실행 여부를 확인한다.
prod local MySQL/Redis는 source of truth가 아니라 rollback/local-data copy로만 표시한다.

레포 원본 스크립트를 서버 실행 위치로 배치한다.
`backup-mysql.sh`는 local-data/rollback copy 확인용이며, 운영 source-of-truth 백업은 12장의 data 서버 절차를 따른다.

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

### 10.1 공식 시나리오 guidance seed hotfix

이 절차는 이미 운영 DB에 들어간 공식 시나리오 `1.1.0`의 player-facing `evidences.guidance_json`만 보강할 때 사용한다.
정식 신규 버전 import 절차가 아니라, 기존 공개 시나리오 row를 안전하게 patch하고 서버 YAML hash를 맞춰 importer가 skip하게 만드는 hotfix 절차다.

적용 조건:

```text
- 증거 guidance처럼 기존 시나리오의 보조 UX 데이터만 추가/수정한다.
- `scenario.code + scenario.version`은 기존 운영 row와 동일하게 유지한다.
- 운영 YAML 파일과 DB `scenarios.content_hash`를 같은 SHA-256으로 맞춘다.
- deploy 후 `[ScenarioImport] SKIPPED ...@1.1.0` 로그를 확인한다.
```

하지 말 것:

```text
- guidance만 추가하는데 `scenario.version`을 올리지 않는다.
- 기존 assetKey를 그대로 둔 채 신규 scenario version을 full import하지 않는다.
- private seed YAML, SQL, DB password, SSH key를 GitHub/Slack/ChatGPT에 붙이지 않는다.
```

주의 이유:

```text
현재 importer는 `scenario.code + scenario.version`이 없으면 새 시나리오 graph를 import한다.
공식 seed가 기존 assetKey를 재사용한 상태에서 version만 올리면 `scenario_assets.asset_key` unique 제약에 걸릴 수 있다.
따라서 기존 운영 row hotfix는 DB patch + content_hash sync 방식으로 처리한다.
```

#### 10.1.1 로컬 검증

Git Bash 기준으로 프로젝트 루트에서 실행한다.

```bash
cd /c/java/assignment/spring/start-up

sha256sum .private/deploy/scenarios/seowolchae.v1.yaml
sha256sum .private/deploy/scenarios/studio9.v1.yaml
```

YAML은 기존 운영 row와 같은 version이어야 한다.

```bash
grep -n '^  version:' .private/deploy/scenarios/seowolchae.v1.yaml
grep -n '^  version:' .private/deploy/scenarios/studio9.v1.yaml
```

기대:

```text
version: "1.1.0"
```

`guidance_json` patch SQL은 `.private/deploy/` 아래에 둔다. 이 파일은 Git에 커밋하지 않는다.

권장 SQL 조건:

```sql
START TRANSACTION;

UPDATE evidences e
JOIN scenarios s ON s.id = e.scenario_id
SET e.guidance_json = '<guidance json>'
WHERE s.code = 'SCENARIO_...'
  AND s.content_version = '1.1.0'
  AND s.status = 'PUBLISHED'
  AND s.visibility = 'PUBLIC'
  AND e.code = 'EVIDENCE_...';

SELECT ROW_COUNT() AS updated_rows;

COMMIT;
```

#### 10.1.2 접속 변수

Git Bash에서 사용한다. SSH alias를 쓸 수 있으면 `APP=clueroom`, `DATA=clueroom-data`로 둬도 된다.

```bash
KEY="/path/to/LightsailDefaultKey-ap-northeast-2.pem"
APP="ubuntu@<prod-app-public-ip-or-ssh-alias>"
DATA="ubuntu@<data-server-public-ip-or-ssh-alias>"

SQL_SCRIPT=".private/deploy/apply_guidance_json_YYYYMMDD.sql"
```

#### 10.1.3 실패한 standby 정리

이전 배포 실패로 standby slot이 남아 있으면 먼저 제거한다. active upstream은 건드리지 않는다.

```bash
ssh -i "$KEY" "$APP" 'bash -s' <<'EOF'
set -euo pipefail

ACTIVE_UPSTREAM="$(grep -oE '127\.0\.0\.1:808[12]' /etc/nginx/conf.d/clueroom-upstream.conf | head -n 1)"

case "$ACTIVE_UPSTREAM" in
  127.0.0.1:8081)
    docker rm -f start-up-app-green 2>/dev/null || true
    ;;
  127.0.0.1:8082)
    docker rm -f start-up-app-blue 2>/dev/null || true
    ;;
  *)
    echo "Unknown active upstream: $ACTIVE_UPSTREAM" >&2
    exit 1
    ;;
esac

/opt/clueroom/bg-status.sh || true
curl -I https://api.clueroom.xyz/actuator/health
EOF
```

예를 들어 active upstream이 `127.0.0.1:8082`이면 `app-green`이 active이므로 `start-up-app-blue`만 제거한다.

#### 10.1.4 서버 seed YAML 교체

```bash
scp -i "$KEY" .private/deploy/scenarios/seowolchae.v1.yaml "$APP:/tmp/seowolchae.v1.yaml"
scp -i "$KEY" .private/deploy/scenarios/studio9.v1.yaml "$APP:/tmp/studio9.v1.yaml"
```

```bash
ssh -i "$KEY" "$APP" 'bash -s' <<'EOF'
set -euo pipefail

cd /opt/clueroom/secrets/scenarios

TS="$(date +%Y%m%d_%H%M%S)"
sudo cp seowolchae.v1.yaml "seowolchae.v1.yaml.bak-$TS"
sudo cp studio9.v1.yaml "studio9.v1.yaml.bak-$TS"

sudo cp /tmp/seowolchae.v1.yaml /opt/clueroom/secrets/scenarios/seowolchae.v1.yaml
sudo cp /tmp/studio9.v1.yaml /opt/clueroom/secrets/scenarios/studio9.v1.yaml

sudo chown root:root seowolchae.v1.yaml studio9.v1.yaml
sudo chmod 600 seowolchae.v1.yaml studio9.v1.yaml

sudo grep -n '^  version:' seowolchae.v1.yaml studio9.v1.yaml
sudo sha256sum seowolchae.v1.yaml studio9.v1.yaml
EOF
```

#### 10.1.5 data 서버 백업

source of truth DB는 data 서버 MySQL이다. patch 전 data 서버에서 백업한다.

```bash
ssh -i "$KEY" "$DATA" 'bash -s' <<'EOF'
set -euo pipefail

/opt/clueroom-data/backup-mysql.sh
ls -lh /opt/clueroom-data/backups/mysql | tail
EOF
```

#### 10.1.6 guidance SQL 적용과 content_hash 동기화

```bash
scp -i "$KEY" "$SQL_SCRIPT" "$DATA:/tmp/$(basename "$SQL_SCRIPT")"
```

서버 YAML hash를 읽는다.

```bash
SEOWOL_HASH="$(ssh -i "$KEY" "$APP" "sudo sha256sum /opt/clueroom/secrets/scenarios/seowolchae.v1.yaml | awk '{print \$1}'")"
STUDIO_HASH="$(ssh -i "$KEY" "$APP" "sudo sha256sum /opt/clueroom/secrets/scenarios/studio9.v1.yaml | awk '{print \$1}'")"

echo "$SEOWOL_HASH"
echo "$STUDIO_HASH"
```

data 서버에서 SQL patch와 hash sync를 실행한다.

```bash
REMOTE_SQL="/tmp/$(basename "$SQL_SCRIPT")"

ssh -i "$KEY" "$DATA" 'bash -s' <<EOF
set -euo pipefail

DB_PASSWORD="\$(docker exec clueroom-data-mysql printenv MYSQL_ROOT_PASSWORD)"

docker exec -i \
  -e MYSQL_PWD="\$DB_PASSWORD" \
  clueroom-data-mysql \
  mysql --default-character-set=utf8mb4 -uroot startup < "$REMOTE_SQL"

docker exec -i \
  -e MYSQL_PWD="\$DB_PASSWORD" \
  clueroom-data-mysql \
  mysql --default-character-set=utf8mb4 -uroot startup <<SQL
UPDATE scenarios
SET content_hash = '$SEOWOL_HASH'
WHERE code = 'SCENARIO_SEOWOLCHAE_LAST_PRESCRIPTION'
  AND content_version = '1.1.0'
  AND status = 'PUBLISHED'
  AND visibility = 'PUBLIC';

SELECT ROW_COUNT() AS seowol_hash_updated;

UPDATE scenarios
SET content_hash = '$STUDIO_HASH'
WHERE code = 'SCENARIO_STUDIO9'
  AND content_version = '1.1.0'
  AND status = 'PUBLISHED'
  AND visibility = 'PUBLIC';

SELECT ROW_COUNT() AS studio_hash_updated;

SELECT id, code, content_version, status, visibility, content_hash
FROM scenarios
WHERE code IN ('SCENARIO_SEOWOLCHAE_LAST_PRESCRIPTION', 'SCENARIO_STUDIO9')
ORDER BY code, id;
SQL
EOF
```

`seowol_hash_updated`와 `studio_hash_updated`는 각각 `1`이어야 한다.

#### 10.1.7 DB patch 확인

아래 쿼리는 public-safe shape다.
실제 guidance 대상 evidence code 목록과 대상 개수는 공개 문서에 남기지 않고, `.private/deploy/apply_guidance_json_YYYYMMDD.sql` 또는 비공개 handoff에서 확인한다.

```bash
ssh -i "$KEY" "$DATA" 'bash -s' <<'EOF'
set -euo pipefail

DB_PASSWORD="$(docker exec clueroom-data-mysql printenv MYSQL_ROOT_PASSWORD)"

docker exec -i \
  -e MYSQL_PWD="$DB_PASSWORD" \
  clueroom-data-mysql \
  mysql --default-character-set=utf8mb4 -uroot startup -e "
SELECT
  s.code AS scenario_code,
  s.content_version,
  e.code AS evidence_code,
  e.guidance_json IS NOT NULL AS has_guidance
FROM evidences e
JOIN scenarios s ON s.id = e.scenario_id
WHERE s.status = 'PUBLISHED'
  AND s.visibility = 'PUBLIC'
  AND s.content_version = '1.1.0'
  AND e.code IN (<private guidance target evidence codes>)
ORDER BY s.code, e.code;
"
EOF
```

기대:

```text
private patch 대상 row가 모두 has_guidance = 1
```

#### 10.1.8 Blue-Green 재배포와 importer skip 확인

```bash
ssh -i "$KEY" "$APP" 'bash -s' <<'EOF'
set -euo pipefail

cd /opt/clueroom/app
/opt/clueroom/deploy.sh
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
EOF
```

importer 로그를 확인한다.

```bash
ssh -i "$KEY" "$APP" 'bash -s' <<'EOF'
set -euo pipefail

docker logs --since 20m start-up-app-blue 2>&1 | grep '\[ScenarioImport\]' || true
docker logs --since 20m start-up-app-green 2>&1 | grep '\[ScenarioImport\]' || true
EOF
```

기대:

```text
[ScenarioImport] SKIPPED SCENARIO_SEOWOLCHAE_LAST_PRESCRIPTION@1.1.0 ...
[ScenarioImport] SKIPPED SCENARIO_STUDIO9@1.1.0 ...
```

`IMPORTED ...@1.1.1` 또는 asset unique 오류가 보이면 서버 YAML version이 잘못 올라갔거나 DB `content_hash`가 서버 YAML hash와 맞지 않는 상태다. 이 경우 active upstream이 전환됐는지 먼저 확인하고, 실패한 standby를 제거한 뒤 YAML version/hash부터 다시 맞춘다.

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

현재 운영 source of truth는 data 서버 MySQL이다.
따라서 운영 백업 완료 기준은 data 서버의 `/opt/clueroom-data` 경로에서 수행되는 백업과 S3 업로드다.

```bash
# [로컬 PC] `clueroom-data` alias는 로컬 SSH config 기준이다.
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail
/opt/clueroom-data/backup-mysql.sh
/opt/clueroom-data/upload-mysql-backup-s3.sh
REMOTE
```

백업 위치:

```text
/opt/clueroom-data/backups/mysql
```

확인:

```bash
# [로컬 PC] `clueroom-data` alias는 로컬 SSH config 기준이다.
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail
ls -lh /opt/clueroom-data/backups/mysql
cat /opt/clueroom-data/backups/mysql/*.sha256 | tail -n 5
cat /opt/clueroom-data/backups/mysql/s3-upload-state.env
REMOTE
```

prod app 서버의 local-data 백업 스크립트는 rollback/local copy 확인용이다.
external-data 운영에서 아래 결과만으로 source-of-truth 백업 완료로 보지 않는다.

```bash
ssh clueroom
/opt/clueroom/backup-mysql.sh
ls -lh /opt/clueroom/backups/mysql
```

레포 원본 local-data script:

```text
scripts/backup-mysql.sh
```

백업 파일(`*.sql.gz`)은 Git에 커밋하지 않는다.
상세 절차와 restore rehearsal는 [docs/infra/OPS_RUNBOOK.md](infra/OPS_RUNBOOK.md)의 external-data 백업/복구 절차를 따른다.

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
job_name: clueroom-app        target: app:8080
job_name: clueroom-app-blue   target: app-blue:8080
job_name: clueroom-app-green  target: app-green:8080
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
# [로컬 개발 PC] local Docker Compose MySQL 확인용이다.
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
# [로컬 개발 PC] local Docker Compose Redis/app 확인용이다.
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
# OPENAI_API_KEY는 설정하지 않는다.
```

AI 활성화:

```properties
SPRING_AI_MODEL_CHAT=openai
# OPENAI_API_KEY는 운영 서버 secret env에서 주입한다.
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

external-data 운영 기준 확인:

```bash
ssh clueroom
cd /opt/clueroom/app
/opt/clueroom/bg-compose exec app-blue printenv DB_HOST
/opt/clueroom/bg-compose exec app-green printenv DB_HOST
nc -vz 172.26.1.185 3306
nc -vz 172.26.1.185 6379
```

data 서버 직접 확인:

```bash
# [로컬 PC] `clueroom-data` alias는 로컬 SSH config 기준이다.
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
DATA_MYSQL_CONTAINER="$(docker ps --format '{{.Names}} {{.Image}}' | awk '/mysql/ {print $1; exit}')"
docker logs --tail=100 "$DATA_MYSQL_CONTAINER"
REMOTE

ssh -t clueroom-data 'DATA_MYSQL_CONTAINER="$(docker ps --format '\''{{.Names}} {{.Image}}'\'' | awk '\''/mysql/ {print $1; exit}'\'')" && docker exec -it "$DATA_MYSQL_CONTAINER" mysql -uroot -p'
```

local-data/rollback copy 확인이 필요할 때만 compose `mysql` 서비스를 본다.

```bash
cd /opt/clueroom/app
# [prod] local-data/rollback copy 확인용이다. 운영 source of truth는 data 서버 MySQL이다.
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
du -sh /opt/clueroom/backups/mysql 2>/dev/null || true
# [로컬 PC] `clueroom-data` alias는 로컬 SSH config 기준이다.
ssh clueroom-data 'du -sh /opt/clueroom-data/backups/mysql 2>/dev/null || true'
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
| `infra/SECURITY_TRAFFIC_ALERT_POLICY.md` | Rate limit, GeoIP/Bot traffic, Grafana alert 정책 |
| `infra/CLUEROOM_SECRET_INPUT_GUIDE.md` | 운영 secret 입력/ACL/검증 절차 |
| `CaseLab_AI_PRD.md` | 제품 범위, MVP 우선순위 |
| `CaseLab_AI_API_Spec.md` | API Request/Response |
| `frontend/CLUEROOM_APP_FLOW_API_GUIDE.md` | Android/Frontend 화면별 API 매핑 |
| `BACKEND_IMPLEMENTATION_GUIDE.md` | 백엔드 구현 규칙 |
| `AI_NPC_PROMPT_POLICY.md` | AI 프롬프트 정책 |
