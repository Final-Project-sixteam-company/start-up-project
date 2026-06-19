#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"
REMOTE_SCRIPT="/tmp/clueroom-start-app-node-remote.sh"

echo "========================================"
echo " ClueRoom App Node Start"
echo "========================================"

cat > "$REMOTE_SCRIPT" << 'REMOTE'
#!/usr/bin/env bash
set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
SECRET_ENV_DIR="/opt/clueroom/secrets/env.d"
RUNTIME_ENV_FILE="/tmp/clueroom-app-node-runtime.env"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-40}"
SLEEP_SECONDS="${SLEEP_SECONDS:-3}"
DATA_SERVER_PRIVATE_IP="${DATA_SERVER_PRIVATE_IP:?DATA_SERVER_PRIVATE_IP is required}"

cleanup_runtime_env() {
  rm -f "$RUNTIME_ENV_FILE"
}

print_diagnostics() {
  echo
  echo "========================================"
  echo "[diagnostics: docker ps -a]"
  echo "========================================"
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' || true

  echo
  echo "========================================"
  echo "[diagnostics: start-up-app inspect]"
  echo "========================================"
  docker inspect -f 'status={{.State.Status}} exitCode={{.State.ExitCode}} error={{.State.Error}} oomKilled={{.State.OOMKilled}} startedAt={{.State.StartedAt}} finishedAt={{.State.FinishedAt}}' start-up-app 2>/dev/null || echo "start-up-app not found"

  echo
  echo "========================================"
  echo "[diagnostics: port 8080]"
  echo "========================================"
  sudo ss -tulnp | grep ':8080' || echo "8080 not listening"

  echo
  echo "========================================"
  echo "[diagnostics: start-up-app logs tail]"
  echo "========================================"
  docker logs --tail=300 start-up-app 2>/dev/null || echo "no start-up-app logs"
}

fail_and_cleanup() {
  echo "ERROR: $1"
  print_diagnostics
  echo "[cleanup] remove unhealthy start-up-app"
  docker rm -f start-up-app 2>/dev/null || true
  rm -f "$RUNTIME_ENV_FILE"
  exit 1
}

trap 'fail_and_cleanup "remote start failed at line $LINENO"' ERR
trap cleanup_runtime_env EXIT
trap 'cleanup_runtime_env; exit 130' HUP INT TERM

cd "$APP_DIR"

echo "[1/9] Set hostname"
sudo hostnamectl set-hostname "${APP_NODE_NAME:-clueroom-app-node}" || true

echo "[2/9] Force external data env"
set_env() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" .env; then
    sed -i "s|^${key}=.*|${key}=${value}|" .env
  else
    echo "${key}=${value}" >> .env
  fi
}

set_env APP_DB_HOST "$DATA_SERVER_PRIVATE_IP"
set_env APP_DB_PORT 3306
set_env APP_REDIS_HOST "$DATA_SERVER_PRIVATE_IP"
set_env APP_REDIS_PORT 6379
set_env AI_LLMOPS_DB_LOGGING_ENABLED false

echo "[runtime] Ensure app-node runtime compose override"
cat > "$APP_DIR/docker-compose.app-node.runtime.yml" << 'EOF_RUNTIME'
services:
  app:
    env_file:
      - /tmp/clueroom-app-node-runtime.env
    volumes:
      - /opt/clueroom/secrets/firebase-service-account.json:/opt/clueroom/secrets/firebase-service-account.json:ro
      - /opt/clueroom/secrets/scenarios:/opt/clueroom/secrets/scenarios:ro
EOF_RUNTIME

echo "[3/9] Check required files"
for f in "$APP_DIR/.env" "$APP_DIR/docker-compose.yml" "$APP_DIR/docker-compose.external-data.yml" "$APP_DIR/build/libs/app.jar" "$SECRET_ENV_DIR/ai.env" "$SECRET_ENV_DIR/oauth.env" "/opt/clueroom/secrets/firebase-service-account.json" "/opt/clueroom/secrets/scenarios"; do
  if [ ! -e "$f" ]; then
    echo "ERROR: required path missing: $f"
    exit 1
  fi
done
OPTIONAL_ENV_FILES=("$SECRET_ENV_DIR/portone.env")
ls -lh "$APP_DIR/build/libs/app.jar"
ls -lh /opt/clueroom/secrets/firebase-service-account.json
echo scenario_file_count=$(find /opt/clueroom/secrets/scenarios -type f | wc -l)

