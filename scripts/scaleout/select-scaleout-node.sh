#!/usr/bin/env bash
set -Eeuo pipefail

NODE="${1:?Usage: select-scaleout-node.sh <node-key>}"
BASE_DIR="/opt/clueroom/scaleout"
COMMON_ENV="$BASE_DIR/common.env"
NODE_ENV="$BASE_DIR/nodes/${NODE}.env"
TARGET_ENV="$BASE_DIR/scaleout.env"

test -f "$COMMON_ENV"
test -f "$NODE_ENV"

cat "$COMMON_ENV" > "$TARGET_ENV"
printf '\n' >> "$TARGET_ENV"
cat "$NODE_ENV" >> "$TARGET_ENV"
chmod 600 "$TARGET_ENV"

echo "selected node: $NODE"
grep -E '^(APP_NODE_NAME|APP_NODE_PRIVATE_IP|APP_NODE_PUBLIC_IP|DATA_SERVER_PRIVATE_IP|OPS_LOKI_PUSH_URL)=' "$TARGET_ENV" || true
