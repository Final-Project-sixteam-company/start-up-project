#!/usr/bin/env bash

set -u
set -o pipefail

APP_DIR="${CLUEROOM_APP_DIR:-/opt/clueroom/app}"
BG_STATUS="${CLUEROOM_BG_STATUS:-/opt/clueroom/bg-status.sh}"
BG_COMPOSE="${CLUEROOM_BG_COMPOSE:-/opt/clueroom/bg-compose}"
UPSTREAM_CONF="${CLUEROOM_UPSTREAM_CONF:-/etc/nginx/conf.d/clueroom-upstream.conf}"
API_HEALTH_URL="${CLUEROOM_API_HEALTH_URL:-https://api.clueroom.xyz/actuator/health}"

section() {
    printf '\n========================================\n'
    printf '[%s]\n' "$1"
    printf '========================================\n'
}

redact_output() {
    sed -E \
        -e 's/(AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|OPENAI_API_KEY|GEMINI_API_KEY|DB_PASSWORD|SLACK_WEBHOOK_URL|FIREBASE_[A-Z0-9_]*|GOOGLE_APPLICATION_CREDENTIALS)=([^[:space:]]+)/\1=[REDACTED]/g' \
        -e 's/(Authorization:[[:space:]]*Bearer[[:space:]]+)[A-Za-z0-9._~+\/=-]+/\1[REDACTED]/Ig' \
        -e 's/(-----BEGIN [A-Z ]+ PRIVATE KEY-----)/[REDACTED PRIVATE KEY BEGIN]/g'
}

run_shell() {
    local label="$1"
    local cmd="$2"

    printf '\n$ %s\n' "$label"
    bash -o pipefail -lc "$cmd" 2>&1 | redact_output
    local rc=${PIPESTATUS[0]}

    if [ "$rc" -ne 0 ]; then
        printf 'WARN: command failed with exit code %s\n' "$rc"
    fi
}

command_exists() {
    command -v "$1" >/dev/null 2>&1
}

detect_active_service() {
    if [ ! -r "$UPSTREAM_CONF" ]; then
        return 1
    fi

    if grep -q '127\.0\.0\.1:8081' "$UPSTREAM_CONF"; then
        printf 'app-blue'
        return 0
    fi

    if grep -q '127\.0\.0\.1:8082' "$UPSTREAM_CONF"; then
        printf 'app-green'
        return 0
    fi

    return 1
}

section "Header"
printf 'capturedAt: %s\n' "$(date -Is 2>/dev/null || date)"
printf 'hostname: %s\n' "$(hostname 2>/dev/null || echo unknown)"
printf 'currentUser: %s\n' "$(whoami 2>/dev/null || echo unknown)"
printf 'currentPath: %s\n' "$(pwd)"

section "Git Status"
if [ -d "$APP_DIR/.git" ]; then
    run_shell "git status -sb" "cd '$APP_DIR' && git status -sb"
    run_shell "git rev-parse --short HEAD" "cd '$APP_DIR' && git rev-parse --short HEAD"
else
    printf 'WARN: app git checkout not found at %s\n' "$APP_DIR"
fi

section "Blue-Green Status"
if [ -x "$BG_STATUS" ]; then
    run_shell "$BG_STATUS" "'$BG_STATUS'"
else
    printf 'WARN: bg-status script not executable at %s\n' "$BG_STATUS"
fi

if [ -r "$UPSTREAM_CONF" ]; then
    run_shell "active upstream grep" "grep -E '127\\.0\\.0\\.1:(8081|8082)' '$UPSTREAM_CONF' || true"
else
    printf 'WARN: upstream config not readable at %s\n' "$UPSTREAM_CONF"
fi

section "External Health"
run_shell "curl -I $API_HEALTH_URL" "curl -sS -I --max-time 10 '$API_HEALTH_URL'"
run_shell "curl -s $API_HEALTH_URL" "curl -sS --max-time 10 '$API_HEALTH_URL'"

section "Docker Containers"
if command_exists docker; then
    run_shell "docker ps" "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
else
    printf 'WARN: docker not installed or not in PATH\n'
fi

section "Docker Compose Status"
if [ -x "$BG_COMPOSE" ]; then
    run_shell "$BG_COMPOSE ps" "'$BG_COMPOSE' ps"
else
    printf 'WARN: bg-compose helper not executable at %s\n' "$BG_COMPOSE"
fi

if [ -d "$APP_DIR" ] && command_exists docker; then
    run_shell "docker compose ps mysql redis prometheus grafana" "cd '$APP_DIR' && docker compose ps mysql redis prometheus grafana"
else
    printf 'WARN: cannot run docker compose status; app dir or docker missing\n'
fi

section "Resource Status"
run_shell "free -m" "free -m"
run_shell "df -h" "df -h"
if command_exists docker; then
    run_shell "docker system df" "docker system df"
    run_shell "docker stats --no-stream" "docker stats --no-stream"
else
    printf 'WARN: docker not installed or not in PATH\n'
fi

section "Nginx Status"
run_shell "sudo nginx -t" "sudo -n nginx -t"
run_shell "systemctl is-active nginx" "systemctl is-active nginx"

section "Monitoring Status"
run_shell "Prometheus health" "curl -sS --max-time 10 http://localhost:9090/-/healthy"
run_shell "Grafana health" "curl -sS --max-time 10 http://localhost:3000/api/health"

section "Fail2Ban Status"
if command_exists fail2ban-client; then
    run_shell "fail2ban status" "sudo -n fail2ban-client status"
    run_shell "fail2ban sshd status" "sudo -n fail2ban-client status sshd"
else
    printf 'WARN: fail2ban not installed or not in PATH\n'
fi

section "MySQL / Redis Container Health"
if command_exists docker; then
    run_shell "mysql health" "docker inspect --format='{{.Name}} {{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' start-up-mysql 2>/dev/null || echo 'WARN: start-up-mysql not found'"
    run_shell "redis health" "docker inspect --format='{{.Name}} {{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' start-up-redis 2>/dev/null || echo 'WARN: start-up-redis not found'"
else
    printf 'WARN: docker not installed or not in PATH\n'
fi

section "Recent Active App Logs"
active_service="$(detect_active_service || true)"
if [ -n "${active_service:-}" ]; then
    printf 'activeService: %s\n' "$active_service"
    if [ -x "$BG_COMPOSE" ]; then
        run_shell "$BG_COMPOSE logs --tail=120 $active_service" "'$BG_COMPOSE' logs --tail=120 '$active_service'"
    else
        printf 'WARN: bg-compose helper not executable at %s\n' "$BG_COMPOSE"
    fi
else
    printf 'WARN: could not determine active app service from %s\n' "$UPSTREAM_CONF"
fi

section "Nginx Recent Error Log"
run_shell "sudo tail -n 80 /var/log/nginx/error.log" "sudo -n tail -n 80 /var/log/nginx/error.log"

section "Snapshot Notes"
cat <<'EOF'
This snapshot is read-only and bounded.
Review output before sharing with an AI tool or team chat.
Do not paste secret values if any command unexpectedly exposed them.
EOF
