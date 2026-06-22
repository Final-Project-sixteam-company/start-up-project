#!/usr/bin/env bash
set -euo pipefail

# 프로젝트 루트로 이동해 compose project name과 volume name이 일관되게 잡히도록 한다.
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Docker Compose down ==="
# 기본 down은 named volume을 보존한다. DB까지 초기화하려면 docker compose down -v를 직접 사용한다.
docker compose down

echo "=== Container status ==="
docker compose ps
