# ClueRoom Scale-out + 수동 Nginx LB 운영 Runbook

## 범위

이 runbook은 2026-06-12에 검증한 Terraform 기반 app node scale-out PoC와 수동 Nginx load balancing 절차를 다룬다.

이 절차는 오토스케일링이 아니며 기본 운영 기준 구조도 아니다. 기본 운영 구조는 기존 prod/data/ops Lightsail 구성과 prod 서버의 active Blue-Green app slot이다.

## 제어 주체

| 주체 | 역할 |
|---|---|
| 로컬 작업 PC | Terraform app node 생성/삭제만 수행 |
| prod server | app node sync/start/check, Nginx upstream apply/rollback 수행 |
| app01/app02 | 임시 Spring Boot app runtime node |
| data server | shared MySQL/Redis 기준 저장소 |
| ops server | Loki/Grafana 관측 |

## Repository 파일

```text
scripts/scaleout/*.sh
infra/terraform/lightsail-app-scaleout/
docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md
docs/infra/poc/POC-006-scaleout-manual-lb.md
```

로컬 Terraform runtime 파일은 커밋하지 않는다.

```text
.terraform/
.terraform.lock.hcl
*.tfvars
*.tfplan
terraform.tfstate
terraform.tfstate.*
```

`/opt/clueroom/secrets/**`, Firebase JSON, JWT secret, DB password, OAuth 값, API key는 커밋하지 않는다.

## Terraform App Node 생성

Terraform은 local workstation에서만 실행한다.

```bash
cd infra/terraform/lightsail-app-scaleout
export AWS_PROFILE=clueroom-scaleout

PROD_CONTROL_PUBLIC_KEY="$(cat /tmp/clueroom-app-node-control.pub)"
EMERGENCY_PUBLIC_KEY="$(cat ~/.ssh/clueroom-scaleout-emergency.pub)"
SSH_ALLOWED_CIDRS='["203.0.113.10/32"]'

terraform init
terraform plan \
  -var-file=scaleout-2nodes.tfvars \
  -var "prod_control_public_key=${PROD_CONTROL_PUBLIC_KEY}" \
  -var "emergency_public_key=${EMERGENCY_PUBLIC_KEY}" \
  -var "ssh_allowed_cidrs=${SSH_ALLOWED_CIDRS}" \
  -out=scaleout-2nodes.tfplan
terraform apply scaleout-2nodes.tfplan
```

prod inventory builder가 사용할 output을 export한다.

```bash
terraform output -json app_private_ips > app_private_ips.json
terraform output -json app_public_ips > app_public_ips.json
```

JSON output 파일은 커밋하지 않고 prod 서버 control directory로만 옮긴다.

## Prod 제어 환경 준비

prod 서버에서는 스크립트를 `/opt/clueroom/scaleout/scripts` 아래에 두고, node inventory는 `/opt/clueroom/scaleout/nodes` 아래에 둔다.

Terraform output을 `/opt/clueroom/scaleout/terraform-output`로 복사했다면 node env 파일을 생성한다.

```bash
/opt/clueroom/scaleout/scripts/build-node-envs-from-terraform-output.sh
```

기존 `scaleout.env`에서 one-node/manual bootstrap을 할 때는 아래 명령을 사용한다.

```bash
/opt/clueroom/scaleout/scripts/init-inventory-from-current.sh
```

디버깅할 때는 node를 명시적으로 선택한다.

```bash
/opt/clueroom/scaleout/scripts/select-scaleout-node.sh app01
```

## App Node 생명주기

아래 명령은 prod 서버에서 실행한다.

```bash
/opt/clueroom/scaleout/scripts/run-all-nodes.sh sync
/opt/clueroom/scaleout/scripts/run-all-nodes.sh start
/opt/clueroom/scaleout/scripts/run-all-nodes.sh verify-secrets
/opt/clueroom/scaleout/scripts/run-all-nodes.sh check
/opt/clueroom/scaleout/scripts/run-all-nodes.sh alloy
```

