#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

echo "========================================"
echo " ClueRoom App Node Sync"
echo "========================================"

cd "$PROD_APP_DIR"

echo "[1/9] Check prod app files"
for f in docker-compose.yml docker-compose.external-data.yml .env gradlew; do
  test -f "$f" || { echo "ERROR: missing $PROD_APP_DIR/$f"; exit 1; }
done

echo "[2/9] Build bootJar on prod"
./gradlew bootJar -x test

JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*plain*' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | awk '{print $2}' || true)"
test -n "$JAR_FILE"
test -f "$JAR_FILE"
echo "JAR_FILE=$JAR_FILE"
ls -lh "$JAR_FILE"

echo "[3/9] Prepare app node directories"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "
  mkdir -p '$APP_NODE_APP_DIR/build/libs'
  mkdir -p '$APP_NODE_SECRET_ENV_DIR'
  chmod 700 /opt/clueroom/secrets /opt/clueroom/secrets/env.d 2>/dev/null || true
"

echo "[4/9] Sync app source without build artifacts"
tar \
  --exclude='.git' \
  --exclude='.gradle' \
  --exclude='.private' \
  --exclude='build' \
  --exclude='.env.before-*' \
  --exclude='.terraform' \
  --exclude='.terraform.lock.hcl' \
  --exclude='*.pem' \
  --exclude='*.key' \
  --exclude='*.p8' \
  --exclude='id_rsa*' \
  --exclude='*.tfvars' \
  --exclude='*.tfplan' \
  --exclude='terraform.tfstate*' \
  -czf - . \
| ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "cd '$APP_NODE_APP_DIR' && tar -xzf - && chmod 600 '$APP_NODE_APP_DIR/.env'"

echo "[5/9] Sync bootJar artifact"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "mkdir -p '$APP_NODE_APP_DIR/build/libs' && rm -f '$APP_NODE_APP_DIR/build/libs/'*.jar"
scp "${SSH_OPTS[@]}" "$JAR_FILE" "$SSH_TARGET:${APP_NODE_APP_DIR}/build/libs/app.jar"

echo "[6/9] Sync secret env files"
cd /opt/clueroom
tar -czf - secrets/env.d \
| ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "
  cd /opt/clueroom &&
  tar -xzf - &&
  chmod 700 /opt/clueroom/secrets /opt/clueroom/secrets/env.d &&
  chmod 600 /opt/clueroom/secrets/env.d/* 2>/dev/null || true
"

echo "[7/9] Sync Firebase/scenario secrets"
/opt/clueroom/scaleout/scripts/sync-app-node-extra-secrets.sh

echo "[8/9] Verify synced files"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "
  ls -al '$APP_NODE_APP_DIR/docker-compose.yml' '$APP_NODE_APP_DIR/docker-compose.external-data.yml' '$APP_NODE_APP_DIR/.env'
  ls -lh '$APP_NODE_APP_DIR/build/libs'
  test -s /opt/clueroom/secrets/firebase-service-account.json && ls -lh /opt/clueroom/secrets/firebase-service-account.json
  echo scenario_file_count=\$(find /opt/clueroom/secrets/scenarios -type f 2>/dev/null | wc -l)
  ls -al '$APP_NODE_SECRET_ENV_DIR'
"

echo "[9/9] Sync completed"
echo "========================================"
echo " ClueRoom App Node Sync Success"
echo "========================================"
