# ClueRoom 인프라 구축 트러블슈팅 기록

> 목적: ClueRoom 최종 프로젝트 인프라 구축 과정에서 실제로 겪은 문제, 원인, 해결 방법, 재발 방지책을 정리한다.
> 성격: 운영 문서 + 발표/면접용 회고 자료 + 후속 유지보수 기준 문서
> 범위: Blue-Green 배포, external data cutover, S3 백업, Grafana/Loki/n8n 알림, Rate Limit/IP 차단, LLMOps, OAuth/JWT 배포, Terraform scale-out + Nginx 수동 로드밸런싱 PoC

---

## 1. 요약

ClueRoom 인프라는 처음부터 완성형으로 설계된 것이 아니라, 실제 운영 중 발견한 위험을 단계적으로 줄이는 방식으로 발전했다.

초기에는 단일 prod 서버에서 Spring Boot, MySQL, Redis, Grafana/Prometheus가 함께 동작했다. 이후 운영 안정성과 scale-out 가능성을 위해 다음 순서로 구조를 개선했다.

```text
단일 prod 서버
→ Blue-Green 배포
→ data 서버 MySQL/Redis 분리
→ ops 서버 Loki/n8n 분리
→ S3 백업/복구 리허설
→ Grafana Alert + n8n Slack 알림
→ Rate Limit / CN Block / manual blocklist
→ LLMOps 관측
→ Terraform app node scale-out
→ prod 제어형 Nginx 수동 로드밸런싱 PoC
```

구축 중 가장 많이 반복된 문제는 다음 네 가지였다.

```text
1. 서버 실제 상태와 스크립트/문서의 가정 불일치
2. secret/env/file mount 누락
3. 로그/알림 쿼리 또는 Nginx 설정의 작은 파싱 오류
4. 수동 복붙/수동 작업으로 인한 자동화 불완전성
```

---

## 2. 최종 운영 기준

현재 정상 운영/PoC 기준은 다음과 같다.

```text
prod 서버
- Nginx
- app-blue/app-green Blue-Green
- Prometheus/Grafana
- Alloy
- scaleout control scripts

data 서버
- MySQL
- Redis
- local backup
- S3 upload
- DATA_HEALTH / S3_BACKUP_HEALTH

ops 서버
- Loki
- n8n
- Slack alert routing
- LLMOps report
- infra snapshot report

scale-out PoC app nodes
- clueroom-app-01
- clueroom-app-02
- app-only Spring Boot
- data server MySQL/Redis 사용
- app node Alloy → ops Loki
```

---

## 3. 트러블슈팅 목록

