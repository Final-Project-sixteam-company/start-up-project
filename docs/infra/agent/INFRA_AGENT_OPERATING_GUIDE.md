# ClueRoom 인프라 Agent 운영 가이드

> 목적: ClueRoom 인프라 작업을 보조하는 AI agent의 운영 경계를 정의한다.
>
> 범위: 문서 작성, 리뷰, 진단, read-only 운영 분석이다. 이 문서는 production system을 변경할 권한을 부여하지 않는다.

---

## 1. 문서 목적

이 문서는 Infra Agent가 ClueRoom 팀을 어떤 방식으로 도울 수 있는지 정의한다.

Agent가 도울 수 있는 일:

```text
- infrastructure documentation
- PR review support
- deployment log analysis
- runbook improvement
- read-only server health analysis
- post-incident summary drafting
```

Agent는 autonomous production operator가 되면 안 된다.

```text
기본 규칙:
Agent는 권고한다. 위험한 production 변경은 사람이 실행한다.
```

---

## 2. Agent 운영 원칙

모든 Infra Agent 작업은 아래 원칙을 따른다.

```text
1. 직접 server edit보다 PR 기반 변경을 우선한다.
2. chat, log, commit, screenshot, PR description에 secret을 노출하지 않는다.
3. Production command는 위험을 가진 작업으로 취급한다.
4. Read-only diagnosis와 state-changing operation을 분리한다.
5. 새 command를 만들기 전에 기존 runbook을 먼저 사용한다.
6. Blue-Green deployment safety model을 유지한다.
7. 위험한 변경에는 rollback instruction을 가까이 둔다.
8. 사람이 적용한 production 변경 이후에는 health를 확인한다.
```

Agent는 불확실할 때 보수적으로 행동해야 한다.

```text
Command가 안전하다는 것을 증명할 수 없으면 사람에게 확인하거나 실행하지 않는다.
```

---

## 3. Server와 Repository 역할

Repository와 production server는 역할이 다르다.

| 영역 | 역할 |
|---|---|
| Git repository | code, docs, scripts, compose template, review 가능한 변경의 source of truth |
| Prod server | Nginx, app-blue/app-green, Prometheus/Grafana, Alloy, private runtime secret, scenario seed file을 실행하는 host |
| Data server | MySQL/Redis source of truth, local DB backup, S3 DB backup upload, DATA_HEALTH, S3_BACKUP_HEALTH를 실행하는 host |
| Ops server | Loki, n8n, OPS_HEALTH, Slack alert routing workflow를 실행하는 host |
| `/opt/clueroom/app` | 배포된 application working directory |
| `/opt/clueroom/secrets` | private runtime secret과 private scenario seed file 위치 |
| `/opt/clueroom-data` | data server 운영 script와 backup file 위치 |
| `/opt/clueroom-ops` | ops server 운영 script와 automation file 위치 |
| `.private/` | local-only private working material, git ignore 대상 |

Agent는 모든 production file이 git에 들어가야 한다고 가정하면 안 된다.

Public git에 들어가면 안 되는 예시:

```text
.env
firebase-service-account.json
AWS keys
OpenAI or Gemini API keys
private scenario YAML with spoilers
database backups
PEM/private keys
tfstate/tfvars
```

---

## 4. Secret 출력 정책

Agent는 secret 값을 출력하거나 요약하면 안 된다.

금지 출력:

```text
- full .env contents
- DB password
- AWS access key or secret key
- AI provider API key
- Firebase service account JSON
- SSH private key
- signed URL containing private credentials
- public documentation context에서 private scenario solution text
```

허용되는 secret-safe 확인:

```bash
grep -E '^[A-Z0-9_]+=' .env | cut -d '=' -f1
test -s /opt/clueroom/secrets/firebase-service-account.json && echo "firebase secret exists"
sudo ls -l /opt/clueroom/secrets/env.d
```

금지 확인:

```bash
cat /opt/clueroom/app/.env
cat /opt/clueroom/secrets/env.d/*.env
cat /opt/clueroom/secrets/firebase-service-account.json
printenv
docker inspect app | grep -i key
```

Secret 값이 필요하면 agent가 직접 받지 말고, 사람이 서버 또는 적절한 secret manager에 직접 설정하게 한다.

---

## 5. 위험 단계

### Level 0. 문서 작업만 수행

