# ClueRoom Scale-out + Manual Nginx LB Runbook

## Scope

This runbook covers the Terraform based app node scale-out PoC and scripted manual Nginx load balancing flow verified on 2026-06-12.

This is not autoscaling and not the default production baseline. The production baseline remains the existing prod/data/ops Lightsail layout with the active Blue-Green app slot on the prod server.

## Control Planes

| Plane | Role |
|---|---|
| Local workstation | Terraform app node create/destroy only |
| prod server | App node sync/start/check, Nginx upstream apply/rollback |
| app01/app02 | Temporary Spring Boot app runtime nodes |
| data server | Shared MySQL/Redis source of truth |
| ops server | Loki/Grafana observability |

## Repository Files

```text
scripts/scaleout/*.sh
infra/terraform/lightsail-app-scaleout/
docs/infra/runbook/SCALEOUT_MANUAL_LB_RUNBOOK.md
docs/infra/poc/POC-006-scaleout-manual-lb.md
```

Do not commit local Terraform runtime files:

```text
.terraform/
.terraform.lock.hcl
*.tfvars
*.tfplan
terraform.tfstate
terraform.tfstate.*
```

Do not commit `/opt/clueroom/secrets/**`, Firebase JSON, JWT secret, DB password, OAuth values, or API keys.

## Terraform App Node Creation

Run Terraform from the local workstation only.

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

Export the outputs for the prod inventory builder.

```bash
terraform output -json app_private_ips > app_private_ips.json
terraform output -json app_public_ips > app_public_ips.json
```

Move the JSON outputs to the prod server control directory without committing them.

## Prod Control Setup

On the prod server, install the scripts under `/opt/clueroom/scaleout/scripts` and keep node inventory under `/opt/clueroom/scaleout/nodes`.

If Terraform outputs were copied to `/opt/clueroom/scaleout/terraform-output`, build node env files:

```bash
/opt/clueroom/scaleout/scripts/build-node-envs-from-terraform-output.sh
```

For a one-node/manual bootstrap from an existing `scaleout.env`:

```bash
/opt/clueroom/scaleout/scripts/init-inventory-from-current.sh
```

Select a node explicitly when debugging:

```bash
/opt/clueroom/scaleout/scripts/select-scaleout-node.sh app01
```

## App Node Lifecycle

Run these from the prod server.

```bash
/opt/clueroom/scaleout/scripts/run-all-nodes.sh sync
/opt/clueroom/scaleout/scripts/run-all-nodes.sh start
/opt/clueroom/scaleout/scripts/run-all-nodes.sh verify-secrets
/opt/clueroom/scaleout/scripts/run-all-nodes.sh check
/opt/clueroom/scaleout/scripts/run-all-nodes.sh alloy
```

Expected checks:

```text
app node local health 200
prod-to-app-node private health 200
DB_HOST / REDIS_HOST point to data server
JWT_SECRET=set
firebase_container=OK
scenario_container_count is non-zero
Loki has docker-app logs for app nodes
```

The verification scripts print only set/empty or file-visible status for secrets. They must not print secret contents.

## Nginx Upstream Apply

The apply script detects the active local Blue-Green upstream from `X-ClueRoom-Upstream`, `/etc/nginx/conf.d/clueroom-upstream.conf`, or `/opt/clueroom/bg-status.sh`.

Canary mode keeps the local active slot weighted higher:

```bash
/opt/clueroom/scaleout/scripts/apply-nginx-scaleout-upstream.sh canary
```

Equal mode distributes between the local active slot and healthy app nodes:

```bash
/opt/clueroom/scaleout/scripts/apply-nginx-scaleout-upstream.sh equal
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 60 scaleout
```

The 2026-06-12 PoC equal-mode upstream contained:

```text
127.0.0.1:8081
172.26.6.201:8080
172.26.2.166:8080
```

The 60-request distribution was approximately 1:1:1:

```text
21 / 19 / 20
```

## Known Nginx Bug Guard

During the PoC, an earlier upstream rewrite produced a malformed local backend such as `127.0.0.1:80` or `127.0.0.1` without the active app port. That is invalid because the Blue-Green app slots listen on `127.0.0.1:8081` or `127.0.0.1:8082`.

Current script guardrails:

```text
- active local upstream must match 127.0.0.1:8081 or 127.0.0.1:8082
- generated upstream config is rejected if local upstream has no port
- distribution verification fails if 127.0.0.1:80 or bare 127.0.0.1 appears
- Nginx config is tested before reload
```

If malformed upstream is detected, rollback immediately.

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
```

## Loki Verification

Query app node logs from the ops server or from a host that can reach Loki.

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

Health checks may not always create Spring application logs. If startup filters return no result, widen the time range and query the full `docker-app` stream before treating it as an app failure.

## Rollback And Cleanup

Restore Nginx to local active only:

```bash
/opt/clueroom/scaleout/scripts/rollback-nginx-scaleout-upstream.sh local-only
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream'
/opt/clueroom/scaleout/scripts/verify-lb-distribution.sh 20 local-only
```

Reset app node runtime:

```bash
/opt/clueroom/scaleout/scripts/run-all-nodes.sh reset
```

Terraform destroy is a separate local workstation step. Confirm the destroy plan only targets the temporary app nodes, scaleout key pair, and related Lightsail port resources before applying.
