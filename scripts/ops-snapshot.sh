#!/usr/bin/env bash

set -u
set -o pipefail

APP_DIR="${CLUEROOM_APP_DIR:-/opt/clueroom/app}"
BG_STATUS="${CLUEROOM_BG_STATUS:-/opt/clueroom/bg-status.sh}"
BG_COMPOSE="${CLUEROOM_BG_COMPOSE:-/opt/clueroom/bg-compose}"
UPSTREAM_CONF="${CLUEROOM_UPSTREAM_CONF:-/etc/nginx/conf.d/clueroom-upstream.conf}"
API_HEALTH_URL="${CLUEROOM_API_HEALTH_URL:-https://api.clueroom.xyz/actuator/health}"
DATA_HOST="${DATA_HOST:-${CLUEROOM_DATA_HOST:-172.26.1.185}}"
DATA_MYSQL_PORT="${DATA_MYSQL_PORT:-3306}"
DATA_REDIS_PORT="${DATA_REDIS_PORT:-6379}"
OPS_LOKI_BASE_URL="${OPS_LOKI_BASE_URL:-${CLUEROOM_OPS_LOKI_BASE_URL:-http://172.26.15.52:3100}}"
OPS_LOKI_READY_URL="${OPS_LOKI_READY_URL:-${OPS_LOKI_BASE_URL%/}/ready}"
ALLOY_CONTAINER="${CLUEROOM_ALLOY_CONTAINER:-start-up-alloy}"
EXPECTED_AI_LLMOPS_DB_LOGGING_ENABLED="${EXPECTED_AI_LLMOPS_DB_LOGGING_ENABLED:-false}"

section() {
    printf '\n========================================\n'
    printf '[%s]\n' "$1"
    printf '========================================\n'
}

redact_output() {
    sed -E \
        -e 's/(AWS_ACCESS_KEY_ID|AWS_SECRET_ACCESS_KEY|SLACK_WEBHOOK_URL|FIREBASE_[A-Z0-9_]*|GOOGLE_APPLICATION_CREDENTIALS|[A-Z0-9_]*(API_KEY|SECRET|TOKEN|PASSWORD)[A-Z0-9_]*)=([^[:space:]]+)/\1=[REDACTED]/g' \
        -e 's/(Authorization:[[:space:]]*Bearer[[:space:]]+)[A-Za-z0-9._~+\/=-]+/\1[REDACTED]/Ig' \
        -e 's/("(apiKey|api_key|password|token|secret)"[[:space:]]*:[[:space:]]*")[^"]+/\1[REDACTED]/Ig' \
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

detect_standby_service() {
    local active="$1"

    case "$active" in
        app-blue) printf 'app-green' ;;
        app-green) printf 'app-blue' ;;
        *) return 1 ;;
    esac
}

container_name_for_service() {
    local service="$1"
    printf 'start-up-%s' "$service"
}

docker_container_running() {
    local container="$1"
    [ "$(docker inspect --format='{{.State.Running}}' "$container" 2>/dev/null || true)" = "true" ]
}

print_app_container_env() {
    local role="$1"
    local service="$2"
    local container
    local env_output
    local db_host
    local db_port
    local redis_host
    local redis_port
    local llmops_db_logging_enabled

    container="$(container_name_for_service "$service")"

    printf '\n%sService: %s\n' "$role" "$service"
    printf 'container: %s\n' "$container"

    if ! command_exists docker; then
        printf 'WARN: docker not installed or not in PATH\n'
        return
    fi

    if ! docker_container_running "$container"; then
        printf 'WARN: container is not running or not found: %s\n' "$container"
        return
    fi

    env_output="$(
        docker exec "$container" sh -c '
            for key in DB_HOST DB_PORT REDIS_HOST REDIS_PORT AI_LLMOPS_DB_LOGGING_ENABLED; do
                value="$(printenv "$key" 2>/dev/null || true)"
                printf "%s=%s\n" "$key" "$value"
            done
        ' 2>&1 | redact_output
    )"

    printf '%s\n' "$env_output"

    db_host="$(printf '%s\n' "$env_output" | awk -F= '$1=="DB_HOST"{print $2}')"
    db_port="$(printf '%s\n' "$env_output" | awk -F= '$1=="DB_PORT"{print $2}')"
    redis_host="$(printf '%s\n' "$env_output" | awk -F= '$1=="REDIS_HOST"{print $2}')"
    redis_port="$(printf '%s\n' "$env_output" | awk -F= '$1=="REDIS_PORT"{print $2}')"
    llmops_db_logging_enabled="$(printf '%s\n' "$env_output" | awk -F= '$1=="AI_LLMOPS_DB_LOGGING_ENABLED"{print $2}')"

    if [ "$db_host" != "$DATA_HOST" ]; then
        printf 'WARN: %s DB_HOST expected %s but got %s\n' "$service" "$DATA_HOST" "${db_host:-<empty>}"
    fi

    if [ "$db_port" != "$DATA_MYSQL_PORT" ]; then
        printf 'WARN: %s DB_PORT expected %s but got %s\n' "$service" "$DATA_MYSQL_PORT" "${db_port:-<empty>}"
    fi

    if [ "$redis_host" != "$DATA_HOST" ]; then
        printf 'WARN: %s REDIS_HOST expected %s but got %s\n' "$service" "$DATA_HOST" "${redis_host:-<empty>}"
    fi

    if [ "$redis_port" != "$DATA_REDIS_PORT" ]; then
        printf 'WARN: %s REDIS_PORT expected %s but got %s\n' "$service" "$DATA_REDIS_PORT" "${redis_port:-<empty>}"
    fi

    if [ "$llmops_db_logging_enabled" != "$EXPECTED_AI_LLMOPS_DB_LOGGING_ENABLED" ]; then
        printf 'WARN: %s AI_LLMOPS_DB_LOGGING_ENABLED expected %s but got %s\n' "$service" "$EXPECTED_AI_LLMOPS_DB_LOGGING_ENABLED" "${llmops_db_logging_enabled:-<empty>}"
    fi
}