| 번호 | 영역 | 증상 | 핵심 원인 | 해결 |
|---:|---|---|---|---|
| 1 | Ops Snapshot | prod snapshot disk가 80~90%로 경고 | 실제 `df` 기준과 snapshot 수집 로직 불일치 | SERVER_HEALTH 기준과 snapshot 기준 분리, 수집 로직 조정 |
| 2 | Gemini/n8n | Gemini 503/429 반복 | Gemini 모델 RPD/TPM 제한, 동시에 호출 집중 | 기본 알림과 AI 분석 분리, 호출 시간 분산, unavailable fallback 메시지 |
| 3 | Grafana Alert | 백업 실패 alert 오탐 | backup age/window와 실행 직후 상태 차이 | DATA_HEALTH/S3_BACKUP_HEALTH 분리, stale 기준 정리 |
| 4 | Notification Policy | n8n Slack 라우팅 누락/혼선 | Grafana label matcher 불일치 | `source=loki`, `service/category/severity` 라벨 정리 |
| 5 | Alloy/Loki | 새 로그가 Loki에 안 보임 | Alloy file/docker source 미등록 또는 label 불일치 | app log는 Docker source, heartbeat는 Loki push로 분리 |
| 6 | S3 Terraform | AWS CLI InvalidClientTokenId | 로컬 AWS credential/profile 오류 | profile 재설정, STS 확인 후 Terraform 실행 |
| 7 | S3 Upload | `s3-upload.log` 없음 | cron 실행 전이라 로그 파일 미생성 | 수동 실행 성공과 cron 로그 생성 시점 구분 |
| 8 | Restore Rehearsal | sha256 검증 주의 | `.sha256` 파일에 원래 local path 포함 | hash 값만 비교하도록 검증 |
| 9 | Nginx Rate Limit | `limit_req_zone already bound` | rate limit zone 파일 중복 | canonical zone 파일 하나만 유지 |
| 10 | Nginx 설정 | `limit_req_dry_run on/off` 동시 존재 | 잘못 만든 conf.d 파일과 기존 snippet 혼재 | 잘못 만든 파일 disable, 기존 snippet을 enforced로 수정 |
| 11 | Nginx 설정 | shell에서 `limit_req: command not found` | Nginx directive를 터미널에 직접 붙여넣음 | 반드시 conf 파일 안에 작성 |
| 12 | CN Block | 403 원인 구분 어려움 | access log에 `cn` flag 없음 | 1차는 403 alert로 운영, 필요 시 log_format에 `cn=$clueroom_is_cn_ip` 추가 |
| 13 | LogQL | Grafana 패널 parse error | `\.` escape 오류 | `[.]env`, backtick raw string 사용 |
| 14 | LLMOps | AI_CALL_CONTEXT가 AI_CALL 쿼리에 섞임 | `|= "AI_CALL"`이 `AI_CALL_CONTEXT`도 매칭 | `|= "AI_CALL "`로 수정 |
| 15 | OAuth/JWT | `/api/auth/oauth` 500 또는 `/api/auth/oauth/kakao/code` `AUTH_010` | `GOOGLE_CLIENT_IDS`, `KAKAO_APP_ID`, Web Kakao용 `KAKAO_REST_API_KEY` 비어 있음 | oauth.env 보강 후 재배포 |
| 16 | OAuth/JWT | migration 전 deploy | schema/column 없는 상태에서 새 앱 배포 가능성 | S3 백업 → migration → deploy 순서로 재정렬 |
| 17 | SSH Alias | prod에서 `ssh clueroom-data` 실패 | SSH alias는 로컬 PC에만 존재 | data 작업은 로컬에서 실행하거나 IP/별도 SSH config 사용 |
| 18 | Lightsail | `get-key-pairs` 빈 결과 | 해당 리전에 Lightsail key pair 없음 | Terraform으로 public key import하여 key pair 생성 |
| 19 | app node bootstrap | cloud-init error `pipefail` | user_data가 `sh`로 실행되어 `set -o pipefail` 미지원 | `#!/bin/sh`, `set -eu`로 수정 |
| 20 | app node SSH | prod → app-01 `Permission denied` | prod control key가 user_data 실패로 authorized_keys에 미등록 | prod control public key를 Lightsail key pair로 사용 |
| 21 | app node build | `COPY build/libs/*.jar app.jar` 실패 | app node에 bootJar 미복사 | sync script가 prod에서 bootJar 생성 후 app.jar 복사 |
| 22 | scaleout script | heredoc 깨짐, `EOF: command not found` | 긴 스크립트 터미널 붙여넣기 실패 | zip으로 검증된 script 배포 |
| 23 | app node start | `Dockerfile missing` | script가 root Dockerfile 존재를 가정 | Dockerfile hard requirement 제거, compose 기준으로 처리 |
| 24 | app node start | Firebase bean 생성 실패 | `firebase-service-account.json` 누락 | extra secret sync에 Firebase JSON 추가 |
| 25 | app node start | ScenarioYamlImportException | `/opt/clueroom/secrets/scenarios` 누락 또는 권한 문제 | sudo tar로 scenarios 복사, container mount 확인 |
| 26 | app node env | JWT secret presence check 실패 | runtime env가 컨테이너 env로 안 들어감 | app-node runtime compose override에서 env_file 명시 |
| 27 | app02 start | runtime override 파일 없음 | app01에만 수동 생성된 파일을 app02가 요구 | start script가 매번 override 파일 자동 생성 |
| 28 | Nginx LB | `127.0.0.1:80` upstream | active upstream 파싱 시 `:8081` 포트 누락 | header/grep/bg-status 기반 robust parser로 수정 |
| 29 | local/prod confusion | 로컬에서 `/etc/nginx/...` 없음 | prod 서버 경로를 로컬에서 실행 | 명령 실행 위치를 문서에 명시 |
| 30 | Scale-out 완성도 | app01 단일 스크립트 | inventory 없이 scaleout.env 하나만 사용 | `nodes/app01.env`, `nodes/app02.env` 기반 inventory 구조 도입 |

---

## 4. 상세 트러블슈팅

---

## 4.1 Ops Snapshot disk 사용률 경고 오탐

### 증상

Slack Ops Snapshot에서 다음과 같은 경고가 반복됐다.

```text
prod snapshot disk=80~93%
SERVER_HEALTH disk=19~20%
```

Grafana/서버 직접 확인에서는 디스크 사용률이 정상인데, snapshot은 critical로 판단했다.

### 원인

서로 다른 기준의 disk metric이 혼재되어 있었다.

```text
SERVER_HEALTH
→ prod 서버 heartbeat가 직접 df 기준으로 기록

prod snapshot
→ 별도 snapshot 수집 로직에서 다른 경로/값을 최대값으로 집계
```

즉 실제 prod root disk는 정상인데, snapshot 쪽이 잘못된 값을 warning/critical 기준으로 사용하고 있었다.

### 해결

```text
1. SERVER_HEALTH를 prod 서버 디스크 판단의 기준으로 사용
2. snapshot 메시지에 SERVER_HEALTH와 prod snapshot 값을 분리해서 표기
3. snapshot disk 경고가 실제 df와 다르면 수집 로직을 우선 의심
```

