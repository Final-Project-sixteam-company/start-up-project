#!/usr/bin/env bash
set -Eeuo pipefail

BASE_DIR="/opt/clueroom/scaleout"
OUTPUT_DIR="$BASE_DIR/terraform-output"
NODES_DIR="$BASE_DIR/nodes"
PRIVATE_JSON="$OUTPUT_DIR/app_private_ips.json"
PUBLIC_JSON="$OUTPUT_DIR/app_public_ips.json"

test -f "$PRIVATE_JSON"
test -f "$PUBLIC_JSON"
mkdir -p "$NODES_DIR"

for key in $(jq -r 'keys[]' "$PRIVATE_JSON"); do
  private_ip="$(jq -r --arg k "$key" '.[$k]' "$PRIVATE_JSON")"
  public_ip="$(jq -r --arg k "$key" '.[$k] // ""' "$PUBLIC_JSON")"

  number="${key#app}"
  if [ "$number" = "$key" ]; then
    app_name="clueroom-${key}"
  else
    app_name="clueroom-app-${number}"
  fi

  cat > "$NODES_DIR/${key}.env" << EOF
APP_NODE_NAME=${app_name}
APP_NODE_PRIVATE_IP=${private_ip}
APP_NODE_PUBLIC_IP=${public_ip}
EOF

  chmod 600 "$NODES_DIR/${key}.env"
  echo "created $NODES_DIR/${key}.env -> $app_name $private_ip"
done
