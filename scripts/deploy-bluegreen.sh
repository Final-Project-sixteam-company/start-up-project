#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
BRANCH="develop"
HEALTH_URL="https://api.clueroom.xyz/actuator/health"
UPSTREAM_FILE="/etc/nginx/conf.d/clueroom-upstream.conf"
SECRET_ENV_DIR="/opt/clueroom/secrets/env.d"
LOCK_FILE="/tmp/clueroom-bluegreen-deploy.lock"
RUNTIME_ENV_FILE=""
SECRET_ENV_FILES=()

cleanup() {
  if [ -n "${RUNTIME_ENV_FILE:-}" ] && [ "$RUNTIME_ENV_FILE" != "$APP_DIR/.env" ]; then
    rm -f "$RUNTIME_ENV_FILE"
  fi

  if [ -n "${UPSTREAM_BACKUP_FILE:-}" ]; then
    rm -f "$UPSTREAM_BACKUP_FILE"
  fi
}

trap cleanup EXIT

echo "========================================"
echo " ClueRoom Blue-Green Deploy Start"
echo "========================================"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "ERROR: another Blue-Green deploy is already running."
  exit 1
fi

cd "$APP_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "ERROR: .env file does not exist."
  exit 1
fi

if [ ! -f "$UPSTREAM_FILE" ]; then
  echo "ERROR: upstream file does not exist: $UPSTREAM_FILE"
  exit 1
fi

chmod 600 "$APP_DIR/.env"

COMPOSE_ARGS=(
  --env-file "$APP_DIR/.env"
)

add_optional_secret_env() {
  local env_file="$1"

  if [ ! -e "$env_file" ]; then
    echo "Optional secret env file not found: $env_file"
    return
  fi

  if [ ! -r "$env_file" ]; then
    echo "ERROR: secret env file is not readable: $env_file"
    exit 1
  fi

  SECRET_ENV_FILES+=("$env_file")
  echo "Found readable secret env file: $env_file"
}

build_runtime_env_file() {
  if [ "${#SECRET_ENV_FILES[@]}" -eq 0 ]; then
    RUNTIME_ENV_FILE="$APP_DIR/.env"
  else
    RUNTIME_ENV_FILE="$(mktemp /tmp/clueroom-runtime-env.XXXXXX)"
    cat "$APP_DIR/.env" > "$RUNTIME_ENV_FILE"

    for secret_env in "${SECRET_ENV_FILES[@]}"; do
      printf '\n' >> "$RUNTIME_ENV_FILE"
      cat "$secret_env" >> "$RUNTIME_ENV_FILE"
    done

    chmod 600 "$RUNTIME_ENV_FILE"
  fi

  export CLUEROOM_RUNTIME_ENV_FILE="$RUNTIME_ENV_FILE"
  echo "Using runtime env file: $RUNTIME_ENV_FILE"
}

restore_upstream_backup() {
  local backup_file="$1"

  if [ -f "$backup_file" ]; then
    sudo cp "$backup_file" "$UPSTREAM_FILE"
  else
    sudo sed -i -E "s#127\.0\.0\.1:808[12]#127.0.0.1:${ACTIVE_PORT}#g" "$UPSTREAM_FILE"
  fi
}

rollback_nginx_upstream() {
  local backup_file="$1"

  echo "Rolling back Nginx upstream to ${ACTIVE_PORT}"
  restore_upstream_backup "$backup_file"
  sudo nginx -t
  sudo systemctl reload nginx
}

echo "[0/9] Detect active upstream"

ACTIVE_PORT="$(grep -oE '127\.0\.0\.1:808[12]' "$UPSTREAM_FILE" | head -n 1 | awk -F: '{print $2}')"

if [ "$ACTIVE_PORT" = "8081" ]; then
  ACTIVE_SERVICE="app-blue"
  TARGET_SERVICE="app-green"
  TARGET_PORT="8082"
  OLD_SERVICE="app-blue"
elif [ "$ACTIVE_PORT" = "8082" ]; then
  ACTIVE_SERVICE="app-green"
  TARGET_SERVICE="app-blue"
  TARGET_PORT="8081"
  OLD_SERVICE="app-green"
else
  echo "ERROR: cannot detect active upstream port from $UPSTREAM_FILE"
  exit 1