### 재발 방지

운영 alert에서 “같은 이름의 metric”이라도 수집 경로가 다르면 별도 라벨과 설명을 둔다.

```text
SERVER_HEALTH disk
prod snapshot disk
data DATA_HEALTH disk
```

---

## 4.2 Gemini API 503/429와 기본 알림 분리

### 증상

n8n에서 Gemini 호출이 반복적으로 실패했다.

```text
503 UNAVAILABLE
This model is currently experiencing high demand

429
Try spacing your requests out using the batching settings
RPD 초과
```

Slack에는 Gemini 분석 실패 메시지가 반복됐다.

### 원인

Gemini API는 무료/제한 구간에서 RPD/TPM 제한이 있고, 당시 여러 workflow가 같은 시간대에 실행되어 호출이 몰렸다.

또 알림 생성 흐름에 AI 분석이 강하게 연결되어 있었다면, AI 실패가 운영 알림 실패처럼 보일 수 있었다.

### 해결

```text
1. 기본 Slack 알림을 AI 분석보다 먼저 전송
2. Gemini 실패 시 GEMINI_*_UNAVAILABLE 상태로 별도 안내
3. workflow 실행 시간을 분산
4. 기본 판단은 rule/metric 기반으로 유지
5. Codex는 자동 fallback이 아니라 수동 handoff report로 분리
```

### 재발 방지

운영 알림은 AI provider에 의존하지 않는다.

```text
Grafana Alert
→ n8n 기본 Slack 알림
→ Gemini 보조 분석
```

AI 분석 실패는 장애가 아니라 “보조 분석 실패”로 취급한다.

---

## 4.3 Grafana Alert 백업 실패 오탐

### 증상

Docker prune 같은 prod 작업 후 data backup alert가 firing되었다가 resolved되었다.

```text
ClueRoom Data MySQL Backup Failed Alert
backup_status=CRITICAL
```

하지만 data 서버에서 확인하면 백업은 정상 상태로 돌아왔다.

### 원인

backup health는 다음 조건을 함께 봤다.

```text
최신 백업 파일 존재
backup age
gzip 검증
```

cron 실행 타이밍과 health push 타이밍이 맞물리면 순간적으로 stale/critical 판단이 발생할 수 있었다.

### 해결

```text
1. data-health-push.sh로 DATA_HEALTH를 명확히 기록
2. S3 업로드는 별도 S3_BACKUP_HEALTH로 분리
3. backup age 기준과 stale 기준을 명시
4. alert는 firing/resolved 흐름을 같이 봄
```

### 재발 방지

백업 관련 alert는 단발 firing보다 resolved 여부와 반복성을 본다.

---

## 4.4 Alloy/Loki 수집 경로 혼동

### 증상

새로 만든 heartbeat나 app node 로그가 Loki에 바로 안 보였다.

### 원인

Loki는 로그 저장소일 뿐이고, 실제 수집 경로는 여러 가지였다.

```text
prod app/nginx logs
→ prod Alloy

data health/S3 health
→ script가 Loki API로 직접 push

ops health
→ ops script가 Loki API로 직접 push

app scale-out node logs
→ app node Alloy
```

새 로그가 어떤 경로로 들어가는지 명확히 나누지 않으면 “Loki 문제”로 오해하기 쉬웠다.

### 해결

로그 종류별 수집 방식을 분리했다.

```text
Spring app event logs
→ app stdout
→ Docker log
→ Alloy
→ Loki

서버 file log
→ Alloy loki.source.file
→ Loki

heartbeat one-line status
→ script direct Loki push
```

### 재발 방지

새 로그를 추가할 때는 먼저 아래를 결정한다.

```text
앱 코드 로그인가?
서버 파일 로그인가?
heartbeat push인가?
```

---

## 4.5 S3 Terraform credential 문제

### 증상

Terraform/S3 작업 전 AWS CLI 확인에서 다음 오류가 발생했다.

```text
InvalidClientTokenId
The security token included in the request is invalid
```

### 원인

로컬 AWS CLI profile의 access key/secret이 잘못되어 있었다.

### 해결

```bash
aws configure --profile <profile>
aws sts get-caller-identity
```

로 의도한 IAM user가 나오는지 확인 후 Terraform을 실행했다.

### 재발 방지

Terraform 작업 전에는 항상 다음을 먼저 확인한다.

```bash
export AWS_PROFILE=<profile>
aws sts get-caller-identity
```

---

## 4.6 S3 backup upload log 없음

### 증상

수동 S3 upload는 성공했지만 다음 파일이 없었다.

```text
/opt/clueroom-data/backups/mysql/s3-upload.log
```

### 원인

수동 실행은 터미널에 출력되고, `s3-upload.log`는 cron 실행 시 redirect로 생성되는 파일이었다.

