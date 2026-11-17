#!/usr/bin/env bash
# Certificat Let's Encrypt via DNS-01 DuckDNS (gratuit) — a executer sur l'hôte nginx (root).
# Usage: DUCKDNS_DOMAIN=mon-dashboard DUCKDNS_TOKEN=xxxx ./certbot-duckdns.sh
set -euo pipefail

DOMAIN=${DUCKDNS_DOMAIN:?DUCKDNS_DOMAIN requis (ex: mon-dashboard.duckdns.org)}
TOKEN=${DUCKDNS_TOKEN:?DUCKDNS_TOKEN requis}

CRED=/root/duckdns-credentials.ini
umask 077
cat > "$CRED" <<CREDENTIALS
dns_duckdns_token=$TOKEN
CREDENTIALS
chmod 600 "$CRED"

certbot certonly --non-interactive --agree-tos -m admin@example.com \
  --dns-duckdns --dns-duckdns-credentials "$CRED" \
  -d "$DOMAIN"

# renouvellement automatique (deja present via le timer systemd certbot)
systemctl reload nginx || true
echo "Certificat OK pour $DOMAIN"
