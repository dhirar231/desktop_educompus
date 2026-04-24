# Guide d'utilisation - Gestion de Contenu Pédagogique

## Vue d'ensemble

Le système de gestion de contenu d'EduCompus permet de créer, organiser et synchroniser sélectivement vos ressources pédagogiques avec Google Drive. Cette fonctionnalité optimise l'utilisation du stockage cloud en ne synchronisant que les contenus essentiels.

## Accès à la fonctionnalité

1. Connectez-vous à EduCompus en tant qu'**Administrateur** ou **Enseignant**
2. Dans le menu de navigation, cliquez sur **📚 Gestion Contenu**
3. L'interface s'ouvre avec 5 onglets principaux

## Types de contenu supportés

### 📖 Cours
- **Titre** : Nom du cours (obligatoire)
- **Description** : Description détaillée
- **Niveau** : Débutant, Intermédiaire, Avancé, Expert
- **Domaine** : Informatique, Mathématiques, Physique, etc.
- **Formateur** : Nom de l'enseignant
- **Durée** : Nombre d'heures du cours
- **Important** : ✅ Coché = Upload vers Google Drive

### 📑 Chapitres
- **Cours** : Sélection du cours parent (obligatoire)
- **Titre** : Nom du chapitre (obligatoire)
- **Ordre** : Position dans le cours (1, 2, 3...)
- **Description** : Contenu du chapitre
- **Important** : ✅ Coché = Upload vers Google Drive

### 📝 Travaux Dirigés (TDs)
- **Cours** : Sélection du cours parent (obligatoire)
- **Chapitre** : Sélection du chapitre (optionnel)
- **Titre** : Nom du TD (obligatoire)
- **Description** : Instructions du TD
- **Important** : ✅ Coché = Upload vers Google Drive

### 🎬 Vidéos Explicatives
- **Cours** : Sélection du cours parent (obligatoire)
- **Chapitre** : Sélection du chapitre (optionnel)
- **Titre** : Nom de la vidéo (obligatoire)
- **URL Vidéo** : Lien vers la vidéo (optionnel si fichier fourni)
- **Description** : Description de la vidéo
- **Important** : ✅ Coché = Upload vers Google Drive

## Règles de synchronisation Google Drive

### ✅ Contenu synchronisé (Important = Coché)
- Fichiers uploadés automatiquement vers Google Drive
- Liens de partage générés automatiquement
- Organisation en dossiers structurés
- Accès public en lecture

### ❌ Contenu non synchronisé (Important = Non coché)
- Stockage uniquement en base de données locale
- Pas d'upload vers Google Drive
- Idéal pour les brouillons et contenus temporaires

## Processus de création de contenu

### Étape 1 : Sélection du type
1. Cliquez sur l'onglet correspondant (Cours, Chapitres, TDs, Vidéos)

### Étape 2 : Remplissage du formulaire
1. Remplissez les champs obligatoires (marqués d'un *)
2. Ajoutez une description détaillée
3. **Important** : Cochez "Important" uniquement pour les contenus essentiels

### Étape 3 : Sélection de fichier (optionnel)
1. Cliquez sur **📁 Sélectionner un fichier**
2. Choisissez votre fichier (PDF, DOC, MP4, etc.)
3. **Note** : Fichier obligatoire pour les contenus importants

### Étape 4 : Création
1. Cliquez sur **✅ Créer le [type]**
2. Attendez la confirmation de création
3. Si important : attendez la confirmation d'upload Drive

## Organisation sur Google Drive

Les fichiers sont automatiquement organisés selon cette structure :

```
📁 EduCompus - Cours/
├── 📁 Nom_du_Cours_1/
│   ├── 📁 Chapitres/
│   │   ├── 📄 Chapitre_1_Introduction.pdf
│   │   └── 📄 Chapitre_2_Concepts.pdf
│   ├── 📁 Travaux Dirigés/
│   │   ├── 📄 TD_Exercices.pdf
│   │   └── 📄 TD_Projet.pdf
│   └── 📁 Vidéos Explicatives/
│       ├── 🎬 Video_Demo.mp4
│       └── 🎬 Video_Tuto.mp4
└── 📁 Nom_du_Cours_2/
    └── ...
```

## Surveillance Google Drive

### Onglet ☁️ Google Drive
- **Statut de connexion** : Vérification de la connectivité
- **Utilisation du stockage** : Barre de progression et statistiques
- **Informations détaillées** : Conseils d'optimisation

### Bouton 🔄 Actualiser
- Met à jour les statistiques en temps réel
- Vérifie la connexion Google Drive
- Affiche les dernières informations d'utilisation

## Bonnes pratiques

### ✅ À faire
- Marquer comme "important" uniquement les contenus finalisés
- Utiliser des titres descriptifs et clairs
- Organiser les chapitres avec des numéros d'ordre logiques
- Vérifier régulièrement l'utilisation du stockage Drive

### ❌ À éviter
- Marquer tous les contenus comme "importants"
- Uploader des fichiers volumineux inutilement
- Créer des contenus sans description
- Ignorer les messages d'erreur de validation

## Gestion des erreurs

### Erreurs de validation courantes
- **"Le titre est obligatoire"** : Remplissez le champ titre
- **"Veuillez sélectionner un cours"** : Choisissez un cours parent
- **"Un fichier est requis pour les contenus importants"** : Ajoutez un fichier ou décochez "Important"

### Erreurs Google Drive
- **"Google Drive non disponible"** : Vérifiez votre connexion internet
- **"Erreur upload Google Drive"** : Vérifiez l'espace disponible
- **"Quota dépassé"** : Libérez de l'espace ou upgradez votre plan

## Conseils d'optimisation

### Gestion de l'espace Drive
1. Surveillez régulièrement l'utilisation dans l'onglet Google Drive
2. Supprimez les anciens fichiers inutiles
3. Compressez les vidéos avant upload
4. Utilisez des formats optimisés (PDF pour documents, MP4 pour vidéos)

### Organisation du contenu
1. Créez d'abord les cours, puis les chapitres, puis les TDs/vidéos
2. Utilisez une nomenclature cohérente pour les titres
3. Numérotez les chapitres dans l'ordre logique
4. Groupez les TDs par thématique

### Workflow recommandé
1. **Brouillon** : Créez le contenu sans cocher "Important"
2. **Révision** : Relisez et corrigez le contenu
3. **Finalisation** : Cochez "Important" et uploadez vers Drive
4. **Partage** : Utilisez les liens Drive générés pour partager

## Support et dépannage

### Logs et diagnostics
- Les erreurs sont affichées dans l'interface
- Consultez les messages de statut après chaque opération
- Utilisez le bouton "Actualiser" pour vérifier la connectivité

### Contact support
En cas de problème persistant :
1. Notez le message d'erreur exact
2. Vérifiez votre configuration Google Drive
3. Contactez l'équipe technique EduCompus

## Limites techniques

- **Taille de fichier** : Maximum 5 TB par fichier
- **Types de fichiers** : Tous formats supportés
- **Quota Google Drive** : Selon votre plan (15 GB gratuit)
- **Vitesse d'upload** : Dépend de votre connexion internet