기대 검증 결과:

```text
app node local health: 200
prod-to-app-node private health: 200
DB_HOST / REDIS_HOST: data 서버를 가리킴
JWT_SECRET=set
firebase_container=OK
scenario_container_count: 0보다 큼
Loki: app node docker-app 로그 확인 가능
```

검증 스크립트는 secret에 대해 set/empty 또는 file-visible 상태만 출력해야 한다. secret 내용 자체를 출력하면 안 된다.

## Nginx Upstream 적용

apply 스크립트는 `X-ClueRoom-Upstream`, `/etc/nginx/conf.d/clueroom-upstream.conf`, `/opt/clueroom/bg-status.sh` 중 가능한 경로에서 active local Blue-Green upstream을 감지한다.

canary mode는 local active slot의 weight를 더 높게 유지한다.

```bash
/opt/clueroom/scaleout/scripts/apply-nginx-scaleout-upstream.sh canary
```

equal mode는 local active slot과 healthy app node 사이에 트래픽을 분산한다.

```bash
/opt/clueroom/scaleout/scripts/apply-nginx-scaleout-upstream.sh equal
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 60 scaleout
```

2026-06-12 PoC의 equal mode upstream:

```text
127.0.0.1:8081
172.26.6.201:8080
172.26.2.166:8080
```

60회 요청 분산 결과는 거의 1:1:1이었다.

```text
21 / 19 / 20
```

## 알려진 Nginx 문제와 Guard

PoC 중 초기 upstream rewrite 경로에서 `127.0.0.1:80` 또는 active app port가 없는 `127.0.0.1` 같은 잘못된 local backend가 생성된 적이 있었다. Blue-Green app slot은 `127.0.0.1:8081` 또는 `127.0.0.1:8082`에서 동작하므로 이 값은 잘못된 설정이다.

현재 스크립트 guardrail:

```text
- active local upstream은 127.0.0.1:8081 또는 127.0.0.1:8082여야 함
- local upstream에 port가 없으면 생성된 upstream config를 거부함
- 127.0.0.1:80 또는 port 없는 127.0.0.1이 보이면 분산 검증 실패
- reload 전에 Nginx config test 수행
```

잘못된 upstream이 감지되면 즉시 rollback한다.

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
```

## Loki 검증

app node 로그는 ops 서버 또는 Loki에 접근 가능한 host에서 조회한다.

```bash
START_NS="$(date -u -d '24 hours ago' +%s%N)"
END_NS="$(date -u +%s%N)"

for NODE in clueroom-app-01 clueroom-app-02; do
  curl -G -s "http://127.0.0.1:3100/loki/api/v1/query_range" \
    --data-urlencode "query={job=\"docker-app\", instance=\"$NODE\"}" \
    --data-urlencode "start=$START_NS" \
    --data-urlencode "end=$END_NS" \
    --data-urlencode 'limit=20' \
    --data-urlencode 'direction=backward' \
  | jq -r '.data.result[] as $s | $s.values[] | "\((.[0][0:10] | tonumber | strftime("%Y-%m-%d %H:%M:%S"))) [\($s.stream.container // "-")] \(.[1])"'
done
```

health check가 항상 Spring application log를 남기지는 않는다. startup filter 결과가 비어 있으면 app 장애로 판단하기 전에 조회 시간을 넓히고 전체 `docker-app` stream을 확인한다.

## Rollback 및 정리

Nginx를 local active only 상태로 복구한다.

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 20 local-only
```

app node runtime을 정리한다.

```bash
/opt/clueroom/scaleout/scripts/run-all-nodes.sh reset
```

Terraform destroy는 별도의 로컬 작업 PC 절차다. 적용 전에 destroy plan 대상이 임시 app node, scaleout key pair, 관련 Lightsail port resource뿐인지 확인한다.
