# Déploiement sur Proxmox (LXC + nginx + DuckDNS)

## 1. Créer la LXC

- Proxmox : CT Debian 12/13, 2 vCPU / 4 Go RAM / 20 Go disque, **unprivileged**.
- Pour Docker dans une LXC unprivileged : activer `nesting=1` et `keyctl=1`
  (`pct set <id> -features nesting=1,keyctl=1`) ou utiliser une LXC privileged dédiée.
- Installer : `apt update && apt install -y docker.io docker-compose-plugin git nginx certbot python3-pip`
- `pip install certbot-dns-duckdns` (plugin DNS-01 DuckDNS).

## 2. Préparer l'application

```bash
git clone https://github.com/AlexandreLP7/finance-dashboard.git /opt/finance-dashboard
cd /opt/finance-dashboard
cp .env.example .env   # renseigner DB_PASSWORD, APP_USER, APP_PASSWORD, DUCKDNS_*
docker compose pull    # images ghcr.io (poussées par le CD)
docker compose up -d   # db + backend + frontend (port 8080)
# SonarQube (CI) : docker compose --profile sonar up -d   -> http://<lxc>:9000
```

## 3. nginx + DuckDNS + TLS (sur l'hôte Proxmox ou la LXC)

```nginx
# /etc/nginx/sites-available/finance-dashboard
server {
    listen 80;
    server_name mon-dashboard.duckdns.org;
    location / { return 301 https://$host$request_uri; }
}
server {
    listen 443 ssl;
    server_name mon-dashboard.duckdns.org;
    ssl_certificate     /etc/letsencrypt/live/mon-dashboard.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/mon-dashboard.duckdns.org/privkey.pem;
    location / {
        proxy_pass http://<IP_LXC>:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Certificat (DNS-01 DuckDNS, gratuit) :

```bash
# /root/duckdns-credentials.ini  (chmod 600, jamais committé)
dns_duckdns_token=<DUCKDNS_TOKEN>
certbot certonly --dns-duckdns --dns-duckdns-credentials /root/duckdns-credentials.ini \
  -d mon-dashboard.duckdns.org
# renouvellement auto : certbot renew (cron/systemd timer déjà fourni)
```

Ouvrir le port 443 sur la box/routeur vers l'hôte Proxmox (NAT) + DuckDNS pointe vers l'IP publique.

## 4. CI/CD GitHub -> LXC

Le workflow `cd.yml` (tag `v*` ou manuel) build les images, les pousse sur ghcr.io
(`ghcr.io/alexandrelp7/finance-dashboard/*`, taguées `latest` + SHA), puis le job `deploy`
tourne sur un **runner GitHub auto-hébergé dans la LXC** : il se connecte lui-même à GitHub,
donc aucun port SSH n'est exposé sur Internet. Il fait `git reset` sur le commit déployé,
`docker compose pull && up -d` avec `TAG=<sha>`, puis vérifie que l'API répond.

Installation du runner (une fois, en root sur la LXC) :

```bash
useradd -m -s /bin/bash runner && usermod -aG docker runner
chown -R runner:runner /opt/finance-dashboard
# télécharger actions-runner-linux-x64 (github.com/actions/runner/releases) dans /home/runner/actions-runner
# token : GitHub > Settings > Actions > Runners > New self-hosted runner
su - runner -c "cd actions-runner && ./config.sh --unattended --url https://github.com/AlexandreLP7/finance-dashboard \
  --token <TOKEN> --name finance-lxc --labels finance-lxc"
cd /home/runner/actions-runner && ./svc.sh install runner && ./svc.sh start
```

Sécurité (dépôt public) : dans *Settings > Actions > General*, activer
« Require approval for all external contributors » pour qu'une PR de fork ne puisse pas
exécuter de code sur le runner. Secrets encore utiles : `SONAR_HOST_URL` / `SONAR_TOKEN` (CI).

## 5. Sauvegardes

```bash
# cron quotidien : dump Postgres
0 3 * * * docker exec finance-dashboard-db-1 pg_dump -U finance finance > /opt/backups/finance-$(date +\%F).sql
```