tcp_check() {
    local label="$1"
    local host="$2"
    local port="$3"

    printf '%s: %s:%s ... ' "$label" "$host" "$port"

    if command_exists timeout; then
        if timeout 3 bash -c 'cat < /dev/null > /dev/tcp/"$1"/"$2"' _ "$host" "$port" 2>/dev/null; then
            printf 'OK\n'
        else
            printf 'CRITICAL: TCP connection failed\n'
        fi
    else
        if bash -c 'cat < /dev/null > /dev/tcp/"$1"/"$2"' _ "$host" "$port" 2>/dev/null; then
            printf 'OK\n'
        else
            printf 'CRITICAL: TCP connection failed\n'
        fi
    fi
}

check_disk_memory_thresholds() {
    local available_mb

    available_mb="$(free -m 2>/dev/null | awk '/^Mem:/ {print $7}')"
    if [ -z "$available_mb" ] && [ -r /proc/meminfo ]; then
        available_mb="$(awk '/^MemAvailable:/ {printf "%d", $2 / 1024}' /proc/meminfo)"
    fi

    if [ -n "$available_mb" ]; then
        if [ "$available_mb" -lt 200 ]; then
            printf 'CRITICAL: available memory below 200MB (%sMB)\n' "$available_mb"
        elif [ "$available_mb" -lt 500 ]; then
            printf 'WARNING: available memory below 500MB (%sMB)\n' "$available_mb"
        else
            printf 'memoryThreshold: OK available=%sMB\n' "$available_mb"
        fi
    else
        printf 'WARN: could not determine available memory\n'
    fi

    if command_exists df; then
        df -P -x tmpfs -x devtmpfs 2>/dev/null | awk '
            NR > 1 {
                usage = $5
                gsub(/%/, "", usage)
                if (usage >= 90) {
                    printf "CRITICAL: disk usage %s%% mount=%s filesystem=%s\n", usage, $6, $1
                    found = 1
                } else if (usage >= 80) {
                    printf "WARNING: disk usage %s%% mount=%s filesystem=%s\n", usage, $6, $1
                    found = 1
                }
            }
            END {
                if (!found) {
                    print "diskThreshold: OK all checked mounts below 80%"
                }
            }
        '
    else
        printf 'WARN: df not installed or not in PATH\n'
    fi
}

check_loki_ready() {
    if ! command_exists curl; then
        printf 'WARN: curl not installed or not in PATH\n'
        return 1
    fi

    if curl -fsS --max-time 8 "$OPS_LOKI_READY_URL" >/dev/null 2>&1; then
        printf 'opsLokiReady: OK %s\n' "$OPS_LOKI_READY_URL"
        return 0
    fi

    printf 'WARN: ops Loki ready check failed: %s\n' "$OPS_LOKI_READY_URL"
    return 1
}

query_loki_marker() {
    local marker="$1"
    local response

    if ! command_exists curl; then
        printf '%s: SKIP curl unavailable\n' "$marker"
        return
    fi

    if ! command_exists jq; then
        printf '%s: SKIP jq unavailable; heartbeat freshness is handled by n8n/Grafana dashboards\n' "$marker"
        return
    fi

    response="$(
        curl -fsS -G --max-time 10 "${OPS_LOKI_BASE_URL%/}/loki/api/v1/query_range" \
            --data-urlencode "query={job=~\".+\"} |= \"$marker\"" \
            --data-urlencode "direction=backward" \
            --data-urlencode "limit=3" \
            --data-urlencode "since=24h" 2>/dev/null || true
    )"

    if [ -z "$response" ]; then
        printf 'WARN: %s query failed or returned empty response\n' "$marker"
        return
    fi

    if [ "$(printf '%s' "$response" | jq -r '[.data.result[].values[]?] | length' 2>/dev/null || echo 0)" = "0" ]; then
        printf 'WARN: %s no recent entries found in last 24h\n' "$marker"
        return
    fi

    printf '%s recent entries (limited):\n' "$marker"
    printf '%s' "$response" | jq -r '
        .data.result[] as $stream
        | $stream.values[]?
        | "\(($stream.stream.job // $stream.stream.container // $stream.stream.service_name // "stream")) \((.[1] | tostring | gsub("[\r\n]+"; " ") | .[0:220]))"
    ' 2>/dev/null | head -n 3 | redact_output
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

