#!/usr/bin/env bash
set -Eeuo pipefail

NODE="${1:?Usage: run-node-action.sh <node-key> <sync|start|check|reset|alloy|verify-secrets>}"
ACTION="${2:?Usage: run-node-action.sh <node-key> <sync|start|check|reset|alloy|verify-secrets>}"

BASE_DIR="/opt/clueroom/scaleout"
SCRIPTS_DIR="$BASE_DIR/scripts"

echo "========================================"
echo " Run action=$ACTION node=$NODE"
echo "========================================"

"$SCRIPTS_DIR/select-scaleout-node.sh" "$NODE"

case "$ACTION" in
  sync) "$SCRIPTS_DIR/sync-app-node.sh" ;;
  start) "$SCRIPTS_DIR/start-app-node.sh" ;;
  check) "$SCRIPTS_DIR/check-app-node-health.sh" ;;
  reset) "$SCRIPTS_DIR/reset-app-node-runtime.sh" ;;
  alloy) "$SCRIPTS_DIR/install-app-node-alloy.sh" ;;
  verify-secrets) "$SCRIPTS_DIR/verify-app-node-secrets.sh" ;;
  *) echo "ERROR: unknown action: $ACTION"; exit 1 ;;
esac
