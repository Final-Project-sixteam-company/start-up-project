#!/usr/bin/env bash
set -Eeuo pipefail

MODE="${1:-canary}"
BASE_DIR="/opt/clueroom/scaleout"
COMMON_ENV="$BASE_DIR/common.env"
NODES_DIR="$BASE_DIR/nodes"
BACKUP_DIR="$BASE_DIR/backups/nginx-upstream"
UPSTREAM_CONF="/etc/nginx/conf.d/clueroom-upstream.conf"

test -f "$COMMON_ENV"
test -d "$NODES_DIR"
source "$COMMON_ENV"
mkdir -p "$BACKUP_DIR"

case "$MODE" in
  canary) LOCAL_WEIGHT=3; NODE_WEIGHT=1 ;;
  equal) LOCAL_WEIGHT=1; NODE_WEIGHT=1 ;;
  *) echo "ERROR: unknown mode: $MODE. Use canary or equal."; exit 1 ;;
esac

echo "========================================"
echo " ClueRoom Nginx Scaleout Upstream Apply"
echo "========================================"
echo "mode=$MODE"

ACTIVE_UPSTREAM="$(
  curl -sI https://api.clueroom.xyz/actuator/health \
    | awk -F': ' 'tolower($1)=="x-clueroom-upstream"{gsub(/\r/,"",$2); print $2; exit}'
)"

if ! echo "$ACTIVE_UPSTREAM" | grep -Eq '^127[.]0[.]0[.]1:(8081|8082)$'; then
  ACTIVE_UPSTREAM="$(grep -Eo '127[.]0[.]0[.]1:(8081|8082)' "$UPSTREAM_CONF" | head -n 1 || true)"
fi

if ! echo "$ACTIVE_UPSTREAM" | grep -Eq '^127[.]0[.]0[.]1:(8081|8082)$'; then
  ACTIVE_SERVICE="$(/opt/clueroom/bg-status.sh | awk -F: '/Active service/{gsub(/^[ \t]+|[ \t]+$/, "", $2); print $2}' || true)"
  case "$ACTIVE_SERVICE" in
    app-blue) ACTIVE_UPSTREAM="127.0.0.1:8081" ;;
    app-green) ACTIVE_UPSTREAM="127.0.0.1:8082" ;;
    *) echo "ERROR: cannot detect active local upstream"; exit 1 ;;
  esac
fi

echo "ACTIVE_UPSTREAM=$ACTIVE_UPSTREAM"
curl -fsS "http://${ACTIVE_UPSTREAM}/actuator/health" >/dev/null
echo "local active health OK"

mapfile -t NODE_ENV_FILES < <(find "$NODES_DIR" -maxdepth 1 -type f -name '*.env' | sort)
HEALTHY_NODE_LINES=()

for node_env in "${NODE_ENV_FILES[@]}"; do
  APP_NODE_NAME=""
  APP_NODE_PRIVATE_IP=""
  APP_NODE_PUBLIC_IP=""
  set -a
  source "$node_env"
  set +a

  echo "[check] $APP_NODE_NAME $APP_NODE_PRIVATE_IP"

  if curl -fsS "http://${APP_NODE_PRIVATE_IP}:8080/actuator/health" >/dev/null; then
    echo "healthy: $APP_NODE_NAME $APP_NODE_PRIVATE_IP"
    HEALTHY_NODE_LINES+=("    server ${APP_NODE_PRIVATE_IP}:8080 max_fails=3 fail_timeout=10s weight=${NODE_WEIGHT};")
  else
    echo "WARN: skip unhealthy node $APP_NODE_NAME $APP_NODE_PRIVATE_IP"
  fi
done

if [ "${#HEALTHY_NODE_LINES[@]}" -lt 1 ]; then
  echo "ERROR: no healthy scaleout app nodes found"
  exit 1
fi

TS="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/clueroom-upstream.conf.before-scaleout-$TS"
sudo cp "$UPSTREAM_CONF" "$BACKUP_FILE"
echo "backup=$BACKUP_FILE"

TMP_CONF="/tmp/clueroom-upstream-scaleout-$TS.conf"
{
  echo "upstream clueroom_backend {"
  echo "    server ${ACTIVE_UPSTREAM} max_fails=3 fail_timeout=10s weight=${LOCAL_WEIGHT};"
  for line in "${HEALTHY_NODE_LINES[@]}"; do
    echo "$line"
  done
  echo "}"
} > "$TMP_CONF"

echo
echo "[new upstream]"
cat "$TMP_CONF"

if grep -Eq 'server 127[.]0[.]0[.]1[[:space:]]' "$TMP_CONF"; then
  echo "ERROR: malformed local upstream without port detected"
  exit 1
fi

if grep -Eq 'server 127[.]0[.]0[.]1:80([[:space:];]|$)' "$TMP_CONF"; then
  echo "ERROR: malformed local upstream detected: 127.0.0.1:80"
  exit 1
fi

sudo cp "$TMP_CONF" "$UPSTREAM_CONF"
sudo nginx -t
sudo systemctl reload nginx

curl -I https://api.clueroom.xyz/actuator/health

echo "========================================"
echo " ClueRoom Nginx Scaleout Upstream Apply Success"
echo "========================================"
echo "mode=$MODE"
echo "backup=$BACKUP_FILE"
