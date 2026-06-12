#!/usr/bin/env bash
set -Eeuo pipefail

source /opt/clueroom/scaleout/scaleout.env

SSH_OPTS=(-o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -i "$APP_NODE_SSH_KEY")
SSH_TARGET="${APP_NODE_SSH_USER}@${APP_NODE_PRIVATE_IP}"
TMP_CONFIG=""
REMOTE_CONFIG="/opt/clueroom/alloy/.config.alloy.$$"
REMOTE_CONFIG_NEEDS_CLEANUP=0

cleanup() {
  if [ -n "${TMP_CONFIG:-}" ]; then
    rm -f "$TMP_CONFIG"
  fi

  if [ "${REMOTE_CONFIG_NEEDS_CLEANUP:-0}" = "1" ]; then
    ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "rm -f '$REMOTE_CONFIG'" >/dev/null 2>&1 || true
  fi
}

trap cleanup EXIT

echo "========================================"
echo " ClueRoom App Node Alloy Install"
echo "========================================"

TMP_CONFIG="$(mktemp /tmp/clueroom-app-node-alloy-config.XXXXXX)"
ORIGINAL_UMASK="$(umask)"
umask 077
cat > "$TMP_CONFIG" << CONFIG
loki.write "ops" {
  endpoint {
    url = "$OPS_LOKI_PUSH_URL"
  }
}

discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

discovery.relabel "app_containers" {
  targets = discovery.docker.containers.targets

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(start-up-app.*)"
    action        = "keep"
  }

  rule {
    source_labels = ["__meta_docker_container_name"]
    regex         = "/(.*)"
    target_label  = "container"
  }

  rule {
    source_labels = ["__meta_docker_container_log_stream"]
    target_label  = "stream"
  }

  rule {
    target_label = "job"
    replacement  = "docker-app"
  }

  rule {
    target_label = "instance"
    replacement  = "$APP_NODE_NAME"
  }
}

loki.source.docker "app" {
  host       = "unix:///var/run/docker.sock"
  targets    = discovery.relabel.app_containers.output
  forward_to = [loki.write.ops.receiver]
}
CONFIG
umask "$ORIGINAL_UMASK"
chmod 600 "$TMP_CONFIG"

echo "[1/5] Copy Alloy config"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "mkdir -p /opt/clueroom/alloy/data && chmod 700 /opt/clueroom/alloy /opt/clueroom/alloy/data"
REMOTE_CONFIG_NEEDS_CLEANUP=1
scp "${SSH_OPTS[@]}" "$TMP_CONFIG" "$SSH_TARGET:$REMOTE_CONFIG"

echo "[2/5] Install/restart Alloy"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "
  set -Eeuo pipefail
  trap 'rm -f \"$REMOTE_CONFIG\"' EXIT
  chmod 600 '$REMOTE_CONFIG'
  mv '$REMOTE_CONFIG' /opt/clueroom/alloy/config.alloy
  chmod 600 /opt/clueroom/alloy/config.alloy
  chmod 700 /opt/clueroom/alloy /opt/clueroom/alloy/data

  docker rm -f clueroom-app-node-alloy 2>/dev/null || true

  docker run -d \
    --name clueroom-app-node-alloy \
    --restart unless-stopped \
    --user root \
    -v /opt/clueroom/alloy/config.alloy:/etc/alloy/config.alloy:ro \
    -v /opt/clueroom/alloy/data:/var/lib/alloy/data \
    -v /var/run/docker.sock:/var/run/docker.sock \
    grafana/alloy:latest \
    run /etc/alloy/config.alloy \
    --storage.path=/var/lib/alloy/data
"
REMOTE_CONFIG_NEEDS_CLEANUP=0

echo "[3/5] Check Alloy container"
ssh "${SSH_OPTS[@]}" "$SSH_TARGET" '
  sleep 3
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep alloy
  docker logs --tail=80 clueroom-app-node-alloy 2>&1 | sed -E "s#https?://[^[:space:]\"]+#<redacted-url>#g"
'

echo "[4/5] Cleanup local temp file"
rm -f "$TMP_CONFIG"
TMP_CONFIG=""

echo "[5/5] Done"
echo "========================================"
echo " ClueRoom App Node Alloy Install Success"
echo "========================================"
