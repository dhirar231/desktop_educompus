# Configuration Google Drive pour EduCompus

## Vue d'ensemble

Le système de gestion de contenu d'EduCompus permet l'upload sélectif vers Google Drive des contenus pédagogiques marqués comme "importants". Cette fonctionnalité optimise l'utilisation du stockage cloud en ne synchronisant que les ressources essentielles.

## Prérequis

1. **Compte Google** avec accès à Google Drive
2. **Projet Google Cloud Platform** avec l'API Google Drive activée
3. **Credentials OAuth 2.0** configurés pour une application de bureau

## Configuration étape par étape

### 1. Créer un projet Google Cloud Platform

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Créez un nouveau projet ou sélectionnez un projet existant
3. Notez l'ID du projet

### 2. Activer l'API Google Drive

1. Dans la console Google Cloud, allez dans **APIs & Services > Library**
2. Recherchez "Google Drive API"
3. Cliquez sur "Google Drive API" et activez-la

### 3. Créer les credentials OAuth 2.0

1. Allez dans **APIs & Services > Credentials**
2. Cliquez sur **+ CREATE CREDENTIALS > OAuth client ID**
3. Si c'est la première fois, configurez l'écran de consentement OAuth :
   - Type d'application : **Application interne** (pour usage interne) ou **Externe**
   - Nom de l'application : **EduCompus Content Management**
   - Email de support utilisateur : votre email
   - Domaines autorisés : votre domaine (si applicable)
4. Créez les credentials OAuth :
   - Type d'application : **Application de bureau**
   - Nom : **EduCompus Desktop Client**
5. Téléchargez le fichier JSON des credentials

### 4. Configurer l'application

1. Renommez le fichier téléchargé en `credentials.json`
2. Placez-le dans `src/main/resources/credentials.json`
3. Assurez-vous que ce fichier n'est **PAS** commité dans Git (ajoutez-le au .gitignore)

### 5. Structure du fichier credentials.json

```json
{
  "installed": {
    "client_id": "votre-client-id.apps.googleusercontent.com",
    "project_id": "votre-project-id",
    "auth_uri": "https://accounts.google.com/o/oauth2/auth",
    "token_uri": "https://oauth2.googleapis.com/token",
    "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
    "client_secret": "votre-client-secret",
    "redirect_uris": [
      "http://localhost"
    ]
  }
}
```

## Première utilisation

### Autorisation OAuth

1. Au premier lancement de la fonctionnalité Google Drive, une fenêtre de navigateur s'ouvrira
2. Connectez-vous avec votre compte Google
3. Autorisez l'application EduCompus à accéder à votre Google Drive
4. Les tokens d'accès seront automatiquement sauvegardés dans le dossier `tokens/`

### Test de la connexion

1. Lancez l'application EduCompus
2. Allez dans **Gestion de Contenu > Onglet Google Drive**
3. Cliquez sur **Actualiser** pour vérifier la connexion
4. Le statut devrait afficher "✅ Google Drive connecté"

## Organisation des fichiers sur Google Drive

L'application crée automatiquement une structure organisée :

```
📁 EduCompus - Cours/
├── 📁 Nom_du_Cours_1/
│   ├── 📁 Chapitres/
│   │   ├── 📄 Chapitre_1_Introduction.pdf
│   │   └── 📄 Chapitre_2_Concepts.pdf
│   ├── 📁 Travaux Dirigés/
│   │   ├── 📄 TD_Exercices_Pratiques.pdf
│   │   └── 📄 TD_Projet_Final.pdf
│   └── 📁 Vidéos Explicatives/
│       ├── 🎬 Video_Introduction.mp4
│       └── 🎬 Video_Demo.mp4
└── 📁 Nom_du_Cours_2/
    └── ...
```

## Règles de synchronisation

### Contenu synchronisé (important = true)
- ✅ Cours marqués comme importants
- ✅ Chapitres marqués comme importants  
- ✅ TDs marqués comme importants
- ✅ Vidéos marquées comme importantes

### Contenu non synchronisé (important = false)
- ❌ Contenus de brouillon
- ❌ Ressources temporaires
- ❌ Contenus en cours de développement

## Sécurité et bonnes pratiques

### Protection des credentials
- ✅ Ajoutez `credentials.json` au `.gitignore`
- ✅ Ne partagez jamais vos credentials
- ✅ Utilisez des comptes de service pour la production

### Gestion des permissions
- Les fichiers uploadés sont automatiquement rendus publics en lecture
- Seuls les utilisateurs autorisés peuvent modifier les fichiers
- L'application utilise les permissions minimales nécessaires

### Surveillance de l'utilisation
- Surveillez l'utilisation du quota Google Drive dans l'onglet statistiques
- Les fichiers volumineux consomment rapidement l'espace disponible
- Supprimez régulièrement les anciens fichiers inutiles

## Dépannage

### Erreur "Fichier de credentials non trouvé"
- Vérifiez que `credentials.json` est dans `src/main/resources/`
- Vérifiez que le fichier n'est pas corrompu
- Retéléchargez les credentials depuis Google Cloud Console

### Erreur "Google Drive non disponible"
- Vérifiez votre connexion internet
- Vérifiez que l'API Google Drive est activée
- Réautorisez l'application si nécessaire

### Erreur de quota dépassé
- Vérifiez l'utilisation dans l'onglet Google Drive
- Supprimez les anciens fichiers
- Considérez l'upgrade vers un plan Google Workspace

### Problèmes d'autorisation
- Supprimez le dossier `tokens/` et réautorisez
- Vérifiez les paramètres OAuth dans Google Cloud Console
- Assurez-vous que l'application n'est pas bloquée par votre organisation

## Support

Pour toute question ou problème :
1. Consultez les logs de l'application
2. Vérifiez la configuration dans Google Cloud Console
3. Contactez l'équipe de développement EduCompus

## Limites et quotas

- **Quota API** : 1 000 requêtes par 100 secondes par utilisateur
- **Taille de fichier** : Maximum 5 TB par fichier
- **Stockage** : Selon votre plan Google (15 GB gratuit)
- **Types de fichiers** : Tous types supportés (PDF, MP4, DOC, etc.)