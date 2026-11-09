# Coolray Home — écran d'accueil pour Flyme Auto (1280x720)

Lanceur d'accueil au style de la vidéo : ciel violet, SUV stylisé, compteur de vitesse GPS
(vitesse, cap, altitude), heure et date, et un dock d'applications.
Le design est original : aucune image ni modèle 3D du revendeur n'est utilisé.

## 1. Obtenir l'APK (sans rien installer sur ton PC)

1. Crée un compte gratuit sur github.com, puis un nouveau dépôt (ex. `coolray-home`).
2. Dépose tout le contenu de ce dossier dans le dépôt
   (bouton « Add file » > « Upload files ». Le dossier caché `.github` doit y être aussi).
3. Ouvre l'onglet **Actions** > « Build APK ». S'il n'a pas démarré : « Run workflow ».
4. Après 2-3 minutes, clique sur l'exécution terminée, puis télécharge **CoolrayHome-apk**
   (en bas de page, dans « Artifacts »). Décompresse le zip : tu obtiens `app-debug.apk`.

## 2. Installer sur la Coolray

- **Clé USB** : copie `app-debug.apk` sur une clé FAT32, branche-la, ouvre-le avec le gestionnaire de fichiers.
- **ADB** : `adb install app-debug.apk`

Au premier lancement, accepte l'autorisation de localisation (nécessaire pour la vitesse GPS).

## 3. Utilisation

- Le dock en bas affiche tes applis épinglées (Google Maps, Organic Maps, OsmAnd, YouTube… si installées).
- « Toutes les apps » ouvre la liste complète ; **appui long** sur une appli = l'épingler ou la retirer du dock (6 max).
- Si Flyme Auto laisse choisir un lanceur par défaut, « Coolray Home » apparaîtra dans la liste.
  Sinon, elle fonctionne comme une appli normale, lancée depuis le menu.

## Limites

- Les valeurs du véhicule du revendeur (batterie, consommation, températures, caméra 360°) viennent du
  firmware modifié et du bus CAN : une appli normale n'y a pas accès. Ici, les données sont celles du GPS.
- Je n'ai pas pu compiler ni tester l'APK dans mon environnement (outils Android inaccessibles) :
  si le build GitHub échoue, envoie-moi le message d'erreur et je corrige.
