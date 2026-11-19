# Architecture

```
Navigateur (HTTPS, xxx.duckdns.org)
        |
        v
nginx (Proxmox/LXC, TLS Let's Encrypt DNS-01 DuckDNS)
        |
        v
frontend (nginx:alpine, assets Vue 3 buildés)  :8080
        |  /api/*  (proxy interne)
        v
backend (Spring Boot 3, JRE 21)                :8080 interne
        |
        v
PostgreSQL 16 (volume docker)
```

- **frontend/** : SPA Vue 3 + TS. `src/api/client.ts` encapsule les appels `/api/*`.
  Pinia pour l'état, Vue Router pour les pages, ECharts pour les graphiques.
- **backend/** : API REST `/api/accounts`, `/api/accounts/{id}/transactions`.
  Sécurité : JWT (access 15 min + refresh rotatif hashé, BCrypt depuis l'env), 2FA TOTP optionnelle, rate limiting sur /api/auth/** ;
  `/actuator/health` public. JPA + Flyway (schéma versionné).
- **Connecteur bancaire** (voir `docs/PSD2.md`) : interface `BankConnector`,
  implémentations PSD2 (API banque) et import CSV/OFX.
- **Analytics** : détection d'anomalies (z-score / moyennes mobiles, doublons),
  budgets par catégorie, conseils par règles — côté backend, exposés via `/api/analytics/*`.

Flux CI/CD : push/PR -> `ci.yml` (init, tests >= 60 %, ESLint, vue-tsc) ;
tag `v*` -> `cd.yml` (build images -> ghcr.io -> SSH -> `docker compose up -d` sur la LXC).
