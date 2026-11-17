# ADR 0003 — SonarQube auto-hébergé

- **Décision** : SonarQube Community Edition dans la LXC (profile `sonar`), pas SonarCloud.
- **Pourquoi** : SonarCloud est payant pour les dépôts privés (~10 €/mois) ; l'auto-hébergé
  est gratuit et les données restent chez toi. Le dépôt étant public, SonarCloud serait
  gratuit aussi, mais l'auto-hébergé garde l'analyse 100 % chez toi.
- **Conséquences** : secrets `SONAR_HOST_URL` / `SONAR_TOKEN` dans GitHub ; job `sonarqube`
  de la CI (étape 4).
