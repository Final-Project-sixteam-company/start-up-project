#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

echo "========================================"
echo " ClueRoom App Node Secret Verify"
echo "========================================"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  echo "[host]"
  test -s /opt/clueroom/secrets/firebase-service-account.json && echo firebase_host=OK
  echo scenario_host_count=$(find /opt/clueroom/secrets/scenarios -type f 2>/dev/null | wc -l)

  echo
  echo "[container]"
  if docker ps --format "{{.Names}}" | grep -q "^start-up-app$"; then
    docker exec start-up-app sh -lc "test -s /opt/clueroom/secrets/firebase-service-account.json && echo firebase_container=OK || echo firebase_container=missing"
    docker exec start-up-app sh -lc "test -d /opt/clueroom/secrets/scenarios && echo scenario_container_count=$(find /opt/clueroom/secrets/scenarios -type f | wc -l) || echo scenario_container=missing"
    docker exec start-up-app sh -lc "test -n \"\$JWT_SECRET\" && echo JWT_SECRET=set || echo JWT_SECRET=empty"
  else
    echo "start-up-app is not running"
  fi
'

echo "========================================"
echo " ClueRoom App Node Secret Verify Done"
echo "========================================"
