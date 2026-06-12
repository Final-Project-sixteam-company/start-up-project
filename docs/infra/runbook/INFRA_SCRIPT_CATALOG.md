# ClueRoom 인프라 쉘 스크립트 카탈로그

작성일: 2026-06-12

이 문서는 repo에 있는 인프라/운영용 쉘 스크립트의 실행 위치, 위험도, 용도, 사전 확인, 롤백 기준을 정리한다. 상세 실행 절차는 [OPS_RUNBOOK.md](../OPS_RUNBOOK.md), [SCALEOUT_MANUAL_LB_RUNBOOK.md](SCALEOUT_MANUAL_LB_RUNBOOK.md), [CLUEROOM_INFRA_TROUBLESHOOTING.md](CLUEROOM_INFRA_TROUBLESHOOTING.md)를 따른다.

## 1. 공통 규칙

- 운영 서버 기준 경로는 `/opt/clueroom`이다.
- repo의 `scripts/*.sh`는 운영 서버에서 `/opt/clueroom/*.sh` 또는 `/opt/clueroom/scaleout/scripts/*.sh`로 배치돼 실행될 수 있다.
- `/opt/clueroom/...` 경로를 쓰는 스크립트는 로컬 PC에서 실행해도 정상 동작하지 않는다.
- secret 값, Firebase JSON 내용, DB password, JWT, API key를 터미널/문서/AI 채팅에 붙여넣지 않는다.
- 변경성 스크립트는 실행 전 `bash -n`으로 문법을 확인한다.
- 운영 traffic, Nginx upstream, app node runtime을 바꾸는 스크립트는 실행 전 rollback 경로를 확인한다.
- scale-out 관련 스크립트는 PoC용이다. 기본 운영 구조는 prod/data/ops 3서버와 prod Blue-Green active slot이다.

## 2. 위험도 기준

| 위험도 | 의미 |
|---|---|
| READ_ONLY | 상태 조회/검증만 수행한다. 운영 상태를 바꾸지 않는다. |
| LOCAL_DEV | 로컬 개발 환경의 compose/build 상태만 바꾼다. 운영 서버 대상이 아니다. |
| MAINTENANCE | 백업, snapshot, inventory 작성, standby 정리처럼 제한된 운영 보조 작업을 수행한다. |
| DEPLOY | 컨테이너 기동, 파일 동기화, Nginx reload, traffic 전환 등 운영 상태를 바꾼다. |
| DESTRUCTIVE | 컨테이너 제거, runtime 삭제, reset처럼 복구 확인 없이 실행하면 영향이 큰 작업이다. |
| ARG_DEPENDENT | wrapper/helper라서 전달한 인자에 따라 위험도가 달라진다. |

## 3. 일반 운영 스크립트

