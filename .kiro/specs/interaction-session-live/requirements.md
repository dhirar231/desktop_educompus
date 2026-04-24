# Requirements Document

## Introduction

Le module "Interaction en session live" permet aux étudiants d'interagir pendant les sessions live en utilisant les fonctionnalités natives des plateformes de visioconférence externes (Google Meet, Zoom). L'application JavaFX sert d'interface de gestion et de redirection vers ces plateformes, sans implémenter de système de visioconférence interne.

## Glossary

- **Application_JavaFX**: L'application principale de gestion des cours et sessions
- **Session_Live**: Une session de cours en temps réel utilisant une plateforme de visioconférence externe
- **Plateforme_Externe**: Service de visioconférence tiers (Google Meet, Zoom, etc.)
- **Enseignant**: Utilisateur créateur et animateur de sessions live
- **Etudiant**: Utilisateur participant aux sessions live
- **Lien_Session**: URL de connexion à la session live générée par la plateforme externe
- **Interface_Session**: Écran de l'application affichant les informations de session
- **Navigateur_Externe**: Application web browser utilisée pour accéder aux sessions live

## Requirements

### Requirement 1: Gestion des Sessions Live par l'Enseignant

**User Story:** En tant qu'enseignant, je veux créer et gérer des sessions live via des plateformes externes, afin de pouvoir organiser des cours interactifs.

#### Acceptance Criteria

1. THE Application_JavaFX SHALL permettre à l'enseignant de créer une nouvelle session live
2. WHEN l'enseignant crée une session, THE Application_JavaFX SHALL permettre de saisir le lien de la plateforme externe
3. THE Application_JavaFX SHALL permettre à l'enseignant de modifier les informations d'une session existante
4. THE Application_JavaFX SHALL permettre à l'enseignant de supprimer une session live
5. THE Application_JavaFX SHALL valider que le lien de session est une URL valide avant sauvegarde

### Requirement 2: Affichage des Sessions pour les Étudiants

**User Story:** En tant qu'étudiant, je veux voir les sessions live disponibles, afin de pouvoir y participer.

#### Acceptance Criteria

1. THE Application_JavaFX SHALL afficher la liste des sessions live disponibles aux étudiants
2. WHEN une session live est disponible, THE Interface_Session SHALL afficher une indication "Session interactive disponible"
3. THE Interface_Session SHALL afficher une icône "Live" pour les sessions actives
4. THE Interface_Session SHALL afficher le message "Les interactions (lever la main, chat) sont disponibles après connexion à la session"
5. THE Application_JavaFX SHALL afficher les informations de base de la session (titre, horaire, enseignant)

### Requirement 3: Redirection vers les Sessions Externes

**User Story:** En tant qu'étudiant, je veux rejoindre facilement une session live, afin de participer aux interactions en temps réel.

#### Acceptance Criteria

1. THE Application_JavaFX SHALL fournir un bouton "Rejoindre session" pour chaque session live
2. WHEN l'étudiant clique sur "Rejoindre session", THE Application_JavaFX SHALL ouvrir le lien dans le navigateur par défaut
3. THE Application_JavaFX SHALL ouvrir la session dans le Navigateur_Externe sans quitter l'application
4. IF le lien de session est invalide, THEN THE Application_JavaFX SHALL afficher un message d'erreur explicite
5. THE Application_JavaFX SHALL maintenir l'état de l'application pendant que l'étudiant utilise la session externe

### Requirement 4: Interactions via Plateformes Externes

**User Story:** En tant qu'étudiant, je veux utiliser les fonctionnalités interactives natives des plateformes, afin de participer activement aux sessions.

#### Acceptance Criteria

1. WHEN l'étudiant rejoint une session via la Plateforme_Externe, THE Plateforme_Externe SHALL permettre de lever la main
2. WHEN l'étudiant est dans la session, THE Plateforme_Externe SHALL permettre d'envoyer des messages dans le chat
3. WHEN l'étudiant le souhaite, THE Plateforme_Externe SHALL permettre d'activer le microphone
4. WHEN l'étudiant le souhaite, THE Plateforme_Externe SHALL permettre d'activer la caméra
5. THE Plateforme_Externe SHALL permettre l'interaction directe avec l'enseignant et les autres étudiants

### Requirement 5: Contraintes Techniques et Sécurité

**User Story:** En tant que développeur, je veux respecter les contraintes techniques, afin de maintenir une application simple et sécurisée.

#### Acceptance Criteria

1. THE Application_JavaFX SHALL NOT implémenter de système de visioconférence interne
2. THE Application_JavaFX SHALL NOT utiliser WebRTC ou technologies de streaming vidéo
3. THE Application_JavaFX SHALL déléguer toutes les interactions temps réel aux plateformes externes
4. THE Application_JavaFX SHALL valider et assainir tous les liens de session avant stockage
5. WHEN un lien de session est saisi, THE Application_JavaFX SHALL vérifier qu'il provient d'une plateforme autorisée

### Requirement 6: Persistance et Synchronisation

**User Story:** En tant qu'utilisateur, je veux que les informations de session soient sauvegardées, afin de pouvoir y accéder ultérieurement.

#### Acceptance Criteria

1. THE Application_JavaFX SHALL sauvegarder les informations de session dans la base de données
2. THE Application_JavaFX SHALL associer chaque session live à un cours existant
3. WHEN une session est modifiée, THE Application_JavaFX SHALL mettre à jour les informations en temps réel
4. THE Application_JavaFX SHALL conserver l'historique des sessions passées
5. WHEN l'application est redémarrée, THE Application_JavaFX SHALL restaurer toutes les sessions sauvegardées

### Requirement 7: Interface Utilisateur et Expérience

**User Story:** En tant qu'utilisateur, je veux une interface claire et intuitive, afin de gérer facilement les sessions live.

#### Acceptance Criteria

1. THE Interface_Session SHALL utiliser des icônes reconnaissables pour identifier les sessions live
2. THE Application_JavaFX SHALL afficher le statut de chaque session (programmée, en cours, terminée)
3. WHEN une session est en cours, THE Interface_Session SHALL afficher un indicateur visuel distinctif
4. THE Application_JavaFX SHALL fournir des messages d'aide contextuelle pour guider l'utilisateur
5. THE Interface_Session SHALL être responsive et s'adapter à différentes tailles d'écran

### Requirement 8: Gestion des Erreurs et Robustesse

**User Story:** En tant qu'utilisateur, je veux être informé des problèmes de connexion, afin de pouvoir les résoudre.

#### Acceptance Criteria

1. IF la connexion à la Plateforme_Externe échoue, THEN THE Application_JavaFX SHALL afficher un message d'erreur descriptif
2. IF le lien de session est expiré, THEN THE Application_JavaFX SHALL informer l'utilisateur et proposer des alternatives
3. WHEN une erreur survient lors de l'ouverture du navigateur, THE Application_JavaFX SHALL proposer de copier le lien manuellement
4. THE Application_JavaFX SHALL logger toutes les tentatives de connexion pour le débogage
5. IF une session est supprimée pendant qu'un étudiant tente de s'y connecter, THEN THE Application_JavaFX SHALL afficher un message approprié