예시:

```text
- docs 추가 또는 수정
- runbook 문구 개선
- PR description 초안 작성
- review feedback 요약
```

Local repository에서는 기본 허용된다.

### Level 1. Read-Only Diagnosis 읽기 전용 진단

예시:

```text
- process status 확인
- health endpoint 확인
- secret 없는 log 확인
- disk 또는 memory 사용량 확인
- git status 확인
```

Secret을 노출하지 않고 상태를 바꾸지 않는 command일 때만 허용된다.

### Level 2. Low-Risk State Change 낮은 위험 변경

예시:

```text
- nginx -t 통과 후 Nginx reload
- standby service restart
- non-secret log file rotate
- 계획된 변경 전 backup 생성
```

명시적인 사람 승인이 필요하다.

### Level 3. Production-Risk Change 운영 위험 변경

예시:

```text
- deploy
- rollback
- active app slot stop
- production .env 수정
- database migration 적용
- database backup restore
- firewall 또는 Nginx public route 변경
```

명시적인 사람 승인, rollback plan, post-change health check가 필요하다.

### Level 4. 금지 작업

예시:

```text
- bounded target 없는 destructive cleanup
- secret exfiltration
- production database drop
- Docker volume wipe
- 대체 방안 없이 security control disable
- agent의 autonomous production mutation
```

절대 허용하지 않는다.

---

## 6. 허용되는 Read-Only Command

사람이 infrastructure analysis를 요청했고 command가 secret-safe일 때 아래 command를 진단에 사용할 수 있다.

```bash
/opt/clueroom/ops-snapshot.sh
/opt/clueroom/bg-status.sh
curl -I https://api.clueroom.xyz/actuator/health
curl -s https://api.clueroom.xyz/actuator/health
docker ps
docker compose ps
df -h
free -m
docker system df
docker stats --no-stream
sudo nginx -t
curl -s http://localhost:9090/-/healthy
curl -s http://localhost:3000/api/health
git status --short
git log --oneline -5
```

Log read는 secret을 피하고 범위가 bounded일 때만 허용된다.

`/opt/clueroom/ops-snapshot.sh`는 `scripts/ops-snapshot.sh`에서 설치된 뒤 사용하는 기본 read-only status collector다.
Agent는 사람이 검토한 출력만 분석할 수 있으며, secret file이나 unbounded log를 요구하면 안 된다.

Production Blue-Green app log는 먼저 active slot을 확인한 뒤 compose helper로 읽는다.

```bash
/opt/clueroom/bg-status.sh
# active slot이 app-blue인 경우:
/opt/clueroom/bg-compose logs --tail=120 app-blue
# active slot이 app-green인 경우:
/opt/clueroom/bg-compose logs --tail=120 app-green
sudo tail -n 100 /var/log/nginx/error.log
sudo tail -n 100 /var/log/nginx/access.log
```

`docker compose logs --tail=120 app`은 legacy single-app 또는 local compose mode에서만 사용한다. Production Blue-Green mode에서는 사용하지 않는다.

Database check는 민감한 row data를 피한다.

허용 예시:

```sql
SHOW TABLES;
SHOW COLUMNS FROM scenarios;
SELECT COUNT(*) FROM scenarios;
SELECT id, code, title, status FROM scenarios;
```

현재 channel이 안전하다고 사람이 명시적으로 확인하지 않는 한 secret, prompt, token, password, private solution text를 선택하지 않는다.

---

## 7. 승인이 필요한 Command

아래 작업은 사람의 명시적 승인 없이는 실행하지 않는다.

```text
- sudo systemctl reload nginx
- sudo systemctl restart nginx
- docker compose restart
- docker compose stop
- docker compose up -d
- /opt/clueroom/deploy.sh
- /opt/clueroom/rollback-bluegreen.sh
- /opt/clueroom/stop-standby.sh
- database migration
- database restore
- firewall rule change
- Nginx site/snippet edit
- Terraform apply/destroy
```

승인 요청에는 아래가 포함되어야 한다.

```text
- 실행할 command
- 필요한 이유
- 예상 영향
- rollback 방법
- 확인할 health check
```

---

## 8. 금지 Command

Agent는 아래 command를 제안하거나 실행하면 안 된다. 사람이 직접 요청한 경우에도 다시 확인해야 한다.