### 해결

```text
수동 실행 성공
→ 즉시 OK

cron 로그
→ cron 시간이 지나야 생성
```

로 판단 기준을 분리했다.

### 재발 방지

문서에 다음을 명시했다.

```text
manual output != cron log file
```

---

## 4.7 S3 복구 리허설 sha256 검증

### 증상

S3에서 `.sql.gz`와 `.sha256`을 내려받아 검증해야 했다.

### 주의점

`.sha256` 파일 안에는 원본 local path가 들어갈 수 있으므로 `sha256sum -c`를 그대로 쓰면 path mismatch가 날 수 있다.

### 해결

hash 값만 비교했다.

```bash
EXPECTED_SHA="$(awk '{print $1}' "$SHA_FILE")"
ACTUAL_SHA="$(sha256sum "$DOWNLOAD_FILE" | awk '{print $1}')"

test "$EXPECTED_SHA" = "$ACTUAL_SHA"
```

### 결과

```text
S3 업로드 파일 무결성 확인
gzip -t 통과
임시 MySQL 컨테이너 복구 성공
```

---

## 4.8 Nginx Rate Limit 중복 zone

### 증상

`nginx -t`에서 실패했다.

```text
limit_req_zone "clueroom_api_per_ip" is already bound
```

### 원인

rate limit zone 파일이 두 개 존재했다.

```text
00-clueroom-rate-limit-zones.conf
clueroom-rate-limit-zones.conf
```

Nginx는 동일 zone 이름을 중복 선언할 수 없다.

### 해결

```text
canonical 파일 하나만 유지
중복 파일은 .disabled-*로 이동
```

### 재발 방지

`limit_req_zone`은 http context에서 한 번만 선언한다.

---

## 4.9 Nginx dry-run on/off 동시 존재

### 증상

`nginx -T`에서 다음이 동시에 보였다.

```text
limit_req_dry_run off;
limit_req_dry_run on;
```

### 원인

실제 API server block은 `/etc/nginx/sites-enabled/clueroom-api`에 있는데, 별도로 `/etc/nginx/conf.d/clueroom-api.conf`를 새로 만들어 전역/다른 context에 지시문이 들어갔다.

또 기존 snippet에는 dry-run이 남아 있었다.

### 해결

```text
1. 잘못 만든 /etc/nginx/conf.d/clueroom-api.conf 비활성화
2. 실제 include되는 snippet 내용을 enforced로 변경
3. nginx -T에서 dry-run on이 사라졌는지 확인
```

### 검증

```bash
sudo nginx -T 2>/dev/null | grep 'limit_req_dry_run on' || echo "dry-run off confirmed"
```

---

## 4.10 CN Block과 403/429 Alert

### 증상

Rate Limit/IP 차단 적용 후 403/429 alert가 발생했다.

```text
403 count=79
429 count=224
```

### 원인

스캔봇이 다음 경로들을 빠르게 요청했다.

```text
/.env
/.env.test
/docker-compose.override.yml
/terraform.tfvars
/wp-config.bak
/storage/logs/laravel.log
```

403은 민감 경로/CN/manual block 등에서 차단된 요청이고, 429는 rate limit에 걸린 요청이었다.

### 해결

```text
403/429 alert는 장애가 아니라 방어 작동 신호로 해석
threshold가 너무 낮으면 조정 가능
자동 ban은 아직 적용하지 않고 manual blocklist만 준비
```

### 재발 방지

403/429 alert 설명에 다음을 명시했다.

```text
차단 증가가 반드시 장애는 아니다.
정상 사용자 영향 여부를 먼저 확인한다.
```

---

## 4.11 LogQL escape 문제

### 증상

Grafana Loki 패널에서 parse error가 발생했다.

```text
invalid char escape
```

### 원인

LogQL string 안에서 `\.` 같은 escape가 잘못 해석되었다.

### 해결

아래처럼 변경했다.

```logql
[.]env
[.]git
```

또는 backtick raw string 사용.

```logql
|~ `([.]env|[.]git|wp-admin|wp-login)`
```

---

## 4.12 AI_CALL_CONTEXT와 AI_CALL 쿼리 충돌

### 증상

LLMOps report에서 기존 AI_CALL 쿼리가 새 `AI_CALL_CONTEXT` 로그까지 잡을 위험이 생겼다.

### 원인

```logql
|= "AI_CALL"
```

은 다음 둘 다 매칭한다.

```text
AI_CALL
AI_CALL_CONTEXT
```

### 해결

기존 AI_CALL 쿼리를 더 정확히 변경했다.

```logql
|= "AI_CALL "
```

AI_CALL_CONTEXT는 별도 쿼리로 조회한다.

```logql
|= "AI_CALL_CONTEXT "
```

### 재발 방지

로그 prefix를 설계할 때는 substring 충돌 가능성을 고려한다.

---

