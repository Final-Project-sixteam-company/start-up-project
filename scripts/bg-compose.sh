#!/usr/bin/env bash

set -Eeuo pipefail

cd /opt/clueroom/app

exec docker compose \
  -f docker-compose.yml \
  -f docker-compose.bluegreen.yml \
  "$@"
