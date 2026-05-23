#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="${APP_DIR:-/opt/clueroom/app}"
SECRET_ENV_DIR="${SECRET_ENV_DIR:-/opt/clueroom/secrets/env.d}"

cd "$APP_DIR"

exec docker compose \
  --env-file "$APP_DIR/.env" \
  --env-file "$SECRET_ENV_DIR/ai.env" \
  --env-file "$SECRET_ENV_DIR/portone.env" \
  --env-file "$SECRET_ENV_DIR/oauth.env" \
  -f "$APP_DIR/docker-compose.yml" \
  -f "$APP_DIR/docker-compose.bluegreen.yml" \
  "$@"
