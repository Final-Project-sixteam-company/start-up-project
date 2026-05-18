# Docker 실행 문서

<!-- README가 길어지지 않도록 Docker 실행과 문제 해결 절차를 이 문서에 모아둔다. -->

이 문서는 `scripts/compose-up.sh` 또는 IntelliJ Run Configuration 실행 중 에러가 났을 때 확인하는 용도입니다.

## 빠른 실행

프로젝트 루트에서 실행합니다.

```bash
bash scripts/compose-up.sh
```

종료:

```bash
bash scripts/compose-down.sh
```

실행 후 확인:

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- MySQL: `localhost:33306`
- Redis: `localhost:16379`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Docker 내부 DB: `mysql:3306 / startup`
- Docker 내부 Redis: `redis:6379`

## 클론 후 더 편한 실행 방법

수동 Shell Script Run Configuration을 꼭 만들 필요는 없습니다.

이 프로젝트는 Gradle task를 제공합니다.

```bash
./gradlew composeUp
./gradlew composeDown
./gradlew composePs
./gradlew composeLogs
```

Windows PowerShell에서는:

```powershell
.\gradlew.bat composeUp
.\gradlew.bat composeDown
```

IntelliJ에서는 Gradle 창에서 `Tasks > docker > composeUp`을 실행하면 됩니다. `.run` 폴더에 Gradle Run Configuration도 같이 커밋되어 있으므로, IntelliJ가 인식하면 상단 실행 목록에서 `composeUp` / `composeDown`을 바로 선택할 수 있습니다.

## Shell Script Run Configuration 수동 등록

사진처럼 Shell Script 방식으로 직접 등록하려면 `compose-up`, `compose-down` 두 개를 각각 만듭니다.

공통 진입:

1. 상단 메뉴 `Run > Edit Configurations...`
2. 왼쪽 상단 `+`
3. `Shell Script` 선택

`compose-up` 설정:

| 항목 | 값 |
| --- | --- |
| Name | `compose-up` |
| Script path | `scripts/compose-up.sh` |
| Working directory | `$ProjectFileDir$` |
| Interpreter path (Windows) | `C:\Program Files\Git\bin\bash.exe` |

`compose-down` 설정:

| 항목 | 값 |
| --- | --- |
| Name | `compose-down` |
| Script path | `scripts/compose-down.sh` |
| Working directory | `$ProjectFileDir$` |
| Interpreter path (Windows) | `C:\Program Files\Git\bin\bash.exe` |

Git Bash 경로가 다르면 PowerShell에서 확인합니다.

```powershell
where.exe bash
```

## 스크립트가 하는 일

`compose-up.sh`:

1. 프로젝트 루트로 이동합니다.
2. `./gradlew bootJar`로 실행 가능한 jar를 만듭니다.
3. `docker compose up -d --build`로 MySQL, Redis, 앱 컨테이너를 띄웁니다.
4. `docker compose ps`로 상태를 보여줍니다.

`compose-down.sh`:

1. 프로젝트 루트로 이동합니다.
2. `docker compose down`으로 컨테이너를 종료합니다.

## 에러가 날 때 확인할 것

### 1. `bootJar` 단계에서 실패

증상:

```text
BUILD FAILED
Compilation failed
```

원인:

- Java 컴파일 오류
- Gradle 의존성 다운로드 실패
- jar 생성 전 컴파일 단계에서 막힘

확인:

```bash
./gradlew bootJar
```

Windows PowerShell:

```powershell
.\gradlew.bat bootJar
```

컴파일 에러를 먼저 고친 뒤 다시 실행합니다.

### 2. Dockerfile의 `COPY build/libs/*.jar app.jar`에서 실패

증상:

```text
COPY build/libs/*.jar app.jar
no source files were specified
```

원인:

- `bootJar`가 성공하지 않아서 `build/libs/*.jar`가 없음
- 프로젝트 루트가 아닌 위치에서 직접 `docker compose up --build`를 실행함
- 스크립트를 거치지 않고 Docker 빌드만 먼저 실행함

확인:

```bash
ls build/libs
```

해결:

```bash
bash scripts/compose-up.sh
```

### 3. Docker가 실행 중이 아님

증상:

```text
Cannot connect to the Docker daemon
```

해결:

- Docker Desktop을 실행합니다.
- Docker Desktop이 완전히 켜진 뒤 다시 실행합니다.

확인:

```bash
docker version
docker compose version
```

### 4. 포트 충돌

증상:

