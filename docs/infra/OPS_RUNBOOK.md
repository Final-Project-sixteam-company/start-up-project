# ClueRoom 운영 명령어 & 장애 대응 Runbook

> 목적: ClueRoom 운영 서버를 유지보수하면서 자주 쓰는 명령어, Blue-Green 배포, CD 후 정리 자동화, 롤백, 로그 확인, 백업/복구, 장애 대응 순서를 빠르게 확인하기 위한 운영 메모입니다.
> 운영 서버 기준 경로는 `/opt/clueroom`입니다.
> 운영/인프라 Agent에게 서버 상태를 전달할 때는 `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md`의 Ops Snapshot Contract를 따릅니다.
> Agent 기반 운영 분석과 자동 조치 제안은 `docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md`의 승인 정책을 따릅니다.

---

## 0. 기본 접속 정보

### 서버 접속

```bash
ssh clueroom
```

### 주요 URL

```text
API Base URL:
https://api.clueroom.xyz

Swagger:
https://api.clueroom.xyz/swagger-ui.html
https://api.clueroom.xyz/swagger-ui/index.html

Health Check:
https://api.clueroom.xyz/actuator/health
```

> `http://api.clueroom.xyz` 요청은 HTTPS로 리다이렉트된다.

---

## 1. 운영 서버 주요 경로

```text
/opt/clueroom/app
→ 백엔드 프로젝트 clone 위치

/opt/clueroom/deploy.sh
→ Blue-Green 배포 스크립트

/opt/clueroom/bg-compose
→ Blue-Green Docker Compose helper

/opt/clueroom/bg-status.sh
→ 현재 active / standby 슬롯 확인 스크립트

/opt/clueroom/stop-standby.sh
→ 현재 active가 아닌 standby 슬롯만 자동 중지하는 스크립트

/opt/clueroom/rollback-bluegreen.sh
→ 현재 active 반대편 슬롯으로 자동 롤백하는 스크립트

/opt/clueroom/backup-mysql.sh
→ local-data / rollback copy MySQL 백업 스크립트

/opt/clueroom/backups/mysql
→ MySQL 백업 파일 저장 위치

External-data 운영에서는 data server MySQL이 source of truth다.
`backup-mysql.sh`는 compose `mysql` 서비스가 있는 local-data/rollback copy 백업용이며, 운영 source of truth 백업은 이 runbook의 external-data 백업 절차를 따른다.

/opt/clueroom/backups/env
→ .env 백업 파일 이동 위치

/opt/clueroom/logs
→ 운영 로그 보관 위치

/opt/clueroom/secrets
→ Firebase service account 등 서버 secret 저장 위치

/opt/clueroom/secrets/env.d
→ 팀원/기능별 secret env 파일 저장 위치
```

### 팀원/기능별 secret 파일

```text
/opt/clueroom/secrets/env.d/ai.env
→ AI API Key / DeepSeek 설정

/opt/clueroom/secrets/env.d/portone.env
→ PortOne 결제 Secret

/opt/clueroom/secrets/env.d/oauth.env
→ OAuth / JWT Secret
```

### 레포 원본 위치와 서버 설치 위치

레포에는 운영 스크립트 원본이 `scripts/` 아래에 있다.

```text
scripts/deploy-bluegreen.sh
scripts/bg-compose.sh
scripts/bg-status.sh
scripts/stop-standby.sh
scripts/rollback-bluegreen.sh
scripts/backup-mysql.sh
```

서버 운영 위치는 `/opt/clueroom`이다.

```text
/opt/clueroom/deploy.sh
/opt/clueroom/bg-compose
/opt/clueroom/bg-status.sh
/opt/clueroom/stop-standby.sh
/opt/clueroom/rollback-bluegreen.sh
/opt/clueroom/backup-mysql.sh
```

PR merge 후 서버에 반영할 때는 레포의 `scripts/*.sh`를 `/opt/clueroom` 운영 위치로 복사한다.

external-data cutover 이후 `/opt/clueroom/deploy.sh`와 `/opt/clueroom/bg-compose`는 아래 compose 조합을 기본으로 사용한다.

```text
docker-compose.yml
docker-compose.external-data.yml
docker-compose.bluegreen.yml
docker-compose.bluegreen.external-data.yml
```

### Auth/JWT schema 반영

Auth 1단계 배포 전 운영 DB에는 아래 migration을 먼저 적용한다.

```bash
cd /opt/clueroom/app
mysql -h 172.26.1.185 -u <user> -p <database> < docs/db/migrations/20260609_add_auth_jwt_schema.sql
```

검증:

```bash
mysql -h 172.26.1.185 -u <user> -p <database> \
  -e "SHOW TABLES LIKE 'users'; SHOW TABLES LIKE 'user_oauth_accounts'; SHOW TABLES LIKE 'auth_refresh_tokens';"
```

운영 env는 `/opt/clueroom/secrets/env.d/oauth.env` 등 secret env로만 주입한다.

```text
JWT_SECRET
JWT_ISSUER
JWT_ACCESS_TOKEN_TTL_SECONDS
JWT_REFRESH_TOKEN_TTL_DAYS
AUTH_DEV_LOGIN_ENABLED
AUTH_MOCK_FALLBACK_ENABLED
AUTH_REQUIRE_AUTHENTICATION
AUTH_ADMIN_SEED_ENABLED
AUTH_ADMIN_SEED_EMAIL
AUTH_ADMIN_SEED_NICKNAME
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_IDS
KAKAO_APP_ID
```

`AUTH_REQUIRE_AUTHENTICATION=true`, `AUTH_DEV_LOGIN_ENABLED=true`, `GOOGLE_CLIENT_ID(S)` 또는 `KAKAO_APP_ID`가 설정된 상태에서 `JWT_SECRET`이 비어 있거나 32자 미만이면 앱은 부팅 단계에서 실패한다.

1단계에서는 기존 API 호환을 위해 `AUTH_REQUIRE_AUTHENTICATION=false`, `AUTH_MOCK_FALLBACK_ENABLED=true`를 유지한다.
Android가 OAuth login과 Bearer token 첨부를 완료한 뒤 `AUTH_REQUIRE_AUTHENTICATION=true`로 전환한다.
보호 모드에서는 명시 public endpoint를 제외한 `/api/**`가 기본 인증 대상이다.
OAuth email 기반 기존 계정 연결은 provider verified email에만 허용한다.
보호 모드에서는 `AUTH_MOCK_FALLBACK_ENABLED=true`가 남아 있어도 token 없는 요청에 `MOCK_USER_ID`를 부여하지 않는다.
CORS preflight `OPTIONS` 요청은 인증 없이 통과해야 한다.
AI rate limit 검증용 admin 계정은 `AUTH_ADMIN_SEED_*` 값으로만 생성/승격한다. 실제 admin email은 서버 secret env에만 저장하고 공개 문서/PR에 기록하지 않는다.

Admin seed 설정 예:

```text
AUTH_ADMIN_SEED_ENABLED=true
AUTH_ADMIN_SEED_EMAIL=<server-secret-admin-email>
AUTH_ADMIN_SEED_NICKNAME=ClueRoom Admin
```

Admin seed 검증:

```bash
mysql -h 172.26.1.185 -u <user> -p <database> \
  -e "SELECT id, role, status FROM users WHERE email = '<server-secret-admin-email>';"
```

기대:

```text
role=ADMIN
status=ACTIVE
```

Admin seed rollback:

```text
1. AUTH_ADMIN_SEED_ENABLED=false 로 되돌리고 Blue-Green 재배포한다.
2. 잘못 승격한 계정이 있으면 운영 DB 백업과 승인 후 role을 USER로 되돌린다.
3. auth_refresh_tokens/user_oauth_accounts/users 테이블 삭제는 하지 않는다.
```

수동 demotion이 승인된 경우에만 실행:

```sql
UPDATE users
SET role = 'USER'
WHERE email = '<server-secret-admin-email>'
  AND role = 'ADMIN';
```

보호 모드 검증:

```bash
curl -i https://api.clueroom.xyz/api/play-sessions/active?scenarioId=1
```

기대:

```text
HTTP/1.1 401
code C003
```

보호 모드 Rollback:

```text
AUTH_REQUIRE_AUTHENTICATION=false 로 되돌리고 Blue-Green 재배포한다.
JWT/auth schema는 유지한다.
```

Auth schema Rollback:

```text
앱 배포 직후 auth API를 사용하지 않았고 token 데이터가 없으면 이전 app 슬롯으로 Blue-Green rollback한다.
auth_refresh_tokens/user_oauth_accounts/users 테이블 삭제는 운영 백업과 승인 후에만 수행한다.
```

