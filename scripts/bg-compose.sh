#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
SECRET_ENV_DIR="/opt/clueroom/secrets/env.d"
RUNTIME_ENV_FILE=""
SECRET_ENV_FILES=()

cleanup() {
  if [ -n "${RUNTIME_ENV_FILE:-}" ] && [ "$RUNTIME_ENV_FILE" != "$APP_DIR/.env" ]; then
    rm -f "$RUNTIME_ENV_FILE"
  fi
}

trap cleanup EXIT

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

  SECRET_ENV_FILES+=("$env_file")
}

build_runtime_env_file() {
  if [ "${#SECRET_ENV_FILES[@]}" -eq 0 ]; then
    RUNTIME_ENV_FILE="$APP_DIR/.env"
  else
    RUNTIME_ENV_FILE="$(mktemp /tmp/clueroom-runtime-env.XXXXXX)"
    cat "$APP_DIR/.env" > "$RUNTIME_ENV_FILE"

    for secret_env in "${SECRET_ENV_FILES[@]}"; do
      printf '\n' >> "$RUNTIME_ENV_FILE"
      cat "$secret_env" >> "$RUNTIME_ENV_FILE"
    done

    chmod 600 "$RUNTIME_ENV_FILE"
  fi

  export CLUEROOM_RUNTIME_ENV_FILE="$RUNTIME_ENV_FILE"
}

add_optional_secret_env "$SECRET_ENV_DIR/ai.env"
add_optional_secret_env "$SECRET_ENV_DIR/portone.env"
add_optional_secret_env "$SECRET_ENV_DIR/oauth.env"
build_runtime_env_file

COMPOSE_ARGS+=(
  -f docker-compose.yml
  -f docker-compose.bluegreen.yml
)

docker compose \
  "${COMPOSE_ARGS[@]}" \
  "$@"
