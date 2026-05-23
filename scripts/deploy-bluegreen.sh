#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
BRANCH="develop"
HEALTH_URL="https://api.clueroom.xyz/actuator/health"
UPSTREAM_FILE="/etc/nginx/conf.d/clueroom-upstream.conf"
SECRET_ENV_DIR="/opt/clueroom/secrets/env.d"

COMPOSE_ARGS=(
  --env-file "$APP_DIR/.env"
  --env-file "$SECRET_ENV_DIR/ai.env"
  --env-file "$SECRET_ENV_DIR/portone.env"
  --env-file "$SECRET_ENV_DIR/oauth.env"
  -f "$APP_DIR/docker-compose.yml"
  -f "$APP_DIR/docker-compose.bluegreen.yml"
)

echo "========================================"
echo " ClueRoom Blue-Green Deploy Start"
echo "========================================"

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
for secret_env in "$SECRET_ENV_DIR"/*.env; do
  if [ -f "$secret_env" ]; then
    chmod 600 "$secret_env"
    echo "Found secret env file: $secret_env"
  fi
done

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
sudo sed -i -E "s#127\.0\.0\.1:808[12]#127.0.0.1:${TARGET_PORT}#g" "$UPSTREAM_FILE"
sudo nginx -t
sudo systemctl reload nginx

echo "[9/9] Check external health"
for i in {1..20}; do
  if curl -fsS "$HEALTH_URL" > /dev/null; then
    echo "External health check success"
    break
  fi

  if [ "$i" = "20" ]; then
    echo "ERROR: external health check failed"
    echo "Rollback command:"
    echo "sudo sed -i -E 's#127\\.0\\.0\\.1:808[12]#127.0.0.1:${ACTIVE_PORT}#g' $UPSTREAM_FILE && sudo nginx -t && sudo systemctl reload nginx"
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
echo "docker compose --env-file $APP_DIR/.env --env-file $SECRET_ENV_DIR/ai.env --env-file $SECRET_ENV_DIR/portone.env --env-file $SECRET_ENV_DIR/oauth.env -f $APP_DIR/docker-compose.yml -f $APP_DIR/docker-compose.bluegreen.yml stop $OLD_SERVICE"

echo ""
echo "Rollback command:"
echo "sudo sed -i -E 's#127\\.0\\.0\\.1:808[12]#127.0.0.1:${ACTIVE_PORT}#g' $UPSTREAM_FILE && sudo nginx -t && sudo systemctl reload nginx"

docker compose "${COMPOSE_ARGS[@]}" ps

echo "========================================"
echo " ClueRoom Blue-Green Deploy Finished"
echo "========================================"