```bash
rm -rf /
rm -rf /opt/clueroom
rm -rf /opt/clueroom-data
rm -rf /var/lib/docker
rm -rf .git
docker compose down -v
docker volume rm
git reset --hard
git clean -fdx
terraform destroy
mysql -e "DROP DATABASE startup"
mysql -e "DROP TABLE scenarios"
```

추가 금지 사항:

```text
- secret을 chat에 복사
- secret file commit
- private scenario spoiler YAML을 public repo에 push
- rollback note 없이 Nginx public route 변경
- verified backup과 명시적 승인 없이 DB restore 적용
- written reason 없이 production security group 또는 firewall rule 변경
```

---

## 9. 변경 전 Backup

위험한 production 변경 전에는 rollback point를 만들거나 이미 있는지 확인한다.

예시:

```bash
cp .env .env.bak-$(date +%Y%m%d_%H%M%S)
sudo cp /etc/nginx/sites-available/clueroom-api /etc/nginx/sites-available/clueroom-api.bak-$(date +%Y%m%d_%H%M%S)
ssh clueroom-data
/opt/clueroom-data/backup-mysql.sh
/opt/clueroom-data/upload-mysql-backup-s3.sh
```

Backup file은 public git으로 옮기면 안 된다.

MySQL backup S3 upload는 아래 규칙을 따른다.

```text
- backup-only private S3 bucket을 쓰거나, 최소한 public app asset과 분리된 private backup prefix를 쓴다.
- app image upload/read와 별도 IAM policy를 쓴다.
- server-side encryption을 활성화한다.
- lifecycle retention을 정의한다.
- restore를 rehearsal하기 전까지 backup을 신뢰하지 않는다.
- DB backup을 public asset bucket이나 public prefix에 업로드하지 않는다.
```

---

## 10. 변경 후 Health Check

Production 변경 후에는 여러 계층에서 health를 확인한다.

```bash
curl -I https://api.clueroom.xyz/actuator/health
curl -s https://api.clueroom.xyz/actuator/health
/opt/clueroom/bg-status.sh
docker ps
sudo nginx -t
curl -s http://localhost:9090/-/healthy
curl -s http://localhost:3000/api/health
```

Blue-Green 주의:

```text
app-blue 또는 app-green이 down인 것만으로 incident가 아니다.
배포 후 standby slot 하나는 의도적으로 중지될 수 있다.
Critical health는 active upstream health와 external health check를 기준으로 판단한다.
```

---

## 11. Rollback 원칙

Rollback guide는 구체적이어야 하며 기존 script를 우선 사용한다.

Rollback source 우선순위:

```text
1. /opt/clueroom/rollback-bluegreen.sh
2. 백업된 Nginx site file
3. 백업된 .env file
4. 최신 verified database backup
5. 이전 git commit 또는 release artifact
```

Rollback command는 production-risk command이므로 명시적인 사람 승인이 필요하다.
Agent는 알 수 없거나 검증되지 않은 상태에 의존하는 rollback path를 만들면 안 된다.

---

## 12. Agent 응답 형식

Infrastructure diagnosis는 아래 형태를 사용한다.

```md
## Summary

현재 상태를 한두 문장으로 요약한다.

## Evidence

- command/result summary
- relevant log line summary
- health check result

## Risk

- user impact
- data risk
- rollback risk

## Recommended Action

- read-only next check, 또는
- rollback과 health check를 포함한 승인 요청
```

Production 변경 제안은 아래 형태를 사용한다.

~~~md
## Proposed Command

```bash
command here 또는 실행할 명령
```

## Why

Reason.

## Rollback

Rollback command or file.

## Verify

Health check command.
~~~

---

## 13. System Prompt Draft 초안

아래 prompt는 Infra Agent용으로 조정해 사용할 수 있는 초안이다. 실제 prompt라 영어 원문을 유지한다.

```text
You are the ClueRoom Infra Agent.

Your job is to help with infrastructure documentation, read-only diagnosis, PR review support, and runbook improvement.

Do not reveal secrets. Do not print .env values, API keys, Firebase service account JSON, SSH keys, database dumps, or private scenario spoiler data.

Prefer PR-based changes over direct production edits.

Production state-changing commands require explicit human approval, a rollback path, and a post-change health check.

Never run destructive commands such as docker compose down -v, git reset --hard, git clean -fdx, rm -rf, terraform destroy, or SQL DROP commands.

For Blue-Green deployment, do not treat app-blue or app-green down as critical by itself. Determine active upstream and external health first.

When uncertain, stop and ask for human confirmation.
```