## 4.13 OAuth provider 미설정으로 `/api/auth/oauth` 500

### 증상

PR #53 배포 후 OAuth smoke test에서 500이 발생했다.

```json
{
  "code": "AUTH_010",
  "message": "OAuth provider is not configured."
}
```

### 원인

컨테이너 env에서 다음 값이 비어 있었다.

```text
GOOGLE_CLIENT_IDS: empty
KAKAO_APP_ID: empty
KAKAO_REST_API_KEY: empty for Web Kakao code-flow
```

JWT secret presence는 정상으로 확인되어 직접 원인이 아니었다.

### 해결

`/opt/clueroom/secrets/env.d/oauth.env`에 provider 값을 넣고 Blue-Green 재배포했다.

초기에는 실제 OAuth 테스트 전까지 placeholder로 기동을 보장할 수 있다.

```text
GOOGLE_CLIENT_IDS: non-empty placeholder
KAKAO_APP_ID: non-empty placeholder
KAKAO_REST_API_KEY: non-empty real value for Web Kakao code-flow
```

실제 로그인 전에는 반드시 실제 값으로 교체해야 한다.

### 재발 방지

OAuth/JWT 배포 전 체크리스트:

```text
JWT_SECRET_PRESENT=yes
GOOGLE_CLIENT_IDS non-empty
KAKAO_APP_ID non-empty
KAKAO_REST_API_KEY non-empty if Web Kakao code-flow is enabled
auth schema migration applied
```

---

## 4.14 migration 전 deploy 순서 문제

### 증상

PR #53/#57 머지 후 migration 전에 deploy가 먼저 진행될 수 있는 상황이 있었다.

### 위험

새 앱이 다음 DB 객체를 기대할 수 있다.

```text
users
user_oauth_accounts
auth_refresh_tokens
evidences.guidance_json
```

마이그레이션 전에 deploy하면 schema validation 또는 runtime error 위험이 있다.

### 해결 순서

```text
1. S3 백업
2. auth schema migration
3. evidence guidance column migration
4. oauth.env 확인
5. Blue-Green deploy
6. smoke test
```

### 재발 방지

DB 변경 PR은 항상 “migration 먼저, deploy 나중”을 runbook에 명시한다.

---

## 4.15 SSH alias 혼동

### 증상

prod 서버에서 다음 명령이 실패했다.

```bash
ssh clueroom-data
```

```text
Could not resolve hostname clueroom-data
```

### 원인

`clueroom-data` alias는 로컬 PC `~/.ssh/config`에만 존재한다. prod 서버에는 해당 alias가 없다.

### 해결

```text
data 서버 작업은 로컬 PC에서 ssh clueroom-data로 실행
prod에서 data로 SSH하려면 별도 IP/key 설정 필요
```

### 재발 방지

문서에 명령 실행 위치를 명시한다.

```text
[로컬]
[prod]
[data]
[ops]
```

---

## 4.16 Lightsail key pair 없음

### 증상

```bash
aws lightsail get-key-pairs
```

결과가 비어 있었다.

### 원인

ap-northeast-2 리전에 기존 Lightsail key pair가 없었다.

### 해결

Terraform에서 scaleout용 public key를 Lightsail key pair로 import하도록 했다.

```text
aws_lightsail_key_pair.prod_control
```

그리고 이 key pair는 prod control public key를 사용하도록 했다.

### 재발 방지

기존 콘솔 key pair에 의존하지 않는다.
Terraform이 scaleout용 key pair를 관리한다.

---

## 4.17 cloud-init pipefail 오류

### 증상

app node bootstrap이 실패했다.

```text
/var/lib/cloud/instance/scripts/part-001: set: Illegal option -o pipefail
```

### 원인

user_data가 `sh`로 실행되었고, `sh`는 `set -o pipefail`을 지원하지 않았다.

### 해결

user_data를 POSIX sh 호환으로 변경했다.

```sh
#!/bin/sh
set -eu
```

또 bootstrap 로그를 별도 파일로 저장했다.

```text
/var/log/clueroom-app-node-bootstrap.log
```

### 재발 방지

cloud-init user_data는 bash 기능을 쓸 경우 반드시 bash로 실행되는지 확인한다.
안전하게 가려면 POSIX sh 호환으로 작성한다.

---

## 4.18 prod → app node SSH 실패

### 증상

```bash
ssh ubuntu@172.26.x.x
```

결과:

```text
Permission denied (publickey)
```

### 원인

처음 설계에서는 prod control public key를 user_data로 app node `authorized_keys`에 추가했다.
하지만 user_data가 pipefail 문제로 실패하면서 prod key도 추가되지 않았다.

### 해결

key 구조를 바꿨다.

```text
Lightsail key pair = prod control public key
local emergency key = user_data로 보조 추가
```

이렇게 해서 user_data가 실패하더라도 prod → app node SSH는 가능하게 했다.

---

