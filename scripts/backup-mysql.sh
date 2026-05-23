#!/usr/bin/env bash

set -Eeuo pipefail

APP_DIR="/opt/clueroom/app"
ENV_FILE="${APP_DIR}/.env"
BACKUP_DIR="/opt/clueroom/backups/mysql"
RETENTION_DAYS=7

echo "========================================"
echo " ClueRoom MySQL Backup Start"
echo "========================================"

if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: .env file not found: $ENV_FILE"
  exit 1
fi

read_env_value() {
  local key="$1"
  grep -E "^${key}=" "$ENV_FILE" | tail -n 1 | cut -d '=' -f2-
}

DB_NAME="$(read_env_value DB_NAME)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"

if [ -z "$DB_NAME" ]; then
  echo "ERROR: DB_NAME is empty"
  exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
  echo "ERROR: DB_PASSWORD is empty"
  exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

cd "$APP_DIR"

echo "[1/4] Check MySQL container"
docker compose ps mysql

echo "[2/4] Check MySQL connection"
docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql mysqladmin ping -uroot --silent

TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[3/4] Dump database: ${DB_NAME}"
docker compose exec -T -e MYSQL_PWD="$DB_PASSWORD" mysql \
  mysqldump -uroot \
  --single-transaction \
  --quick \
  --routines \
  --triggers \
  --databases "$DB_NAME" \
  | gzip -9 > "$BACKUP_FILE"

chmod 600 "$BACKUP_FILE"

echo "[4/4] Remove old backups older than ${RETENTION_DAYS} days"
find "$BACKUP_DIR" -type f -name "${DB_NAME}_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete

echo "Backup completed:"
ls -lh "$BACKUP_FILE"

echo "========================================"
echo " ClueRoom MySQL Backup Success"
echo "========================================"