echo "[4/9] Build runtime env file"
cat "$APP_DIR/.env" > "$RUNTIME_ENV_FILE"
printf '\n' >> "$RUNTIME_ENV_FILE"
cat "$SECRET_ENV_DIR/ai.env" >> "$RUNTIME_ENV_FILE"
printf '\n' >> "$RUNTIME_ENV_FILE"
cat "$SECRET_ENV_DIR/oauth.env" >> "$RUNTIME_ENV_FILE"
for optional_env in "${OPTIONAL_ENV_FILES[@]}"; do
  if [ -s "$optional_env" ]; then
    printf '\n' >> "$RUNTIME_ENV_FILE"
    cat "$optional_env" >> "$RUNTIME_ENV_FILE"
  else
    echo "optional env not found or empty: $optional_env"
  fi
done
chmod 600 "$RUNTIME_ENV_FILE"

echo "[5/9] Check data server ports"
timeout 3 bash -c "</dev/tcp/${DATA_SERVER_PRIVATE_IP}/3306" && echo "data mysql port open"
timeout 3 bash -c "</dev/tcp/${DATA_SERVER_PRIVATE_IP}/6379" && echo "data redis port open"

echo "[6/9] Compose config sanity"
docker compose \
  --env-file "$RUNTIME_ENV_FILE" \
  -f docker-compose.yml \
  -f docker-compose.external-data.yml \
  -f docker-compose.app-node.runtime.yml \
  config | grep -E 'DB_HOST:|DB_PORT:|REDIS_HOST:|REDIS_PORT:|AI_LLMOPS_DB_LOGGING_ENABLED|JWT_ISSUER|AUTH_REQUIRE_AUTHENTICATION|AUTH_MOCK_FALLBACK_ENABLED' || true

docker compose \
  --env-file "$RUNTIME_ENV_FILE" \
  -f docker-compose.yml \
  -f docker-compose.external-data.yml \
  -f docker-compose.app-node.runtime.yml \
  config | grep -q 'JWT_SECRET' && echo "JWT_SECRET present in compose config"

echo "[7/9] Rebuild/start app"
docker rm -f start-up-app 2>/dev/null || true

docker compose \
  --env-file "$RUNTIME_ENV_FILE" \
  -f docker-compose.yml \
  -f docker-compose.external-data.yml \
  -f docker-compose.app-node.runtime.yml \
  up -d --build app

echo "[8/9] Wait app health: max=${MAX_ATTEMPTS}, sleep=${SLEEP_SECONDS}s"
for i in $(seq 1 "$MAX_ATTEMPTS"); do
  if curl -fsS http://127.0.0.1:8080/actuator/health >/tmp/app-health.json; then
    echo "app health OK"
    cat /tmp/app-health.json
    echo
    echo "[9/9] Final status"
    docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
    rm -f "$RUNTIME_ENV_FILE"
    exit 0
  fi

  STATE="$(docker inspect -f '{{.State.Status}}' start-up-app 2>/dev/null || echo 'missing')"
  EXIT_CODE="$(docker inspect -f '{{.State.ExitCode}}' start-up-app 2>/dev/null || echo '-')"
  echo "waiting app health... ${i}/${MAX_ATTEMPTS} state=${STATE} exitCode=${EXIT_CODE}"

  if [ "$STATE" = "exited" ] || [ "$STATE" = "dead" ] || [ "$STATE" = "missing" ]; then
    fail_and_cleanup "app container is not running during health wait"
  fi
  sleep "$SLEEP_SECONDS"
done

fail_and_cleanup "app health timeout"
REMOTE

echo "[1/4] Copy remote script"
scp "${SSH_OPTS[@]}" "$REMOTE_SCRIPT" "$SSH_TARGET:/tmp/clueroom-start-app-node-remote.sh"

echo "[2/4] Execute remote script"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "chmod +x /tmp/clueroom-start-app-node-remote.sh && DATA_SERVER_PRIVATE_IP='$DATA_SERVER_PRIVATE_IP' APP_NODE_NAME='$APP_NODE_NAME' MAX_ATTEMPTS=40 SLEEP_SECONDS=3 /tmp/clueroom-start-app-node-remote.sh"

echo "[3/4] Cleanup local temp script"
rm -f "$REMOTE_SCRIPT"

echo "[4/4] Done"
echo "========================================"
echo " ClueRoom App Node Start Success"
echo "========================================"