---

## 2. 절대 하지 말 것

아래 명령어는 데이터 삭제, secret 노출, 운영 장애 가능성이 있으므로 함부로 실행하지 않는다.

```bash
docker compose down -v
```

```bash
git clean -fdx
```

```bash
rm -rf /opt/clueroom
```

```bash
cat /opt/clueroom/app/.env
```

```bash
cat /opt/clueroom/secrets/firebase-service-account.json
```

```bash
cat /opt/clueroom/secrets/env.d/ai.env
cat /opt/clueroom/secrets/env.d/portone.env
cat /opt/clueroom/secrets/env.d/oauth.env
```

```bash
terraform destroy
```

```sql
DROP DATABASE ...
DROP TABLE ...
```

### Secret 확인 원칙

Secret 값은 직접 출력하지 않는다.
항상 `set / empty` 방식으로만 확인한다.

예:

```bash
docker exec start-up-app-green sh -c 'test -n "$OPENAI_API_KEY" && echo OPENAI_API_KEY=set || echo OPENAI_API_KEY=empty'
```

---

## 3. 현재 운영 구조 요약

```text
Client
  ↓
https://api.clueroom.xyz
  ↓
Nginx
  ↓
clueroom_backend upstream
  ├─ app-blue  : 127.0.0.1:8081
  └─ app-green : 127.0.0.1:8082
  ↓
data server 172.26.1.185
  ├─ MySQL
  └─ Redis
S3
FCM
```

prod 서버의 운영 app은 data 서버 MySQL/Redis를 사용한다.
prod local MySQL/Redis는 운영 source of truth가 아니며, 남아 있더라도 rollback/local-data copy 또는 stop-only 정리 대상이다.

현재 active는 아래 명령어로 확인한다.

```bash
curl -I https://api.clueroom.xyz/actuator/health
```

```text
X-ClueRoom-Upstream: 127.0.0.1:8081
→ app-blue active

X-ClueRoom-Upstream: 127.0.0.1:8082
→ app-green active
```

### 운영 보안 hardening 적용 상태

운영 서버에는 아래 hardening을 적용했다.

```text
1. 외부에서 /actuator/prometheus 접근 차단
2. /actuator/health 외 actuator 민감 endpoint 외부 차단
3. /.env, /.git, wp-admin, phpmyadmin 등 봇 스캔 경로 차단
4. legacy app(start-up-app) 중지, Blue-Green 슬롯(app-blue/app-green)만 운영
5. Fail2Ban sshd jail 적용
6. Nginx API per-IP rate limit enforced
7. Nginx per-IP connection limit enforced
8. CN IPv4 CIDR block 적용
9. manual blocklist snippet 준비
```

외부에서 허용되는 actuator endpoint는 health check뿐이다.
Rate Limit, CN block, manual blocklist 정책은 `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md`를 기준으로 운영한다.
현재 운영 Nginx는 dry-run이 아니라 enforcement 상태이며, 403/429는 Grafana/n8n/Slack alert에서 warning으로 본다.
새로운 threshold 변경, 국가 차단 확대, manual blocklist 추가는 증거와 rollback 경로를 확인한 뒤 적용한다.
Grafana Alert 정책은 `docs/infra/SECURITY_TRAFFIC_ALERT_POLICY.md`를 기준으로 한다.

```bash
curl -I https://api.clueroom.xyz/actuator/health
```

차단 확인:

```bash
curl -I https://api.clueroom.xyz/actuator/prometheus
curl -I https://api.clueroom.xyz/actuator/env
curl -I https://api.clueroom.xyz/actuator/beans
curl -I https://api.clueroom.xyz/.env
curl -I https://api.clueroom.xyz/.git/config
curl -I https://api.clueroom.xyz/wp-admin/
curl -I https://api.clueroom.xyz/phpmyadmin/
```

기대:

```text
/actuator/health
→ 200

/actuator/prometheus, /actuator/env, /.env, /.git/*, wp-admin, phpmyadmin
→ 404 또는 403
```

> Prometheus 내부 scrape는 Nginx 외부 URL이 아니라 Docker 내부 또는 localhost 경로를 사용한다.
> 외부 `https://api.clueroom.xyz/actuator/prometheus`는 공개하지 않는다.

Nginx 설정 변경 후 검증:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
curl -I https://api.clueroom.xyz/actuator/prometheus
```

### legacy app 컨테이너 중지 원칙

운영 traffic은 Nginx Blue-Green upstream을 통해 app-blue 또는 app-green 중 하나로만 간다.
기존 단일 app 컨테이너인 `start-up-app`은 legacy이며 운영 traffic 대상이 아니다.

확인:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'start-up-app($|-blue|-green)|NAME'
/opt/clueroom/bg-status.sh
```

정상 기준:

```text
start-up-app-blue 또는 start-up-app-green 중 active 슬롯 1개가 Nginx upstream 대상
standby 슬롯은 배포 직후 rollback 확인 후 stop 가능
start-up-app legacy 컨테이너는 운영에서 중지 상태 유지
```

legacy 컨테이너가 실행 중이면 아래처럼 중지한다.

```bash
cd /opt/clueroom/app
docker compose stop app
```

> `docker compose down` 또는 volume 삭제 명령은 사용하지 않는다.

### Fail2Ban sshd jail 운영

SSH brute-force와 반복 실패 접속을 줄이기 위해 Fail2Ban `sshd` jail을 적용한다.

상태 확인:

```bash
sudo fail2ban-client status
sudo fail2ban-client status sshd
```

ignoreip 확인:

```bash
sudo fail2ban-client get sshd ignoreip
```

특정 IP unban:

```bash
sudo fail2ban-client set sshd unbanip <차단된_IP>
```

특정 IP ban 여부 확인:

```bash
sudo fail2ban-client status sshd | grep -E 'Currently banned|Banned IP list'
```

Fail2Ban 로그 확인:

```bash
sudo journalctl -u fail2ban --since "1 hour ago"
```

SSH 인증 실패 확인:

```bash
sudo journalctl -u ssh --since "1 hour ago"
```

팀원 제한 계정 SSH 안내:

```text
팀원별 제한 계정은 SSH key / username / host를 잘못 입력해 반복 실패하면 Fail2Ban에 의해 ban될 수 있다.
접속이 갑자기 안 되면 무리하게 계속 재시도하지 말고, 본인 public IP를 인프라 담당자에게 전달해 unban 여부를 확인한다.
```

---

## 4. 배포 운영 흐름

### 기본 배포는 GitHub Actions CD 사용

일반 배포는 GitHub Actions에서 실행한다.

```text
GitHub
→ Actions
→ Backend CD
→ Run workflow
→ develop
```

CD는 서버에 SSH 접속해서 아래 스크립트를 실행한다.

```bash
/opt/clueroom/deploy.sh
```

### `/opt/clueroom/deploy.sh`가 하는 일

```text
1. 현재 active upstream 확인
2. develop 최신 코드 pull
3. bootJar 빌드
4. 현재 active 반대편 슬롯을 target으로 선택
5. target 슬롯 app-blue 또는 app-green 재빌드/실행
6. target health check
7. Nginx upstream 전환
8. 외부 health check
9. 실패 시 기존 upstream으로 rollback
10. 성공 시 이전 active 슬롯은 rollback용으로 유지
```

### CD 후 해야 하는 일

CD가 성공하면 바로 standby를 끄지 말고 먼저 상태를 확인한다.

```bash
/opt/clueroom/bg-status.sh
```

정상이고 잠시 확인 후 문제가 없으면 standby 자동 정리 스크립트를 실행한다.

```bash
/opt/clueroom/stop-standby.sh
```

문제가 있으면 rollback 스크립트를 실행한다.

```bash
/opt/clueroom/rollback-bluegreen.sh
```

> 이제 사람이 직접 “8081이면 green 끄고, 8082면 blue 끄고”를 판단하지 않는다.
> `stop-standby.sh`가 현재 active를 자동 판별해서 반대편만 중지한다.

---

## 5. Blue-Green 핵심 명령어

### 현재 active / standby 상태 확인

```bash
/opt/clueroom/bg-status.sh
```

또는 단순 헤더 확인:

```bash
curl -I https://api.clueroom.xyz/actuator/health
```

### Blue-Green Docker Compose helper

```bash
/opt/clueroom/bg-compose ps
```

external-data helper config 검증:

