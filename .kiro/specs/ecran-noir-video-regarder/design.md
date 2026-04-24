# Lecture Vidéo Intégrée - Design Technique

## Overview

Ce document définit l'approche technique pour corriger le problème de lecture vidéo dans l'application JavaFX EduCompus. Le bug se manifeste lorsque les vidéos s'ouvrent dans le navigateur externe via `UrlOpener.open()`, causant des écrans noirs pour les vidéos générées par IA et une expérience utilisateur fragmentée. La solution consiste à implémenter une lecture intégrée avec JavaFX MediaView, permettant la lecture automatique et les contrôles intégrés.

## Glossary

- **Bug_Condition (C)**: La condition qui déclenche le bug - quand l'utilisateur clique sur "▶ Regarder" et que la vidéo s'ouvre dans le navigateur externe
- **Property (P)**: Le comportement désiré - la vidéo doit s'ouvrir dans l'application JavaFX avec MediaView et lecture automatique
- **Preservation**: Les fonctionnalités existantes de navigation, téléchargement PDF et progression qui doivent rester inchangées
- **MediaView**: Le composant JavaFX pour la lecture vidéo intégrée dans l'interface utilisateur
- **VideoExplicative**: L'entité représentant une vidéo explicative avec URL, titre et description
- **UrlOpener**: L'utilitaire actuel qui ouvre les URLs dans le navigateur externe (à remplacer pour les vidéos)

## Bug Details

### Bug Condition

Le bug se manifeste lorsqu'un utilisateur clique sur le bouton "▶ Regarder" d'une vidéo explicative. Le système utilise actuellement `UrlOpener.open()` qui lance le navigateur externe, causant une rupture de l'expérience utilisateur et des problèmes d'affichage pour les vidéos générées par IA.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type UserClickEvent
  OUTPUT: boolean
  
  RETURN input.buttonClicked == "▶ Regarder"
         AND input.resourceType == "VideoExplicative"
         AND currentVideoPlaybackMethod == "EXTERNAL_BROWSER"
         AND NOT videoOpenedInIntegratedPlayer
END FUNCTION
```

### Examples

- **Exemple 1**: L'utilisateur clique sur "▶ Regarder" pour une vidéo HeyGen → La vidéo s'ouvre dans Chrome → Écran noir ou problème de lecture
- **Exemple 2**: L'utilisateur clique sur "▶ Regarder" pour une vidéo Synthesia → La vidéo s'ouvre dans Firefox → L'utilisateur doit quitter l'application
- **Exemple 3**: L'utilisateur regarde une vidéo dans le navigateur → Il doit naviguer manuellement pour revenir à l'application
- **Cas limite**: Vidéo avec URL invalide → Doit afficher une erreur dans l'application plutôt que d'ouvrir le navigateur

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- La navigation dans la liste des cours doit continuer à fonctionner exactement comme avant
- L'affichage des détails de chapitre (titre, description, TD) doit rester inchangé
- Le téléchargement des fichiers PDF de chapitre et TD doit continuer à fonctionner
- Le système de progression (marquer comme terminé) doit rester fonctionnel

**Scope:**
Toutes les interactions qui n'impliquent PAS le clic sur "▶ Regarder" pour les vidéos doivent être complètement inaffectées par ce correctif. Cela inclut :
- Les clics sur les boutons de téléchargement PDF
- Les interactions avec les contrôles de traduction
- La navigation entre les chapitres et cours
- Les interactions avec les boutons "Marquer comme lu"

## Hypothesized Root Cause

Basé sur l'analyse du code, les causes les plus probables sont :

1. **Utilisation Systématique d'UrlOpener**: La méthode `openUrl()` dans `FrontCourseDetailController` utilise toujours `UrlOpener.open()` sans distinction entre vidéos et autres URLs
   - Ligne 613: `com.educompus.util.UrlOpener.open(url);`
   - Aucune logique pour détecter les fichiers vidéo (.mp4, .avi, etc.)

2. **Absence de MediaView**: L'interface FXML ne contient aucun composant MediaView pour la lecture vidéo intégrée
   - Le fichier `FrontCourseDetail.fxml` n'a pas de MediaView configuré
   - Aucune infrastructure pour la lecture vidéo dans l'application

3. **Architecture de Navigation**: La méthode `buildVideoRow()` crée directement un bouton qui appelle `openUrl()`
   - Pas de distinction entre types de contenu (vidéo vs web)
   - Pas de modal ou fenêtre dédiée pour la lecture vidéo

4. **Gestion des Formats Vidéo**: Aucune validation ou détection du type de fichier vidéo
   - Les URLs peuvent pointer vers différents formats (.mp4, .webm, etc.)
   - Pas de gestion des erreurs spécifiques aux médias

## Correctness Properties

Property 1: Bug Condition - Lecture Vidéo Intégrée

_For any_ interaction où l'utilisateur clique sur "▶ Regarder" pour une VideoExplicative avec une URL valide, le système fixé SHALL ouvrir la vidéo dans un lecteur MediaView intégré à l'application, démarrer la lecture automatiquement, et fournir des contrôles de lecture (play/pause/volume).

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Fonctionnalités Non-Vidéo

_For any_ interaction qui n'implique PAS le clic sur "▶ Regarder" pour les vidéos (téléchargements PDF, navigation, progression, traduction), le code fixé SHALL produire exactement le même comportement que le code original, préservant toutes les fonctionnalités existantes de l'application.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

En supposant que notre analyse de cause racine est correcte :

**File**: `src/main/resources/View/front/FrontCourseDetail.fxml`

**Ajouts FXML**:
1. **Modal de Lecture Vidéo**: Ajouter un StackPane modal avec MediaView
   - StackPane overlay pour assombrir l'arrière-plan
   - VBox container avec MediaView et contrôles
   - Boutons de contrôle (play/pause, volume, fermer)

2. **Styling CSS**: Ajouter les classes CSS pour le lecteur vidéo
   - Style pour le modal overlay
   - Style pour les contrôles de lecture
   - Animations de transition

**File**: `src/main/java/com/educompus/controller/front/FrontCourseDetailController.java`

**Specific Changes**:
1. **Ajout des Composants FXML**: Déclarer les nouveaux éléments MediaView et contrôles
   - `@FXML private StackPane videoModal;`
   - `@FXML private MediaView mediaView;`
   - `@FXML private Button playPauseBtn, closeVideoBtn;`

2. **Modification de buildVideoRow()**: Changer l'action du bouton "▶ Regarder"
   - Remplacer `openUrl(url)` par `openVideoInApp(url)`
   - Détecter les URLs vidéo vs autres URLs

3. **Nouvelle Méthode openVideoInApp()**: Implémenter la lecture intégrée
   - Créer un objet Media avec l'URL
   - Configurer MediaView avec lecture automatique
   - Afficher le modal de lecture

4. **Gestion des Contrôles**: Implémenter les handlers pour play/pause/volume
   - Méthodes FXML pour les contrôles de lecture
   - Gestion des erreurs de chargement média

5. **Détection du Type de Fichier**: Ajouter une logique pour identifier les vidéos
   - Vérifier l'extension (.mp4, .avi, .webm, etc.)
   - Fallback vers UrlOpener pour les non-vidéos

## Testing Strategy

### Validation Approach

La stratégie de test suit une approche en deux phases : d'abord, exposer des contre-exemples qui démontrent le bug sur le code non corrigé, puis vérifier que le correctif fonctionne correctement et préserve le comportement existant.

### Exploratory Bug Condition Checking

**Goal**: Exposer des contre-exemples qui démontrent le bug AVANT d'implémenter le correctif. Confirmer ou réfuter l'analyse de cause racine. Si nous réfutons, nous devrons re-hypothétiser.

**Test Plan**: Écrire des tests qui simulent des clics sur "▶ Regarder" pour différents types de vidéos et vérifier que le comportement actuel (ouverture navigateur) se produit. Exécuter ces tests sur le code NON CORRIGÉ pour observer les échecs et comprendre la cause racine.

**Test Cases**:
1. **Test Vidéo MP4**: Simuler un clic sur "▶ Regarder" pour une vidéo .mp4 (échouera sur le code non corrigé - ouvrira le navigateur)
2. **Test Vidéo HeyGen**: Simuler un clic pour une vidéo générée par HeyGen (échouera - écran noir dans le navigateur)
3. **Test Vidéo Synthesia**: Simuler un clic pour une vidéo Synthesia (échouera - ouverture navigateur externe)
4. **Test URL Invalide**: Simuler un clic avec URL vidéo invalide (peut échouer - erreur navigateur au lieu d'erreur app)

**Expected Counterexamples**:
- Les vidéos s'ouvrent dans le navigateur externe au lieu du lecteur intégré
- Causes possibles : utilisation systématique d'UrlOpener, absence de MediaView, pas de détection de type fichier

### Fix Checking

**Goal**: Vérifier que pour toutes les entrées où la condition de bug est vraie, la fonction corrigée produit le comportement attendu.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := handleVideoClick_fixed(input)
  ASSERT expectedBehavior(result)
END FOR
```