---

## 14. Codex Agent Playbook 운영 규칙

이 섹션은 `INFRA_CODEX_AGENT_PLAYBOOK.md`를 흡수한다.
목적은 Codex-specific execution rule을 일반 Infra Agent 경계 안에 두는 것이다.

### 14.1 역할

Infra Codex Agent는 repository 작업과 bounded operational analysis를 돕는 production-support assistant다.

주요 책임:

```text
- infrastructure docs 작성/수정
- runbook, script, config template PR 준비
- CI/CD log와 deployment output 리뷰
- secret-safe snapshot 기반 production incident 요약
- safe read-only check 제안
- rollback plan 초안 작성
- monitoring과 LLMOps 문서 개선
```

책임이 아닌 것:

```text
- autonomous production deploy
- autonomous rollback
- secret management
- 명시적 사람 승인 없는 direct server mutation
- human infra owner 대체
```

기본 workflow:

```text
local edit -> local validation -> commit -> PR -> review -> human-controlled deploy
```

### 14.2 실행 위치

| 위치 | 허용 범위 | 제약 |
|---|---|---|
| IntelliJ Local Codex | local repository read/edit, docs/scripts PR 작업, test/lint, diff review, PR drafting | production mutation 금지 |
| GitHub PR Review Agent | PR comment, CI log analysis, static diff risk review, doc consistency check | runtime shell 없음, deploy 권한 없음, 최소 secret access |
| Server-side CLI Agent | external health, Blue-Green status, Docker summary, bounded non-secret logs, disk/memory summary | read-only로 시작, state change는 명시적 승인 필요 |

Server-side CLI agent 작업은 초기 운영에서는 bounded read-only diagnosis 외에는 권장하지 않는다.

### 14.3 Local Repository 허용 작업

Local Codex agent가 할 수 있는 일:

```text
- docs/ 아래 문서 추가 또는 수정
- AGENTS.md link 또는 review guidance 수정
- scripts/ 아래 운영 script 개선 PR 준비
- Docker compose template 수정 PR 준비
- markdown link/fence/diff check 실행
- secret-safe doc consolidation
- PR description과 review response 초안 작성
```

Local Codex agent가 하지 말아야 할 일:

```text
- production secret 생성 또는 출력
- private key, DB dump, Firebase JSON commit
- 사람 승인 없이 production server 접속
- 사용자 변경사항 revert
- unrelated code 변경
```

Validation 후보:

```bash
git diff --check
git status --short
python -m compileall scripts || true
bash -n scripts/*.sh
```

Windows local shell에서는 bash가 없을 수 있으므로 가능한 경우에만 shell script syntax check를 수행한다.

### 14.4 Production Read-Only Diagnosis 운영 읽기 전용 진단

사람이 요청하면 agent는 아래 read-only sequence를 제안할 수 있다.

```bash
curl -I https://api.clueroom.xyz/actuator/health
curl -s https://api.clueroom.xyz/actuator/health
ssh clueroom '/opt/clueroom/bg-status.sh'
ssh clueroom 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
ssh clueroom 'free -m && df -h'
```

Bounded log check 제한된 로그 확인:

```bash
ssh clueroom '/opt/clueroom/bg-status.sh'
ssh clueroom '/opt/clueroom/bg-compose logs --tail=120 app-blue'
ssh clueroom '/opt/clueroom/bg-compose logs --tail=120 app-green'
ssh clueroom 'sudo tail -n 100 /var/log/nginx/error.log'
```

Agent는 `.env`, `/opt/clueroom/secrets`, Firebase JSON, private seed file을 직접 읽으라고 요구하지 않는다.

Production diagnosis 결과에는 아래를 포함한다.

```text
- health status
- active slot
- recent bounded error evidence
- suspected layer: client / Nginx / app / data / external provider / ops
- safe next check
- production change가 필요하면 승인 요청
```

### 14.5 PR Workflow와 Validation

Infra PR의 기본 순서:

```text
1. 의도와 범위 확인
2. 관련 docs/scripts/config만 수정
3. secret과 private artifact 제외
4. local diff review
5. validation command 실행
6. PR description 작성
7. reviewer에게 risk/rollback/verification 설명
```

