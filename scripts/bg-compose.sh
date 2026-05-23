#!/usr/bin/env bash

cd /opt/clueroom/app

docker compose \
  --env-file .env \
  --env-file /opt/clueroom/secrets/env.d/ai.env \
  --env-file /opt/clueroom/secrets/env.d/portone.env \
  --env-file /opt/clueroom/secrets/env.d/oauth.env \
  -f docker-compose.yml \
  -f docker-compose.bluegreen.yml \
  "$@"