```bash
cd /opt/clueroom/app
/opt/clueroom/bg-compose config > /tmp/bg-compose-external-config.yml

grep -n -A45 -E '^  app-blue:|^  app-green:' /tmp/bg-compose-external-config.yml \
  | grep -E 'app-blue:|app-green:|DB_HOST:|DB_PORT:|REDIS_HOST:|REDIS_PORT:|AI_LLMOPS_DB_LOGGING_ENABLED|depends_on'
```

기대값:

```text
app-blue/app-green DB_HOST: 172.26.1.185
app-blue/app-green DB_PORT: "3306"
app-blue/app-green REDIS_HOST: 172.26.1.185
app-blue/app-green REDIS_PORT: "6379"
app-blue/app-green AI_LLMOPS_DB_LOGGING_ENABLED: "false"
app-blue/app-green 아래 mysql/redis depends_on 없음
prometheus가 legacy app에 depends_on하지 않음
```

현재 실행 컨테이너 env 확인:

```bash
ACTIVE_SERVICE="$(/opt/clueroom/bg-status.sh | awk -F: '/Active service/{gsub(/^[ \t]+|[ \t]+$/, "", $2); print $2}')"
STANDBY_SERVICE="$(/opt/clueroom/bg-status.sh | awk -F: '/Standby service/{gsub(/^[ \t]+|[ \t]+$/, "", $2); print $2}')"

docker exec "start-up-$ACTIVE_SERVICE" printenv | grep -E 'DB_HOST|DB_PORT|REDIS_HOST|REDIS_PORT|AI_LLMOPS_DB_LOGGING_ENABLED'
docker exec "start-up-$STANDBY_SERVICE" printenv | grep -E 'DB_HOST|DB_PORT|REDIS_HOST|REDIS_PORT|AI_LLMOPS_DB_LOGGING_ENABLED'
```

health 확인:

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
curl https://api.clueroom.xyz/actuator/health
```

### Blue 실행

```bash
/opt/clueroom/bg-compose up -d --build app-blue
```

### Green 실행

```bash
/opt/clueroom/bg-compose up -d --build app-green
```

### Blue health check

```bash
curl http://127.0.0.1:8081/actuator/health
```

### Green health check

```bash
curl http://127.0.0.1:8082/actuator/health
```

### Blue 로그 확인

```bash
/opt/clueroom/bg-compose logs -f app-blue
```

또는:

```bash
docker logs -f start-up-app-blue
```

### Green 로그 확인

```bash
/opt/clueroom/bg-compose logs -f app-green
```

또는:

```bash
docker logs -f start-up-app-green
```

---

## 6. CD 후 standby 자동 정리

CD 성공 후에는 아래를 실행한다.

```bash
/opt/clueroom/stop-standby.sh
```

이 스크립트는 다음을 수행한다.

```text
1. 현재 Nginx upstream 확인
2. active가 app-blue면 app-green만 stop
3. active가 app-green이면 app-blue만 stop
4. stop 전 외부 health check
5. stop 후 외부 health check
6. 이상이 있으면 방금 stop한 service를 다시 start
7. 복구 후 외부 health check
```

### 수동으로 standby를 끄지 않는 이유

사람이 직접 아래처럼 판단하면 실수할 수 있다.

```text
X-ClueRoom-Upstream: 127.0.0.1:8081
→ active = app-blue
→ app-green stop 해야 함
```

```text
X-ClueRoom-Upstream: 127.0.0.1:8082
→ active = app-green
→ app-blue stop 해야 함
```

실수로 active를 stop하면 API가 내려갈 수 있다.
따라서 운영에서는 `stop-standby.sh`를 사용한다.

---

## 7. Blue-Green 롤백

새 배포 후 문제가 생기면 아래를 실행한다.

```bash
/opt/clueroom/rollback-bluegreen.sh
```

이 스크립트는 다음을 수행한다.

```text
1. 현재 active upstream 확인
2. 반대편 슬롯을 rollback target으로 선택
3. rollback target 컨테이너 start
4. target health check
5. Nginx upstream을 rollback target으로 전환
6. 외부 health check
7. 실패하면 기존 upstream으로 재복구
```

### 롤백 전제

롤백은 이전 슬롯 컨테이너가 남아 있어야 안전하다.

```text
stop 상태
→ start로 되살릴 수 있음

rm으로 삭제된 상태
→ 같은 버전 그대로 복구가 어려울 수 있음
```

따라서 배포 후 standby는 보통 `stop`만 하고, `rm`은 하지 않는다.

---

## 8. Blue / Green 수동 전환

자동 스크립트를 우선 사용한다.
아래는 비상시 수동 전환용이다.

### Nginx를 Blue로 전환

```bash
sudo sed -i -E 's#127\.0\.0\.1:808[12]#127.0.0.1:8081#g' /etc/nginx/conf.d/clueroom-upstream.conf
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

정상 기대:

```text
X-ClueRoom-Upstream: 127.0.0.1:8081
```

### Nginx를 Green으로 전환

```bash
sudo sed -i -E 's#127\.0\.0\.1:808[12]#127.0.0.1:8082#g' /etc/nginx/conf.d/clueroom-upstream.conf
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

정상 기대:

```text
X-ClueRoom-Upstream: 127.0.0.1:8082
```

---

## 9. 서버 기본 명령어

### 현재 위치 확인

```bash
pwd
```

### 폴더 이동

```bash
cd /opt/clueroom/app
```

### 파일 목록 확인

```bash
ls -al
```

### 파일 내용 확인

```bash
cat 파일명
```

긴 파일은 `less` 사용:

```bash
less 파일명
```

나가기:

```text
q
```

### 파일 편집

```bash
nano 파일명
```

저장:

```text
Ctrl + O
Enter
Ctrl + X
```

### 파일/폴더 생성

```bash
mkdir -p 경로
```

### 파일 복사

```bash
cp 원본 대상
```

### 파일 이동/이름 변경

```bash
mv 원본 대상
```

### 파일 삭제

```bash
rm 파일명
```

> `rm -rf`는 반드시 경로를 확인하고 실행한다.

### 권한 변경

```bash
chmod 600 파일명
chmod +x 스크립트명.sh
```

### 소유자 변경

```bash
sudo chown -R ubuntu:ubuntu 경로
```

---

## 10. 서버 상태 확인

### 메모리 확인

```bash
free -m
```

### 디스크 확인

```bash
df -h
```

### CPU / 프로세스 확인

```bash
top
```

나가기:

```text
q
```

더 보기 좋은 버전:

```bash
htop
```

나가기:

```text
F10
```

또는:

```text
q
```

### 현재 포트 확인

```bash
sudo ss -tulnp
```

### 서버 IP 확인

```bash
ip addr
```

### 현재 접속 사용자 확인

```bash
whoami
```

### 현재 시간 확인

```bash
date
```

---

## 11. Docker / Docker Compose 명령어

### 실행 중인 컨테이너 확인

```bash
docker ps
```

### 중지된 컨테이너까지 확인

```bash
docker ps -a
```

### Docker Compose 서비스 상태 확인

```bash
cd /opt/clueroom/app
docker compose ps
```

### 이미지 확인

```bash
docker images
```

### 컨테이너 로그 확인

```bash
docker logs 컨테이너명
```

예:

```bash
docker logs --tail=200 start-up-app-green
```

실시간 로그:

```bash
docker logs -f start-up-app-green
```

### Compose 로그 확인

```bash
cd /opt/clueroom/app
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

### 컨테이너 리소스 확인

```bash
docker stats
```

나가기:

```text
Ctrl + C
```

### Docker 디스크 사용량 확인

```bash
docker system df
```

### 중지된 컨테이너 정리

```bash
docker container prune
```

확인 물음이 나오면:

```text
y
```

### 안 쓰는 이미지 정리

```bash
docker image prune
```

### 특정 이미지 삭제

```bash
docker rmi 이미지명
```

---

## 12. Nginx 명령어

### Nginx 상태 확인

```bash
sudo systemctl status nginx
```

### Nginx 설정 문법 검사

```bash
sudo nginx -t
```

### Nginx reload

```bash
sudo systemctl reload nginx
```

### Nginx restart

```bash
sudo systemctl restart nginx
```

> 설정 변경 후에는 항상 `sudo nginx -t`를 먼저 실행한다.

### Nginx 전체 설정 확인

```bash
sudo nginx -T
```

### ClueRoom Nginx 설정 확인

```bash
sudo cat /etc/nginx/sites-available/clueroom-api
```

### upstream 설정 확인

```bash
cat /etc/nginx/conf.d/clueroom-upstream.conf
```

---

## 13. Nginx 로그 확인

### Access log