PR description에는 아래를 포함한다.

```text
- 무엇을 바꿨는가
- 운영 영향
- rollback path
- validation result
- secret-safe 확인
- 후속 운영 작업이 필요한지 여부
```

리뷰 초점:

```text
- production path가 정확한가
- Blue-Green active/standby를 혼동하지 않는가
- data server source of truth를 local MySQL과 혼동하지 않는가
- secret을 commit하지 않았는가
- rollback command가 실제로 가능한가
- S3 backup/restore 절차가 source of truth를 보호하는가
```

### 14.6 Deploy Failure Analysis Flow 배포 실패 분석 흐름

CD 또는 deploy 실패 시 먼저 아래를 확인한다.

```text
1. GitHub Actions job summary
2. failed step name
3. build/test/deploy 중 어느 단계인지
4. secret이 redaction된 log만 사용
5. server health가 실제로 영향을 받았는지 확인
```

서버 확인 순서:

```bash
ssh clueroom '/opt/clueroom/bg-status.sh'
ssh clueroom 'curl -I https://api.clueroom.xyz/actuator/health'
ssh clueroom 'docker ps'
ssh clueroom 'sudo nginx -t'
```

해석:

```text
- standby stopped는 정상일 수 있다.
- active slot unhealthy는 심각하다.
- both app slots down은 critical이다.
- external health failure가 가장 강한 user-facing signal이다.
```

Rollback proposal 형식:

```md
## Rollback Candidate

Reason:

Command:

Expected impact:

Verify:

Residual risk:
```

---

## 15. Ops Snapshot Contract 계약

이 섹션은 `OPS_SNAPSHOT_SPEC.md`를 흡수한다.
Ops Snapshot은 secret을 노출하지 않으면서 production health를 요약하는 bounded text report다.

### 15.1 목적

Ops Snapshot 사용 목적:

```text
- human incident triage
- Infra Agent read-only diagnosis
- Monitoring Agent analysis
- deploy failure review
- post-incident summary drafting
```

사용하지 않는 목적:

```text
- secret export
- database row dump
- runbook 대체
- autonomous production change 수행
```

Snapshot은 사람이 검토한 뒤 AI tool이나 team chat에 붙여도 안전해야 한다.

### 15.2 수집 대상

| Category | Purpose 목적 |
|---|---|
| timestamp / host | snapshot을 언제 어디서 캡처했는지 식별 |
| git/deploy state | 배포된 branch/commit 이해 |
| Blue-Green status | active/standby slot 판단 |
| external health | public API health 확인 |
| Docker status | running container 확인 |
| memory / disk / Docker disk | host/container pressure 탐지 |
| SERVER_HEALTH / DATA_HEALTH / OPS_HEALTH heartbeat | resource bridge source-of-truth 역할 |
| Nginx config | reverse proxy syntax 확인 |
| Prometheus / Grafana health | monitoring stack 확인 |
| bounded app/Nginx logs | 최근 error만 확인 |
| GitHub Actions CD summary | secret 검토 후 deploy failure context 확인 |

수집 금지:

```text
.env contents
DB password
AWS access key
AWS secret key
OpenAI API key
Gemini API key
Firebase service account JSON
SSH private key
private scenario spoiler YAML
database dump contents
full request/response bodies containing user data
signed URLs containing credentials
```

Snapshot script에서 실행하지 말 것:

```bash
cat /opt/clueroom/app/.env
cat /opt/clueroom/secrets/env.d/*.env
cat /opt/clueroom/secrets/firebase-service-account.json
printenv
docker inspect without an explicit secret-safe --format
mysqldump
cat /opt/clueroom/secrets/scenarios/*.yaml
```

### 15.3 출력 형식

권장 형식:

```text
# ClueRoom Ops Snapshot

snapshotVersion:
capturedAt:
host:
environment:
operator:

## Summary
...

## Health
...

## Blue-Green
...

## Runtime
...

## Resources
...

## Monitoring
...

## Logs
...

## Risk Notes
...
```

Machine-readable JSON은 나중에 추가할 수 있지만, MVP 형식은 사람이 읽기 쉬워야 한다.

### 15.4 Snapshot Command Set 명령 목록

