# ClueRoom Infra Codex Agent Playbook

> Purpose: define how Codex should support ClueRoom infrastructure documentation, PR review, deploy analysis, and read-only operational diagnosis.
>
> Status: planning document. This does not authorize autonomous production changes.

---

## 1. Infra Codex Agent Role

The Infra Codex Agent is a production-support assistant for repository work and bounded operational analysis.

Primary responsibilities:

```text
- write and update infrastructure docs
- prepare PRs for runbooks, scripts, and config templates
- review CI/CD logs and deployment output
- summarize production incidents from secret-safe snapshots
- propose safe read-only checks
- help draft rollback plans
- improve monitoring and LLMOps documentation
```

Non-responsibilities:

```text
- autonomous production deploy
- autonomous rollback
- secret management
- direct server mutation without explicit human approval
- replacing the human infra owner
```

The agent should default to PR-based work.

---

## 2. Execution Locations

### 2.1 IntelliJ Local Codex

This is the default and preferred execution location.

Allowed scope:

```text
- local repository file reads
- local repository documentation edits
- script draft changes for PR review
- test and lint command execution
- git diff review
- PR title/body drafting
```

Production server direct mutation is not allowed from this mode.

The expected workflow is:

```text
local edit -> local validation -> commit -> PR -> review -> human-controlled deploy
```

### 2.2 GitHub PR Review Agent

This is a future or optional execution location.

Good fit:

```text
- PR review comments
- CI log analysis
- static diff risk analysis
- documentation consistency checks
```

Constraints:

```text
- minimum secret access
- no runtime production shell
- no direct deploy rights
- no private scenario spoiler data unless repository and channel are approved
```

### 2.3 Server-Side CLI Agent

This is not recommended for early ClueRoom operations.

If introduced later, start with read-only commands only.

Allowed initial scope:

```text
- external health checks
- Blue-Green status checks
- Docker status summaries
- bounded non-secret log tails
- disk/memory summaries
```

Commands requiring explicit approval:

```text
- sudo commands
- docker restart/start/stop
- Nginx reload
- deploy/rollback scripts
- DB migration or restore
- editing files under /opt/clueroom
```

Prohibited:

```text
- reading secret files
- destructive cleanup
- database DROP
- docker compose down -v
- git reset --hard
- git clean -fdx
```

Reference:

```text
docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md
```

---

## 3. Local Repository Allowed Work

The local Codex agent may:

```text
- add or update docs under docs/
- update AGENTS.md links or review guidance
- update README links
- update runbook wording
- draft shell scripts without executing them on production
- update sample env documentation without adding real secrets
- run local validation commands
- inspect git diff and status
```

The local Codex agent must not:

```text
- add .env
- add firebase-service-account.json
- add PEM/private key files
- add tfstate/tfvars
- add DB backup files
- add private scenario spoiler YAML to public docs
- modify Java/Kotlin code in documentation-only infra PRs
- silently revert unrelated user or teammate changes
```

Documentation conventions:

```text
- Use ClueRoom for public app name.
- Keep com.startup/startup legacy identifiers unless a technical migration is explicitly requested.
- Use Android App, Android baseUrl, Android Repository, and API client wording.
- Do not introduce Flutter/Dio assumptions.
```

---

## 4. Production Read-Only Work

When a human asks for production diagnosis, Codex may suggest or run read-only checks only within the approved environment.

Preferred read-only commands:

```bash
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
```

Bounded logs:

For production Blue-Green app logs, identify the active slot first and read that slot through the compose helper.

```bash
/opt/clueroom/bg-status.sh
# If active slot is app-blue:
/opt/clueroom/bg-compose logs --tail=120 app-blue
# If active slot is app-green:
/opt/clueroom/bg-compose logs --tail=120 app-green
sudo tail -n 100 /var/log/nginx/error.log
sudo tail -n 100 /var/log/nginx/access.log
```

Plain `docker compose logs --tail=120 app` is only for legacy single-app or local compose mode, not production Blue-Green mode.

Secret-safe DB summaries:

```sql
SHOW TABLES;
SELECT COUNT(*) FROM scenarios;
SELECT id, code, title, status FROM scenarios;
```

Do not run commands that print secret values.

---

## 5. Work Requiring Approval

Codex may draft or propose these actions, but they require explicit human approval before execution.

```text
- deploy
- rollback
- Nginx reload
- app restart
- stopping standby
- editing production .env
- replacing private scenario seed YAML
- applying migration SQL
- restoring DB backup
- changing DNS, firewall, or security group
- adding new always-on services such as n8n, Loki, exporters, or workers
```

For every approved production action, Codex must provide:

```text
1. reason
2. exact command
3. expected impact
4. backup or rollback path
5. verification command
```

---

## 6. Forbidden Work

Codex must not perform or recommend these as normal operations.