```bash
sudo tail -f /var/log/nginx/access.log
```

나가기:

```text
Ctrl + C
```

### Error log

```bash
sudo tail -f /var/log/nginx/error.log
```

나가기:

```text
Ctrl + C
```

### 최근 에러 80줄

```bash
sudo tail -n 80 /var/log/nginx/error.log
```

### 요청 IP 상위 확인

```bash
sudo awk '{print $1}' /var/log/nginx/access.log | sort | uniq -c | sort -nr | head
```

### 404 많이 발생한 IP 확인

```bash
sudo grep " 404 " /var/log/nginx/access.log | awk '{print $1}' | sort | uniq -c | sort -nr | head
```

---

## 14. HTTPS / Certbot 확인

### 인증서 목록 확인

```bash
sudo certbot certificates
```

### HTTP → HTTPS 리다이렉트 확인

```bash
curl -I http://api.clueroom.xyz/actuator/health
```

정상 기대:

```text
HTTP/1.1 301 Moved Permanently
Location: https://api.clueroom.xyz/actuator/health
```

### HTTPS health 확인

```bash
curl https://api.clueroom.xyz/actuator/health
```

### 인증서 갱신 테스트

```bash
sudo certbot renew --dry-run
```

---

## 15. Prometheus / Grafana 확인

운영 정책:

```text
Grafana:
https://monitor.clueroom.xyz 로 팀원 공유
Nginx HTTPS reverse proxy 뒤에서만 접근
팀원별 Viewer 계정 발급
Admin 계정 공유 금지

Prometheus:
외부 직접 공개 금지
Grafana가 Docker 내부 URL http://prometheus:9090 으로 조회
https://api.clueroom.xyz/actuator/prometheus 외부 접근은 Nginx에서 차단

서버 로그:
인프라 담당자가 SSH로 확인
ops Loki/Alloy는 snapshot 확인 대상
원문 secret/user input이 노출되지 않도록 query와 공유 범위를 제한
```

3000/9090 포트는 운영 서버 방화벽에 열지 않는다. Compose host binding도 `127.0.0.1` 기준으로 유지한다.

### Prometheus health

```bash
curl -s http://localhost:9090/-/healthy
```

정상:

```text
Prometheus Server is Healthy.
```

### Grafana health

```bash
curl -s http://localhost:3000/api/health | jq
```

외부 HTTPS 확인:

```bash
curl -I https://monitor.clueroom.xyz
```

### Prometheus target 확인

```bash
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job:.labels.job, scrapeUrl:.scrapeUrl, health:.health, lastError:.lastError}'
```

### 로컬 PC에서 Prometheus SSH 터널로 접속

로컬 Git Bash 새 창에서 실행한다.

```bash
ssh -N -L 9090:localhost:9090 clueroom
```

이 창은 닫지 않는다.

브라우저에서 접속:

```text
Prometheus:
http://localhost:9090
```

---

## 16. MySQL 백업

백업 정책과 S3 보관 원칙은 `CLUEROOM_INFRASTRUCTURE_STRATEGY.md`의 backup/restore 전략을 따른다.
이 Runbook은 운영자가 실행할 명령어만 관리한다.

### 백업 대상 판정

external-data cutover 이후 운영 기준은 아래처럼 분리한다.

```text
data server MySQL
→ 운영 source of truth
→ 반드시 external-data 백업 명령으로 dump한다.

prod local compose mysql
→ rollback/local-data copy
→ backup-mysql.sh로 백업할 수 있지만 운영 source of truth 백업으로 간주하지 않는다.
```

`backup-mysql.sh`는 `docker compose exec ... mysql`을 사용한다.
따라서 external-data 운영에서 local `mysql` 서비스가 `local-data` profile 뒤로 빠졌거나 rollback copy로만 남아 있으면, 이 스크립트는 실 운영 DB가 아닌 local copy를 백업할 수 있다.

### data server 자동 백업 / S3 업로드 기준

현재 운영 source of truth 백업은 data 서버에서 실행한다.

```text
# host: clueroom-data-01
10 3 * * * /opt/clueroom-data/backup-mysql.sh
20 3 * * * /opt/clueroom-data/upload-mysql-backup-s3.sh
* * * * * /opt/clueroom-data/data-health-push.sh
*/5 * * * * /opt/clueroom-data/s3-backup-health-push.sh
```

수동 확인:

```bash
ssh clueroom-data 'crontab -l | grep -E "backup-mysql|upload-mysql-backup-s3|data-health|s3-backup-health"'
```

백업 성공 기준:

```text
- /opt/clueroom-data/backups/mysql/*.sql.gz 생성
- gzip -t 통과
- .sha256 sidecar 생성
- S3 daily prefix에 .sql.gz와 .sha256 업로드
- s3-upload-state.env의 S3_UPLOAD_STATUS=OK
- S3_BACKUP_HEALTH status=OK가 ops Loki에 push됨
```

### external-data 운영 DB 수동 백업

운영 source of truth를 백업할 때 사용한다.
기본 실행 위치는 data server다.
prod app 서버의 `/opt/clueroom/app` 경로와 `/opt/clueroom/backup-mysql.sh`는 local-data/rollback copy용이므로 source-of-truth 백업 절차의 기본 경로로 쓰지 않는다.

권장: data server에서 직접 실행한다.

```bash
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail

/opt/clueroom-data/backup-mysql.sh

DATE_PATH="$(date +%Y/%m/%d)"
BACKUP_FILE="$(ls -t /opt/clueroom-data/backups/mysql/*.sql.gz | head -n 1)"
BACKUP_DIR="$(dirname "$BACKUP_FILE")"
BACKUP_BASE="$(basename "$BACKUP_FILE")"
test -s "$BACKUP_FILE"
gzip -t "$BACKUP_FILE"
(cd "$BACKUP_DIR" && sha256sum "$BACKUP_BASE" > "$BACKUP_BASE.sha256")
ls -lh "$BACKUP_FILE" "$BACKUP_FILE.sha256"
REMOTE
```

대안: `mysqldump` client가 설치된 app/ops host에서 data server를 원격 dump한다.
이 경로는 app host에 MySQL client가 없으면 사용할 수 없다.
아래 명령은 prod app `.env`의 `APP_DB_HOST`/`APP_DB_PORT`를 우선 사용하고, 없으면 `DB_HOST`/`DB_PORT`를 fallback으로 사용한다.

```bash
cd /opt/clueroom/app
set -euo pipefail
set -a
. ./.env
set +a

TARGET_DB_HOST="${APP_DB_HOST:-${DB_HOST:-}}"
TARGET_DB_PORT="${APP_DB_PORT:-${DB_PORT:-3306}}"
TARGET_DB_USER="${DB_USERNAME:-root}"
TARGET_DB_NAME="${DB_NAME:-startup}"

test -n "$TARGET_DB_HOST"
test -n "${DB_PASSWORD:-}"

TS="$(date +%Y%m%d_%H%M%S)"
DATE_PATH="$(date +%Y/%m/%d)"
BACKUP_DIR="/opt/clueroom/backups/mysql/external"
BACKUP_FILE="${BACKUP_DIR}/${TARGET_DB_NAME}_external_${TS}.sql.gz"

mkdir -p "$BACKUP_DIR"
rm -f "$BACKUP_FILE" "$BACKUP_FILE.sha256"

MYSQL_PWD="$DB_PASSWORD" mysqldump \
  -h "$TARGET_DB_HOST" \
  -P "$TARGET_DB_PORT" \
  -u "$TARGET_DB_USER" \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  "$TARGET_DB_NAME" | gzip > "$BACKUP_FILE"

test -s "$BACKUP_FILE"
gzip -t "$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"
(cd "$BACKUP_DIR" && sha256sum "$(basename "$BACKUP_FILE")" > "$(basename "$BACKUP_FILE").sha256")
ls -lh "$BACKUP_FILE" "$BACKUP_FILE.sha256"
```

### external-data S3 업로드

S3 업로드는 private backup bucket과 전용 IAM principal이 준비된 뒤에만 수행한다.
AWS credential은 git, PR, Slack, AI prompt에 남기지 않는다.

