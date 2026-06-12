#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

echo "========================================"
echo " ClueRoom App Node Health Check"
echo "========================================"

echo "[1/4] Remote local health on app node"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  curl -I http://127.0.0.1:8080/actuator/health
  curl http://127.0.0.1:8080/actuator/health
'

echo
echo "[2/4] Prod to app-node private health"
curl -I "http://${APP_NODE_PRIVATE_IP}:8080/actuator/health"
curl "http://${APP_NODE_PRIVATE_IP}:8080/actuator/health"

echo
echo "[3/4] App container env check"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  CONTAINER="$(docker ps --format "{{.Names}}" | grep "^start-up-app" | head -n 1)"
  echo "CONTAINER=$CONTAINER"
  docker exec "$CONTAINER" printenv | grep -E "DB_HOST|DB_PORT|REDIS_HOST|REDIS_PORT|OPENAI_BASE_URL|OPENAI_CHAT_MODEL|AI_LLMOPS_DB_LOGGING_ENABLED|AUTH_REQUIRE_AUTHENTICATION|AUTH_MOCK_FALLBACK_ENABLED|JWT_ISSUER"
  docker exec "$CONTAINER" sh -lc '\''test -n "$JWT_SECRET" && echo "JWT_SECRET=set" || echo "JWT_SECRET=empty"'\''
  docker exec "$CONTAINER" sh -lc '\''test -s /opt/clueroom/secrets/firebase-service-account.json && echo "FIREBASE_SECRET=file-visible" || echo "FIREBASE_SECRET=missing"'\''
'

echo
echo "[4/4] Containers"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'

echo "========================================"
echo " ClueRoom App Node Health Check Success"
echo "========================================"