```bash
date -Is
hostname
whoami
pwd
cd /opt/clueroom/app
git status --short
git log --oneline -5
/opt/clueroom/bg-status.sh
curl -I --max-time 10 https://api.clueroom.xyz/actuator/health
curl -s --max-time 10 https://api.clueroom.xyz/actuator/health
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
docker compose ps
free -m
df -h
docker system df
docker stats --no-stream
sudo nginx -t
curl -s --max-time 10 http://localhost:9090/-/healthy
curl -s --max-time 10 http://localhost:3000/api/health
```

Active app log 수집:

```bash
/opt/clueroom/bg-status.sh
# active slot이 app-blue인 경우:
/opt/clueroom/bg-compose logs --tail=120 app-blue
# active slot이 app-green인 경우:
/opt/clueroom/bg-compose logs --tail=120 app-green
sudo tail -n 100 /var/log/nginx/error.log
```

최근 deploy context는 사람이 secret을 검토한 뒤 최신 GitHub Actions `Backend CD` run에서 가져오는 것이 좋다.
`/opt/clueroom/logs/deploy.log`가 있다고 가정하지 않는다.

### 15.5 Parser Rules 파서 규칙

Disk source-of-truth 우선순위:

```text
1. prod는 SERVER_HEALTH disk_max_percent
2. data는 DATA_HEALTH disk_max_percent
3. ops는 OPS_HEALTH disk_max_percent
4. fallback: matching host section의 df output
5. 사용 금지: logs, curl output, docker stats, HTTP headers, prose에 나온 임의 percentage token
```

Memory 해석:

```text
- Prod memory는 가능한 경우 available memory로 표시한다.
- available memory < 200MB는 service impact, OOM, restart loop, active health failure와 함께 나타날 때만 CRITICAL로 본다.
- available memory < 500MB는 기본적으로 WARNING/report-grade다.
- memory WARNING 단독은 hourly Slack noise가 아니라 manual review 또는 24h report로 보낸다.
```

Notification 역할 분리:

```text
Grafana Alert: event notification.
Ops Snapshot Agent: periodic status report.
```

### 15.6 Ops Snapshot Script PoC 절차

Repository path 저장소 경로:

```text
scripts/ops-snapshot.sh
```

Production install path 운영 설치 경로:

```text
/opt/clueroom/ops-snapshot.sh
```

PR merge와 사람 승인 후 설치:

```bash
cd /opt/clueroom/app
cp scripts/ops-snapshot.sh /opt/clueroom/ops-snapshot.sh
chmod +x /opt/clueroom/ops-snapshot.sh
bash -n /opt/clueroom/ops-snapshot.sh
```

실행:

```bash
/opt/clueroom/ops-snapshot.sh | tee /tmp/clueroom-ops-snapshot.txt
```

Secret smoke 민감값 점검:

```bash
grep -Ei 'AWS_SECRET|OPENAI_API_KEY|DB_PASSWORD|PRIVATE KEY|BEGIN|SLACK_WEBHOOK|FIREBASE|SERVICE_ACCOUNT' /tmp/clueroom-ops-snapshot.txt && echo "POTENTIAL SECRET FOUND" || echo "snapshot redaction OK"
```

Grep은 warning이나 documentation text 안의 key name도 잡을 수 있다.
중요한 것은 실제 secret 값이 출력에 나타나지 않는 것이다.

### 15.7 Secret-Safe Review Checklist 점검표

```text
[ ] .env 값 없음
[ ] API key 없음
[ ] Firebase JSON 없음
[ ] SSH key 없음
[ ] database dump content 없음
[ ] private scenario spoiler YAML 없음
[ ] signed URL credential 없음
[ ] log가 bounded됨
[ ] Blue-Green active/standby 해석 포함
[ ] external health result 포함
[ ] disk severity가 가능한 경우 heartbeat disk_max_percent를 근거로 함
[ ] WARNING/INFO output은 Grafana Alert event가 아니면 report/manual-review로 표시됨
```

---

## 16. Monitoring Agent 운영 모델

이 섹션은 `MONITORING_AGENT_PLAN.md`를 흡수한다.
Monitoring Agent는 운영 signal을 요약하고 위험을 분류하는 보조자이며, production을 자동 변경하지 않는다.

### 16.1 목적과 입력

Monitoring Agent의 목적:

