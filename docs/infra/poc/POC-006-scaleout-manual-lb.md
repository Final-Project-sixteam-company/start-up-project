# POC-006. Terraform App Node Scale-out + 수동 Nginx LB

## 상태

2026-06-12 PoC 검증 완료.

이 문서는 오토스케일링 완료 보고서가 아니며, 현재 운영 기준 구조 전환 문서도 아니다. ClueRoom이 Terraform으로 임시 app node를 추가하고, prod 서버에서 제어하는 수동 Nginx upstream 변경으로 app layer scale-out을 검증했다는 기록이다.

## 검증 결과

```text
Terraform으로 app01/app02 생성
prod inventory로 app node 관리
prod에서 jar/env/secrets/scenarios 동기화
app01/app02가 8080 포트에서 Spring Boot 기동
app01/app02가 data 서버 MySQL/Redis에 연결
app node Alloy가 docker-app 로그를 ops Loki로 전송
Nginx upstream에 local active + app01 + app02 포함
equal mode 60회 요청 분산 결과 21 / 19 / 20 확인
```

검증된 equal mode upstream:

```text
127.0.0.1:8081
172.26.6.201:8080
172.26.2.166:8080
```

## PoC 인증샷

아래 이미지는 2026-06-12 PoC 검증 인증샷의 공개용 사본이다. public IPv4/IPv6 값과 로그인 source IP는 마스킹했다. secret 값, Firebase JSON 내용, DB password, JWT 값, API key는 포함하지 않았다.

app node private upstream, data/ops 내부 endpoint처럼 이미 이 문서에 검증값으로 기록된 RFC1918 private IP는 라우팅 증거로 남겨뒀다.

<details>
<summary>1. prod 서버에서 생성한 app node inventory</summary>

<img src="assets/scaleout-20260612/scaleout-01-node-inventory-redacted.png" alt="public IP를 마스킹한 scale-out app node inventory" width="820">

</details>

<details>
<summary>2. app private/public IP map Terraform output</summary>

<img src="assets/scaleout-20260612/scaleout-02-terraform-output-redacted.png" alt="public IP를 마스킹한 app node IP map Terraform output" width="820">

</details>

<details>
<summary>3. app01/app02 runtime secret 및 scenario 검증</summary>

<img src="assets/scaleout-20260612/scaleout-03-secret-runtime-verify-redacted.png" alt="Firebase, scenario count, JWT_SECRET set 상태를 확인한 app node runtime 검증" width="720">

</details>

<details>
<summary>4. prod 제어 흐름에서 확인한 app01 health</summary>

<img src="assets/scaleout-20260612/scaleout-04-app01-health-redacted.png" alt="public IP를 마스킹한 app01 health 검증" width="820">

</details>

<details>
<summary>5. Nginx equal mode upstream 및 60회 요청 분산</summary>

<img src="assets/scaleout-20260612/scaleout-05-nginx-equal-lb.png" alt="세 backend에 대한 Nginx equal mode upstream 및 60회 요청 분산 결과" width="820">

</details>

<details>
<summary>6. app01/app02 Loki startup log 검증</summary>

<img src="assets/scaleout-20260612/scaleout-06-loki-startup-logs.png" alt="app01/app02 Loki startup log" width="640">

</details>

<details>
<summary>7. prod 제어 흐름에서 확인한 app02 health</summary>

<img src="assets/scaleout-20260612/scaleout-07-app02-health-redacted.png" alt="public IP를 마스킹한 app02 health 검증" width="820">

</details>

<details>
<summary>8. PoC 당시 Lightsail console instance 상태</summary>

<img src="assets/scaleout-20260612/scaleout-08-lightsail-instances-redacted.png" alt="public IP를 마스킹한 scale-out PoC 당시 Lightsail instance 실행 상태" width="760">

</details>

## Secret 및 Runtime 검증

이 PoC에서는 app01/app02가 단순히 health만 200으로 응답하는 서버가 아니라, 실제 운영에 필요한 runtime 구성을 갖고 있는지 확인했다.

```text
JWT_SECRET=set
Firebase service account file: host/container에서 확인
scenario file count: 18
DB/Redis env: data 서버를 가리킴
```

이 문서에는 secret 값, Firebase JSON 내용, DB password, JWT 값, API key를 저장하지 않는다.

## 발견 후 수정한 Nginx 문제

초기 upstream rewrite 경로에서 `127.0.0.1:80` 또는 port 없는 `127.0.0.1` 같은 잘못된 local backend가 생성된 적이 있었다. 올바른 local active backend는 Blue-Green app slot이다.

```text
127.0.0.1:8081
또는
127.0.0.1:8082
```

최종 apply/verify 스크립트는 active local upstream port를 반드시 요구한다. `127.0.0.1:80` 또는 port 없는 `127.0.0.1`이 보이면 분산 검증을 실패시킨다.

## 이 PoC로 확인한 것

- app layer를 별도 Lightsail node에서 실행할 수 있다.
- app runtime이 shared data 서버의 MySQL/Redis를 사용할 수 있다.
- 저장소에 secret을 저장하지 않고 prod 서버에서 sync/start/check를 제어할 수 있다.
- ops Loki가 Alloy를 통해 app node 로그를 수집할 수 있다.
- Nginx가 local active와 임시 app node 사이에 수동으로 트래픽을 분산할 수 있다.
- rollback으로 local active Blue-Green slot만 남기는 상태로 복구할 수 있다.

## 한계

- DB/Redis HA를 검증한 것은 아니다.
- AWS ALB, ASG, ECS, 관리형 autoscaling을 도입한 것이 아니다.
- app01/app02를 상시 운영 기준 구조로 전환한 것이 아니다.
- 수동 rollback과 Terraform cleanup 필요성이 사라진 것이 아니다.

## Repository 산출물

| 경로 | 역할 |
|---|---|
| `scripts/scaleout/*.sh` | prod 제어형 sync/start/check/Alloy/Nginx 보조 스크립트 |
| `infra/terraform/lightsail-app-scaleout/` | 임시 Lightsail app node Terraform 모듈 |
| `docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md` | 운영 절차와 rollback 기준 |

## 운영 판단

PoC는 성공으로 본다. 다만 script와 Terraform 모듈은 재실행 가능한 PoC/runbook 산출물로 보관하고, 팀이 추가 app node 상시 운영을 명시적으로 결정하기 전까지 기본 운영 상태는 기존 prod/data/ops 기준 구조로 유지한다.
