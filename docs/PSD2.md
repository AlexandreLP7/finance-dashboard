# Récupération des données bancaires (légal & sécurisé)

## Cadre légal

- **DSP2 / PSD2** : ta banque doit fournir un accès API aux prestataires agréés,
  avec authentification forte (SCA) et consentement explicite. C'est la voie légale.
- **RGPD** : tes données financières sont tes données personnelles ; elles restent sur
  ton serveur (self-hosted), jamais chez un tiers.
- **Interdit ici** : le scraping du site bancaire (violation des CGU, non conforme PSD2,
  dangereux pour tes identifiants). Aucun code de scraping ne sera écrit.

## Ta banque : Crédit Mutuel de Bretagne (fédération Crédit Mutuel Arkéa)

Le Crédit Mutuel de Bretagne fait partie du groupe **Crédit Mutuel Arkéa**, qui héberge
les API PSD2 de ses banques fédérales sur son portail développeur : **https://getarkea.com**
(« Arkea dev portal »).

- **Standard** : **STET PSD2** (standard français utilisé par Arkéa), ex. v1.6.x.
  Endpoints AIS (lecture seule, ce dont on a besoin) :
  - `POST /consents` → crée un consentement (redirect SCA du client)
  - `GET /accounts` → liste des comptes consentis
  - `GET /accounts/{accountId}/balances` → soldes
  - `GET /accounts/{accountId}/transactions?bookingStatus=booked` → transactions
- **Auth** : OAuth2 (authorization code + SCA pour le consentement, client credentials
  pour le TPP) → `POST /oauth/token` → `access_token` (90 jours) + `refresh_token`.
- **Production** : mTLS avec certificat eIDAS **QWAC** + signature des requêtes avec
  certificat **QSealC**, et statut de **prestataire agréé AISP enregistré à l'ACPR**.
  ⚠️ Pour un particulier, ce statut + les certificats eIDAS représentent une démarche
  administrative lourde et potentiellement payante (QTSP). C'est le seul point « payant /
  contraignant » du projet, signalé à l'avance.
- **Sandbox** : **gratuit**, certificats de test fournis par Arkéa, données fictives
  (personas) → idéal pour le développement et les tests automatisés (connecteur testé
  contre la sandbox en CI).
- **Secours production gratuit** : import manuel CSV/OFX depuis ton espace client CMB
  (100 % légal, aucune inscription requise) via `CsvImportService`.

## Configuration (100 % par variables d'environnement, jamais en dur)

```bash
BANK_PSD2_BASE_URL=https://<host-sandbox-ou-prod>/stet/psd2/v1.6.2   # ex. sandbox getarkea
BANK_PSD2_CLIENT_ID=...            # délivré à l'inscription sur getarkea.com
BANK_PSD2_CLIENT_SECRET=...        # JAMAIS committé (secret GitHub / .env serveur)
BANK_PSD2_REDIRECT_URI=https://<ton-domaine>.duckdns.org/api/bank/callback
BANK_PSD2_QWAC_PATH=/run/secrets/bank_qwac.pem   # prod uniquement
BANK_PSD2_QSEALC_KEY_PATH=/run/secrets/bank_qsealc.key
```

Les tokens bancaires sont stockés **chiffrés au repos** (AES via Jasypt) en base, jamais
en clair ; les logs ne contiennent jamais de données de compte.

## Flux dans l'app

1. L'utilisateur clique « Connecter mon compte Crédit Mutuel » → `GET /api/bank/connect`
   renvoie l'URL de consentement Arkéa (OAuth2 + SCA).
2. Callback `GET /api/bank/callback?code=...` → échange du code contre les tokens →
   stockage chiffré + `POST /consents` (accès comptes + transactions, 90 jours, renouvelable).
3. Synchronisation périodique : `GET /accounts`, puis balances + transactions par compte,
   upsert en base (dédup par hash compte+date+montant+libellé).

## Design

Interface `BankConnector` (startConsent, handleCallback, listAccounts, listTransactions,
listBalances) avec deux implémentations : `ArkeaPsD2Connector` (STET, getarkea.com) et
`CsvImportService`. Le reste de l'application ne dépend que de l'interface.