```bash
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail

set -a
. /opt/clueroom-data/secrets/aws-backup.env
set +a

DATE_PATH="$(date +%Y/%m/%d)"
BACKUP_FILE="$(ls -t /opt/clueroom-data/backups/mysql/*.sql.gz | head -n 1)"
BACKUP_DIR="$(dirname "$BACKUP_FILE")"
BACKUP_BASE="$(basename "$BACKUP_FILE")"

test -s "$BACKUP_FILE"
gzip -t "$BACKUP_FILE"
(cd "$BACKUP_DIR" && sha256sum -c "$BACKUP_BASE.sha256")

aws s3 cp "$BACKUP_FILE" \
  "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/${DATE_PATH}/${BACKUP_BASE}" \
  --only-show-errors \
  --server-side-encryption AES256

aws s3 cp "$BACKUP_FILE.sha256" \
  "s3://${S3_BACKUP_BUCKET}/${S3_BACKUP_PREFIX}/daily/${DATE_PATH}/${BACKUP_BASE}.sha256" \
  --only-show-errors \
  --server-side-encryption AES256

aws s3api head-object \
  --bucket "$S3_BACKUP_BUCKET" \
  --key "${S3_BACKUP_PREFIX}/daily/${DATE_PATH}/${BACKUP_BASE}" \
  --query '{Size:ContentLength, LastModified:LastModified}'
REMOTE
```

### cron 확인

운영 서버 또는 data server에서 현재 어떤 DB를 백업하는지 먼저 확인한다.

```bash
crontab -l | grep -E 'backup|mysql|mysqldump' || true
sudo grep -R -nE 'backup|mysql|mysqldump' /etc/cron* 2>/dev/null || true
```

판정:

```text
cron이 /opt/clueroom/backup-mysql.sh를 실행한다
→ local compose mysql 백업이다.
→ external-data 운영 source of truth 백업으로 간주하면 안 된다.

cron이 data server host를 대상으로 mysqldump를 실행한다
→ source of truth 백업 후보가 될 수 있다.
→ 백업 파일, checksum, S3 업로드, restore rehearsal까지 확인한다.
```

external-data 운영 백업 cron 예시:

권장 실행 위치는 data server다.

```cron
# host: clueroom-data-01
10 3 * * * /opt/clueroom-data/backup-mysql.sh >> /opt/clueroom-data/logs/mysql-backup.log 2>&1
20 3 * * * /opt/clueroom-data/upload-mysql-backup-s3.sh >> /opt/clueroom-data/logs/mysql-backup-s3.log 2>&1
```

app/ops host에서 원격 dump를 수행하는 대안 경로는 명시적으로 host label을 붙인다.

```cron
# host: clueroom-api-prod-01 or clueroom-ops-01
0 3 * * * /opt/clueroom/backup-mysql-external.sh >> /opt/clueroom/logs/mysql-backup-external.log 2>&1
```

이 PR은 실제 운영 서버 cron을 변경하지 않는다.
운영 cron이 여전히 `backup-mysql.sh`만 실행 중이면 문서 모순이 아니라 실제 백업 공백이므로 data-server-aware 백업 스크립트로 별도 조치한다.

### local-data / rollback copy 수동 백업

```bash
/opt/clueroom/backup-mysql.sh
```

이 명령은 compose `mysql` 서비스가 대상이다.
external-data 운영 source of truth 백업으로 사용하지 않는다.

### 백업 파일 확인

external-data source of truth 백업 파일은 data server에서 확인한다.

```bash
ssh clueroom-data 'ls -lh /opt/clueroom-data/backups/mysql'
```

local-data / rollback copy 백업 파일은 prod app 서버에서만 아래 경로로 확인한다.

```bash
ls -lh /opt/clueroom/backups/mysql
```

### 백업 로그 확인

현재 `backup-mysql.sh` 자체는 로그 파일을 직접 생성하지 않는다.
아래 로그는 cron redirect를 설정한 경우에만 존재한다.

```bash
tail -f /opt/clueroom/logs/mysql-backup.log
```

---

## 17. MySQL 복구

> 복구는 DB를 덮어쓸 수 있으므로 반드시 신중하게 실행한다.
> 복구 전 현재 source of truth DB를 한 번 더 백업하는 것을 권장한다.
> 운영 DB 직접 복구 전에 rehearsal host 또는 임시 MySQL 컨테이너에서 복구 검증을 먼저 수행한다.

### 복구 전 백업 대상 확인

```bash
cd /opt/clueroom/app
set -a
. ./.env
set +a

TARGET_DB_HOST="${APP_DB_HOST:-${DB_HOST:-}}"
TARGET_DB_PORT="${APP_DB_PORT:-${DB_PORT:-3306}}"
TARGET_DB_USER="${DB_USERNAME:-root}"
TARGET_DB_NAME="${DB_NAME:-startup}"

test -n "$TARGET_DB_HOST"
test -n "$DB_PASSWORD"
echo "restore target: ${TARGET_DB_HOST}:${TARGET_DB_PORT}/${TARGET_DB_NAME}"
```

external-data 운영에서는 위 target이 data server private IP 또는 내부 DNS인지 확인한다.
`mysql` 또는 `localhost`로 나오면 local rollback copy를 복구하는 것이므로 운영 source of truth 복구가 아니다.

### 복구 전 source of truth 재백업

복구 직전에는 section 16의 external-data 백업 명령으로 현재 source of truth를 한 번 더 백업한다.
`backup-mysql.sh`만 실행하면 local copy만 백업할 수 있다.

### external-data 운영 DB 복구 명령어

운영 source of truth에 직접 restore하는 명령이다.
담당자 승인, 쓰기 트래픽 차단 또는 점검창 확보, rehearsal 성공 후에만 실행한다.

아래 primary path는 app/ops host에서 실행한다. 이 절차는 `/opt/clueroom/app/.env`에서 `TARGET_DB_*`와 `DB_PASSWORD`를 읽으므로, data server 로컬 경로를 직접 `BACKUP`으로 쓰지 않는다. data server의 백업 파일은 먼저 app/ops host의 `/tmp/clueroom-restore/`로 복사한다.

```bash
mkdir -p /tmp/clueroom-restore

scp clueroom-data:/opt/clueroom-data/backups/mysql/백업파일명.sql.gz \
  /tmp/clueroom-restore/

scp clueroom-data:/opt/clueroom-data/backups/mysql/백업파일명.sql.gz.sha256 \
  /tmp/clueroom-restore/ 2>/dev/null || true
```

```bash
BACKUP=/tmp/clueroom-restore/백업파일명.sql.gz
BACKUP_DIR="$(dirname "$BACKUP")"
BACKUP_BASE="$(basename "$BACKUP")"

set -o pipefail
test -f "$BACKUP"
gzip -t "$BACKUP"
if [ -f "$BACKUP.sha256" ]; then
  (cd "$BACKUP_DIR" && sha256sum -c "$BACKUP_BASE.sha256")
fi

gunzip -c "$BACKUP" | MYSQL_PWD="$DB_PASSWORD" mysql \
  -h "$TARGET_DB_HOST" \
  -P "$TARGET_DB_PORT" \
  -u "$TARGET_DB_USER" \
  "$TARGET_DB_NAME"
```

### local-data / rollback copy 복구 명령어

compose local `mysql` 서비스에 복구할 때만 사용한다.
external-data 운영 source of truth 복구 명령이 아니다.

```bash
cd /opt/clueroom/app
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
BACKUP=/opt/clueroom/backups/mysql/백업파일명.sql.gz
set -o pipefail
test -f "$BACKUP"
gzip -t "$BACKUP"
gunzip -c "$BACKUP" | \
  docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysql -uroot
```

예:

```bash
cd /opt/clueroom/app
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
BACKUP=/opt/clueroom/backups/mysql/startup_20260523_030000.sql.gz
set -o pipefail
test -f "$BACKUP"
gzip -t "$BACKUP"
gunzip -c "$BACKUP" | \
  docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysql -uroot
```

### 복구 rehearsal 절차

운영 DB를 덮어쓰기 전에 임시 MySQL 컨테이너에서 백업 파일이 복구 가능한지 검증한다.

```bash
BACKUP=/tmp/clueroom-restore/백업파일명.sql.gz
BACKUP_DIR="$(dirname "$BACKUP")"
BACKUP_BASE="$(basename "$BACKUP")"

set -o pipefail
test -f "$BACKUP"
gzip -t "$BACKUP"
if [ -f "$BACKUP.sha256" ]; then
  (cd "$BACKUP_DIR" && sha256sum -c "$BACKUP_BASE.sha256")
fi
```

```bash
docker run -d --name clueroom-restore-check \
  -e MYSQL_ROOT_PASSWORD=restorecheck \
  -e MYSQL_DATABASE=startup \
  mysql:8
```

```bash
sleep 20
set -o pipefail
gzip -t "$BACKUP"
gunzip -c "$BACKUP" | \
  docker exec -i clueroom-restore-check \
  mysql -uroot -prestorecheck startup
```

