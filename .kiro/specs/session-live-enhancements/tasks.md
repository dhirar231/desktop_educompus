# Plan d'Implémentation : Session Live Enhancements - Système de Notifications Automatiques

## Vue d'ensemble

Implémentation du système de notifications automatiques pour les sessions live EduCompus. Ce système complète les notifications Google Calendar existantes en fournissant des alertes locales JavaFX avec planification automatique, prévention des doublons, et interface non-bloquante. L'approche s'intègre avec l'infrastructure Live Session existante sans modifications majeures.

## Tâches

- [ ] 1. Infrastructure de base - Modèles et énumérations
  - [x] 1.1 Créer l'énumération `NotificationType` dans `src/main/java/com/educompus/model/`
    - Implémenter les types : THIRTY_MINUTES, FIVE_MINUTES
    - Ajouter les codes et descriptions correspondants
    - _Requirements: 3.1, 5.2_

  - [x] 1.2 Créer le modèle `NotificationState` dans `src/main/java/com/educompus/model/`
    - Implémenter tous les champs (id, sessionId, type, scheduledTime, sentTime, sent, createdAt)
    - Ajouter les constructeurs et getters/setters
    - _Requirements: 3.7, 5.2_

  - [ ]* 1.3 Écrire les tests unitaires pour `NotificationState` et `NotificationType`
    - Tester la création d'instances et les getters/setters
    - Valider les codes et descriptions des types
    - _Requirements: 3.1_

- [ ] 2. Schéma de base de données et repository
  - [x] 2.1 Créer le script SQL `notification_migration.sql` dans `src/main/resources/sql/`
    - Définir la table notification_state avec toutes les colonnes
    - Ajouter les contraintes, index et clés étrangères
    - Inclure la migration des sessions existantes
    - _Requirements: 3.7, 5.2_

  - [x] 2.2 Implémenter `NotificationRepository` dans `src/main/java/com/educompus/repository/`
    - Créer les méthodes de vérification d'état (isNotificationSent, isNotificationScheduled)
    - Implémenter les méthodes de gestion d'état (markNotificationSent, createNotificationStatesForSession)
    - Ajouter les méthodes de nettoyage et récupération
    - _Requirements: 3.6, 3.7, 5.2_

  - [ ]* 2.3 Écrire un test basé sur les propriétés pour la prévention des doublons
    - **Property 5: Gestion sélective des notifications**
    - **Valide: Requirements 3.6**

  - [ ]* 2.4 Écrire les tests unitaires pour `NotificationRepository`
    - Tester toutes les opérations de gestion d'état
    - Valider les requêtes de vérification et nettoyage
    - _Requirements: 3.7_

- [ ] 3. Services de notification et planification
  - [x] 3.1 Implémenter `NotificationService` dans `src/main/java/com/educompus/service/`
    - Créer les méthodes sendInfoNotification et sendUrgentNotification
    - Implémenter buildNotificationMessage avec contenu complet
    - Ajouter joinSessionDirectly avec gestion d'erreurs
    - _Requirements: 3.4, 3.5, 5.3, 5.6_

  - [ ]* 3.2 Écrire un test basé sur les propriétés pour le contenu des notifications
    - **Property 8: Contenu complet des notifications**
    - **Valide: Requirements 5.3**

  - [x] 3.3 Implémenter `NotificationSchedulerService` dans `src/main/java/com/educompus/service/`
    - Créer les méthodes start, stop et checkPendingNotifications
    - Implémenter processSessionNotifications avec logique de timing
    - Ajouter shouldSendNotification avec fenêtre de 2 minutes
    - _Requirements: 3.1, 5.2, 5.7_

  - [ ]* 3.4 Écrire un test basé sur les propriétés pour la planification automatique
    - **Property 1: Notification automatique universelle**
    - **Valide: Requirements 3.1, 5.2**

  - [ ]* 3.5 Écrire les tests unitaires pour `NotificationService`
    - Tester la génération de contenu et l'affichage des popups
    - Valider la différenciation entre notifications info et urgentes
    - _Requirements: 3.4, 3.5_

- [ ] 4. Checkpoint - Infrastructure de notification complète
  - Vérifier que tous les tests passent, demander à l'utilisateur si des questions se posent.

