#!/usr/bin/env bash
set -euo pipefail

# 어디서 실행하든 프로젝트 루트 기준으로 Gradle/Docker 명령을 실행한다.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Git Bash/WSL에서는 gradlew, Windows 환경에서는 gradlew.bat을 사용한다.
if [[ -x "./gradlew" ]]; then
  GRADLE_CMD="./gradlew"
else
  GRADLE_CMD="./gradlew.bat"
fi

echo "=== [1/3] Gradle bootJar build ==="
"$GRADLE_CMD" bootJar

echo "=== [2/3] Docker Compose up ==="
# Dockerfile은 build/libs/*.jar를 복사하므로 bootJar 이후에 compose build를 실행해야 한다.
docker compose up -d --build

echo "=== [3/3] Container status ==="
docker compose ps

echo
echo "App:   http://localhost:${SERVER_PORT:-8080}"
echo "MySQL: localhost:${MYSQL_HOST_PORT:-33306}"
echo "Redis: localhost:${REDIS_HOST_PORT:-16379}"
