# Plan d'implémentation : Validation du Contenu des Cours

## Vue d'ensemble

Implémentation du workflow de validation des cours (EN_ATTENTE → APPROUVE / REFUSE) dans EduCompus (JavaFX 17 / MySQL / JDBC / MVC). Les tâches suivent l'ordre des dépendances : schéma → modèle → repository → service → contrôleurs → vues → CSS → tests.

## Tâches

- [x] 1. Migration SQL et enum CoursStatut
  - [x] 1.1 Créer l'enum `CoursStatut` dans `com.educompus.model`
    - Implémenter les valeurs `EN_ATTENTE`, `APPROUVE`, `REFUSE`
    - Implémenter `libelle()`, `badgeCssClass()`, `fromString(String)`
    - _Requi
      - [ ]* 1.2 Écrire les tests unitaires pour `CoursStatut`
    - Tester `fromString()` avec `"APPROUVE"`, `"REFUSE"`, `"EN_ATTENTE"`, `null`, valeur inconnue
    - Tester `libelle()` et `badgeCssClass()` pour chaque valeur
    - _Requirements: 1.1_

- [x] 2. Mise à jour du modèle `Cours`
  - [x] 2.1 Ajouter les trois nouveaux champs dans `com.educompus.model.Cours`
    - Ajouter `statut: CoursStatut` (défaut `EN_ATTENTE`), `commentaireAdmin: String` (nullable), `createdById: int`
    - Ajouter les getters et setters correspondants
    - _Requirements: 1.1, 1.6, 2.1_

- [x] 3. Implémenter `CoursValidationRepository`
  - [x] 3.1 Créer la classe `CoursValidationRepository` dans `com.educompus.repository`
    - Implémenter le constructeur avec appel à `ensureValidationSchema()`
    - Implémenter `ensureValidationSchema()` : migration idempotente via `columnExists()` pour ajouter `statut`, `commentaire_admin`, `created_by_id` à la table `cours`
    - _Requirements: 1.1, 1.6_

  - [x] 3.2 Implémenter les méthodes de lecture
    - Implémenter `findById(int coursId)` : retourne `Cours` ou `null`
    - Implémenter `listCoursEnAttente()` : `WHERE statut = 'EN_ATTENTE'`
    - Implémenter `listCoursApprouves(String query)` : `WHERE statut = 'APPROUVE'` avec filtre optionnel sur titre/domaine/formateur
    - Implémenter `listCoursByEnseignant(int enseignantId)` : `WHERE created_by_id = ?`
    - Mapper les colonnes `statut`, `commentaire_admin`, `created_by_id` dans le `ResultSet`
    - _Requirements: 3.1, 4.1, 4.2, 4.3, 2.2_

  - [x] 3.3 Implémenter les méthodes de transition de statut
    - Implémenter `approuver(int coursId)` : `UPDATE cours SET statut='APPROUVE' WHERE id=? AND statut='EN_ATTENTE'`; lever `IllegalArgumentException("Cours introuvable.")` si absent, `IllegalStateException("Transition de statut invalide.")` si statut != EN_ATTENTE
    - Implémenter `refuser(int coursId, String commentaire)` : même logique avec `statut='REFUSE'` et `commentaire_admin=?`
    - Implémenter `reinitialiserStatut(int coursId)` : `UPDATE cours SET statut='EN_ATTENTE', commentaire_admin=NULL WHERE id=?`
    - _Requirements: 1.3, 1.4, 1.5, 2.5, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 3.4 Écrire le test de propriété — Property 7 : listCoursEnAttente retourne uniquement les EN_ATTENTE
    - **Property 7 : listCoursEnAttente retourne uniquement les EN_ATTENTE**
    - **Validates: Requirements 3.1**

  - [ ]* 3.5 Écrire le test de propriété — Property 8 : listCoursApprouves retourne uniquement les APPROUVE
    - **Property 8 : listCoursApprouves retourne uniquement les APPROUVE**
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [ ]* 3.6 Écrire le test de propriété — Property 6 : filtrage par enseignant retourne uniquement ses cours
    - **Property 6 : filtragePareEnseignant**
    - **Validates: Requirements 2.2**

  - [ ]* 3.7 Écrire le test de propriété — Property 11 : cours inexistant lève IllegalArgumentException
    - **Property 11 : coursInexistantLeveIllegalArgument**
    - **Validates: Requirements 5.3, 5.4**

- [ ] 4. Checkpoint — Vérifier que le schéma et le repository compilent et que les tests passent
  - S'assurer que tous les tests passent, poser des questions à l'utilisateur si nécessaire.

