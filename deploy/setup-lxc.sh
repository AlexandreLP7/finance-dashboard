#!/usr/bin/env bash
# Creation d'une LXC Debian 13 prete pour Docker — a executer sur l'hôte Proxmox (root).
# Usage: ./setup-lxc.sh <ctid> <hostname> <storage> <bridge> <ip>
# Ex:    ./setup-lxc.sh 900 finance "local-lvm" vmbr0 "192.168.1.50/24"
set -euo pipefail

CTID=${1:?CTID requis}
HOSTNAME=${2:?hostname requis}
STORAGE=${3:?storage requis (ex: local-lvm)}
BRIDGE=${4:?bridge requis (ex: vmbr0)}
IP=${5:?ip requis (ex: 192.168.1.50/24)}
GATEWAY=${6:-192.168.1.1}

echo "==> Creation du CT $CTID ($HOSTNAME)"
pct create "$CTID" "local:vztmpl/debian-13-standard_amd64.tar.zst" \
  --hostname "$HOSTNAME" \
  --storage "$STORAGE" --rootfs "${STORAGE}:8" \
  --net0 "name=eth0,bridge=$BRIDGE,ip=$IP,gw=$GATEWAY" \
  --features nesting=1,keyctl=1 \
  --memory 4096 --cores 2 --swap 512

pct start "$CTID"
sleep 5

echo "==> Installation docker + outils"
pct exec "$CTID" -- bash -c "apt-get update && apt-get install -y docker.io docker-compose-plugin git nginx certbot python3-pip curl && pip install --break-system-packages certbot-dns-duckdns"

echo "==> LXC $CTID prete. Deployer ensuite: git clone ... && cp .env.example .env && docker compose up -d"