## 4.19 app node JAR 누락

### 증상

Docker build 실패.

```text
COPY build/libs/*.jar app.jar
lstat /build/libs: no such file or directory
```

### 원인

Dockerfile은 이미 빌드된 bootJar를 복사하는 방식인데, app node에 `build/libs/*.jar`를 복사하지 않았다.

### 해결

`sync-app-node.sh`에 다음을 추가했다.

```text
prod에서 ./gradlew bootJar -x test
최신 bootJar 찾기
app node /opt/clueroom/app/build/libs/app.jar로 scp
```

### 재발 방지

app node는 빌드 서버가 아니라 실행 노드다.
artifact sync는 scaleout script가 담당한다.

---

## 4.20 긴 heredoc 붙여넣기 깨짐

### 증상

스크립트 실행 중 다음 오류가 발생했다.

```text
EOF: command not found
REMOTE: command not found
EOFo ...
REMOTEttp ...
```

### 원인

긴 heredoc 스크립트를 터미널에 직접 붙여넣는 과정에서 경계 문자열이 깨졌다.

### 해결

긴 스크립트는 zip 파일로 전달하고 서버에서 unzip/cp 방식으로 설치했다.

```text
scaleout_scripts_fix_v2.zip
scaleout_scripts_fix_v3.zip
scaleout_inventory_v5.zip
```

### 재발 방지

운영 서버에 긴 shell script를 붙여넣지 않는다.

```text
로컬에서 파일 생성
→ scp
→ unzip
→ bash -n
```

방식으로 배포한다.

---

## 4.21 Dockerfile 위치 가정 오류

### 증상

`sync-app-node.sh`가 실패했다.

```text
ERROR: required file missing in prod app dir: Dockerfile
```

### 원인

script가 root `Dockerfile` 존재를 필수로 가정했지만, 실제 compose 구조에서는 root Dockerfile이 없거나 다른 경로를 쓸 수 있었다.

### 해결

Dockerfile hard requirement를 제거했다.

```text
docker-compose.yml
docker-compose.external-data.yml
.env
gradlew
```

등 실제 필요한 파일만 pre-check했다.

### 재발 방지

스크립트는 repo 구조를 과도하게 가정하지 않고, compose가 실제 build context/dockerfile을 판단하게 둔다.

---

## 4.22 Firebase secret 누락

### 증상

app node Spring Boot가 부팅 중 종료됐다.

```text
Error creating bean with name 'firebaseApp'
/opt/clueroom/secrets/firebase-service-account.json (No such file or directory)
```

### 원인

`/opt/clueroom/secrets/env.d`는 복사했지만, 파일형 secret인 `firebase-service-account.json`은 복사하지 않았다.

### 해결

`sync-app-node-extra-secrets.sh`에 Firebase JSON 복사를 추가했다.

```text
/opt/clueroom/secrets/firebase-service-account.json
→ app node 동일 경로
```

또 runtime compose override에 mount를 추가했다.

```yaml
volumes:
  - /opt/clueroom/secrets/firebase-service-account.json:/opt/clueroom/secrets/firebase-service-account.json:ro
```

---

## 4.23 scenario YAML 디렉터리 누락/권한 문제

### 증상

app node Spring Boot가 부팅 중 종료됐다.

```text
ScenarioYamlImportException:
시나리오 YAML 경로가 존재하지 않습니다:
/opt/clueroom/secrets/scenarios
```

또 sync 중에는 권한 문제가 있었다.

```text
tar: secrets/scenarios/... Cannot open: Permission denied
```

### 원인

scenario files는 `/opt/clueroom/secrets/scenarios`에 있었고, 일반 ubuntu 권한으로는 읽을 수 없는 파일이 포함되어 있었다.

### 해결

```text
sudo tar -C /opt/clueroom -czf - secrets/scenarios
```

로 prod에서 읽고, app node에 압축 해제했다.

또 runtime compose override에 mount를 추가했다.

```yaml
volumes:
  - /opt/clueroom/secrets/scenarios:/opt/clueroom/secrets/scenarios:ro
```

### 검증

```text
scenario_host_count=18
scenario_container_count=18
```

---

## 4.24 JWT secret presence check 실패

### 증상

app node health는 200이지만 secret 검증에서 다음이 나왔다.

```text
JWT_SECRET_PRESENT=no
```

### 원인

Docker Compose의 `--env-file`은 compose interpolation에는 쓰였지만, 컨테이너 env로 모든 secret을 넣는 구조와 맞지 않았다.

### 해결

app node runtime env 파일을 만들고 runtime compose override에서 명시적으로 사용했다.

```text
/tmp/clueroom-app-node-runtime.env
```

```yaml
services:
  app:
    env_file:
      - /tmp/clueroom-app-node-runtime.env
```

### 검증

```text
JWT_SECRET_PRESENT=yes
```

---

## 4.25 app02 runtime override 누락

