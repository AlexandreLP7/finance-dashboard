# ADR 0001 — Monorepo front + back

- **Décision** : un seul dépôt `finance-dashboard` avec `frontend/` (Vue 3 + TS) et
  `backend/` (Spring Boot), plutôt que deux dépôts.
- **Pourquoi** : projet personnel à une personne ; CI/CD et contrat API versionnés
  ensemble ; déploiement atomique via un seul `docker-compose.yml`.
- **Conséquences** : la CI lance les deux stacks de tests ; les images Docker sont
  buildées depuis la racine du dépôt (context `.`).
