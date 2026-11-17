# Finance Dashboard

Dashboard personnel de finances, auto-hébergé : centralise tes comptes bancaires,
importe tes transactions, détecte les anomalies, produit des graphiques et des
conseils. **100 % self-hosted sur ta LXC Proxmox** (tes données ne quittent jamais
ton serveur).

## Stack

- **Front** : Vue 3 + TypeScript + Vite + Pinia + Vue Router + Apache ECharts
- **Back** : Java 21 + Spring Boot 3 (Web, Data JPA, Security, Actuator), PostgreSQL + Flyway
- **CI/CD** : GitHub Actions (CI : init → tests → ESLint → SonarQube → check TypeScript ;
  CD : images Docker sur ghcr.io → déploiement SSH sur la LXC)
- **Infra** : LXC Proxmox + Docker Compose + nginx + DuckDNS + Let's Encrypt (DNS-01)

## Arborescence

```
finance-dashboard/
├── frontend/            # Vue 3 + TS (Vite, Vitest, ESLint, vue-tsc)
├── backend/             # Spring Boot 3 (Maven, JUnit 5, JaCoCo)
├── docker/              # Dockerfiles front/back + conf nginx interne
├── .github/workflows/   # ci.yml (5 étapes) + cd.yml (build + déploiement)
├── docker-compose.yml   # db + backend + frontend (+ sonarqube en profile "sonar")
├── docs/                # architecture, déploiement, PSD2, SonarQube, ADR
└── sonar-project.properties
```

## Démarrage rapide (dev)

```bash
# Backend (Java 21 + Maven requis)
cd backend && mvn spring-boot:run        # http://localhost:8080 (actuator/health public)

# Frontend (Node 20+ requis)
cd frontend && npm install && npm run dev # http://localhost:5173 (proxy /api -> :8080)
```

## Qualité & CI (ordre demandé)

Le workflow `.github/workflows/ci.yml` enchaîne sur chaque push/PR :

1. **Init** : checkout + setup JDK 21 / Node 20 + caches Maven/npm
2. **Tests** : `mvn verify` (JUnit 5 + JaCoCo, **seuil de couverture 60 % imposé**) puis
   `npm run test:coverage` (Vitest, **seuils 60 % imposés** dans `vite.config.ts`)
3. **ESLint** : `npm run lint` (flat config ESLint 9 + eslint-plugin-vue + typescript-eslint)
4. **SonarQube** : scan vers l'instance **auto-hébergée** (voir `docs/SONARQUBE.md`)
5. **TypeScript check** : `vue-tsc --noEmit`

Secrets GitHub requis : `SONAR_HOST_URL`, `SONAR_TOKEN` (SonarQube), et pour le CD :
`SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, `DEPLOY_PATH`.

## Production (LXC Proxmox)

Voir [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) : création de la LXC, Docker Compose,
nginx en reverse proxy, certificat Let's Encrypt via **DuckDNS (DNS-01)**, sauvegardes.

Le CD pousse les images sur `ghcr.io` puis déploie par SSH (`docker compose pull && up -d`).

## Données bancaires : légal & sécurisé

Voir [docs/PSD2.md](docs/PSD2.md). Ta banque est le **Crédit Mutuel de Bretagne** (groupe
Arkéa) : récupération via l'**API PSD2 officielle hébergée sur le portail développeur Arkéa
(getarkea.com, standard STET)**, OAuth2 + authentification forte, jamais de scraping.
Un connecteur d'import **CSV/OFX** (100 % légal, gratuit) est prévu en secours.
Sandbox Arkéa gratuite pour le dev/CI ; la production exige le statut AISP (ACPR) +
certificats eIDAS QWAC/QSealC, potentiellement payants — signalé à l'avance. Les
identifiants/tokens bancaires ne sont jamais committés : variables d'environnement /
secrets uniquement, tokens chiffrés au repos.

## Coûts

Tout est gratuit : GitHub (dépôt + Actions), ghcr.io, Docker, PostgreSQL, nginx,
DuckDNS, Let's Encrypt, SonarQube Community auto-hébergé, ECharts (Apache-2.0).
Points payants possibles (signalés à l'avance) : certificat eIDAS/QWAC exigé par certaines
banques en production PSD2, et éventuellement l'API d'un agrégateur tiers si on en ajoutait
un (non prévu). Les conseils financiers sont générés par un moteur de règles local (gratuit).

## Sécurité

Secret scanning GitHub + Dependabot actifs, `.env` jamais committé, HTTPS obligatoire,
authentification HTTP Basic (BCrypt) en attendant JWT, IBAN stocké masqué uniquement,
principe RGPD : données sur ton serveur uniquement, effacement possible.
