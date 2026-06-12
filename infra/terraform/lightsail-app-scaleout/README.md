# Lightsail App Scale-out Terraform

Terraform app node scale-out 및 수동 Nginx LB PoC에 사용할 임시 ClueRoom app node를 만든다.

이 모듈은 오토스케일링이 아니며 운영 load balancer를 만들지 않는다. prod 서버에서 제어하는 Nginx upstream script에 붙일 임시 app node만 생성한다.

이 모듈이 생성하는 것:

- `clueroom-app-01`, `clueroom-app-02` 같은 Lightsail app instance
- prod control public key 기반 Lightsail key pair
- 명시한 emergency/team CIDR만 허용하는 SSH 22번 port rule
- Docker, swap, `/opt/clueroom` 디렉터리를 준비하는 bootstrap user data

로컬 runtime 파일은 커밋하지 않는다.

```text
.terraform/
.terraform.lock.hcl
*.tfvars
*.tfplan
terraform.tfstate
terraform.tfstate.*
```

필수 변수:

| 변수 | 출처 |
|---|---|
| `prod_control_public_key` | 대응되는 SSH 인증 정보가 prod control 서버에 저장된 public key |
| `emergency_public_key` | 선택 사항인 local/team 비상 public key |
| `ssh_allowed_cidrs` | 명시적인 team/prod control SSH CIDR. `0.0.0.0/0` 사용 금지 |
| `app_servers` | 생성할 임시 app node map |

2-node plan 예시:

```bash
PROD_CONTROL_PUBLIC_KEY="$(cat /tmp/clueroom-app-node-control.pub)"
EMERGENCY_PUBLIC_KEY="$(cat ~/.ssh/clueroom-scaleout-emergency.pub)"
SSH_ALLOWED_CIDRS='["203.0.113.10/32"]'

terraform plan \
  -var-file=scaleout-2nodes.tfvars \
  -var "prod_control_public_key=${PROD_CONTROL_PUBLIC_KEY}" \
  -var "emergency_public_key=${EMERGENCY_PUBLIC_KEY}" \
  -var "ssh_allowed_cidrs=${SSH_ALLOWED_CIDRS}" \
  -out=scaleout-2nodes.tfplan
```

apply 후 prod inventory builder가 사용할 output JSON을 export한다.

```bash
terraform output -json app_private_ips > app_private_ips.json
terraform output -json app_public_ips > app_public_ips.json
```

이 JSON 파일은 prod 서버의 `/opt/clueroom/scaleout/terraform-output/`로 옮긴다. 커밋하지 않는다.

destroy는 별도 정리 단계다. destroy plan을 적용하기 전에 대상이 임시 app node, scaleout key pair, 관련 Lightsail public port resource뿐인지 확인한다.