| 스크립트 | 주 실행 위치 | 위험도 | 용도 | 실행 전 확인 | 롤백/주의 |
|---|---|---|---|---|---|
| `scripts/compose-up.sh` | 로컬 개발 PC | LOCAL_DEV | `bootJar` 후 로컬 Docker Compose를 build/up 한다. | Docker 실행 상태, Gradle wrapper, 로컬 port 충돌 | 운영 서버 배포용이 아니다. |
| `scripts/compose-down.sh` | 로컬 개발 PC | LOCAL_DEV | 로컬 Docker Compose를 down 한다. named volume은 보존한다. | 현재 compose project가 로컬 개발용인지 확인 | DB 초기화가 필요하면 별도로 `docker compose down -v`를 직접 판단한다. |
| `scripts/bg-compose.sh` | prod 서버 | ARG_DEPENDENT | Blue-Green/external-data compose file과 secret env를 묶어 `docker compose` 명령을 실행하는 helper다. | `/opt/clueroom/app/.env`, `/opt/clueroom/secrets/env.d/*` 읽기 권한 | 전달한 `up`, `stop`, `logs`, `ps` 등에 따라 위험도가 달라진다. |
| `scripts/bg-status.sh` | prod 서버 | READ_ONLY | 현재 active upstream, active/standby service, 외부 health, container 상태를 출력한다. | `/etc/nginx/conf.d/clueroom-upstream.conf`, `/opt/clueroom/bg-compose` 존재 | 조회 전용이다. active port가 `8081/8082`가 아니면 즉시 원인 확인한다. |
| `scripts/deploy-bluegreen.sh` | prod 서버 | DEPLOY | `develop` 최신 코드를 pull/build하고 standby slot을 새 target으로 띄운 뒤 Nginx upstream을 전환한다. 운영 배치 경로는 `/opt/clueroom/deploy.sh`다. | git 상태, secret env, active port, target health, Nginx config | 실패 시 upstream backup 또는 이전 active port로 rollback한다. 성공 후 이전 slot은 수동 정리한다. |
| `scripts/rollback-bluegreen.sh` | prod 서버 | DEPLOY | 현재 active 반대편 slot을 health 확인 후 Nginx upstream 대상으로 되돌린다. | rollback target container가 존재하는지, target health | target이 없으면 새로 build하지 않는다. rollback 후 문제 slot은 수동 중지한다. |
| `scripts/stop-standby.sh` | prod 서버 | MAINTENANCE | active가 아닌 standby slot을 중지한다. | 외부 health OK, active upstream 감지 | 중지 후 health 실패 시 standby를 다시 start하는 자동 복구가 있다. |
| `scripts/backup-mysql.sh` | prod 서버 local-data/rollback copy | MAINTENANCE | compose `mysql` DB를 gzip dump로 백업하고 7일 초과 백업을 삭제한다. | `/opt/clueroom/app/.env`, compose `mysql`, DB_NAME/DB_PASSWORD | external-data 운영에서 source of truth 백업이 아니다. data server 백업 절차와 구분한다. |
| `scripts/ops-snapshot.sh` | prod 서버 | READ_ONLY | git, Blue-Green, external data, Docker, Nginx, monitoring, Loki/Alloy, resource 상태를 redaction 후 출력한다. | Docker, curl, optional jq, sudo -n 권한 | 조회 전용이지만 출력 공유 전 secret redaction 여부를 육안 확인한다. |

## 4. Scale-out PoC 제어 스크립트

이 섹션의 스크립트는 Terraform 기반 app node scale-out PoC용이다. 운영 기준 구조 전환용이 아니며, PoC 종료 후에는 Nginx를 local-only로 되돌리고 app node runtime을 정리해야 한다.