### Preservation Checking

**Goal**: Vérifier que pour toutes les entrées où la condition de bug n'est PAS vraie, la fonction corrigée produit le même résultat que la fonction originale.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT handleClick_original(input) = handleClick_fixed(input)
END FOR
```

**Testing Approach**: Les tests basés sur les propriétés sont recommandés pour la vérification de préservation car :
- Ils génèrent automatiquement de nombreux cas de test à travers le domaine d'entrée
- Ils attrapent les cas limites que les tests unitaires manuels pourraient manquer
- Ils fournissent de fortes garanties que le comportement est inchangé pour toutes les entrées non-buggy

**Test Plan**: Observer le comportement sur le code NON CORRIGÉ d'abord pour les téléchargements PDF et autres interactions, puis écrire des tests basés sur les propriétés capturant ce comportement.

**Test Cases**:
1. **Préservation Téléchargement PDF**: Vérifier que les clics sur "⬇ Télécharger" continuent à fonctionner
2. **Préservation Navigation**: Vérifier que la navigation entre chapitres fonctionne correctement
3. **Préservation Progression**: Vérifier que "Marquer comme lu" continue à fonctionner
4. **Préservation Traduction**: Vérifier que les fonctionnalités de traduction restent intactes

### Unit Tests

- Tester la détection du type de fichier vidéo pour différentes extensions
- Tester la création et configuration des objets Media et MediaView
- Tester la gestion des erreurs de chargement vidéo (URL invalide, format non supporté)
- Tester que les clics non-vidéo continuent d'utiliser UrlOpener

### Property-Based Tests

- Générer des URLs aléatoires et vérifier que les vidéos s'ouvrent dans MediaView
- Générer des configurations de chapitre aléatoires et vérifier la préservation des téléchargements PDF
- Tester que toutes les interactions non-vidéo continuent à fonctionner à travers de nombreux scénarios

### Integration Tests

- Tester le flux complet de lecture vidéo dans chaque contexte de chapitre
- Tester la commutation entre lecture vidéo et navigation normale
- Tester que les contrôles vidéo (play/pause/volume) fonctionnent correctement
- Tester la fermeture du lecteur vidéo et le retour à la liste des chapitres