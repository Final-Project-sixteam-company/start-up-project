# ClueRoom 운영 명령어 & 장애 대응 Runbook

> 목적: ClueRoom 운영 서버를 유지보수하면서 자주 쓰는 명령어, Blue-Green 배포, CD 후 정리 자동화, 롤백, 로그 확인, 백업/복구, 장애 대응 순서를 빠르게 확인하기 위한 운영 메모입니다.
> 운영 서버 기준 경로는 `/opt/clueroom`입니다.
> 운영/인프라 Agent에게 서버 상태를 전달할 때는 `docs/infra/agent/OPS_SNAPSHOT_SPEC.md`를 따릅니다.
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
→ MySQL 백업 스크립트

/opt/clueroom/backups/mysql
→ MySQL 백업 파일 저장 위치

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
MySQL Docker
Redis Docker
S3
FCM
```

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

서버 로그:
인프라 담당자가 SSH로 확인
추후 필요 시 Loki/Promtail 도입
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

### 수동 백업

```bash
/opt/clueroom/backup-mysql.sh
```

### 백업 파일 확인

```bash
ls -lh /opt/clueroom/backups/mysql
```

### 백업 로그 확인

```bash
tail -f /opt/clueroom/logs/mysql-backup.log
```

### cron 확인

```bash
crontab -l
```

예상:

```cron
0 3 * * * /opt/clueroom/backup-mysql.sh >> /opt/clueroom/logs/mysql-backup.log 2>&1
```

---

## 17. MySQL 복구

> 복구는 DB를 덮어쓸 수 있으므로 반드시 신중하게 실행한다.
> 복구 전 현재 DB를 한 번 더 백업하는 것을 권장한다.

### 복구 전 백업

```bash
/opt/clueroom/backup-mysql.sh
```

### 복구 명령어

```bash
cd /opt/clueroom/app
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
gunzip -c /opt/clueroom/backups/mysql/백업파일명.sql.gz | \
  docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysql -uroot
```

예:

```bash
cd /opt/clueroom/app
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d '=' -f2-)"
gunzip -c /opt/clueroom/backups/mysql/startup_20260523_030000.sql.gz | \
  docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysql -uroot
```

---

## 18. Git 상태 확인

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

## 19. 자주 생기는 문제와 대응

### 문제 1. Swagger가 열리지 않음

확인:

```bash
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

## 20. 장애 발생 시 기본 확인 순서

장애가 나면 아래 순서로 확인한다.

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
docker compose logs --tail=100 mysql
docker compose logs --tail=100 redis
```

---

## 21. 운영 중 변경 전 체크리스트

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

## 22. AI 에이전트에게 맡길 때 주의사항

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

## 23. 빠른 명령어 요약

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
/opt/clueroom/backup-mysql.sh
ls -lh /opt/clueroom/backups/mysql
```

### Resource

```bash
free -m
df -h
sudo ss -tulnp
```
