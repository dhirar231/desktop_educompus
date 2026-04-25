# Plan d'Implémentation : Module Interaction en Session Live

## Vue d'ensemble

Implémentation du module "Interaction en session live" pour EduCompus (JavaFX 17 / MySQL / JDBC / MVC). Ce module permet aux enseignants de créer et gérer des sessions live via des plateformes externes (Google Meet, Zoom), et aux étudiants de rejoindre ces sessions pour participer aux interactions en temps réel. L'approche suit l'architecture existante avec délégation complète aux plateformes externes.

## Tâches

- [ ] 1. Infrastructure de base - Modèles et énumération
  - [x] 1.1 Créer l'énumération `SessionStatut` dans `src/main/java/com/educompus/model/`
    - Implémenter les statuts : PROGRAMMEE, EN_COURS, TERMINEE, ANNULEE
    - Ajouter les libellés français correspondants
    - _Requirements: 7.2_

  - [x] 1.2 Créer le modèle `SessionLive` dans `src/main/java/com/educompus/model/`
    - Implémenter tous les champs selon la conception (id, titre, description, lienSession, etc.)
    - Ajouter les getters/setters et constructeurs
    - Inclure les champs dénormalisés pour l'affichage (coursTitre, enseignantNom)
    - _Requirements: 6.1, 6.2_

  - [ ]* 1.3 Écrire les tests unitaires pour `SessionLive` et `SessionStatut`
    - Tester la création d'instances et les getters/setters
    - Valider les transitions de statut
    - _Requirements: 6.1_

- [ ] 2. Schéma de base de données et repository
  - [x] 2.1 Créer le script SQL `session_live_schema.sql` dans `src/main/resources/sql/`
    - Définir la table session_live avec toutes les colonnes
    - Ajouter les contraintes et index appropriés
    - Inclure les clés étrangères vers la table cours
    - _Requirements: 6.1, 6.2_

  - [x] 2.2 Implémenter `SessionLiveRepository` dans `src/main/java/com/educompus/repository/`
    - Créer toutes les méthodes CRUD de base
    - Implémenter les requêtes spécialisées (findByCourseId, findByStatus, etc.)
    - Ajouter les méthodes de statistiques
    - _Requirements: 6.1, 6.3, 6.4_

  - [ ]* 2.3 Écrire les tests unitaires pour `SessionLiveRepository`
    - Tester toutes les opérations CRUD
    - Valider les requêtes spécialisées avec des données de test
    - _Requirements: 6.1_

- [ ] 3. Services de validation et logique métier
  - [x] 3.1 Implémenter `SessionLiveValidationService` dans `src/main/java/com/educompus/service/`
    - Créer toutes les méthodes de validation selon la conception
    - Implémenter la validation des URLs de plateformes autorisées
    - Ajouter la validation des dates et associations cours
    - _Requirements: 1.5, 5.4, 5.5_

  - [ ]* 3.2 Écrire un test basé sur les propriétés pour la validation d'URL
    - **Property 1: Validation d'URL universelle**
    - **Valide: Requirements 1.5, 5.4, 5.5**

  - [x] 3.3 Implémenter `SessionLiveService` dans `src/main/java/com/educompus/service/`
    - Créer toutes les opérations CRUD avec validation
    - Implémenter la logique métier (démarrer/terminer session, vérifications)
    - Ajouter la gestion des plateformes autorisées
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 6.2_

  - [ ]* 3.4 Écrire un test basé sur les propriétés pour l'association cours
    - **Property 5: Association cours obligatoire**
    - **Valide: Requirements 6.2**

  - [ ]* 3.5 Écrire les tests unitaires pour `SessionLiveService`
    - Tester toutes les opérations CRUD avec validation
    - Valider la logique métier et les transitions de statut
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 4. Checkpoint - Infrastructure de base complète
  - Vérifier que tous les tests passent, demander à l'utilisateur si des questions se posent.

