#!/usr/bin/env bash
set -Eeuo pipefail

MODE="${1:-local-only}"
BASE_DIR="/opt/clueroom/scaleout"
BACKUP_DIR="$BASE_DIR/backups/nginx-upstream"
UPSTREAM_CONF="/etc/nginx/conf.d/clueroom-upstream.conf"
LOCK_FILE="/tmp/clueroom-scaleout-nginx.lock"
TMP_CONF=""
CURRENT_BACKUP=""
TS="$(date +%Y%m%d_%H%M%S)"

exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  echo "ERROR: another scaleout Nginx operation is already running"
  exit 1
fi

cleanup() {
  if [ -n "${TMP_CONF:-}" ]; then
    rm -f "$TMP_CONF"
  fi
}

trap cleanup EXIT
mkdir -p "$BACKUP_DIR"

CURRENT_BACKUP="$BACKUP_DIR/clueroom-upstream.conf.before-rollback-$TS"
sudo cp "$UPSTREAM_CONF" "$CURRENT_BACKUP"

restore_current_upstream() {
  if [ -s "$CURRENT_BACKUP" ]; then
    echo "restore upstream config from pre-rollback backup: $CURRENT_BACKUP"
    sudo cp "$CURRENT_BACKUP" "$UPSTREAM_CONF"
    sudo nginx -t && sudo systemctl reload nginx || true
  fi
}

detect_local_active() {
  local active

  active="$(grep -Eo '127[.]0[.]0[.]1:(8081|8082)' "$UPSTREAM_CONF" | head -n 1 || true)"
  if echo "$active" | grep -Eq '^127[.]0[.]0[.]1:(8081|8082)$'; then
    echo "$active"
    return
  fi

  active="$(/opt/clueroom/bg-status.sh | awk -F: '/Active service/{gsub(/^[ \t]+|[ \t]+$/, "", $2); print $2}' || true)"
  case "$active" in
    app-blue) echo "127.0.0.1:8081" ;;
    app-green) echo "127.0.0.1:8082" ;;
    *) echo "ERROR: cannot detect active local upstream" >&2; return 1 ;;
  esac
}

case "$MODE" in
  local-only)
    LOCAL_ACTIVE="$(detect_local_active)"
    TMP_CONF="/tmp/clueroom-upstream-local-only-$(date +%Y%m%d_%H%M%S).conf"
    cat > "$TMP_CONF" << EOF
upstream clueroom_backend {
    server ${LOCAL_ACTIVE} max_fails=3 fail_timeout=10s;
}
EOF

    echo "restore local-only upstream: $LOCAL_ACTIVE"
    sudo cp "$TMP_CONF" "$UPSTREAM_CONF"
    ;;

  latest-backup)
    LATEST_BACKUP="$(ls -t "$BACKUP_DIR"/clueroom-upstream.conf.before-scaleout-* 2>/dev/null | head -n 1 || true)"
    if [ -z "$LATEST_BACKUP" ]; then
      echo "ERROR: no upstream backup found in $BACKUP_DIR"
      exit 1
    fi

    echo "rollback from backup: $LATEST_BACKUP"
    sudo cp "$LATEST_BACKUP" "$UPSTREAM_CONF"
    ;;

  *)
    echo "ERROR: unknown mode: $MODE. Use local-only or latest-backup."
    exit 1
    ;;
esac

if ! sudo nginx -t; then
  restore_current_upstream
  exit 1
fi

if ! sudo systemctl reload nginx; then
  restore_current_upstream
  exit 1
fi
curl -sI https://api.clueroom.xyz/actuator/health | grep -i 'X-ClueRoom-Upstream' || true
