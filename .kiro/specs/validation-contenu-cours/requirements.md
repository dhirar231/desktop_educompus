# Requirements Document

## Introduction

Ce document décrit les exigences pour le workflow de validation du contenu des cours dans la plateforme EduCompus (JavaFX 17 / MySQL). Actuellement, les cours créés par les enseignants sont immédiatement visibles par tous. La fonctionnalité à implémenter introduit un cycle de vie de statut (EN_ATTENTE → APPROUVE / REFUSE) afin qu'un administrateur puisse contrôler la qualité du contenu avant publication. Seuls les cours approuvés seront visibles par les étudiants.

## Glossaire

- **Cours** : Entité principale représentant un cours pédagogique, identifiée par un `id` unique en base de données.
- **Statut** : Valeur énumérée associée à un cours. Valeurs possibles : `EN_ATTENTE`, `APPROUVE`, `REFUSE`.
- **Enseignant** : Utilisateur dont le champ `teacher = true` dans `AuthUser`. Peut créer et gérer ses propres cours.
- **Administrateur** : Utilisateur dont le champ `admin = true` dans `AuthUser`. Peut approuver ou refuser les cours en attente.
- **Étudiant** : Utilisateur dont les champs `admin = false` et `teacher = false`. Consulte uniquement les cours approuvés.
- **Commentaire_Admin** : Texte optionnel saisi par l'administrateur lors d'un refus, expliquant la raison.
- **Workflow_Validation** : Processus métier décrivant les transitions de statut d'un cours.
- **CoursValidationRepository** : Composant d'accès aux données responsable des opérations liées au statut des cours.
- **CoursValidationService** : Service métier orchestrant les transitions de statut et les règles associées.
- **BackValidationController** : Contrôleur JavaFX côté administration pour la gestion des cours en attente.
- **FrontCoursesController** : Contrôleur JavaFX côté étudiant affichant la liste des cours.
- **TeacherCoursesController** : Contrôleur JavaFX côté enseignant affichant ses cours avec leur statut.

---

## Requirements

### Requirement 1 : Cycle de vie du statut d'un cours

**User Story :** En tant qu'administrateur, je veux que chaque cours passe par un processus de validation, afin de garantir la qualité du contenu publié sur la plateforme.

#### Acceptance Criteria

1. THE `Cours` SHALL posséder un champ `statut` de type `VARCHAR(20)` en base de données, avec la valeur par défaut `EN_ATTENTE`.
2. WHEN un `Enseignant` crée un cours, THE `Workflow_Validation` SHALL attribuer automatiquement le statut `EN_ATTENTE` au cours créé.
3. WHEN un `Administrateur` approuve un cours dont le statut est `EN_ATTENTE`, THE `Workflow_Validation` SHALL mettre à jour le statut du cours à `APPROUVE`.
4. WHEN un `Administrateur` refuse un cours dont le statut est `EN_ATTENTE`, THE `Workflow_Validation` SHALL mettre à jour le statut du cours à `REFUSE`.
5. IF un cours a le statut `APPROUVE` ou `REFUSE`, THEN THE `Workflow_Validation` SHALL rejeter toute tentative de transition vers un autre statut sans action explicite de l'administrateur.
6. THE `Cours` SHALL conserver un champ `commentaire_admin` de type `TEXT` nullable en base de données pour stocker la raison d'un refus.

---

### Requirement 2 : Rôle Enseignant — création et suivi de ses cours

**User Story :** En tant qu'enseignant, je veux créer des cours et voir leur statut de validation, afin de savoir si mon contenu a été approuvé ou refusé.

#### Acceptance Criteria

1. WHEN un `Enseignant` soumet un nouveau cours via le formulaire de création, THE `CoursValidationService` SHALL persister le cours avec le statut `EN_ATTENTE` et l'identifiant de l'enseignant.
2. THE `TeacherCoursesController` SHALL afficher uniquement les cours dont l'`id_enseignant` correspond à l'enseignant connecté.
3. THE `TeacherCoursesController` SHALL afficher le statut (`EN_ATTENTE`, `APPROUVE`, `REFUSE`) de chaque cours sous forme de badge visuel coloré.
4. WHERE le statut d'un cours est `REFUSE`, THE `TeacherCoursesController` SHALL afficher le `Commentaire_Admin` associé au cours.
5. WHEN un `Enseignant` modifie un cours dont le statut est `REFUSE`, THE `CoursValidationService` SHALL remettre le statut du cours à `EN_ATTENTE` et effacer le `Commentaire_Admin` précédent.
6. IF un `Enseignant` tente de modifier un cours dont le statut est `EN_ATTENTE` ou `APPROUVE`, THEN THE `TeacherCoursesController` SHALL afficher un message d'information indiquant que la modification n'est pas autorisée dans cet état.

---

### Requirement 3 : Rôle Administrateur — modération des cours en attente

