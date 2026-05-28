#!/usr/bin/env bash

set -Eeuo pipefail

UPSTREAM_FILE="/etc/nginx/conf.d/clueroom-upstream.conf"
HEALTH_URL="https://api.clueroom.xyz/actuator/health"
BG_COMPOSE="/opt/clueroom/bg-compose"
LOCK_FILE="/tmp/clueroom-bluegreen-deploy.lock"
UPSTREAM_BACKUP_FILE=""

cleanup() {
  if [ -n "${UPSTREAM_BACKUP_FILE:-}" ]; then
    rm -f "$UPSTREAM_BACKUP_FILE"
  fi
}

trap cleanup EXIT

restore_upstream() {
  local previous_port="$1"

  if [ -f "$UPSTREAM_BACKUP_FILE" ]; then
    sudo cp "$UPSTREAM_BACKUP_FILE" "$UPSTREAM_FILE"
  else
    sudo sed -i -E "s#127\.0\.0\.1:808[12]#127.0.0.1:${previous_port}#g" "$UPSTREAM_FILE"
  fi
}

revert_to_previous_upstream() {
  local previous_port="$1"

  echo "Trying to revert to previous port: $previous_port"
  restore_upstream "$previous_port"
  sudo nginx -t
  sudo systemctl reload nginx
}

echo "========================================"
echo " ClueRoom Blue-Green Rollback"
echo "========================================"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "ERROR: another Blue-Green operation is already running."
  exit 1
fi

if [ ! -f "$UPSTREAM_FILE" ]; then
  echo "ERROR: upstream file not found: $UPSTREAM_FILE"
  exit 1
fi

if [ ! -x "$BG_COMPOSE" ]; then
  echo "ERROR: bg-compose helper is not executable: $BG_COMPOSE"
  exit 1
fi

CURRENT_ADDR="$(grep -oE '127\.0\.0\.1:808[12]' "$UPSTREAM_FILE" | head -n 1 || true)"

if [ -z "$CURRENT_ADDR" ]; then
  echo "ERROR: cannot detect current active upstream from $UPSTREAM_FILE"
  exit 1
fi

CURRENT_PORT="$(echo "$CURRENT_ADDR" | awk -F: '{print $2}')"

if [ "$CURRENT_PORT" = "8081" ]; then
  CURRENT_SERVICE="app-blue"
  TARGET_SERVICE="app-green"
  TARGET_PORT="8082"
elif [ "$CURRENT_PORT" = "8082" ]; then
  CURRENT_SERVICE="app-green"
  TARGET_SERVICE="app-blue"
  TARGET_PORT="8081"
else
  echo "ERROR: unknown current port: $CURRENT_PORT"
  exit 1
fi

echo "Current active service : $CURRENT_SERVICE"
echo "Current active port    : $CURRENT_PORT"
echo "Rollback target        : $TARGET_SERVICE"
echo "Rollback target port   : $TARGET_PORT"

echo ""
echo "[1/5] Check rollback target container exists"
if ! docker ps -a --format '{{.Names}}' | grep -qx "start-up-${TARGET_SERVICE}"; then
  echo "ERROR: rollback target container does not exist: start-up-${TARGET_SERVICE}"
  echo "Rollback requires the previous slot container to exist."
  echo "Do not rebuild target automatically because it may use the current code."
  exit 1
fi

echo "Rollback target container exists"

echo ""
echo "[2/5] Start rollback target if stopped"
"$BG_COMPOSE" start "$TARGET_SERVICE"

echo ""
echo "[3/5] Check rollback target health"
for i in {1..40}; do
  if curl -fsS --max-time 10 "http://127.0.0.1:${TARGET_PORT}/actuator/health" > /dev/null; then
    echo "Rollback target health OK"
    break
  fi

  if [ "$i" = "40" ]; then
    echo "ERROR: rollback target health failed"
    "$BG_COMPOSE" logs --tail=120 "$TARGET_SERVICE" || true
    exit 1
  fi

  echo "Rollback target health retry $i/40..."
  sleep 3
done

echo ""
echo "[4/5] Switch Nginx upstream to rollback target"
UPSTREAM_BACKUP_FILE="$(mktemp /tmp/clueroom-upstream.XXXXXX.conf)"
sudo cp "$UPSTREAM_FILE" "$UPSTREAM_BACKUP_FILE"
sudo sed -i -E "s#127\.0\.0\.1:808[12]#127.0.0.1:${TARGET_PORT}#g" "$UPSTREAM_FILE"
if ! sudo nginx -t; then
  echo "ERROR: nginx config test failed after rollback switch"
  revert_to_previous_upstream "$CURRENT_PORT"
  exit 1
fi

if ! sudo systemctl reload nginx; then
  echo "ERROR: nginx reload failed after rollback switch"
  revert_to_previous_upstream "$CURRENT_PORT"
  exit 1
fi

echo ""
echo "[5/5] Check external health"
for i in {1..20}; do
  if curl -fsS --max-time 10 "$HEALTH_URL" > /dev/null; then
    echo "External health OK"
    break
  fi

  if [ "$i" = "20" ]; then
    echo "ERROR: external health failed after rollback switch"
    revert_to_previous_upstream "$CURRENT_PORT"
    exit 1
  fi

  echo "External health retry $i/20..."
  sleep 3
done

echo ""
echo "Rollback complete"
curl -I --max-time 10 "$HEALTH_URL" 2>/dev/null | grep -Ei "HTTP/|X-ClueRoom-Upstream" || true

echo ""
echo "Previous problematic service is still running: $CURRENT_SERVICE"
echo "If rollback is stable, stop it manually:"
echo "$BG_COMPOSE stop $CURRENT_SERVICE"

echo ""
"$BG_COMPOSE" ps