```bash
cat /opt/clueroom/app/.env
cat /opt/clueroom/secrets/env.d/*.env
cat /opt/clueroom/secrets/firebase-service-account.json
printenv
docker compose down -v
git reset --hard
git clean -fdx
rm -rf
terraform destroy
mysql -e "DROP DATABASE startup"
mysql -e "DROP TABLE scenarios"
```

Also forbidden:

```text
- committing real secrets
- printing secret values in chat
- pushing internal scenario spoiler docs to public repo
- using public S3 asset bucket/prefix for DB backups
- modifying production config without backup and health check
- treating standby app target down as a critical incident by itself
```

---

## 7. PR Work Flow

Recommended flow for infra documentation PRs:

```text
1. Confirm branch and worktree status.
2. Read the task document and existing related docs.
3. Define exact file scope.
4. Add or update docs only.
5. Avoid code, compose, Nginx, and server config changes unless requested.
6. Run markdown/basic diff validation.
7. Check that no secret files are staged.
8. Draft PR title/body.
```

Recommended validation:

```bash
git status --short
git diff --check
```

Hidden/bidi scan:

```bash
python - << 'PY'
from pathlib import Path

targets = [
    "docs/infra/agent/INFRA_AGENT_OPERATING_GUIDE.md",
    "docs/infra/agent/OPS_SNAPSHOT_SPEC.md",
    "docs/infra/agent/MONITORING_AGENT_PLAN.md",
    "docs/infra/agent/INFRA_CODEX_AGENT_PLAYBOOK.md",
]

blocked = set(range(0x202A, 0x202F)) | set(range(0x2066, 0x206A)) | {0x200E, 0x200F, 0x200B, 0xFEFF}
bad = []
for p in targets:
    path = Path(p)
    if not path.exists():
        continue
    text = path.read_text(encoding="utf-8", errors="replace")
    for i, ch in enumerate(text):
        if ord(ch) in blocked:
            bad.append((p, i, hex(ord(ch))))

if bad:
    print("FOUND hidden/bidi/zero-width chars:")
    for item in bad:
        print(item)
    raise SystemExit(1)

print("hidden/bidi scan PASS")
PY
```

Secret-file scan:

```bash
git status --short | grep -E '\.env|firebase-service-account|pem|\.key|tfstate|tfvars|sql\.gz|secrets/'
```

No real secret file should appear.

---

## 8. Deploy Failure Analysis Flow

When a deployment fails, Codex should not jump to rollback without evidence.

Use this order:

```text
1. Confirm external health.
2. Confirm Blue-Green active/standby status.
3. Check deploy script output.
4. Check active app container status.
5. Check bounded active app logs.
6. Check Nginx syntax and routing symptoms.
7. Check DB/Redis connectivity symptoms.
8. Identify whether rollback is needed.
9. Provide rollback command as human-approved candidate.
10. Provide post-rollback health checks.
```

Blue-Green interpretation:

```text
- standby stopped can be normal.
- active slot unhealthy is serious.
- both app slots down is critical.
- external health failure is the strongest user-facing signal.
```

Rollback proposal format:

```md
## Rollback Candidate

Reason:

Command:

Expected impact:

Verify:

Residual risk:
```

---

## 9. Validation Commands

For documentation-only infra PRs:

```bash
git status --short
git diff --check
```

For script or config-template PRs:

```bash
shellcheck path/to/script.sh
bash -n path/to/script.sh
```

If shellcheck is unavailable, note that it was not run.

For Java/Kotlin code changes, do not include them in this documentation-only flow unless the user explicitly changes the task scope.

---

## 10. Prompt Examples

### 10.1 Documentation Task

```text
Read CODEX_INFRA_AGENT_DOCS_TASK.md and update only the requested infra docs.

Do not modify Java/Kotlin code.
Do not add real secrets.
Do not edit docker-compose, Nginx, or production scripts unless explicitly requested.

Keep existing docs roles intact.
Use Android App / Android baseUrl wording, not Flutter/Dio.
Run git diff --check and secret-file scan before final response.
```

### 10.2 PR Review Task

```text
Review this infra PR for production safety.

Prioritize:
1. secret exposure
2. destructive or risky commands
3. missing rollback path
4. Blue-Green health logic
5. monitoring false positives
6. missing validation

Report findings first with file/line references.
```

### 10.3 Deploy Failure Task

```text
Analyze this secret-safe deploy output and ops snapshot.

Do not ask for secrets.
Do not recommend destructive commands.
Separate observed facts from inference.
Remember that standby app-blue/app-green down can be normal.

Return:
- severity
- evidence
- likely cause
- safe read-only next checks
- human-approved remediation candidates
- rollback note
```

### 10.4 Server Read-Only Diagnosis Task

```text
Run only read-only checks.

Allowed:
- /opt/clueroom/bg-status.sh
- curl health endpoints
- docker ps / docker compose ps
- df -h / free -m / docker system df
- sudo nginx -t
- bounded log tails

Forbidden:
- cat .env
- printenv
- docker inspect
- restart/reload/stop/start
- deploy/rollback
- rm/git reset/git clean

Summarize evidence and next safe checks.
```