section "External Data Target"
cat <<EOF
mode: external-data
dataHost: $DATA_HOST
mysqlTcp: $DATA_HOST:$DATA_MYSQL_PORT
redisTcp: $DATA_HOST:$DATA_REDIS_PORT
prodLocalMysqlRedisRole: local rollback copy only, not source of truth
expectedAppDBHost: $DATA_HOST
expectedAppRedisHost: $DATA_HOST
expectedAI_LLMOPS_DB_LOGGING_ENABLED: $EXPECTED_AI_LLMOPS_DB_LOGGING_ENABLED
EOF

section "App Container Env"
active_service="$(detect_active_service || true)"
if [ -n "${active_service:-}" ]; then
    standby_service="$(detect_standby_service "$active_service" || true)"
    print_app_container_env "active" "$active_service"
    if [ -n "${standby_service:-}" ]; then
        print_app_container_env "standby" "$standby_service"
    else
        printf 'WARN: could not determine standby service from active service %s\n' "$active_service"
    fi
else
    printf 'WARN: could not determine active app service from %s\n' "$UPSTREAM_CONF"
fi

section "Data Server Connectivity"
tcp_check "dataMySQL" "$DATA_HOST" "$DATA_MYSQL_PORT"
tcp_check "dataRedis" "$DATA_HOST" "$DATA_REDIS_PORT"
printf 'note: TCP-only check; no DB auth query is executed.\n'

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
printf '\nThreshold summary:\n'
check_disk_memory_thresholds
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

section "Ops Loki / Alloy"
check_loki_ready
if command_exists docker; then
    if docker_container_running "$ALLOY_CONTAINER"; then
        printf 'alloyContainer: OK running %s\n' "$ALLOY_CONTAINER"
        printf '\nRecent Alloy warn/error lines, max 30:\n'
        docker logs --tail=30 "$ALLOY_CONTAINER" 2>&1 \
            | grep -Ei 'warn|error|failed|level=(warn|error)' \
            | tail -n 30 \
            | redact_output || printf 'No warn/error lines in last 30 Alloy log lines.\n'
    else
        printf 'WARN: Alloy container is not running or not found: %s\n' "$ALLOY_CONTAINER"
    fi
else
    printf 'WARN: docker not installed or not in PATH\n'
fi

printf '\nHeartbeat markers from ops Loki, limited output:\n'
printf 'parserHint: disk source of truth is SERVER_HEALTH/DATA_HEALTH/OPS_HEALTH disk_max_percent; raw df percentages are fallback only.\n'
if check_loki_ready >/dev/null; then
    query_loki_marker "DATA_HEALTH"
    query_loki_marker "SERVER_HEALTH"
    query_loki_marker "OPS_HEALTH"
else
    printf 'WARN: heartbeat query skipped because ops Loki ready check failed. Freshness should be checked in n8n/Grafana.\n'
fi

section "Fail2Ban Status"
if command_exists fail2ban-client; then
    run_shell "fail2ban status" "sudo -n fail2ban-client status"
    run_shell "fail2ban sshd status" "sudo -n fail2ban-client status sshd"
else
    printf 'WARN: fail2ban not installed or not in PATH\n'
fi

section "Local Rollback MySQL Redis"
printf 'role: local rollback copy only; not production source of truth in external-data mode\n'
printf 'note: local MySQL/Redis down is not an immediate production outage if external data connectivity is OK.\n'
if command_exists docker; then
    run_shell "local mysql rollback-copy health" "docker inspect --format='{{.Name}} running={{.State.Running}} {{if .State.Health}}health={{.State.Health.Status}}{{else}}health=no-healthcheck{{end}}' start-up-mysql 2>/dev/null || echo 'local rollback mysql: not found'"
    run_shell "local redis rollback-copy health" "docker inspect --format='{{.Name}} running={{.State.Running}} {{if .State.Health}}health={{.State.Health.Status}}{{else}}health=no-healthcheck{{end}}' start-up-redis 2>/dev/null || echo 'local rollback redis: not found'"
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
Current production app containers use the external data server; local MySQL/Redis are rollback/local-data only.
Heartbeat freshness is primarily monitored by n8n/Grafana; this script only prints limited Loki samples when available.
Ops Snapshot Agent should use SERVER_HEALTH/DATA_HEALTH/OPS_HEALTH disk_max_percent as disk source of truth; raw snapshot percentages are fallback only.
Prod available-memory WARNING is report-grade unless paired with service impact; Grafana Alert owns event notifications.
Review output before sharing with an AI tool or team chat.
Do not paste secret values if any command unexpectedly exposed them.
EOF