```bash
docker exec -i clueroom-restore-check \
  mysql -uroot -prestorecheck -e "SHOW TABLES;" startup
```

검증 후 정리:

```bash
docker rm -f clueroom-restore-check
```

복구 rehearsal에서 확인할 것:

```text
- gzip 파일이 정상 해제되는가?
- SQL import가 중간에 실패하지 않는가?
- 주요 테이블이 존재하는가?
- 운영 DB에 직접 넣기 전에 백업 파일 경로가 맞는가?
```

---

## 18. Nginx Rate Limit / IP Block 운영

정책 기준은 `SECURITY_TRAFFIC_ALERT_POLICY.md`를 따른다.
현재 운영 Nginx는 rate limit enforcement 상태다. Dry-run은 신규 threshold 검증이나 rollback 시 참고하는 절차이며, 현재 기본 상태가 아니다.

### 현재 방어 구조

```text
Client
  ↓
Nginx
  ├─ CN IPv4 block
  ├─ manual blocklist
  ├─ API per-IP rate limit
  ├─ per-IP connection limit
  └─ upstream app-blue/app-green
```

### 현재 설정 요약

```text
limit_req_zone clueroom_api_per_ip: 20r/s
burst=60 nodelay
limit_req_dry_run off
limit_req_status 429
limit_conn per IP: 30
CN IPv4 CIDR block 적용
manual blocklist snippet 준비
```

### 설정 확인

```bash
ssh clueroom
sudo nginx -T 2>/dev/null | grep -nE 'limit_req_zone|limit_conn_zone|limit_req_dry_run|limit_req zone|limit_conn|limit_req_status|clueroom-blocked-ips|clueroom-cn|clueroom_is_cn_ip'
```

기대:

```text
limit_req_dry_run off
limit_req zone=clueroom_api_per_ip burst=60 nodelay
limit_conn clueroom_conn_per_ip 30
geo $clueroom_is_cn_ip
include /etc/nginx/snippets/clueroom-cn-block.conf
include /etc/nginx/snippets/clueroom-blocked-ips.conf
```

dry-run이 켜져 있으면 현재 운영 기준과 다르다.

```bash
sudo nginx -T 2>/dev/null | grep 'limit_req_dry_run on' || echo "dry-run off confirmed"
```

### Health / 정상 요청 확인

```bash
curl -I https://api.clueroom.xyz/actuator/health
curl https://api.clueroom.xyz/actuator/health
```

반복 health smoke:

```bash
for i in $(seq 1 30); do
  curl -s -o /dev/null -w "%{http_code}\n" https://api.clueroom.xyz/actuator/health
done | sort | uniq -c
```

기대:

```text
30 200
```

### 403 / 429 의미

```text
403
→ 요청 차단
→ CN block, manual blocklist, 민감 경로 차단 등이 원인

429
→ rate limit
→ 짧은 시간에 너무 많은 요청을 제한
```

주의:

```text
rate limit은 자동 ban이 아니다.
스캐너가 다시 요청하면 제한 범위 안에서는 재시도 가능하다.
403/429 증가는 보통 방어가 작동 중이라는 의미지만, 정상 사용자 영향 여부를 확인해야 한다.
```

### 403 / 429 Loki 확인

ops 서버:

```bash
ssh clueroom-ops
```

최근 403/429:

```bash
curl -G -s "http://127.0.0.1:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="nginx", instance="clueroom-api-prod-01", log_type="access"} |~ ` HTTP/[0-9.]+" (403|429) ` ' \
  --data-urlencode 'limit=100' \
  --data-urlencode 'direction=backward' \
| jq -r '.data.result[] as $s | $s.values[] | "\((.[0][0:10] | tonumber | strftime("%Y-%m-%d %H:%M:%S"))) \(.[1])"'
```

IP별 집계:

```bash
curl -G -s "http://127.0.0.1:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="nginx", instance="clueroom-api-prod-01", log_type="access"} |~ ` HTTP/[0-9.]+" (403|429) ` ' \
  --data-urlencode 'limit=200' \
  --data-urlencode 'direction=backward' \
| jq -r '.data.result[] as $s | $s.values[] | .[1]' \
| awk '{print $1}' \
| sort \
| uniq -c \
| sort -nr \
| head -n 20
```

### 민감 경로 스캔 확인

```bash
curl -G -s "http://127.0.0.1:3100/loki/api/v1/query_range" \
  --data-urlencode 'query={job="nginx", instance="clueroom-api-prod-01", log_type="access"} |~ `([.]env|[.]git|wp-admin|wp-login|phpmyadmin|pma|terraform[.]tfvars)`' \
  --data-urlencode 'limit=100' \
  --data-urlencode 'direction=backward'
```

### CN block 확인

CN block 설정 파일:

```text
/etc/nginx/conf.d/clueroom-cn-geo.conf
/etc/nginx/snippets/clueroom-cn-block.conf
/etc/nginx/geoip/cn-aggregated.map
```

특정 IP가 CN map에 포함되는지 확인:

```bash
python3 - << 'PY'
import ipaddress

ips = [
    "67.205.139.199",
    "106.75.184.142",
]

nets = []
with open("/etc/nginx/geoip/cn-aggregated.map") as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        cidr = line.split()[0]
        nets.append(ipaddress.ip_network(cidr, strict=False))

for ip in ips:
    addr = ipaddress.ip_address(ip)
    print(ip, "CN_MATCH=", any(addr in net for net in nets))
PY
```

### Manual blocklist 추가

반복 악성 IP가 명확한 경우에만 추가한다.

```bash
sudo cp /etc/nginx/snippets/clueroom-blocked-ips.conf \
  /etc/nginx/snippets/clueroom-blocked-ips.conf.before-manual-blocklist-change-$(date +%Y%m%d_%H%M%S)

sudo nano /etc/nginx/snippets/clueroom-blocked-ips.conf
```

예:

```nginx
deny 67.205.139.199;
```

적용:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

주의:

```text
팀원 IP 또는 정상 사용자 IP를 넣지 않는다.
일회성 스캐너는 굳이 수동 ban하지 않는다.
```

### Manual blocklist 회수 / 복구

정상 사용자 IP를 잘못 차단했거나 오탐이 의심되면 먼저 해당 IP만 제거한다.

```bash
BAD_IP="203.0.113.10"
sudo cp /etc/nginx/snippets/clueroom-blocked-ips.conf \
  /etc/nginx/snippets/clueroom-blocked-ips.conf.before-manual-blocklist-unblock-$(date +%Y%m%d_%H%M%S)

awk -v ip="$BAD_IP" '$0 != "deny " ip ";" { print }' \
  /etc/nginx/snippets/clueroom-blocked-ips.conf \
| sudo tee /etc/nginx/snippets/clueroom-blocked-ips.conf.tmp > /dev/null
sudo mv /etc/nginx/snippets/clueroom-blocked-ips.conf.tmp \
  /etc/nginx/snippets/clueroom-blocked-ips.conf

sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

여러 줄을 잘못 수정했거나 즉시 원복이 필요하면 백업 파일을 복구한다.

```bash
ls -al /etc/nginx/snippets | grep 'clueroom-blocked-ips.conf.before-manual-blocklist' || true

BLOCKLIST_BACKUP=/etc/nginx/snippets/clueroom-blocked-ips.conf.before-manual-blocklist-change-YYYYMMDD_HHMMSS
test -f "$BLOCKLIST_BACKUP"
sudo cp "$BLOCKLIST_BACKUP" /etc/nginx/snippets/clueroom-blocked-ips.conf

sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

### CN blocklist 업데이트

수동 실행:

```bash
/opt/clueroom/update-cn-blocklist.sh
```

cron 확인:

```bash
crontab -l | grep update-cn-blocklist
```

예상:

```text
30 4 * * 1 /opt/clueroom/update-cn-blocklist.sh
```

로그:

```bash
tail -n 100 /opt/clueroom/logs/cn-block-update.log
```

### Rate Limit 완화

정상 사용자가 429를 많이 받으면 threshold를 완화한다. 변경 전에는 현재 설정을 백업한다.

```bash
sudo cp /etc/nginx/sites-available/clueroom-api \
  /etc/nginx/sites-available/clueroom-api.before-rate-limit-change-$(date +%Y%m%d_%H%M%S)

sudo cp /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf \
  /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf.before-rate-limit-change-$(date +%Y%m%d_%H%M%S) 2>/dev/null || true
```

snippet을 수정한다.

```bash
sudo nano /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf
```

예:

