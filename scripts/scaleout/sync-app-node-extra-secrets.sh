#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"

SRC_FIREBASE="/opt/clueroom/secrets/firebase-service-account.json"
SRC_SCENARIOS="/opt/clueroom/secrets/scenarios"
TMP_FIREBASE=""
REMOTE_FIREBASE="/tmp/firebase-service-account.json.scaleout.$$"
REMOTE_FIREBASE_NEEDS_CLEANUP=0

cleanup() {
  if [ -n "${TMP_FIREBASE:-}" ]; then
    rm -f "$TMP_FIREBASE"
  fi

  if [ "${REMOTE_FIREBASE_NEEDS_CLEANUP:-0}" = "1" ]; then
    ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "rm -f '$REMOTE_FIREBASE'" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

echo "========================================"
echo " ClueRoom App Node Extra Secret Sync"
echo "========================================"

echo "[1/7] Check prod Firebase service account file"
sudo test -s "$SRC_FIREBASE"
sudo ls -lh "$SRC_FIREBASE"

echo "[2/7] Check prod scenarios directory"
sudo test -d "$SRC_SCENARIOS"
SCENARIO_FILE_COUNT="$(sudo find "$SRC_SCENARIOS" -type f | wc -l | tr -d ' ')"
echo "prod scenario file count=$SCENARIO_FILE_COUNT"
if [ "$SCENARIO_FILE_COUNT" -lt 1 ]; then
  echo "ERROR: prod scenarios directory is empty"
  exit 1
fi

echo "[3/7] Prepare app node secret directories"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  mkdir -p /opt/clueroom/secrets /opt/clueroom/secrets/scenarios
  chmod 700 /opt/clueroom/secrets /opt/clueroom/secrets/scenarios
'

echo "[4/7] Copy Firebase service account JSON without printing content"
TMP_FIREBASE="$(mktemp /tmp/firebase-service-account.json.scaleout.XXXXXX)"
sudo cp "$SRC_FIREBASE" "$TMP_FIREBASE"
sudo chown "$(id -u):$(id -g)" "$TMP_FIREBASE"
chmod 600 "$TMP_FIREBASE"
REMOTE_FIREBASE_NEEDS_CLEANUP=1
scp "${SSH_OPTS[@]}" "$TMP_FIREBASE" "$SSH_TARGET:$REMOTE_FIREBASE"

ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "
  set -Eeuo pipefail
  trap 'rm -f \"$REMOTE_FIREBASE\"' EXIT
  mv '$REMOTE_FIREBASE' /opt/clueroom/secrets/firebase-service-account.json
  chmod 600 /opt/clueroom/secrets/firebase-service-account.json
  test -s /opt/clueroom/secrets/firebase-service-account.json
  ls -lh /opt/clueroom/secrets/firebase-service-account.json
"
REMOTE_FIREBASE_NEEDS_CLEANUP=0

echo "[5/7] Sync scenarios directory using sudo tar on prod"
sudo tar -C /opt/clueroom -czf - secrets/scenarios \
| ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  set -Eeuo pipefail

  TARGET="/opt/clueroom/secrets/scenarios"
  STAGING="/opt/clueroom/secrets/.scenarios-sync.$$"
  BACKUP="/opt/clueroom/secrets/.scenarios-prev.$$"

  cleanup_scenario_sync() {
    status=$?
    if [ "$status" -ne 0 ] && [ -n "${BACKUP:-}" ] && [ -d "$BACKUP" ] && [ ! -e "$TARGET" ]; then
      mv "$BACKUP" "$TARGET" || true
    fi
    rm -rf "$STAGING"
    return "$status"
  }

  trap cleanup_scenario_sync EXIT

  rm -rf "$STAGING" "$BACKUP"
  mkdir -p "$STAGING"
  tar -xzf - -C "$STAGING" --strip-components=2
  test "$(find "$STAGING" -type f | wc -l)" -gt 0
  chmod 700 /opt/clueroom/secrets "$STAGING"
  chmod -R go-rwx "$STAGING"

  if [ -d "$TARGET" ]; then
    mv "$TARGET" "$BACKUP"
  fi
  mv "$STAGING" "$TARGET"
  rm -rf "$BACKUP"
  BACKUP=""
  trap - EXIT
'

echo "[6/7] Verify app node extra secrets on host"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  test -s /opt/clueroom/secrets/firebase-service-account.json
  test -d /opt/clueroom/secrets/scenarios
  echo firebase=OK
  echo scenario_file_count=$(find /opt/clueroom/secrets/scenarios -type f | wc -l)
  ls -ld /opt/clueroom/secrets/scenarios
'

echo "[7/7] Done"
echo "========================================"
echo " ClueRoom App Node Extra Secret Sync Success"
echo "========================================"
