#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="${APP_DIR}/.env.prod"
BACKUP_DIR="${BACKUP_DIR:-${APP_DIR}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [ -f "$ENV_FILE" ]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

PG_USER="${POSTGRES_USER:-imdb}"
PG_DB="${POSTGRES_DB:-imdbselector}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TARGET="${BACKUP_DIR}/imdbselector_${TIMESTAMP}.sql.gz"

umask 077
mkdir -p "$BACKUP_DIR"

docker exec imdbselector-postgres pg_dump -U "$PG_USER" "$PG_DB" | gzip > "$TARGET"
chmod 600 "$TARGET"

find "$BACKUP_DIR" -type f -name "imdbselector_*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
echo "Backup saved: $TARGET"