```nginx
limit_req zone=clueroom_api_per_ip burst=100 nodelay;
limit_req_dry_run off;
limit_req_status 429;
```

검증:

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

### 긴급 rollback

백업 파일을 먼저 찾는다.

```bash
ls -al /etc/nginx/sites-available | grep -E 'before-rate-limit|clueroom-api.*bak' || true
ls -al /etc/nginx/snippets | grep -E 'before-rate-limit|clueroom-api-rate-limit-dryrun.*bak' || true
sudo find /etc/nginx -maxdepth 3 -type f -name '*before-rate-limit*' -print
```

백업 파일명은 실제 출력값으로 교체한다.

```bash
SITE_BACKUP=/etc/nginx/sites-available/clueroom-api.before-rate-limit-change-YYYYMMDD_HHMMSS
SNIPPET_BACKUP=/etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf.before-rate-limit-change-YYYYMMDD_HHMMSS

test -f "$SITE_BACKUP"
sudo cp "$SITE_BACKUP" /etc/nginx/sites-available/clueroom-api

if [ -f "$SNIPPET_BACKUP" ]; then
  sudo cp "$SNIPPET_BACKUP" /etc/nginx/snippets/clueroom-api-rate-limit-dryrun.conf
else
  echo "optional snippet backup not found; site config rollback will continue"
fi
```

검증 후 reload한다.

```bash
sudo nginx -t
sudo systemctl reload nginx
curl -I https://api.clueroom.xyz/actuator/health
```

rate limit 자체를 일시 중단해야 하는 경우에는 snippet include 또는 `limit_req` 라인을 제거/주석 처리한다.
그 변경은 정상 사용자 영향이 큰 경우에만 수행하고, 변경 전후 403/429와 health를 확인한다.
---

## 19. Git 상태 확인

### 서버 레포 상태 확인

```bash
cd /opt/clueroom/app
git status --short
```

### 원격 최신 반영

현재는 CD 또는 deploy.sh가 배포를 담당한다.
서버에서 직접 commit/push하지 않는다.

### 서버 레포를 origin/develop과 맞춰야 할 때

먼저 백업이 필요하다. 바로 실행하지 말고 상황을 확인한다.

```bash
git fetch origin develop
```

위험한 명령:

```bash
git reset --hard origin/develop
```

이 명령은 tracked 파일의 서버 수정사항을 날린다.
실행 전 반드시 백업하고 확인한다.

절대 함부로 실행하지 말 것:

```bash
git clean -fdx
```

---

## 20. 자주 생기는 문제와 대응

### 문제 1. Swagger가 열리지 않음

확인:

```bash
curl -I https://api.clueroom.xyz/swagger-ui.html
curl -I https://api.clueroom.xyz/swagger-ui/index.html
curl -I https://api.clueroom.xyz/v3/api-docs
curl -I https://api.clueroom.xyz/actuator/health
```

Nginx 확인:

```bash
sudo nginx -t
sudo tail -n 80 /var/log/nginx/error.log
```

App 확인:

```bash
docker ps
docker logs --tail=200 start-up-app-green
docker logs --tail=200 start-up-app-blue
```

---

### 문제 2. Swagger가 http로 API를 호출함

증상:

```text
Swagger 페이지는 https인데, Try it out 요청이 http://api.clueroom.xyz 로 나감
```

확인:

```bash
sudo nginx -T | grep -A20 -B5 "proxy_pass"
```

Nginx 설정에 아래가 있어야 한다.

```nginx
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host $host;
proxy_set_header X-Forwarded-Port $server_port;
```

백엔드 설정에는 아래가 있어야 한다.

```yaml
server:
  forward-headers-strategy: ${SERVER_FORWARD_HEADERS_STRATEGY:framework}
```

수정 후:

```bash
sudo nginx -t
sudo systemctl reload nginx
/opt/clueroom/deploy.sh
```

---

### 문제 3. 502 Bad Gateway

의미:

```text
Nginx는 살아 있지만 upstream app으로 연결하지 못함
```

현재 upstream 확인:

```bash
curl -I https://api.clueroom.xyz/actuator/health
cat /etc/nginx/conf.d/clueroom-upstream.conf
```

Blue health:

```bash
curl http://127.0.0.1:8081/actuator/health
```

Green health:

```bash
curl http://127.0.0.1:8082/actuator/health
```

컨테이너 확인:

```bash
docker ps
docker logs --tail=200 start-up-app-blue
docker logs --tail=200 start-up-app-green
```

Nginx 로그:

```bash
sudo tail -n 80 /var/log/nginx/error.log
```

---

### 문제 4. App 컨테이너가 계속 재시작됨

확인:

```bash
docker ps -a
docker inspect start-up-app-blue --format='RestartCount={{.RestartCount}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}} Error={{.State.Error}}'
docker inspect start-up-app-green --format='RestartCount={{.RestartCount}} OOMKilled={{.State.OOMKilled}} ExitCode={{.State.ExitCode}} Error={{.State.Error}}'
```

로그:

```bash
docker logs --tail=250 start-up-app-blue
docker logs --tail=250 start-up-app-green
```

메모리 확인:

```bash
free -m
docker stats --no-stream
```

`OOMKilled=true`면 메모리 부족 가능성이 있다.
임시로 모니터링을 끄고 확인할 수 있다.

```bash
docker compose stop prometheus grafana
```

---

### 문제 5. FCM service account 파일을 못 찾음

컨테이너에서 확인:

```bash
docker exec start-up-app-green sh -c 'echo FCM_SERVICE_ACCOUNT_PATH=$FCM_SERVICE_ACCOUNT_PATH; test -f "$FCM_SERVICE_ACCOUNT_PATH" && echo "FCM file OK" || echo "FCM file MISSING"'
```

또는 blue:

```bash
docker exec start-up-app-blue sh -c 'echo FCM_SERVICE_ACCOUNT_PATH=$FCM_SERVICE_ACCOUNT_PATH; test -f "$FCM_SERVICE_ACCOUNT_PATH" && echo "FCM file OK" || echo "FCM file MISSING"'
```

서버 파일 확인:

```bash
ls -al /opt/clueroom/secrets
```

경로 확인:

```bash
grep -n "FCM_SERVICE_ACCOUNT_PATH\|SECRETS_HOST_DIR" /opt/clueroom/app/.env
```

---

### 문제 6. AI Key가 안 들어감

컨테이너에서 set 여부만 확인:

```bash
docker exec start-up-app-green sh -c 'echo SPRING_AI_MODEL_CHAT=$SPRING_AI_MODEL_CHAT; echo OPENAI_BASE_URL=$OPENAI_BASE_URL; echo OPENAI_CHAT_MODEL=$OPENAI_CHAT_MODEL; test -n "$OPENAI_API_KEY" && echo OPENAI_API_KEY=set || echo OPENAI_API_KEY=empty'
```

Secret 파일 존재 확인:

```bash
ls -al /opt/clueroom/secrets/env.d
```

`ai.env`는 직접 cat하지 않는다.

---

### 문제 7. Nginx 재시작 실패

문법 확인:

```bash
sudo nginx -t
```

문제 파일 열기:

```bash
sudo nano /etc/nginx/sites-available/clueroom-api
```

upstream 확인:

```bash
cat /etc/nginx/conf.d/clueroom-upstream.conf
```

---

### 문제 8. 413 Request Entity Too Large

이미지 업로드 시 발생할 수 있다.

Nginx 설정 확인:

```bash
sudo grep -n "client_max_body_size" /etc/nginx/sites-available/clueroom-api
```

현재 설정:

```nginx
client_max_body_size 12M;
```

업로드 크기를 늘리려면 Nginx와 Spring Multipart 설정을 같이 늘려야 한다.

---

### 문제 9. 디스크 부족

확인:

```bash
df -h
docker system df
```

불필요 컨테이너 정리:

```bash
docker container prune
```

안 쓰는 이미지 정리:

```bash
docker image prune
```

백업 파일 확인:

```bash
du -sh /opt/clueroom/backups/mysql
ls -lh /opt/clueroom/backups/mysql
```

---

### 문제 10. 메모리 부족

확인:

```bash
free -m
docker stats --no-stream
```

임시 대응:

```bash
docker compose stop prometheus grafana
```

또는 standby app 자동 중지:

```bash
/opt/clueroom/stop-standby.sh
```

---

### 문제 11. 새 배포 후 오류 발생

먼저 현재 상태를 확인한다.

```bash
/opt/clueroom/bg-status.sh
```

새 배포가 문제라고 판단되면 롤백한다.