| 스크립트 | 주 실행 위치 | 위험도 | 용도 | 실행 전 확인 | 롤백/주의 |
|---|---|---|---|---|---|
| `scripts/scaleout/init-inventory-from-current.sh` | prod 서버 | MAINTENANCE | 기존 `scaleout.env`에서 `common.env`와 단일 node env를 생성한다. | `/opt/clueroom/scaleout/scaleout.env` 존재, private IP/Loki URL 값 | `/opt/clueroom/scaleout/*.env`는 운영 제어 파일이므로 public 문서에 붙이지 않는다. |
| `scripts/scaleout/build-node-envs-from-terraform-output.sh` | prod 서버 | MAINTENANCE | Terraform output JSON에서 `/opt/clueroom/scaleout/nodes/*.env`를 생성한다. | `app_private_ips.json`, `app_public_ips.json`, `jq` | Terraform output 파일은 repo에 커밋하지 않는다. |
| `scripts/scaleout/select-scaleout-node.sh` | prod 서버 | MAINTENANCE | 선택한 node env와 `common.env`를 합쳐 active `scaleout.env`를 만든다. | node key, `common.env`, `nodes/<key>.env` | 이후 sync/start/check 대상이 바뀐다. public IP와 Loki push URL은 출력 시 redacted 처리한다. |
| `scripts/scaleout/run-node-action.sh` | prod 서버 | ARG_DEPENDENT | 단일 node 선택 후 `sync/start/check/reset/alloy/verify-secrets/scrub-secrets` 중 하나를 실행한다. | node key, action 오타 여부 | action의 위험도를 그대로 따른다. `reset`, `scrub-secrets`는 DESTRUCTIVE다. |
| `scripts/scaleout/run-all-nodes.sh` | prod 서버 | ARG_DEPENDENT | 모든 node에 동일 action을 순차 실행한다. | nodes directory 대상 목록 | 모든 node에 영향을 준다. `reset`, `scrub-secrets`, `sync`, `start`, `alloy`는 특히 재확인한다. |
| `scripts/scaleout/sync-app-node.sh` | prod 서버 -> app node | DEPLOY | prod의 app source, jar, env, secret env, Firebase/scenario secret을 app node로 동기화한다. | SSH key, node private IP, prod app files, bootJar 성공 | `.private`, Terraform 산출물, key 파일은 tar 제외된다. secret 내용은 출력하지 않는다. |
| `scripts/scaleout/sync-app-node-extra-secrets.sh` | prod 서버 -> app node | DEPLOY | Firebase service account와 scenario secret directory를 app node로 복사한다. | prod secret 파일/디렉터리 존재, scenario file count | 파일 존재와 크기만 확인한다. secret JSON 내용은 출력하지 않는다. Firebase 전송용 임시 파일은 실패/중단 시 cleanup trap으로 제거하고, scenario directory는 staging 성공 후 교체한다. |
| `scripts/scaleout/start-app-node.sh` | prod 서버 -> app node | DEPLOY | app node의 `.env`를 external data 기준으로 보정하고 `start-up-app` container를 기동한다. | sync 완료, data server 3306/6379 TCP, runtime env 파일 생성 | health timeout 시 container를 제거하고 진단 로그를 출력한다. secret이 합쳐진 runtime env는 종료/중단 시 cleanup한다. |
| `scripts/scaleout/check-app-node-health.sh` | prod 서버 -> app node | READ_ONLY | app node local health, prod->node health, container env/secret visible 상태를 확인한다. | app node SSH, app container running | JWT secret은 존재 여부만 출력한다. 값 출력 금지. |
| `scripts/scaleout/verify-app-node-secrets.sh` | prod 서버 -> app node | READ_ONLY | host/container에서 Firebase, scenarios, JWT secret 존재 여부를 확인한다. | app node SSH, optional app container running | 값은 출력하지 않는다. container count는 container 내부에서 계산하고, 존재 여부와 count만 본다. |
| `scripts/scaleout/install-app-node-alloy.sh` | prod 서버 -> app node | DEPLOY | app node에 Grafana Alloy container를 설치/재시작하고 Docker app log를 ops Loki로 전송한다. | `OPS_LOKI_PUSH_URL`, Docker socket, app node SSH | 기존 `clueroom-app-node-alloy` container를 제거 후 재생성한다. Loki push URL이 들어간 local/remote temp config와 최종 `config.alloy`는 `600` 권한으로 유지한다. |
| `scripts/scaleout/reset-app-node-runtime.sh` | prod 서버 -> app node | DESTRUCTIVE | app node의 `start-up-app`, Alloy container, runtime env 파일을 제거한다. | PoC 종료/정리 대상 node 확인 | 실행 후 해당 app node는 traffic 대상이면 안 된다. 먼저 Nginx local-only rollback을 확인한다. |
| `scripts/scaleout/scrub-app-node-secrets.sh` | prod 서버 -> app node | DESTRUCTIVE | PoC app node에 복사된 env.d, Firebase JSON, scenario files, app `.env`, Alloy config를 제거한다. | Nginx local-only rollback 완료, target node 확인, `CONFIRM_SCRUB_APP_NODE_SECRETS=YES` | destroy 전 app node를 잠시 유지할 때 secret 잔존을 줄인다. 실행 후 app runtime은 재기동할 수 없다. |
| `scripts/scaleout/apply-nginx-scaleout-upstream.sh` | prod 서버 | DEPLOY | local active backend와 healthy app nodes를 Nginx upstream에 넣고 reload한다. `canary/equal` 모드 지원. | local active health, node health, `common.env`, `nodes/*.env` | flock으로 동시 실행을 막는다. 적용 전 upstream backup 생성. `nginx -t`/reload 실패 시 backup으로 자동 원복한다. `127.0.0.1:80`/port 누락 guard가 있다. |
| `scripts/scaleout/rollback-nginx-scaleout-upstream.sh` | prod 서버 | DEPLOY | Nginx upstream을 local-only 또는 최신 backup으로 되돌린다. | local active `8081/8082` 감지, Nginx config | flock으로 동시 실행을 막는다. 기본은 `local-only`다. rollback 적용 전 backup을 만들고 실패 시 원복한다. PoC 종료 시 먼저 이 스크립트로 app node traffic을 제거한다. |
| `scripts/scaleout/verify-lb-distribution.sh` | 로컬 PC 또는 prod 서버 | READ_ONLY | public health header의 `X-ClueRoom-Upstream` 분산 결과를 집계하고 기대 상태를 검증한다. | 기대값 `scaleout/local-only/any`, 요청 횟수 | health 요청만 보낸다. `127.0.0.1:80` 또는 port 누락이 보이면 즉시 rollback한다. |