```text
Bind for 0.0.0.0:8080 failed: port is already allocated
Bind for 0.0.0.0:33306 failed: port is already allocated
Bind for 0.0.0.0:16379 failed: port is already allocated
Bind for 0.0.0.0:9090 failed: port is already allocated
Bind for 0.0.0.0:3000 failed: port is already allocated
```

원인:

- 8080: 다른 Spring 앱이 이미 실행 중
- 33306: 다른 MySQL 컨테이너나 로컬 프로세스가 사용 중
- 16379: 다른 Redis 컨테이너나 로컬 Redis가 사용 중
- 9090: 다른 Prometheus가 사용 중
- 3000: 다른 Grafana나 프론트 개발 서버가 사용 중

Windows 확인:

```powershell
netstat -ano | findstr :8080
netstat -ano | findstr :33306
netstat -ano | findstr :16379
netstat -ano | findstr :9090
netstat -ano | findstr :3000
taskkill /PID <PID> /F
```

macOS/Linux 확인:

```bash
lsof -i :8080
lsof -i :33306
lsof -i :16379
lsof -i :9090
lsof -i :3000
kill -9 <PID>
```

포트를 바꾸고 싶으면 `.env`에 값을 넣습니다.

```properties
SERVER_PORT=8081
MYSQL_HOST_PORT=33307
REDIS_HOST_PORT=16380
PROMETHEUS_HOST_PORT=9091
GRAFANA_HOST_PORT=3001
```

### 5. MySQL 컨테이너가 healthy가 되지 않음

증상:

```text
container start-up-mysql is unhealthy
```

확인:

```bash
docker compose logs mysql
```

자주 나는 원인:

- 기존 MySQL 볼륨에 예전 비밀번호/DB 상태가 남아 있음
- `DB_PASSWORD` 값을 바꾼 뒤 기존 볼륨을 그대로 사용함

로컬 데이터가 필요 없으면 볼륨까지 삭제하고 다시 띄웁니다.

```bash
docker compose down -v
bash scripts/compose-up.sh
```

### 6. 앱 컨테이너가 DB 연결 실패로 종료됨

증상:

```text
Communications link failure
Access denied for user
```

확인:

```bash
docker compose logs app
docker compose logs mysql
```

기본 Docker DB 설정:

- host: `mysql`
- port: `3306`
- database: `startup`
- username: `root`
- password: `.env`의 `DB_PASSWORD`, 없으면 `12345678`

`.env`를 바꿨는데 계속 실패하면 기존 볼륨의 MySQL 계정 정보와 달라졌을 수 있으니 `docker compose down -v` 후 다시 실행합니다.

### 7. Redis 연결 또는 Redisson 락 사용 중 실패

증상:

```text
Unable to connect to Redis server
Redis connection failed
```

확인:

```bash
docker compose logs redis
docker compose logs app
docker compose ps
```

기본 Docker Redis 설정:

- host: `redis`
- port: `6379`
- password: `.env`의 `REDIS_PASSWORD`, 없으면 없음

로컬에서 앱만 IntelliJ로 실행하고 Redis는 Docker로 띄우는 경우에는 `.env`에 아래 값이면 됩니다.

```properties
REDIS_HOST=localhost
REDIS_PORT=16379
```

### 8. Spring AI를 OpenAI로 켰는데 부팅 또는 호출이 실패함

기본값은 AI 모델을 만들지 않는 `none`입니다. 이 상태에서는 `OPENAI_API_KEY`가 없어도 앱이 부팅됩니다.

OpenAI를 실제로 쓰려면 `.env`에 아래처럼 넣습니다.

```properties
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=gpt-4o-mini
OPENAI_CHAT_TEMPERATURE=0.4
```

Docker 실행도 같은 `.env` 값을 읽습니다.

키 없이 `SPRING_AI_MODEL_CHAT=openai`만 켜면 OpenAI ChatModel 생성 또는 실제 호출 단계에서 실패할 수 있습니다. 로컬 기본 개발에서는 `SPRING_AI_MODEL_CHAT=none`으로 두고, AI 기능을 붙이는 시점에만 `openai`로 전환하는 편이 안전합니다.

### 9. Grafana에 접속했는데 데이터가 안 보임

기본 접속 정보:

- URL: `http://localhost:3000`
- ID: `.env`의 `GRAFANA_ADMIN_USER`, 기본 `admin`
- PW: `.env`의 `GRAFANA_ADMIN_PASSWORD`, 기본 `admin`

Prometheus 데이터소스는 자동 등록됩니다.

확인:

```bash
docker compose logs prometheus
docker compose logs grafana
```

Prometheus target 상태도 확인합니다.

```text
http://localhost:9090/targets
```