- [ ] 5. Extension des services existants
  - [x] 5.1 Étendre `SessionLiveRepository` avec méthodes de recherche temporelle
    - Ajouter findUpcomingSessions pour plage de temps donnée
    - Optimiser les requêtes avec filtres de statut
    - _Requirements: 3.1, 5.2_

  - [x] 5.2 Modifier `SessionLiveService` pour intégrer les notifications
    - Étendre creer() pour planifier automatiquement les notifications
    - Modifier modifier() pour replanifier si dates changées
    - Ajouter supprimer() pour nettoyer les états de notification
    - _Requirements: 3.2, 5.2_

  - [ ]* 5.3 Écrire un test basé sur les propriétés pour les événements de session
    - **Property 2: Notification d'événements de session**
    - **Valide: Requirements 3.2**

  - [ ]* 5.4 Écrire les tests unitaires pour les extensions de service
    - Tester l'intégration avec la planification de notifications
    - Valider la mise à jour automatique des états
    - _Requirements: 3.2_

- [ ] 6. Gestion des erreurs et récupération
  - [ ] 6.1 Créer les classes d'exceptions personnalisées dans `src/main/java/com/educompus/exception/`
    - Implémenter NotificationSchedulingException, NotificationDisplayException, DuplicateNotificationException
    - Ajouter les constructeurs appropriés avec cause
    - _Requirements: 3.8_

  - [ ] 6.2 Implémenter `NotificationRecoveryService` dans `src/main/java/com/educompus/service/`
    - Créer recoverMissedNotifications pour récupération après redémarrage
    - Ajouter la logique de notification de rattrapage
    - Implémenter findMissedNotifications avec fenêtre temporelle
    - _Requirements: 3.8_

  - [ ]* 6.3 Écrire un test basé sur les propriétés pour le mécanisme de fallback
    - **Property 7: Mécanisme de fallback email**
    - **Valide: Requirements 3.8**

  - [ ]* 6.4 Écrire les tests unitaires pour la gestion d'erreurs
    - Tester les mécanismes de récupération et fallback
    - Valider la gestion des exceptions personnalisées
    - _Requirements: 3.8_

- [ ] 7. Intégration avec l'application principale
  - [x] 7.1 Modifier `EduCompusApplication` pour initialiser le planificateur
    - Ajouter le démarrage automatique de NotificationSchedulerService dans start()
    - Implémenter l'arrêt propre dans stop()
    - Configurer les dépendances entre services
    - _Requirements: 3.1, 5.2_

  - [x] 7.2 Créer `NotificationPopupController` dans `src/main/java/com/educompus/controller/`
    - Implémenter la gestion des interactions utilisateur avec les popups
    - Ajouter les actions pour "Rejoindre maintenant" et "Plus tard"
    - Gérer l'auto-fermeture des notifications info
    - _Requirements: 3.4, 5.5, 5.6_

  - [ ]* 7.3 Écrire un test basé sur les propriétés pour la fonctionnalité snooze
    - **Property 9: Fonctionnalité snooze**
    - **Valide: Requirements 5.5**

  - [ ]* 7.4 Écrire les tests unitaires pour l'intégration application
    - Tester l'initialisation et l'arrêt du planificateur
    - Valider les interactions avec les popups
    - _Requirements: 3.1_

- [ ] 8. Checkpoint - Intégration système complète
  - Vérifier que le planificateur démarre correctement avec l'application, demander à l'utilisateur si des questions se posent.

- [ ] 9. Styles et interface utilisateur
  - [x] 9.1 Ajouter les styles CSS pour les notifications dans `styles/educompus.css`
    - Créer les styles .notification-info pour les alertes 30 minutes
    - Ajouter les styles .notification-urgent pour les alertes 5 minutes
    - Définir les animations et effets visuels (dropshadow, couleurs)
    - _Requirements: 3.4, 3.5_

  - [ ] 9.2 Implémenter les indicateurs visuels différenciés
    - Configurer les couleurs et bordures selon le type de notification
    - Ajouter les icônes et symboles d'urgence (⚠️)
    - Définir les styles pour les boutons d'action
    - _Requirements: 3.4, 3.5_

  - [ ]* 9.3 Écrire un test basé sur les propriétés pour la différenciation visuelle
    - **Property 4: Différenciation par niveau d'urgence**
    - **Valide: Requirements 3.4, 3.5**

