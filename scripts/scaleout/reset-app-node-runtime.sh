#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

echo "========================================"
echo " ClueRoom App Node Reset Runtime"
echo "========================================"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  docker rm -f start-up-app 2>/dev/null || true
  docker rm -f clueroom-app-node-alloy 2>/dev/null || true
  rm -f /tmp/clueroom-app-node-runtime.env
  docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
'

echo "========================================"
echo " ClueRoom App Node Reset Runtime Done"
echo "========================================"