```text
- Grafana/Loki/health snapshot을 읽고 요약한다.
- 반복적인 alert 해석을 줄인다.
- Blue-Green 오탐을 줄인다.
- data/prod/ops heartbeat를 함께 해석한다.
- 사람에게 다음 read-only check 또는 승인 필요한 작업을 제안한다.
```

허용 입력:

```text
- Ops Snapshot output after human secret review
- Grafana alert payload without secrets
- bounded Loki log excerpts
- SERVER_HEALTH / DATA_HEALTH / OPS_HEALTH heartbeat summaries
- GitHub Actions failure summaries after secret review
```

금지 입력:

```text
- raw .env
- API keys
- Firebase JSON
- DB dump
- private scenario YAML
- unbounded logs
- raw user request/response bodies
```

### 16.2 Output JSON Format 출력 형식

Monitoring Agent는 가능하면 아래와 같은 구조화 output을 만든다.

```json
{
  "status": "INFO|WARNING|CRITICAL",
  "summary": "short human-readable summary",
  "evidence": [
    {
      "source": "SERVER_HEALTH|DATA_HEALTH|OPS_HEALTH|Grafana|Loki|Snapshot",
      "detail": "bounded evidence without secrets"
    }
  ],
  "suspectedLayer": "client|nginx|app|data|ops|external-provider|unknown",
  "recommendedNextChecks": [
    "read-only command or dashboard check"
  ],
  "requiresHumanApproval": false,
  "proposedChange": null,
  "rollbackHint": null,
  "confidence": "low|medium|high"
}
```

상태 의미:

```text
INFO: 운영 영향 없음. 주기 리포트 또는 참고용.
WARNING: 확인 필요. 즉시 변경보다 추가 read-only check 우선.
CRITICAL: 사용자 영향 또는 데이터 위험 가능성이 높음. 사람에게 즉시 알림.
```

Agent는 확신이 낮으면 `confidence=low`를 표시하고, 단정하지 않는다.

### 16.3 Severity와 Failure Type

대표 failure type:

```text
EXTERNAL_HEALTH_DOWN
ACTIVE_SLOT_UNHEALTHY
BOTH_APP_SLOTS_DOWN
NGINX_CONFIG_INVALID
DATA_MYSQL_DOWN
DATA_REDIS_DOWN
DATA_BACKUP_STALE
S3_BACKUP_FAILED
OPS_LOKI_DOWN
OPS_N8N_DOWN
PROMETHEUS_SCRAPE_GAP
APP_ERROR_SPIKE
NGINX_5XX_SPIKE
AI_FAILURE_SPIKE
AI_FALLBACK_SPIKE
DISK_PRESSURE
MEMORY_PRESSURE
DEPLOY_SCRIPT_FAIL
AI_COST_RISK
OBSERVABILITY_GAP
```

Severity 규칙:

```text
- External API health failure는 transient로 증명되기 전까지 CRITICAL이다.
- Active upstream health failure는 CRITICAL이다.
- app-blue 또는 app-green down 단독은 CRITICAL이 아니다.
- 두 app slot이 모두 down이면 CRITICAL이다.
- Nginx syntax failure는 reload 상태에 따라 WARNING 또는 CRITICAL이다.
- Disk severity는 가능한 경우 heartbeat disk_max_percent에서 가져온다.
- Prod available-memory WARNING 단독은 OOM, restart loop, active health failure, user-facing impact와 함께 나타나지 않으면 report-grade다.
```

### 16.4 실행과 Notification Path

권장 도입 순서:

```text
Phase 1: 사람이 검토한 Ops Snapshot으로 Manual / Local PoC
Phase 2: Discord/Slack manual alert summary
Phase 3: infra server separation
Phase 4: scheduled summary 또는 webhook payload용 n8n workflow
Phase 5: team log sharing이 필요해지면 ops Loki/Alloy log search와 retention policy 확장
Phase 6: n8n으로 부족할 때만 dedicated worker 검토
```

현재 production app server에 Loki와 n8n을 한꺼번에 추가하지 않는다.
현재 ops-side Loki/Alloy는 read-only signal source로 사용할 수 있다.
주의점은 app server에 runtime service를 더 올리지 않는 것이다.

Notification policy 알림 정책:

