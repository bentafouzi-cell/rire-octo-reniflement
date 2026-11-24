# Gestion commerciale Android

Application Android hors ligne en arabe pour les petits commerces. Les données sont enregistrées localement sur l'appareil (Room/SQLite).

## État
MVP fonctionnel ajouté dans `app/` : tableau de bord, produits (ajout/modification), vente rapide avec décrémentation atomique du stock, saisie des dépenses et rapports simples.

## Ouvrir dans Android Studio
1. Ouvrir le dépôt comme projet Android/Gradle.
2. Utiliser JDK 17 et Android SDK 35.
3. Si Android Studio demande les scripts Gradle Wrapper, lancer `gradle wrapper --gradle-version 8.9` depuis la racine (ou `./setup-wrapper.sh` si Gradle est installé), puis synchroniser le projet.
4. Lancer la configuration `app` sur un appareil ou un émulateur Android 8.0 (API 26) ou ultérieur.

## Fonctions
- Interface arabe RTL
- Produits, catégories, prix et quantités (création et modification)
- Vente rapide par produit, vérification du stock et mise à jour dans une transaction locale
- Dépenses
- Tableau de bord du jour et résumé des ventes/dépenses
- Fonctionnement hors ligne

## Stack
Kotlin, Jetpack Compose, Room, Gradle Kotlin DSL, JDK 17.

Le dépôt conserve les fichiers Harness/AI existants (`app.py`, `dockerfile`, `script.sh`).
