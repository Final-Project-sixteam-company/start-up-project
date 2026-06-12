#!/usr/bin/env bash
set -Eeuo pipefail

BASE_DIR="/opt/clueroom/scaleout"
CURRENT_ENV="$BASE_DIR/scaleout.env"
COMMON_ENV="$BASE_DIR/common.env"
NODES_DIR="$BASE_DIR/nodes"

test -f "$CURRENT_ENV"
mkdir -p "$NODES_DIR"

write_env_var() {
  local key="$1"
  local value="$2"
  printf '%s=%q\n' "$key" "$value"
}

set -a
source "$CURRENT_ENV"
set +a

: "${DATA_SERVER_PRIVATE_IP:?DATA_SERVER_PRIVATE_IP missing in $CURRENT_ENV}"
: "${OPS_LOKI_PUSH_URL:?OPS_LOKI_PUSH_URL missing in $CURRENT_ENV}"

{
  write_env_var APP_NODE_SSH_USER "${APP_NODE_SSH_USER:-ubuntu}"
  write_env_var APP_NODE_SSH_KEY "${APP_NODE_SSH_KEY:-/opt/clueroom/scaleout/keys/app-node-control}"

  write_env_var PROD_APP_DIR "${PROD_APP_DIR:-/opt/clueroom/app}"
  write_env_var PROD_SECRET_ENV_DIR "${PROD_SECRET_ENV_DIR:-/opt/clueroom/secrets/env.d}"

  write_env_var APP_NODE_APP_DIR "${APP_NODE_APP_DIR:-/opt/clueroom/app}"
  write_env_var APP_NODE_SECRET_ENV_DIR "${APP_NODE_SECRET_ENV_DIR:-/opt/clueroom/secrets/env.d}"

  write_env_var DATA_SERVER_PRIVATE_IP "$DATA_SERVER_PRIVATE_IP"
  write_env_var OPS_LOKI_PUSH_URL "$OPS_LOKI_PUSH_URL"
} > "$COMMON_ENV"

NODE_KEY="${APP_NODE_NAME:-clueroom-app-01}"
NODE_KEY="${NODE_KEY#clueroom-}"
NODE_KEY="${NODE_KEY//-}"
[ "$NODE_KEY" = "app1" ] && NODE_KEY="app01"

: "${APP_NODE_PRIVATE_IP:?APP_NODE_PRIVATE_IP missing}"

{
  write_env_var APP_NODE_NAME "${APP_NODE_NAME:-clueroom-app-01}"
  write_env_var APP_NODE_PRIVATE_IP "$APP_NODE_PRIVATE_IP"
  write_env_var APP_NODE_PUBLIC_IP "${APP_NODE_PUBLIC_IP:-}"
} > "$NODES_DIR/${NODE_KEY}.env"

chmod 600 "$COMMON_ENV" "$NODES_DIR/${NODE_KEY}.env"
echo "created: $COMMON_ENV"
echo "created: $NODES_DIR/${NODE_KEY}.env"
