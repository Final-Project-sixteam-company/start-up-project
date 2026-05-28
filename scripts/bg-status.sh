#!/usr/bin/env bash

set -Eeuo pipefail

UPSTREAM_FILE="/etc/nginx/conf.d/clueroom-upstream.conf"
HEALTH_URL="https://api.clueroom.xyz/actuator/health"
BG_COMPOSE="/opt/clueroom/bg-compose"

echo "========================================"
echo " ClueRoom Blue-Green Status"
echo "========================================"

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
  STANDBY_PORT="8082"
elif [ "$ACTIVE_PORT" = "8082" ]; then
  ACTIVE_SERVICE="app-green"
  STANDBY_SERVICE="app-blue"
  STANDBY_PORT="8081"
else
  echo "ERROR: unknown active port: $ACTIVE_PORT"
  exit 1
fi

echo "Active upstream : $ACTIVE_ADDR"
echo "Active service  : $ACTIVE_SERVICE"
echo "Standby service : $STANDBY_SERVICE"
echo "Standby port    : $STANDBY_PORT"

echo ""
echo "[External health]"
curl -I --max-time 10 "$HEALTH_URL" 2>/dev/null | grep -Ei "HTTP/|X-ClueRoom-Upstream" || true

echo ""
echo "[Containers]"
"$BG_COMPOSE" ps
