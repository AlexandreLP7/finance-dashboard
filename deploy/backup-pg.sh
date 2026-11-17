#!/usr/bin/env bash
# Sauvegarde quotidienne de PostgreSQL (a planifier en cron sur la LXC).
# cron: 0 3 * * * /opt/finance-dashboard/deploy/backup-pg.sh
set -euo pipefail

BACKUP_DIR=${BACKUP_DIR:-/opt/backups}
mkdir -p "$BACKUP_DIR"
STAMP=$(date +%F)
docker exec finance-dashboard-db-1 pg_dump -U "${DB_USER:-finance}" "${DB_NAME:-finance}" \
  | gzip > "$BACKUP_DIR/finance-$STAMP.sql.gz"
find "$BACKUP_DIR" -name 'finance-*.sql.gz' -mtime +30 -delete
echo "Backup OK: $BACKUP_DIR/finance-$STAMP.sql.gz"
