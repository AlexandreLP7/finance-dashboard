# SonarQube — retiré du projet

SonarQube (serveur auto-hébergé + job CI `4-SonarQube`) a été **retiré** du projet :
le conteneur `sonarqube:community` (2–4 Go de RAM) provoquait des OOM-kill du runner
auto-hébergé sur la petite LXC.

La CI couvre désormais : init → tests (JaCoCo/Vitest ≥ 60 %) → ESLint → vue-tsc.
Si un jour tu veux réintroduire une analyse de code statique, préférez une solution
hébergée gratuite (ex. SonarCloud sur dépôt public) ou une instance dédiée avec assez de RAM.
