# Lightsail App Scale-out Terraform

Creates temporary ClueRoom app nodes for the Terraform app node scale-out and scripted manual Nginx LB PoC.

This module is not autoscaling and does not create a production load balancer. It creates temporary app nodes that are attached by prod-controlled Nginx upstream scripts.

This module creates:

- Lightsail app instances, such as `clueroom-app-01`, `clueroom-app-02`
- a Lightsail key pair from the prod control public key
- restricted SSH port 22 rule for explicit emergency/team CIDRs
- bootstrap user data that installs Docker, swap, and `/opt/clueroom` directories

Do not commit local runtime files:

```text
.terraform/
.terraform.lock.hcl
*.tfvars
*.tfplan
terraform.tfstate
terraform.tfstate.*
```

Required variables:

| Variable | Source |
|---|---|
| `prod_control_public_key` | Public key whose corresponding SSH credential is stored on the prod control server |
| `emergency_public_key` | Optional local/team emergency public key |
| `ssh_allowed_cidrs` | Explicit team/prod control SSH CIDRs. Do not use `0.0.0.0/0`. |
| `app_servers` | Map of temporary app nodes to create |

Example 2-node plan:

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

After apply, export output JSON for the prod inventory builder:

```bash
terraform output -json app_private_ips > app_private_ips.json
terraform output -json app_public_ips > app_public_ips.json
```

Move those JSON files to `/opt/clueroom/scaleout/terraform-output/` on the prod server. Do not commit them.

Destroy is a separate cleanup step. Before applying a destroy plan, confirm the plan only targets temporary app nodes, the scaleout key pair, and related Lightsail public port resources.
