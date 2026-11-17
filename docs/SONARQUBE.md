# SonarQube auto-hébergé (gratuit) pour la CI

SonarCloud n'est gratuit que pour les dépôts **publics** (payant ~10 €/mois en privé).
On héberge donc **SonarQube Community** dans la LXC (profile `sonar` du docker-compose).

## Mise en place

```bash
docker compose --profile sonar up -d        # http://<IP_LXC>:9000
# login initial admin/admin -> changer le mot de passe
# Administration > Security > Users : créer un token "ci-token"
# Projects > Create project (manually) : key = finance-dashboard
```

## Secrets GitHub (dépôt) à renseigner

- `SONAR_HOST_URL` = `http://<IP_LXC>:9000` (ou via le domaine DuckDNS en HTTPS si exposé)
- `SONAR_TOKEN` = token ci-token

Le job `sonarqube` de `.github/workflows/ci.yml` scanne alors le code + les rapports de
couverture (JaCoCo XML backend, lcov frontend) uploadés par les jobs de test.