- [x] 5. Implémenter `CoursWorkflowService`
  - [x] 5.1 Créer la classe `CoursWorkflowService` dans `com.educompus.service`
    - Injecter `CoursValidationRepository` et `CourseManagementRepository`
    - Implémenter `soumettre(Cours cours, int enseignantId)` : vérifier `AppState.isTeacher()`, setter `statut=EN_ATTENTE` et `createdById=enseignantId`, persister via `coursRepo.createCours()`
    - Implémenter `approuver(int coursId, int adminId)` : vérifier `AppState.isAdmin()`, déléguer à `validationRepo.approuver()`
    - Implémenter `refuser(int coursId, int adminId, String commentaire)` : vérifier `AppState.isAdmin()`, déléguer à `validationRepo.refuser()`
    - Implémenter `reinitialiserPourModification(int coursId)` : vérifier `AppState.isTeacher()` et `statut == REFUSE`, déléguer à `validationRepo.reinitialiserStatut()`
    - _Requirements: 1.2, 1.3, 1.4, 2.1, 2.5, 5.1, 5.2, 5.5, 5.6_

  - [ ]* 5.2 Écrire le test de propriété — Property 1 : soumission initialise le statut EN_ATTENTE
    - **Property 1 : soumettreInitialiseEnAttente**
    - **Validates: Requirements 1.2, 2.1**

  - [ ]* 5.3 Écrire le test de propriété — Property 2 : approbation transite vers APPROUVE
    - **Property 2 : approuverTransiteVersApprouve**
    - **Validates: Requirements 1.3, 3.2**

  - [ ]* 5.4 Écrire le test de propriété — Property 3 : refus transite vers REFUSE avec commentaire persisté
    - **Property 3 : refuserTransiteVersRefuseAvecCommentaire**
    - **Validates: Requirements 1.4, 3.4**

  - [ ]* 5.5 Écrire le test de propriété — Property 4 : transition invalide levée pour statut non EN_ATTENTE
    - **Property 4 : transitionInvalideLeveException**
    - **Validates: Requirements 1.5, 5.5, 5.6**

  - [ ]* 5.6 Écrire le test de propriété — Property 5 : modification d'un cours REFUSE remet EN_ATTENTE
    - **Property 5 : reinitialiserRemetEnAttente**
    - **Validates: Requirements 2.5**

  - [ ]* 5.7 Écrire le test de propriété — Property 9 : contrôle d'accès admin
    - **Property 9 : nonAdminNeePeutPasApprouverRefuser**
    - **Validates: Requirements 5.1**

  - [ ]* 5.8 Écrire le test de propriété — Property 10 : contrôle d'accès enseignant
    - **Property 10 : nonEnseignantNePeutPasSoumettre**
    - **Validates: Requirements 5.2**

  - [ ]* 5.9 Écrire les tests unitaires pour `CoursWorkflowService`
    - Tester `soumettre()` avec un cours valide (statut EN_ATTENTE attendu)
    - Tester `approuver()` et `refuser()` avec un cours EN_ATTENTE (mock repository)
    - Tester les cas d'erreur : cours inexistant, statut invalide, rôle incorrect
    - _Requirements: 1.2, 1.3, 1.4, 5.1, 5.2, 5.3, 5.4_

- [ ] 6. Checkpoint — Vérifier que le service compile et que tous les tests passent
  - S'assurer que tous les tests passent, poser des questions à l'utilisateur si nécessaire.

- [x] 7. Mettre à jour `BackCoursesController`
  - [x] 7.1 Modifier la méthode `createCours()` dans `BackCoursesController`
    - Remplacer l'appel direct à `repository.createCours()` par `CoursWorkflowService.soumettre(cours, AppState.getUserId())`
    - Afficher une alerte `INFORMATION` indiquant que le cours est en attente de validation
    - _Requirements: 1.2, 2.1, 6.6_

