#!/usr/bin/env bash

set -Eeuo pipefail

UPSTREAM_FILE="/etc/nginx/conf.d/clueroom-upstream.conf"
HEALTH_URL="https://api.clueroom.xyz/actuator/health"
BG_COMPOSE="/opt/clueroom/bg-compose"
LOCK_FILE="/tmp/clueroom-bluegreen-deploy.lock"

echo "========================================"
echo " ClueRoom Stop Standby Slot"
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

ACTIVE_ADDR="$(grep -oE '127\.0\.0\.1:808[12]' "$UPSTREAM_FILE" | head -n 1 || true)"

if [ -z "$ACTIVE_ADDR" ]; then
  echo "ERROR: cannot detect active upstream from $UPSTREAM_FILE"
  exit 1
fi

ACTIVE_PORT="$(echo "$ACTIVE_ADDR" | awk -F: '{print $2}')"

if [ "$ACTIVE_PORT" = "8081" ]; then
  ACTIVE_SERVICE="app-blue"
  STANDBY_SERVICE="app-green"
elif [ "$ACTIVE_PORT" = "8082" ]; then
  ACTIVE_SERVICE="app-green"
  STANDBY_SERVICE="app-blue"
else
  echo "ERROR: unknown active port: $ACTIVE_PORT"
  exit 1
fi

echo "Active upstream : $ACTIVE_ADDR"
echo "Active service  : $ACTIVE_SERVICE"
echo "Standby service : $STANDBY_SERVICE"

echo ""
echo "[1/3] Check external health before stopping standby"
if ! curl -fsS --max-time 10 "$HEALTH_URL" > /dev/null; then
  echo "ERROR: external health check failed. Do not stop standby."
  exit 1
fi

echo "External health OK"

echo ""
echo "[2/3] Stop standby service: $STANDBY_SERVICE"
"$BG_COMPOSE" stop "$STANDBY_SERVICE"

echo ""
echo "[3/3] Check external health after stopping standby"
if ! curl -fsS --max-time 10 "$HEALTH_URL" > /dev/null; then
  echo "ERROR: external health failed after stopping standby."
  echo "Try starting standby again:"
  echo "$BG_COMPOSE start $STANDBY_SERVICE"
  exit 1
fi

echo "External health OK"

echo ""
echo "Standby stopped safely: $STANDBY_SERVICE"
curl -I --max-time 10 "$HEALTH_URL" 2>/dev/null | grep -Ei "HTTP/|X-ClueRoom-Upstream" || true

echo ""
"$BG_COMPOSE" ps