- [ ] 10. Gestion des préférences utilisateur
  - [ ] 10.1 Créer `UserNotificationPreferences` dans `src/main/java/com/educompus/model/`
    - Implémenter les champs pour configuration des notifications
    - Ajouter les options de timing personnalisé (15min, 1h, 24h)
    - Inclure les paramètres de fuseau horaire
    - _Requirements: 3.3, 5.1, 5.7_

  - [ ] 10.2 Étendre `NotificationSchedulerService` pour respecter les préférences
    - Modifier calculateNotificationTime pour adaptation fuseau horaire
    - Ajouter la vérification des préférences utilisateur avant envoi
    - Implémenter la logique de désactivation sélective
    - _Requirements: 3.3, 5.1, 5.7_

  - [ ]* 10.3 Écrire un test basé sur les propriétés pour les préférences utilisateur
    - **Property 3: Respect des préférences utilisateur**
    - **Valide: Requirements 3.3, 5.1**

  - [ ]* 10.4 Écrire un test basé sur les propriétés pour l'adaptation fuseau horaire
    - **Property 11: Adaptation aux fuseaux horaires**
    - **Valide: Requirements 5.7**

- [ ] 11. Historique et monitoring
  - [ ] 11.1 Implémenter la gestion de l'historique des notifications
    - Étendre NotificationRepository avec méthodes d'historique
    - Ajouter findNotificationHistory avec filtres temporels
    - Créer cleanupOldNotificationStates pour maintenance automatique
    - _Requirements: 3.7_

  - [ ] 11.2 Ajouter le logging détaillé pour monitoring
    - Implémenter les logs de démarrage/arrêt du planificateur
    - Ajouter les logs de succès/échec d'envoi de notifications
    - Créer les logs de récupération et nettoyage
    - _Requirements: 3.7_

  - [ ]* 11.3 Écrire un test basé sur les propriétés pour l'historique complet
    - **Property 6: Historique complet des notifications**
    - **Valide: Requirements 3.7**

  - [ ]* 11.4 Écrire un test basé sur les propriétés pour le marquage d'état
    - **Property 10: Gestion d'état des rappels**
    - **Valide: Requirements 5.6**

- [ ] 12. Tests d'intégration et validation
  - [ ] 12.1 Créer les tests d'intégration pour le workflow complet
    - Tester le cycle : création session → planification → envoi notification
    - Valider l'intégration avec JavaFX Timeline et Alert API
    - Vérifier la persistance des états en base de données
    - _Requirements: 3.1, 3.2, 5.2_

  - [ ]* 12.2 Créer les tests de timing précis
    - Tester la précision des notifications dans la fenêtre de 2 minutes
    - Valider la récupération après redémarrage application
    - Vérifier la prévention des doublons sur longue durée
    - _Requirements: 3.1, 5.2_

  - [ ] 12.3 Effectuer les tests de performance du planificateur
    - Mesurer l'impact sur l'interface utilisateur (doit être nul)
    - Tester avec un grand nombre de sessions programmées
    - Valider la consommation mémoire du Timeline
    - _Requirements: 3.1_

- [ ] 13. Checkpoint final - Validation système complète
  - Vérifier que toutes les notifications fonctionnent correctement, que les performances sont optimales, demander à l'utilisateur si des questions se posent.

## Notes

- Les tâches marquées avec `*` sont optionnelles et peuvent être ignorées pour un MVP plus rapide
- Chaque tâche référence les exigences spécifiques pour la traçabilité
- Les checkpoints permettent une validation incrémentale
- Les tests de propriétés valident les 11 propriétés universelles identifiées dans la conception
- Les tests unitaires valident des exemples spécifiques et cas d'erreur
- L'architecture s'intègre avec l'infrastructure Live Session existante sans modifications majeures
- Le système utilise JavaFX Timeline pour la planification non-bloquante
- Les notifications complètent (ne remplacent pas) les notifications Google Calendar existantes

## Priorités d'implémentation

1. **Infrastructure de base** (Tâches 1-4) : Modèles, base de données, services de notification
2. **Planification et intégration** (Tâches 5-8) : Services étendus, gestion d'erreurs, intégration application
3. **Interface et préférences** (Tâches 9-11) : Styles, préférences utilisateur, historique
4. **Tests et validation** (Tâches 12-13) : Tests d'intégration, performance, validation finale

Cette approche garantit une progression logique avec validation à chaque étape majeure, en se concentrant sur le système de notifications automatiques comme demandé par l'utilisateur.