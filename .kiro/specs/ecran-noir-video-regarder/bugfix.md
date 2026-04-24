# Bugfix Requirements Document

## Introduction

Ce document définit les exigences pour corriger le problème de lecture vidéo dans l'application JavaFX EduCompus. Actuellement, les vidéos s'ouvrent dans le navigateur externe via UrlOpener.open(), ce qui cause des écrans noirs/vides pour les vidéos en mode simulation. L'utilisateur souhaite maintenant une lecture intégrée dans l'application JavaFX avec MediaView pour une meilleure expérience utilisateur.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN l'utilisateur clique sur "▶ Regarder" pour une vidéo THEN le système ouvre la vidéo dans le navigateur externe via UrlOpener.open()

1.2 WHEN une vidéo s'ouvre dans le navigateur externe THEN l'utilisateur doit quitter l'application pour visualiser le contenu

1.3 WHEN une vidéo en mode simulation s'ouvre dans le navigateur externe THEN l'écran reste noir/vide car le contenu n'est pas compatible

1.4 WHEN l'utilisateur veut revenir à l'application après avoir regardé une vidéo THEN il doit naviguer manuellement entre les fenêtres

### Expected Behavior (Correct)

2.1 WHEN l'utilisateur clique sur "▶ Regarder" pour une vidéo THEN le système SHALL ouvrir la vidéo dans l'application JavaFX avec MediaView

2.2 WHEN une vidéo s'ouvre dans l'application THEN la lecture SHALL démarrer automatiquement

2.3 WHEN une vidéo est affichée dans MediaView THEN l'utilisateur SHALL pouvoir contrôler la lecture (play/pause/volume) directement dans l'application

2.4 WHEN l'utilisateur termine de regarder une vidéo THEN il SHALL pouvoir fermer le lecteur et revenir à la liste des chapitres sans quitter l'application

### Unchanged Behavior (Regression Prevention)

3.1 WHEN l'utilisateur navigue dans la liste des cours THEN le système SHALL CONTINUE TO afficher correctement tous les cours disponibles

3.2 WHEN l'utilisateur consulte les détails d'un chapitre THEN le système SHALL CONTINUE TO afficher les informations du chapitre (titre, description, TD, etc.)

3.3 WHEN l'utilisateur télécharge un fichier PDF de chapitre ou TD THEN le système SHALL CONTINUE TO permettre le téléchargement correct

3.4 WHEN l'utilisateur marque un chapitre comme terminé THEN le système SHALL CONTINUE TO sauvegarder et afficher correctement la progression