## 5. 대표 실행 흐름

### 로컬 개발

```bash
scripts/compose-up.sh
scripts/compose-down.sh
```

운영 배포와 무관하다. 로컬 DB 초기화가 필요한 경우에만 별도로 `docker compose down -v`를 판단한다.

### Blue-Green 운영 배포

```bash
/opt/clueroom/bg-status.sh
/opt/clueroom/deploy.sh
/opt/clueroom/bg-status.sh
/opt/clueroom/stop-standby.sh
```

문제 발생 시:

```bash
/opt/clueroom/rollback-bluegreen.sh
```

### 운영 상태 공유

```bash
/opt/clueroom/ops-snapshot.sh
```

출력 공유 전 secret, token, password, private key, webhook URL이 남아 있지 않은지 확인한다.

### Scale-out PoC

```bash
/opt/clueroom/scaleout/scripts/build-node-envs-from-terraform-output.sh
/opt/clueroom/scaleout/scripts/run-all-nodes.sh sync
/opt/clueroom/scaleout/scripts/run-all-nodes.sh start
/opt/clueroom/scaleout/scripts/run-all-nodes.sh check
/opt/clueroom/scaleout/scripts/run-all-nodes.sh verify-secrets
/opt/clueroom/scaleout/scripts/run-all-nodes.sh alloy
/opt/clueroom/scaleout/scripts/apply-nginx-scaleout-upstream.sh equal
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 60 scaleout
```

PoC 종료 시:

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 20 local-only
/opt/clueroom/scaleout/scripts/run-all-nodes.sh reset
CONFIRM_SCRUB_APP_NODE_SECRETS=YES /opt/clueroom/scaleout/scripts/run-all-nodes.sh scrub-secrets
```

Terraform destroy는 local workstation에서 별도 절차로 수행한다.

## 6. 변경 전 검증

스크립트 수정 PR에서는 최소한 다음을 확인한다.

```bash
bash -n scripts/*.sh
bash -n scripts/scaleout/*.sh
git diff --check
```

추가로 scale-out 관련 수정이면 다음도 확인한다.

```bash
git grep -n -E "127[.]0[.]0[.]1:80|0[.]0[.]0[.]0/0|terraform[.]tfstate|[.]tfvars|[.]tfplan" -- scripts infra docs
```

위 검색 결과가 의도된 guard, 금지 파일 설명, 예시 CIDR 문서가 아니라면 수정한다.

## 7. 범위 제외

- `scripts/prepare-commit-msg`는 Git hook helper라서 이 인프라 운영 카탈로그에서는 제외한다.
- Terraform `plan/apply/destroy` 명령은 쉘 스크립트가 아니라 [SCALEOUT_MANUAL_LB_RUNBOOK.md](SCALEOUT_MANUAL_LB_RUNBOOK.md)의 절차로 관리한다.
