# ADR 0002 — Connecteur bancaire PSD2 + import CSV

- **Décision** : accéder aux données bancaires via l'API PSD2 officielle de la banque
  (Crédit Mutuel de Bretagne → portail développeur Arkéa getarkea.com, standard **STET**),
  encapsulée derrière une interface `BankConnector` ; import CSV/OFX manuel en secours.
  **Aucun scraping.**
- **Pourquoi** : seule voie légale et sûre en DSP2 ; le scraping viole les CGU et la
  réglementation. L'interface permet de changer de banque sans toucher au domaine.
- **Conséquences** : configuration par env (`BANK_PSD2_*`), tokens jamais committés,
  chiffrés au repos ; consentement + SCA gérés par redirect OAuth2.
