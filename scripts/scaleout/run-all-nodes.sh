#!/usr/bin/env bash
set -Eeuo pipefail

ACTION="${1:?Usage: run-all-nodes.sh <sync|start|check|reset|alloy|verify-secrets|scrub-secrets>}"
BASE_DIR="/opt/clueroom/scaleout"
NODES_DIR="$BASE_DIR/nodes"
SCRIPTS_DIR="$BASE_DIR/scripts"

mapfile -t nodes < <(find "$NODES_DIR" -maxdepth 1 -type f -name '*.env' -printf '%f\n' | sed 's/\.env$//' | sort)

if [ "${#nodes[@]}" -lt 1 ]; then
  echo "ERROR: no nodes found in $NODES_DIR"
  exit 1
fi

for node in "${nodes[@]}"; do
  "$SCRIPTS_DIR/run-node-action.sh" "$node" "$ACTION"
done
