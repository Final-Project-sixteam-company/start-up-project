#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
SECRET_ENV_DIR="/opt/clueroom/secrets/env.d"

cd "$APP_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  echo "ERROR: .env file does not exist: $APP_DIR/.env"
  exit 1
fi

if [ ! -r "$APP_DIR/.env" ]; then
  echo "ERROR: .env file is not readable: $APP_DIR/.env"
  exit 1
fi

COMPOSE_ARGS=(
  --env-file "$APP_DIR/.env"
)

add_optional_secret_env() {
  local env_file="$1"

  if [ ! -e "$env_file" ]; then
    return
  fi

  if [ ! -r "$env_file" ]; then
    echo "ERROR: secret env file is not readable: $env_file"
    exit 1
  fi

  COMPOSE_ARGS+=(--env-file "$env_file")
}

add_optional_secret_env "$SECRET_ENV_DIR/ai.env"
add_optional_secret_env "$SECRET_ENV_DIR/portone.env"
add_optional_secret_env "$SECRET_ENV_DIR/oauth.env"

COMPOSE_ARGS+=(
  -f docker-compose.yml
  -f docker-compose.bluegreen.yml
)

exec docker compose \
  "${COMPOSE_ARGS[@]}" \
  "$@"