```bash
/opt/clueroom/rollback-bluegreen.sh
```

롤백 후 상태 확인:

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
```

---

## 21. 장애 발생 시 기본 확인 순서

장애가 나면 아래 순서로 확인한다.

표준 read-only snapshot이 필요하면 PR로 관리되는 스크립트를 운영 위치에 설치한 뒤 사용한다.

```bash
cp /opt/clueroom/app/scripts/ops-snapshot.sh /opt/clueroom/ops-snapshot.sh
chmod +x /opt/clueroom/ops-snapshot.sh
bash -n /opt/clueroom/ops-snapshot.sh
/opt/clueroom/ops-snapshot.sh | tee /tmp/clueroom-ops-snapshot.txt
```

Ops Snapshot v3 기준:

```text
- prod app-blue/app-green DB/Redis target: data server 172.26.1.185
- prod local MySQL/Redis: rollback/local-data copy only, not source of truth
- app container env 출력 범위: DB_HOST, DB_PORT, REDIS_HOST, REDIS_PORT, AI_LLMOPS_DB_LOGGING_ENABLED
- data connectivity: 3306/6379 TCP check only, no password query
- ops Loki ready: http://172.26.15.52:3100/ready
- Alloy: start-up-alloy running 여부와 제한된 warn/error log만 확인
- heartbeat: DATA_HEALTH, SERVER_HEALTH, OPS_HEALTH는 Loki sample이 가능할 때 1~3개만 출력
- Ops Snapshot Agent disk 판단: SERVER_HEALTH/DATA_HEALTH/OPS_HEALTH disk_max_percent가 source of truth
- raw snapshot text의 임의 percentage는 disk 판단 fallback으로만 사용
- prod available-memory WARNING은 즉시 Slack 이벤트가 아니라 정기 리포트/수동 검토용
```

필요하면 실행 시점에만 아래 값을 override한다. 이 값들은 secret이 아니다.

```bash
DATA_HOST=172.26.1.185 \
OPS_LOKI_BASE_URL=http://172.26.15.52:3100 \
/opt/clueroom/ops-snapshot.sh | tee /tmp/clueroom-ops-snapshot.txt
```

Snapshot 출력은 AI 도구나 팀 채팅에 붙이기 전에 secret 값이 없는지 사람이 한 번 확인한다.
`.env` 전체, Firebase JSON, API key, DB password, private key는 snapshot이나 팀 채팅에 붙이지 않는다.

### n8n Workflow 운영 확인

ops 서버의 n8n은 Grafana alert routing, periodic ops status, LLMOps summary, Codex handoff report를 담당한다.
현재 workflow 원문 JSON은 secret-safe 문서가 아니므로 public repo에 커밋하지 않는다.

현재 운영 workflow:

| Workflow | Purpose | Trigger | Expected Output |
|---|---|---|---|
| `ClueRoom - Grafana Alert Router v8 Budgeted Gemini 3.5` | Grafana alert event routing | Grafana webhook | Basic Slack alert first, optional Gemini analysis second |
| `ClueRoom - Ops Snapshot Agent v5 Lite Daily Budget` | Periodic ops health report | Manual / every 24h | Basic ops status first, optional Gemini ops analysis second |
| `ClueRoom - LLMOps Light Monitor v4 Budgeted Gemini 3.5` | Hourly AI_CALL cost/failure/latency summary | Manual / every 1h | Basic LLMOps summary first, optional Gemini analysis second |
| `ClueRoom - Infra Codex Handoff Report v1` | Daily infra review handoff | Manual / every 24h | Slack report for human/Codex review |
| `ClueRoom - LLMOps Codex Handoff Report v2` | Daily LLMOps review handoff | Manual / every 24h | Slack report for human/Codex review |

Check n8n health from the ops server:

```bash
curl -I http://127.0.0.1:5678/
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' | grep -E 'n8n|NAME'
docker logs --tail=120 n8n
```

If the container name is different, confirm it with `docker ps` first.

n8n workflow verification after import/edit:

```text
1. Confirm workflow is active only after reviewing credentials and webhook target.
2. Run manual trigger for non-webhook workflows.
3. Confirm basic Slack message is sent before Gemini analysis.
4. Confirm Gemini failure, timeout, or daily budget skip does not block basic Slack.
5. Confirm Slack output does not include .env values, API keys, DB passwords, private keys, raw user questions, raw AI answers, or scenario spoilers.
6. Confirm Grafana webhook workflow receives only expected alert payloads.
```

Workflow export/backup rule:

```text
- Store workflow backups in a private ops backup location, not the public repo.
- Redact or exclude webhook paths, credential IDs, Slack channel IDs, API URLs with keys, and prompt bodies before sharing.
- Public documentation may include workflow name, trigger type, monitored signal, output type, model name, retry count, and daily budget.
```

Current Gemini fail-soft policy:

```text
Grafana Alert Router: gemini-3.5-flash, daily limit 3, retry once after 70s
LLMOps Light Monitor: gemini-3.5-flash, daily limit 2, retry once after 70s
Ops Snapshot Agent: gemini-2.5-flash-lite, daily limit 1, retry once after 70s
```

Codex handoff workflows create reports only.
They do not grant Codex permission to deploy, rollback, mutate production config, edit secrets, or run destructive commands.

```bash
/opt/clueroom/bg-status.sh
```

```bash
curl -I https://api.clueroom.xyz/actuator/health
```

```bash
cat /etc/nginx/conf.d/clueroom-upstream.conf
```

```bash
docker ps
```

```bash
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8082/actuator/health
```

```bash
docker logs --tail=200 start-up-app-blue
docker logs --tail=200 start-up-app-green
```

```bash
sudo nginx -t
sudo tail -n 80 /var/log/nginx/error.log
```

```bash
free -m
df -h
```

```bash
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail
docker compose ps
/opt/clueroom-data/data-health-push.sh
/opt/clueroom-data/s3-backup-health-push.sh
REMOTE
```

---

## 22. 운영 중 변경 전 체크리스트

배포 또는 설정 변경 전에 확인한다.

```bash
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
docker ps
free -m
df -h
sudo nginx -t
```

DB 변경이 있거나 위험한 작업 전에는 백업한다.

```bash
/opt/clueroom/backup-mysql.sh
```

---

## 23. AI 에이전트에게 맡길 때 주의사항

AI 에이전트가 인프라 명령어를 제안하거나 실행하게 할 때는 아래 원칙을 지킨다.

```text
1. read-only 명령부터 실행한다.
2. sudo, rm, docker compose down, git reset, git clean, terraform apply/destroy는 반드시 사용자 승인 후 실행한다.
3. .env, secret, private key, firebase-service-account.json 내용을 출력하지 않는다.
4. secret 값은 set/empty 여부만 확인한다.
5. 변경 전 백업 명령을 먼저 제안한다.
6. 변경 후 health check와 rollback 방법을 함께 확인한다.
7. 운영 서버에서 commit/push하지 않는다.
8. 서버는 pull/deploy 대상이고, 로컬 레포가 source of truth다.
```

---

## 24. 빠른 명령어 요약

### Health

```bash
curl -I https://api.clueroom.xyz/actuator/health
curl https://api.clueroom.xyz/actuator/health
```

### Deploy

```bash
/opt/clueroom/deploy.sh
```

### CD 후 상태 확인

```bash
/opt/clueroom/bg-status.sh
```

### CD 성공 후 standby 자동 중지

```bash
/opt/clueroom/stop-standby.sh
```

### 문제 발생 시 rollback

```bash
/opt/clueroom/rollback-bluegreen.sh
```

### Docker

```bash
docker ps
docker ps -a
docker stats
docker system df
```

### Logs

```bash
docker logs --tail=200 start-up-app-blue
docker logs --tail=200 start-up-app-green
sudo tail -n 80 /var/log/nginx/error.log
```

### Nginx

```bash
sudo nginx -t
sudo systemctl reload nginx
cat /etc/nginx/conf.d/clueroom-upstream.conf
```

### Blue-Green

```bash
/opt/clueroom/bg-status.sh
/opt/clueroom/bg-compose ps
/opt/clueroom/stop-standby.sh
/opt/clueroom/rollback-bluegreen.sh
```

### Backup

```bash
ssh clueroom-data 'bash -se' << 'REMOTE'
set -euo pipefail
/opt/clueroom-data/backup-mysql.sh
/opt/clueroom-data/upload-mysql-backup-s3.sh
cat /opt/clueroom-data/backups/mysql/s3-upload-state.env
REMOTE
```

### Resource

```bash
free -m
df -h
sudo ss -tulnp
```