### 증상

app02 start 실패.

```text
open /opt/clueroom/app/docker-compose.app-node.runtime.yml: no such file or directory
```

### 원인

app01에서는 중간에 수동으로 runtime override 파일을 만든 적이 있었지만, app02는 새 노드라 해당 파일이 없었다.

### 해결

`start-app-node.sh`가 실행될 때마다 app node에 runtime override compose 파일을 자동 생성하도록 수정했다.

### 재발 방지

노드별로 수동으로 만든 파일에 의존하지 않는다.
start script가 필요한 runtime 파일을 매번 idempotent하게 생성한다.

---

## 4.26 Nginx upstream 포트 누락

### 증상

분산 확인에서 잘못된 upstream이 보였다.

```text
127.0.0.1:80
```

### 원인

active upstream을 파싱할 때 `awk -F:`로 잘라서 `127.0.0.1:8081` 중 포트가 날아갔다.

Nginx는 `server 127.0.0.1`을 기본 포트 80으로 해석했다.

### 해결

active upstream 감지 로직을 수정했다.

```text
1. X-ClueRoom-Upstream header에서 우선 확인
2. upstream conf에서 127.0.0.1:8081/8082 추출
3. bg-status active service로 fallback
```

또 validation을 추가했다.

```text
127.0.0.1:80 나오면 실패
127.0.0.1 포트 없는 값 나오면 실패
```

### 검증

canary:

```text
30 127.0.0.1:8081
10 172.26.6.201:8080
```

equal:

```text
20 127.0.0.1:8081
20 172.26.6.201:8080
20 172.26.2.166:8080
```

---

## 4.27 local/prod 명령 실행 위치 혼동

### 증상

로컬 Git Bash에서 다음 명령을 실행했다.

```bash
cat /etc/nginx/conf.d/clueroom-upstream.conf
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 60 scaleout
```

결과:

```text
No such file or directory
```

### 원인

해당 파일과 스크립트는 prod 서버에 있다. 로컬 PC에는 없다.

### 해결

문서/가이드에 실행 위치를 명확히 표시한다.

```text
[로컬 Git Bash]
[prod 서버]
[ops 서버]
[data 서버]
```

---

## 4.28 app01 Loki 조회가 안 보임

### 증상

ops에서 다음 쿼리로 app01이 안 보였다.

```logql
{job="docker-app", instance="clueroom-app-01"} |~ "(Started StartUpApplication|Tomcat started...)"
```

### 원인

app01은 이미 이전 시점에 startup log가 찍혔고, 조회 window 또는 필터 조건에 걸리지 않을 수 있었다.
health check는 항상 애플리케이션 로그를 남기는 것이 아니므로, 단순 health 요청으로 해당 필터 로그가 새로 생기지 않을 수 있다.

### 해결

24시간 범위로 넓히고 필터를 완화해서 확인한다.

```bash
START_NS="$(date -u -d '24 hours ago' +%s%N)"
END_NS="$(date -u +%s%N)"
```

필터 없는 쿼리로도 확인한다.

```logql
{job="docker-app", instance="clueroom-app-01"}
```

### 재발 방지

Loki 인증샷은 다음 둘 중 하나를 사용한다.

```text
1. 앱 startup 직후 바로 캡처
2. 시간 범위 명시한 query_range 사용
```

---

## 4.29 단일 node script에서 inventory 기반 script로 전환

### 문제

초기 scaleout script는 단일 파일만 사용했다.

```text
/opt/clueroom/scaleout/scaleout.env
```

이 방식은 app01 한 대에는 충분하지만 app02/app03으로 확장하려면 계속 파일을 덮어써야 한다.

### 해결

inventory 구조로 바꿨다.

```text
/opt/clueroom/scaleout/common.env
/opt/clueroom/scaleout/nodes/app01.env
/opt/clueroom/scaleout/nodes/app02.env
```

실행 방식:

```bash
run-node-action.sh app01 check
run-node-action.sh app02 check
run-all-nodes.sh check
```

### 결과

Terraform output을 prod inventory로 변환하고, app01/app02에 동일한 작업을 반복할 수 있게 됐다.

---

## 5. 최종 Scale-out PoC 결과

### 검증된 구조

```text
Terraform
→ app01/app02 Lightsail 생성

prod
→ app node inventory 생성
→ app node sync/start/check/alloy
→ Nginx upstream apply/rollback

app nodes
→ Spring Boot app-only
→ data server MySQL/Redis 사용
→ Firebase/scenarios/JWT runtime 구성
→ Alloy로 logs push

ops
→ Loki에서 app node logs 확인
```

### 핵심 인증 결과

```text
app01 private: 172.26.6.201
app02 private: 172.26.2.166

Nginx equal upstream:
127.0.0.1:8081 weight=1
172.26.6.201:8080 weight=1
172.26.2.166:8080 weight=1

분산 결과:
20 127.0.0.1:8081
20 172.26.6.201:8080
20 172.26.2.166:8080
```