**User Story :** En tant qu'administrateur, je veux voir tous les cours en attente de validation et pouvoir les approuver ou les refuser avec un commentaire optionnel, afin de contrôler la qualité du contenu publié.

#### Acceptance Criteria

1. THE `BackValidationController` SHALL afficher la liste de tous les cours dont le statut est `EN_ATTENTE`.
2. WHEN un `Administrateur` clique sur "Approuver" pour un cours `EN_ATTENTE`, THE `CoursValidationService` SHALL mettre à jour le statut du cours à `APPROUVE` et rafraîchir la liste.
3. WHEN un `Administrateur` clique sur "Refuser" pour un cours `EN_ATTENTE`, THE `BackValidationController` SHALL afficher un champ de saisie pour le `Commentaire_Admin`.
4. WHEN un `Administrateur` confirme le refus avec ou sans commentaire, THE `CoursValidationService` SHALL mettre à jour le statut du cours à `REFUSE` et persister le `Commentaire_Admin`.
5. THE `BackValidationController` SHALL afficher le nombre total de cours en attente sous forme de compteur.
6. IF aucun cours n'est en attente de validation, THEN THE `BackValidationController` SHALL afficher un message indiquant qu'il n'y a aucun cours à modérer.
7. THE `BackValidationController` SHALL également permettre à l'administrateur de consulter les cours `APPROUVE` et `REFUSE` dans des onglets ou sections séparés.

---

### Requirement 4 : Rôle Étudiant — visibilité des cours approuvés uniquement

**User Story :** En tant qu'étudiant, je veux voir uniquement les cours approuvés, afin de ne pas être exposé à du contenu non validé.

#### Acceptance Criteria

1. WHEN un `Étudiant` accède à la liste des cours, THE `FrontCoursesController` SHALL récupérer et afficher uniquement les cours dont le statut est `APPROUVE`.
2. IF un cours a le statut `EN_ATTENTE` ou `REFUSE`, THEN THE `FrontCoursesController` SHALL exclure ce cours de la liste affichée à l'étudiant.
3. THE `CoursValidationRepository` SHALL fournir une méthode `listCoursApprouves(String query)` retournant uniquement les cours avec le statut `APPROUVE`.
4. WHEN la liste des cours approuvés est vide, THE `FrontCoursesController` SHALL afficher l'état vide existant ("Aucun cours disponible").

---

### Requirement 5 : Règles métier et contrôle d'accès

**User Story :** En tant que responsable de la plateforme, je veux que les actions de validation soient strictement réservées aux rôles autorisés, afin d'éviter toute manipulation non autorisée du statut des cours.

#### Acceptance Criteria

1. IF un utilisateur dont `admin = false` tente d'appeler une opération d'approbation ou de refus, THEN THE `CoursValidationService` SHALL lever une `IllegalStateException` avec le message "Action réservée à l'administrateur.".
2. IF un utilisateur dont `teacher = false` tente de créer un cours, THEN THE `CoursValidationService` SHALL lever une `IllegalStateException` avec le message "Action réservée à l'enseignant.".
3. THE `CoursValidationService` SHALL valider que le cours cible existe en base de données avant toute transition de statut.
4. IF le cours cible n'existe pas, THEN THE `CoursValidationService` SHALL lever une `IllegalArgumentException` avec le message "Cours introuvable.".
5. THE `CoursValidationService` SHALL valider que le statut actuel du cours est `EN_ATTENTE` avant d'autoriser une approbation ou un refus.
6. IF le statut actuel du cours n'est pas `EN_ATTENTE`, THEN THE `CoursValidationService` SHALL lever une `IllegalStateException` avec le message "Transition de statut invalide.".

---

### Requirement 6 : Retour visuel et notifications en interface

**User Story :** En tant qu'utilisateur de la plateforme, je veux recevoir un retour visuel clair lors des changements de statut, afin de comprendre immédiatement l'état de mes cours ou des actions effectuées.

#### Acceptance Criteria

1. WHEN le statut d'un cours est `EN_ATTENTE`, THE `TeacherCoursesController` SHALL afficher un badge de couleur orange avec le libellé "En attente".
2. WHEN le statut d'un cours est `APPROUVE`, THE `TeacherCoursesController` SHALL afficher un badge de couleur verte avec le libellé "Approuvé".
3. WHEN le statut d'un cours est `REFUSE`, THE `TeacherCoursesController` SHALL afficher un badge de couleur rouge avec le libellé "Refusé".
4. WHEN un `Administrateur` approuve un cours, THE `BackValidationController` SHALL afficher une alerte JavaFX de type `INFORMATION` confirmant l'approbation.
5. WHEN un `Administrateur` refuse un cours, THE `BackValidationController` SHALL afficher une alerte JavaFX de type `INFORMATION` confirmant le refus.
6. WHEN un `Enseignant` soumet un cours, THE `TeacherCoursesController` SHALL afficher une alerte JavaFX de type `INFORMATION` indiquant que le cours est en attente de validation.
