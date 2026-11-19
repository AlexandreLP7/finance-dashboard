# ADR 0003 — SonarQube auto-hébergé : **RETIRÉ**

**Statut : retiré** (2026-11-19). Cette décision est abandonnée : le conteneur
`sonarqube:community` (2–4 Go de RAM) + le scanner CI provoquaient des OOM-kill du
runner auto-hébergé sur la LXC applicative.

**Conséquences du retrait :**
- service `sonarqube` supprimé de `docker-compose.yml` (plus de profile `sonar`) ;
- job `4-SonarQube` supprimé de `.github/workflows/ci.yml` (CI : init → tests ≥ 60 % → ESLint → vue-tsc) ;
- secrets `SONAR_HOST_URL` / `SONAR_TOKEN` à supprimer côté GitHub (Settings → Secrets) ;
- `sonar-project.properties` neutralisé.

Si une analyse statique est réintroduite un jour : préférer SonarCloud (gratuit sur dépôt
public) ou une instance SonarQube sur une machine avec suffisamment de RAM, hors de la LXC applicative.