### 의미

```text
기존 prod active app + Terraform app node 2대
총 3개 backend로 요청 분산을 검증했다.
```

---

## 6. 최종 체크리스트

새 app node를 추가할 때는 다음 순서를 따른다.

```text
[로컬]
1. Terraform app_servers에 app02/app03 추가
2. terraform plan/apply
3. terraform output -json app_private_ips/app_public_ips 생성
4. prod로 output json 복사

[prod]
5. build-node-envs-from-terraform-output.sh
6. run-all-nodes.sh sync
7. run-all-nodes.sh start
8. run-all-nodes.sh check
9. run-all-nodes.sh verify-secrets
10. run-all-nodes.sh alloy
11. apply-nginx-scaleout-upstream.sh canary
12. verify-lb-distribution.sh 60 scaleout
13. 필요 시 apply-nginx-scaleout-upstream.sh equal
14. 인증샷
15. rollback 또는 local-only 원복

[ops]
16. Loki에서 app node logs 확인
```

---

## 7. 배운 점

### 1. “서버가 떴다”와 “운영 가능한 app node”는 다르다

단순히 Lightsail instance가 Running이면 끝이 아니다.

```text
Docker
swap
app jar
env
secret env
Firebase JSON
scenario files
DB/Redis 연결
health
Loki logs
Nginx upstream
```

까지 확인해야 한다.

### 2. secret은 env 파일만 있는 것이 아니다

이번 프로젝트에는 파일형 secret도 있었다.

```text
firebase-service-account.json
scenarios/*.yaml
```

scale-out sync는 env만 복사하면 부족하다.

### 3. health 200은 최소 조건일 뿐이다

health 200이어도 다음이 누락될 수 있다.

```text
JWT secret presence
Firebase file
scenario files
Loki logs
```

그래서 별도 `verify-secrets`가 필요했다.

### 4. Nginx 설정은 문법 성공과 의미 성공이 다르다

`nginx -t`는 통과했지만 아래는 잘못된 설정이었다.

```nginx
server 127.0.0.1;
```

Nginx는 기본 포트 80으로 해석했고, 실제 의도는 `127.0.0.1:8081`이었다.

### 5. 자동화는 “한 번 성공한 명령 모음”이 아니라 “다시 실행 가능한 절차”여야 한다

app01에서 수동으로 만든 파일이 app02에는 없어서 실패했다.
이후 start script가 runtime override 파일을 매번 생성하도록 바꿨다.

### 6. 긴 운영 스크립트는 터미널 복붙보다 파일 배포가 안전하다

heredoc 깨짐으로 여러 번 실패했다.
최종적으로는 zip 배포 → unzip → bash -n → 실행 방식이 안정적이었다.

---

## 8. 남은 한계

이번 PoC는 다음을 완료한 것이 아니다.

```text
오토스케일링
AWS Managed Load Balancer
Nginx HA
DB HA
Redis HA
multi-AZ
무중단 rolling deploy for app nodes
```

정확한 표현은 다음이다.

```text
Terraform 기반 app node scale-out PoC
prod 제어형 app node 복제/기동 자동화
Nginx upstream 기반 scripted manual load balancing
app layer scale-out 검증
```

---

## 9. 운영 종료/정리 기준

PoC 후 정리 순서:

```text
1. Nginx local-only 원복
2. verify-lb-distribution.sh 20 local-only로 local active만 확인
3. run-all-nodes.sh reset
4. 필요 시 CONFIRM_SCRUB_APP_NODE_SECRETS=YES /opt/clueroom/scaleout/scripts/run-all-nodes.sh scrub-secrets
5. terraform plan -destroy
6. destroy 대상이 app nodes/key pair/ports뿐인지 확인
7. terraform apply destroy
8. Lightsail app node 제거 확인
9. prod inventory stale 파일 백업 후 정리
```

절대 하지 말 것:

```text
prod/data/ops destroy
S3 bucket destroy
backup IAM user destroy
docker system prune --volumes
secret 파일 출력
```

---

## 10. 최종 결론

ClueRoom 인프라 구축 과정의 핵심 트러블슈팅은 단순 오류 수정이 아니라, 운영 구조를 점진적으로 안정화하는 과정이었다.

특히 scale-out 단계에서는 다음을 실제로 검증했다.

```text
1. Terraform으로 app node 생성
2. prod 서버가 app node를 제어
3. prod의 app artifact/env/secrets/scenarios를 app node로 동기화
4. app node가 data server MySQL/Redis를 사용
5. app node Alloy가 ops Loki로 로그 전송
6. prod Nginx upstream이 local active + app01 + app02로 요청 분산
```

이로써 ClueRoom은 단일 서버 배포를 넘어, app layer scale-out이 가능한 구조를 PoC 수준에서 증명했다.