fi

echo "Current active service: $ACTIVE_SERVICE"
echo "Target service: $TARGET_SERVICE"
echo "Target port: $TARGET_PORT"

echo "[1/9] Check secret env files"
add_optional_secret_env "$SECRET_ENV_DIR/ai.env"
add_optional_secret_env "$SECRET_ENV_DIR/portone.env"
add_optional_secret_env "$SECRET_ENV_DIR/oauth.env"
build_runtime_env_file

COMPOSE_ARGS+=(
  -f "$APP_DIR/docker-compose.yml"
  -f "$APP_DIR/docker-compose.external-data.yml"
  -f "$APP_DIR/docker-compose.bluegreen.yml"
  -f "$APP_DIR/docker-compose.bluegreen.external-data.yml"
)

echo "[2/9] Fetch latest code"
git fetch origin "$BRANCH"

echo "[3/9] Pull latest develop"
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

echo "[4/9] Build Spring Boot jar"
chmod +x ./gradlew
./gradlew clean bootJar --no-daemon

echo "[5/9] Validate docker compose"
docker compose "${COMPOSE_ARGS[@]}" config > /dev/null

echo "[6/9] Build and start target container: $TARGET_SERVICE"
docker compose "${COMPOSE_ARGS[@]}" up -d --build "$TARGET_SERVICE"

echo "[7/9] Check target health"
for i in {1..40}; do
  if curl -fsS "http://127.0.0.1:${TARGET_PORT}/actuator/health" > /dev/null; then
    echo "Target health check success"
    break
  fi

  if [ "$i" = "40" ]; then
    echo "ERROR: target health check failed"
    docker compose "${COMPOSE_ARGS[@]}" logs --tail=120 "$TARGET_SERVICE"
    exit 1
  fi

  echo "Target health retry $i/40..."
  sleep 3
done

echo "[8/9] Switch Nginx upstream to $TARGET_PORT"
UPSTREAM_BACKUP_FILE="$(mktemp /tmp/clueroom-upstream.XXXXXX.conf)"
sudo cp "$UPSTREAM_FILE" "$UPSTREAM_BACKUP_FILE"
sudo sed -i -E "s#127\.0\.0\.1:808[12]#127.0.0.1:${TARGET_PORT}#g" "$UPSTREAM_FILE"
if ! sudo nginx -t; then
  echo "ERROR: nginx config test failed after upstream switch"
  rollback_nginx_upstream "$UPSTREAM_BACKUP_FILE"
  exit 1
fi

if ! sudo systemctl reload nginx; then
  echo "ERROR: nginx reload failed after upstream switch"
  rollback_nginx_upstream "$UPSTREAM_BACKUP_FILE"
  exit 1
fi

echo "[9/9] Check external health"
for i in {1..20}; do
  if curl -fsS "$HEALTH_URL" > /dev/null; then
    echo "External health check success"
    break
  fi

  if [ "$i" = "20" ]; then
    echo "ERROR: external health check failed"
    rollback_nginx_upstream "$UPSTREAM_BACKUP_FILE"

    if curl -fsS "$HEALTH_URL" > /dev/null; then
      echo "Rollback external health check success"
    else
      echo "WARNING: rollback completed, but external health check still fails"
    fi

    exit 1
  fi

  echo "External health retry $i/20..."
  sleep 3
done

echo "========================================"
echo " Blue-Green Deploy Success"
echo "========================================"
echo "Previous active service is still running for rollback: $OLD_SERVICE"
echo "Current active upstream:"
curl -I "$HEALTH_URL" 2>/dev/null | grep -i "X-ClueRoom-Upstream" || true

echo ""
echo "If everything is stable, you may stop the old service manually:"
echo "/opt/clueroom/bg-compose stop $OLD_SERVICE"

echo ""
echo "Rollback command:"
echo "sudo sed -i -E 's#127\\.0\\.0\\.1:808[12]#127.0.0.1:${ACTIVE_PORT}#g' $UPSTREAM_FILE && sudo nginx -t && sudo systemctl reload nginx"

docker compose "${COMPOSE_ARGS[@]}" ps

echo "========================================"
echo " ClueRoom Blue-Green Deploy Finished"
echo "========================================"