- [ ] 5. Contrôleur de gestion enseignant (Backend)
  - [x] 5.1 Créer `BackSessionLiveController` dans `src/main/java/com/educompus/controller/back/`
    - Implémenter toutes les actions CRUD selon la conception
    - Ajouter la gestion des filtres et recherche
    - Créer les méthodes de gestion des sessions (démarrer/terminer)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 7.2_

  - [x] 5.2 Créer l'interface FXML `BackSessionLive.fxml` dans `src/main/resources/View/back/`
    - Définir la structure de l'interface de gestion
    - Inclure ListView avec cellules personnalisées
    - Ajouter les champs de recherche et filtres
    - _Requirements: 7.1, 7.2, 7.5_

  - [ ]* 5.3 Écrire un test basé sur les propriétés pour l'affichage du statut
    - **Property 6: Affichage du statut universel**
    - **Valide: Requirements 7.2**

  - [ ]* 5.4 Écrire les tests unitaires pour `BackSessionLiveController`
    - Tester les actions CRUD et la gestion des filtres
    - Valider les interactions avec les services
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 6. Contrôleur d'affichage étudiant (Frontend)
  - [x] 6.1 Créer `FrontSessionLiveController` dans `src/main/java/com/educompus/controller/front/`
    - Implémenter l'affichage des sessions pour les étudiants
    - Créer la méthode de construction des cartes de session
    - Ajouter la gestion de la redirection vers plateformes externes
    - _Requirements: 2.1, 2.5, 3.1, 3.2_

  - [ ]* 6.2 Écrire un test basé sur les propriétés pour l'affichage des informations
    - **Property 2: Affichage complet des informations de session**
    - **Valide: Requirements 2.5**

  - [x] 6.3 Créer l'interface FXML `FrontSessionLive.fxml` dans `src/main/resources/View/front/`
    - Définir la structure d'affichage pour les étudiants
    - Créer les cartes de session avec indicateurs visuels
    - Inclure les boutons "Rejoindre session"
    - _Requirements: 2.2, 2.3, 3.1, 7.1_

  - [ ]* 6.4 Écrire un test basé sur les propriétés pour le bouton rejoindre
    - **Property 3: Bouton de rejoindre universel**
    - **Valide: Requirements 3.1**

  - [ ]* 6.5 Écrire les tests unitaires pour `FrontSessionLiveController`
    - Tester l'affichage des sessions et la construction des cartes
    - Valider la gestion des erreurs de redirection
    - _Requirements: 2.1, 2.5, 3.1, 3.2_

- [ ] 7. Gestion des erreurs et logging
  - [x] 7.1 Créer les classes d'exceptions personnalisées dans `src/main/java/com/educompus/exception/`
    - Implémenter `SessionNotActiveException`, `InvalidSessionLinkException`, etc.
    - Ajouter les constructeurs appropriés
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 7.2 Implémenter `SessionLiveErrorHandler` dans `src/main/java/com/educompus/util/`
    - Créer la gestion centralisée des erreurs de session
    - Ajouter les messages d'erreur appropriés selon le type d'erreur
    - Implémenter la logique de fallback (copie manuelle du lien)
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ]* 7.3 Écrire un test basé sur les propriétés pour la gestion d'erreur
    - **Property 4: Gestion d'erreur pour liens invalides**
    - **Valide: Requirements 3.4**

  - [ ]* 7.4 Écrire un test basé sur les propriétés pour le logging
    - **Property 8: Logging des tentatives de connexion**
    - **Valide: Requirements 8.4**

  - [ ]* 7.5 Écrire un test basé sur les propriétés pour les erreurs de connexion
    - **Property 9: Gestion d'erreur de connexion**
    - **Valide: Requirements 8.1**