- [x] 8. Mettre à jour `FrontCoursesController`
  - [x] 8.1 Modifier `initialize()` dans `FrontCoursesController`
    - Instancier `CoursValidationRepository` et remplacer `repository.listCours("")` par `validationRepo.listCoursApprouves("")`
    - S'assurer que `applyFilter` continue de filtrer sur la liste déjà filtrée (APPROUVE uniquement)
    - Afficher l'état vide existant si la liste est vide
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 9. Créer `BackValidationController` et `BackValidation.fxml`
  - [x] 9.1 Créer `BackValidationController` dans `com.educompus.controller.back`
    - Déclarer les champs `@FXML` : `enAttenteListView`, `approuveListView`, `refuseListView`, `compteurEnAttenteLabel`, `emptyStatePane`
    - Implémenter `initialize()` : charger les listes via `CoursWorkflowService`
    - Implémenter `onApprouver()` : appeler `workflowService.approuver()`, afficher alerte `INFORMATION`, rafraîchir
    - Implémenter `onRefuser()` : ouvrir dialog commentaire via `showCommentaireDialog()`, appeler `workflowService.refuser()`, afficher alerte `INFORMATION`, rafraîchir
    - Implémenter `onRefresh()` et `reloadLists()`
    - Implémenter `showCommentaireDialog(Cours cours)` : `TextInputDialog` pour saisir le commentaire
    - Afficher le compteur EN_ATTENTE et l'état vide si aucun cours
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 6.4, 6.5_

  - [x] 9.2 Créer `BackValidation.fxml` dans `src/main/resources/View/back/`
    - Structurer la vue avec trois sections (EN_ATTENTE, APPROUVE, REFUSE) via `TabPane` ou `VBox`
    - Inclure `ListView` pour chaque section, `Label` compteur, `VBox` état vide, boutons "Approuver" / "Refuser" / "Rafraîchir"
    - Lier les `fx:id` aux champs du contrôleur
    - _Requirements: 3.1, 3.5, 3.6, 3.7_

- [x] 10. Créer `TeacherCoursesController` et `TeacherCourses.fxml`
  - [x] 10.1 Créer `TeacherCoursesController` dans `com.educompus.controller.back`
    - Déclarer les champs `@FXML` : `coursListView`, `addCoursBtn`
    - Implémenter `initialize()` : charger les cours de l'enseignant connecté via `validationRepo.listCoursByEnseignant(AppState.getUserId())`
    - Implémenter `reloadCours()` et `buildBadge(CoursStatut statut)` : retourner un `Label` avec `statut.libelle()` et classe CSS `statut.badgeCssClass()`
    - Implémenter `onAddCours()` : déléguer à `CoursWorkflowService.soumettre()`
    - Implémenter `onEditCours()` : vérifier le statut — si REFUSE appeler `reinitialiserPourModification()`, sinon afficher alerte `INFORMATION`
    - Afficher `commentaireAdmin` pour les cours REFUSE
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6, 6.1, 6.2, 6.3, 6.6_

  - [x] 10.2 Créer `TeacherCourses.fxml` dans `src/main/resources/View/back/`
    - Structurer la vue avec `ListView` pour les cours, boutons "Ajouter" et "Modifier"
    - Chaque cellule affiche le titre du cours et le badge de statut
    - Lier les `fx:id` aux champs du contrôleur
    - _Requirements: 2.2, 2.3, 2.4_

  - [ ]* 10.3 Écrire le test de propriété — Property 12 : badge visuel correspond au statut
    - **Property 12 : badgeCorrespondAuStatut**
    - **Validates: Requirements 6.1, 6.2, 6.3**

- [x] 11. Ajouter les styles CSS des badges
  - [x] 11.1 Ajouter les classes `.badge-en-attente`, `.badge-approuve`, `.badge-refuse` dans `styles/educompus.css`
    - `.badge-en-attente` : fond orange `#f97316`, texte blanc, border-radius 12px, padding 2px 10px, font-size 11px, bold
    - `.badge-approuve` : fond vert `#22c55e`, texte blanc, mêmes dimensions
    - `.badge-refuse` : fond rouge `#ef4444`, texte blanc, mêmes dimensions
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] 12. Ajouter la dépendance jqwik et finaliser les tests de propriétés
  - [ ] 12.1 Ajouter la dépendance `jqwik 1.8.4` dans `pom.xml`
    - Ajouter `<dependency>net.jqwik:jqwik:1.8.4:test</dependency>`
    - _Requirements: (infrastructure de test)_

  - [ ]* 12.2 Créer `CoursWorkflowServicePropertyTest` dans `src/test/java/com/educompus/service/`
    - Implémenter les 12 tests de propriétés jqwik (Properties 1–12) avec `@Property(tries = 100)`
    - Utiliser des `@Provide` pour générer des `Cours` valides, des commentaires arbitraires, des identifiants
    - Mocker `AppState` pour simuler les rôles admin/teacher/étudiant
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 2.2, 2.5, 3.1, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.1, 6.2, 6.3_

- [ ] 13. Checkpoint final — Vérifier que tous les tests passent et que l'application compile
  - S'assurer que tous les tests passent, poser des questions à l'utilisateur si nécessaire.

## Notes

- Les tâches marquées `*` sont optionnelles et peuvent être ignorées pour un MVP rapide
- Chaque tâche référence les exigences spécifiques pour la traçabilité
- Les tests de propriétés (jqwik) valident les propriétés universelles définies dans le design
- Les tests unitaires valident les exemples concrets et les cas limites
- Le pattern `columnExists()` déjà présent dans `CourseManagementRepository` doit être réutilisé pour la migration idempotente
