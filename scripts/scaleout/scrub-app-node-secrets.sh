#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

if [ "${CONFIRM_SCRUB_APP_NODE_SECRETS:-}" != "YES" ]; then
  echo "ERROR: set CONFIRM_SCRUB_APP_NODE_SECRETS=YES to scrub app node secrets"
  exit 1
fi

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

echo "========================================"
echo " ClueRoom App Node Secret Scrub"
echo "========================================"
echo "target=${APP_NODE_NAME:-unknown} ${APP_NODE_PRIVATE_IP}"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  set -Eeuo pipefail

  echo "[1/4] Stop containers that may mount app node secrets"
  docker rm -f start-up-app clueroom-app-node-alloy 2>/dev/null || true

  echo "[2/4] Remove copied secret material"
  rm -rf /opt/clueroom/secrets/env.d
  rm -f /opt/clueroom/secrets/firebase-service-account.json
  rm -rf /opt/clueroom/secrets/scenarios
  rm -f /opt/clueroom/app/.env
  rm -f /tmp/clueroom-app-node-runtime.env

  echo "[3/4] Remove generated config that can contain credential-bearing endpoints"
  rm -f /opt/clueroom/alloy/config.alloy
  rm -f /opt/clueroom/alloy/.config.alloy.*

  echo "[4/4] Verify scrubbed paths"
  for path in \
    /opt/clueroom/secrets/env.d \
    /opt/clueroom/secrets/firebase-service-account.json \
    /opt/clueroom/secrets/scenarios \
    /opt/clueroom/app/.env \
    /tmp/clueroom-app-node-runtime.env \
    /opt/clueroom/alloy/config.alloy
  do
    if [ -e "$path" ]; then
      echo "ERROR: scrub target remains: $path"
      exit 1
    fi
  done
'

echo "========================================"
echo " ClueRoom App Node Secret Scrub Done"
echo "========================================"