- [ ] 8. Intégration avec l'interface utilisateur existante
  - [x] 8.1 Modifier `FrontCourseDetailController` pour intégrer l'affichage des sessions live
  - [x] 8.2 Modifier `FrontCourseDetail.fxml` pour inclure la section sessions live
    - Ajouter la zone d'affichage des sessions live
    - Intégrer les messages d'information pour les étudiants
    - _Requirements: 2.2, 2.3, 2.4_

  - [ ]* 8.3 Écrire un test basé sur les propriétés pour la responsivité
    - **Property 7: Responsivité de l'interface**
    - **Valide: Requirements 7.5**

- [ ] 9. Styles et apparence
  - [x] 9.1 Ajouter les styles CSS pour les sessions live dans `styles/educompus.css`
    - Créer les styles pour les cartes de session
    - Ajouter les indicateurs visuels (Live, statuts)
    - Définir les styles pour les boutons "Rejoindre session"
    - _Requirements: 7.1, 7.3_

  - [ ] 9.2 Créer les icônes et indicateurs visuels
    - Ajouter les icônes pour les différents statuts de session
    - Créer l'icône "Live" pour les sessions actives
    - Définir les couleurs et styles pour les plateformes
    - _Requirements: 2.3, 7.1, 7.3_

- [ ] 10. Checkpoint - Interface utilisateur complète
  - Vérifier que toutes les interfaces fonctionnent correctement, demander à l'utilisateur si des questions se posent.

- [ ] 11. Tests d'intégration et validation finale
  - [ ] 11.1 Créer les tests d'intégration pour le workflow complet
    - Tester le cycle complet : création → affichage → redirection
    - Valider l'intégration avec les plateformes externes
    - Vérifier la persistance des données
    - _Requirements: 1.1, 2.1, 3.1, 6.1_

  - [ ]* 11.2 Créer les tests de propriétés pour les scénarios d'intégration
    - Tester les propriétés universelles sur des scénarios complets
    - Valider la robustesse avec des données générées aléatoirement
    - _Requirements: Toutes les propriétés 1-9_

  - [ ] 11.3 Effectuer les tests de validation avec de vraies URLs de plateformes
    - Tester avec des URLs Google Meet et Zoom réelles
    - Valider la redirection vers le navigateur
    - Vérifier la gestion des erreurs de connexion
    - _Requirements: 3.2, 3.4, 8.1_

- [ ] 12. Documentation et finalisation
  - [ ] 12.1 Créer la documentation utilisateur pour les enseignants
    - Documenter la création et gestion des sessions live
    - Expliquer l'obtention des liens de plateformes externes
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [ ] 12.2 Créer la documentation utilisateur pour les étudiants
    - Expliquer comment rejoindre une session live
    - Documenter les fonctionnalités disponibles sur les plateformes
    - _Requirements: 2.1, 3.1, 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 12.3 Mettre à jour le schéma de base de données principal
    - Intégrer le script session_live_schema.sql dans setup_complete.sql
    - Vérifier la compatibilité avec le schéma existant
    - _Requirements: 6.1, 6.2_

- [ ] 13. Checkpoint final - Validation complète
  - Vérifier que tous les tests passent, que l'intégration fonctionne correctement, demander à l'utilisateur si des questions se posent.

## Notes

- Les tâches marquées avec `*` sont optionnelles et peuvent être ignorées pour un MVP plus rapide
- Chaque tâche référence les exigences spécifiques pour la traçabilité
- Les checkpoints permettent une validation incrémentale
- Les tests de propriétés valident les propriétés universelles de correction
- Les tests unitaires valident des exemples spécifiques et cas d'erreur
- L'architecture suit les patterns existants de l'application EduCompus
- La délégation complète aux plateformes externes simplifie l'implémentation
- Les styles CSS maintiennent la cohérence visuelle avec l'interface existante

## Priorités d'implémentation

1. **Infrastructure de base** (Tâches 1-4) : Modèles, base de données, services
2. **Interfaces utilisateur** (Tâches 5-10) : Contrôleurs, vues, intégration
3. **Tests et validation** (Tâches 11-13) : Tests d'intégration, documentation, finalisation

Cette approche garantit une progression logique avec validation à chaque étape majeure.