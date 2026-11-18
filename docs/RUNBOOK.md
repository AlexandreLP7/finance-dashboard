# Runbook — mise en production (jalon 3)

## 0. Prérequis
- LXC créée (voir `deploy/setup-lxc.sh`) avec Docker, IP fixe `LXC_IP`.
- Compte DuckDNS : domaine `DOMAIN.duckdns.org` pointant vers l'IP publique de la box.
- Inscription portail développeur Arkéa (getarkea.com) pour le connecteur PSD2 (sandbox gratuite d'abord).

## 1. LXC applicative
```bash
# sur l'hôte Proxmox
./deploy/setup-lxc.sh 900 finance local-lvm vmbr0 192.168.1.50/24
# dans la LXC
apt-get update && apt-get install -y git
docker --version
git clone https://github.com/AlexandreLP7/finance-dashboard.git /opt/finance-dashboard
cd /opt/finance-dashboard
cp .env.example .env   # renseigner TOUT (voir ci-dessous)
```

Variables `.env` obligatoires en prod :
`DB_PASSWORD`, `APP_USER`, `APP_PASSWORD`, `APP_ENCRYPTION_KEY` (32 octets base64 — `openssl rand -base64 32`),
`JWT_SECRET` (≥ 32 octets base64 — `openssl rand -base64 48`, signe les jetons d'auth),
`DUCKDNS_DOMAIN`, `DUCKDNS_TOKEN`, `TAG=latest`, et pour la banque : `BANK_PSD2_BASE_URL`, `BANK_PSD2_CLIENT_ID`,
`BANK_PSD2_CLIENT_SECRET`, `BANK_PSD2_REDIRECT_URI=https://DOMAIN.duckdns.org/bank-callback`.
Optionnelles : `RATE_LIMIT_AUTH_PER_MINUTE` (20 par défaut), `APP_TOTP_ISSUER`.

## Authentification (jalon 4)

L'API exige un jeton JWT (`Authorization: Bearer ...`) sauf `/api/auth/login`, `/api/auth/refresh`,
`/api/auth/2fa/verify` et `/actuator/health`. Le premier démarrage crée l'utilisateur `APP_USER`/`APP_PASSWORD`.
- Access token : 15 min. Refresh token : 7 jours, stocké hashé (SHA-256) en base, rotatif et révocable (`/api/auth/logout`).
- 2FA TOTP (RFC 6238) activable depuis l'UI (Paramètres) : `POST /api/auth/2fa/setup` renvoie un secret + URI `otpauth://` (QR code).
- Rate limiting : 20 requêtes/min/IP sur `/api/auth/**` (429 au-delà).
- Le front stocke le refresh token en `localStorage` et le access token en mémoire ; sur 401 il tente un refresh une fois puis redirige vers `/login`.

## 2. SonarQube (pour la CI, étape 4)
```bash
docker compose --profile sonar up -d    # http://LXC_IP:9000
```
Puis créer le projet `finance-dashboard` + un token, et ajouter dans le dépôt GitHub
(Settings → Secrets and variables → Actions) : `SONAR_HOST_URL=http://LXC_IP:9000` (ou via le domaine HTTPS), `SONAR_TOKEN=...`.

## 3. Mise en ligne (nginx + DuckDNS + TLS)
```bash
# sur l'hôte nginx (Proxmox ou la LXC)
cp deploy/nginx-finance-dashboard.conf /etc/nginx/sites-available/finance-dashboard.conf
sed -i "s/__DOMAIN__/DOMAIN.duckdns.org/g; s/__LXC_IP__/LXC_IP/g" /etc/nginx/sites-available/finance-dashboard.conf
ln -sf /etc/nginx/sites-available/finance-dashboard.conf /etc/nginx/sites-enabled/
DUCKDNS_DOMAIN=DOMAIN.duckdns.org DUCKDNS_TOKEN=... ./deploy/certbot-duckdns.sh
nginx -t && systemctl reload nginx
# NAT box: ouvrir le 443 (et 80) vers l'hôte Proxmox
```

## 4. Premier déploiement via le CD
Secrets GitHub (Actions) à créer : `SSH_HOST` (IP publique ou interne de la LXC selon le réseau),
`SSH_USER`, `SSH_PRIVATE_KEY` (clé dédiée, dans `~/.ssh/authorized_keys` de la LXC), `DEPLOY_PATH=/opt/finance-dashboard`.
Puis : `git tag v0.1.0 && git push origin v0.1.0` (ou bouton « Run workflow » sur `cd.yml`)
→ build des images → push ghcr.io → SSH → `docker compose pull && docker compose up -d`.

Vérifier : `https://DOMAIN.duckdns.org` (page de login APP_USER/APP_PASSWORD, jeton JWT ensuite),
`/actuator/health` via le proxy interne, onglet Transactions → « Connecter ma banque (PSD2) ».

## 5. Exploitation
- Sauvegardes : `0 3 * * * /opt/finance-dashboard/deploy/backup-pg.sh` (cron LXC).
- Mises à jour : nouveau tag `vX.Y.Z` → CD automatique.
- Logs : `docker compose logs -f backend`.
- Rollback : `TAG=<ancien-sha> docker compose up -d`.