```text
CRITICAL -> Slack immediate notification 허용
WARNING / INFO -> manual execution summary 또는 24h report 권장
Event-style alerting -> Grafana Alert 담당
Periodic state reporting -> Ops Snapshot Agent 담당
```

### 16.5 False Positive 방지

Prometheus는 아래 job을 scrape할 수 있다.

```text
clueroom-app
clueroom-app-blue
clueroom-app-green
```

`app-blue target down` 또는 `app-green target down` 단독으로 critical alert를 만들지 않는다.
더 강한 signal을 사용한다.

```text
- external /actuator/health
- /opt/clueroom/bg-status.sh
- active Nginx upstream
- both app targets down
- active slot health fail
```

Exporter gap은 확실하지 않은 사실을 만들어내지 말고 `OBSERVABILITY_GAP`으로 보고한다.

| Signal | 현재 MVP | 정확한 Alert에 필요한 것 |
|---|---|---|
| host disk/cpu/memory | manual command 또는 heartbeat bridge | node_exporter |
| container CPU/RAM | `docker stats` manual check 수동 확인 | cAdvisor |
| MySQL health | app health 또는 manual check | mysqld_exporter 또는 health bridge |
| Redis health | app health 또는 manual check | redis_exporter 또는 health bridge |

AI cost 방어는 계층적으로 처리한다.

```text
1. Edge-level bot traffic은 Nginx IP rate limit으로 방어한다.
2. userId, sessionId, scenarioId, featureType 기준 Redis backend quota를 둔다.
3. interrogation, scenario validation, final deduction용 AI quota를 분리한다.
4. quota hit와 fallback usage를 metric 또는 log summary로 관측한다.
```

### 16.6 현재 n8n Infra/Ops Workflow

현재 ops-side n8n export에는 아래 Infra/Ops workflow가 있다.

| Workflow | 역할 | 입력 signal | Agent 경계 |
|---|---|---|---|
| `ClueRoom - Ops Snapshot Agent v5 Lite Daily Budget` | periodic ops status report | `/opt/clueroom/ops-snapshot.sh`, `DATA_HEALTH`, `SERVER_HEALTH` | Report-only. Gemini analysis는 보조이며 production을 변경하면 안 된다. |
| `ClueRoom - Infra Codex Handoff Report v1` | daily Codex-ready infra handoff | Ops snapshot, `DATA_HEALTH`, `SERVER_HEALTH`, `OPS_HEALTH`, recent Nginx 5xx, recent app ERROR/Exception, recent `AI_CALL` | Handoff-only. Codex는 review와 action 제안을 할 수 있지만 production 변경에는 사람 승인이 필요하다. |
| `ClueRoom - Grafana Alert Router v8 Budgeted Gemini 3.5` | event alert router | Grafana alert payload와 related Loki logs | Notification-only. Basic Slack alert가 우선이며 Gemini analysis는 선택 context다. |

Ops Snapshot Agent deterministic check 결정적 점검:

```text
- SSH/snapshot execution failure
- snapshot의 external API health signal
- Nginx syntax pass/fail pattern
- DATA_HEALTH MySQL/Redis/disk/backup status
- SERVER_HEALTH prod disk status
- recent ERROR/Exception/FATAL pattern count
```

Workflow severity 규칙:

```text
CRITICAL:
- snapshot/SSH collection failed
- DATA_HEALTH 또는 SERVER_HEALTH가 CRITICAL 보고
- data MySQL/Redis failure
- heartbeat 기준 data/prod disk CRITICAL
- Nginx syntax failure pattern
- recent ERROR/Exception/FATAL pattern count가 매우 높음

WARNING:
- DATA_HEALTH 또는 SERVER_HEALTH가 WARNING 보고
- heartbeat 기준 prod/data disk WARNING
- bounded recent ERROR/Exception pattern 존재
```

Workflow는 deterministic Slack을 먼저 보낸다.
Gemini analysis는 선택이며 daily budget/retry policy로 제한된다.
Gemini를 사용할 수 없어도 workflow는 basic status report를 반드시 생성해야 한다.

Codex handoff report는 incident automation이 아니다.
사람 또는 PR-review Codex session에 전달하는 구조화 input이다.
Handoff에는 log sample이 포함될 수 있지만 `.env`, API key, DB password, private key, Firebase JSON, private scenario spoiler YAML, raw request/response body, database dump를 포함하면 안 된다.
