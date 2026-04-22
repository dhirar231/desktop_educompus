# Design Document — Validation du Contenu des Cours

## Overview

Ce document décrit la conception technique du workflow de validation des cours pour EduCompus (JavaFX 17 / MySQL / JDBC / MVC).

L'objectif est d'introduire un cycle de vie de statut (`EN_ATTENTE → APPROUVE / REFUSE`) sur l'entité `Cours`. Les enseignants soumettent des cours qui restent invisibles aux étudiants jusqu'à approbation par un administrateur. Le design s'intègre dans l'architecture existante sans réécriture majeure.

**Décisions de conception clés :**
- Un enum `CoursStatut` typé remplace les chaînes brutes en Java, tout en restant `VARCHAR(20)` en base.
- Un nouveau `CoursValidationRepository` est créé séparément de `CourseManagementRepository` pour respecter le principe de responsabilité unique.
- Un `CoursWorkflowService` orchestre les transitions de statut avec les contrôles de rôle via `AppState`.
- Le `CoursValidationService` existant (validation de formulaire) est conservé tel quel — aucun conflit de nommage.
- La migration de schéma suit le pattern `ensureCoursSchema()` déjà en place (ALTER TABLE idempotent).

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Couche Vue (FXML)                        │
│  BackValidation.fxml  TeacherCourses.fxml  FrontCourses.fxml   │
└──────────────┬──────────────────┬──────────────────────────────┘
               │                  │
┌──────────────▼──────────────────▼──────────────────────────────┐
│                     Couche Contrôleur                           │
│  BackValidationController  TeacherCoursesController            │
│  BackCoursesController (modifié)  FrontCoursesController (mod) │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                      Couche Service                             │
│  CoursWorkflowService  (nouveau)                                │
│  CoursService  (existant, inchangé)                             │
│  CoursValidationService  (existant, inchangé)                   │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                    Couche Repository                            │
│  CoursValidationRepository  (nouveau)                           │
│  CourseManagementRepository  (existant, modifié à la marge)     │
└──────────────┬──────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────┐
│                    Base de données MySQL                        │
│  table cours  (+ colonnes statut, commentaire_admin, created_by_id) │
└─────────────────────────────────────────────────────────────────┘
```

**Flux de validation :**

```
Enseignant                Admin                  Étudiant
    │                       │                       │
    │ soumettre(cours)       │                       │
    │──────────────────────►│                       │
    │  statut=EN_ATTENTE     │                       │
    │                       │ listCoursEnAttente()   │
    │                       │◄──────────────────────│
    │                       │                       │
    │                       │ approuver(id) ──────► statut=APPROUVE
    │                       │   ou                  │
    │                       │ refuser(id, comment)─► statut=REFUSE
    │                       │                       │
    │                       │              listCoursApprouves()
    │                       │                       │◄──────────
```

---

## Components and Interfaces

### 1. Enum `CoursStatut`

```java
package com.educompus.model;

public enum CoursStatut {
    EN_ATTENTE,
    APPROUVE,
    REFUSE;

    /** Libellé d'affichage français. */
    public String libelle() {
        return switch (this) {
            case EN_ATTENTE -> "En attente";
            case APPROUVE   -> "Approuvé";
            case REFUSE     -> "Refusé";
        };
    }

    /** Classe CSS du badge associé. */
    public String badgeCssClass() {
        return switch (this) {
            case EN_ATTENTE -> "badge-en-attente";
            case APPROUVE   -> "badge-approuve";
            case REFUSE     -> "badge-refuse";
        };
    }

    public static CoursStatut fromString(String value) {
        if (value == null) return EN_ATTENTE;
        return switch (value.toUpperCase()) {
            case "APPROUVE"  -> APPROUVE;
            case "REFUSE"    -> REFUSE;
            default          -> EN_ATTENTE;
        };
    }
}
```

### 2. Modèle `Cours` — champs ajoutés

```java
// Champs supplémentaires dans com.educompus.model.Cours
private CoursStatut statut = CoursStatut.EN_ATTENTE;
private String commentaireAdmin;   // nullable
private int createdById;           // 0 = non défini

// Getters / setters correspondants
public CoursStatut getStatut() { ... }
public void setStatut(CoursStatut statut) { ... }
public String getCommentaireAdmin() { ... }
public void setCommentaireAdmin(String commentaireAdmin) { ... }
public int getCreatedById() { ... }
public void setCreatedById(int createdById) { ... }
```

### 3. `CoursValidationRepository`

```java
package com.educompus.repository;

public final class CoursValidationRepository {

    public CoursValidationRepository() { ensureValidationSchema(); }

    /**
     * Passe le statut du cours à APPROUVE.
     * @throws IllegalArgumentException si le cours n'existe pas
     * @throws IllegalStateException    si le statut n'est pas EN_ATTENTE
     */
    public void approuver(int coursId);

    /**
     * Passe le statut du cours à REFUSE et persiste le commentaire.
     * @param commentaire peut être null ou vide
     * @throws IllegalArgumentException si le cours n'existe pas
     * @throws IllegalStateException    si le statut n'est pas EN_ATTENTE
     */
    public void refuser(int coursId, String commentaire);

    /**
     * Remet le statut à EN_ATTENTE et efface le commentaire_admin.
     * Utilisé quand un enseignant modifie un cours REFUSE.
     */
    public void reinitialiserStatut(int coursId);

    /** Retourne tous les cours avec statut = EN_ATTENTE. */
    public List<Cours> listCoursEnAttente();

    /**
     * Retourne tous les cours avec statut = APPROUVE,
     * filtrés optionnellement par query (titre, domaine, formateur).
     */
    public List<Cours> listCoursApprouves(String query);

    /** Retourne tous les cours créés par un enseignant donné. */
    public List<Cours> listCoursByEnseignant(int enseignantId);

    /** Retourne un cours par son id, ou null si absent. */
    public Cours findById(int coursId);

    // Méthode privée de migration de schéma
    private void ensureValidationSchema();
}
```

### 4. `CoursWorkflowService`

```java
package com.educompus.service;

public final class CoursWorkflowService {

    private final CoursValidationRepository validationRepo;
    private final CourseManagementRepository coursRepo;

    public CoursWorkflowService() { ... }

    /**
     * Soumet un cours créé par un enseignant.
     * Vérifie que l'appelant est TEACHER via AppState.
     * Persiste le cours avec statut=EN_ATTENTE et createdById=enseignantId.
     *
     * @throws IllegalStateException    si AppState.isTeacher() == false
     * @throws IllegalArgumentException si le cours est invalide
     */
    public void soumettre(Cours cours, int enseignantId);

    /**
     * Approuve un cours EN_ATTENTE.
     * Vérifie que l'appelant est ADMIN via AppState.
     *
     * @throws IllegalStateException    si AppState.isAdmin() == false
     *                                  ou si statut != EN_ATTENTE ("Transition de statut invalide.")
     * @throws IllegalArgumentException si le cours n'existe pas ("Cours introuvable.")
     */
    public void approuver(int coursId, int adminId);

    /**
     * Refuse un cours EN_ATTENTE avec un commentaire optionnel.
     * Vérifie que l'appelant est ADMIN via AppState.
     *
     * @throws IllegalStateException    si AppState.isAdmin() == false
     *                                  ou si statut != EN_ATTENTE ("Transition de statut invalide.")
     * @throws IllegalArgumentException si le cours n'existe pas ("Cours introuvable.")
     */
    public void refuser(int coursId, int adminId, String commentaire);

    /**
     * Remet un cours REFUSE à EN_ATTENTE lors d'une modification par l'enseignant.
     * Vérifie que l'appelant est TEACHER.
     *
     * @throws IllegalStateException    si AppState.isTeacher() == false
     *                                  ou si statut != REFUSE
     */
    public void reinitialiserPourModification(int coursId);
}
```

### 5. `BackValidationController`

```java
package com.educompus.controller.back;

public final class BackValidationController {

    @FXML private ListView<Cours> enAttenteListView;
    @FXML private ListView<Cours> approuveListView;
    @FXML private ListView<Cours> refuseListView;
    @FXML private Label compteurEnAttenteLabel;
    @FXML private VBox emptyStatePane;

    @FXML private void initialize();
    @FXML private void onApprouver();   // approuve le cours sélectionné
    @FXML private void onRefuser();     // ouvre dialog commentaire puis refuse
    @FXML private void onRefresh();

    private void reloadLists();
    private void showCommentaireDialog(Cours cours);
}
```

### 6. `TeacherCoursesController`

```java
package com.educompus.controller.back;

public final class TeacherCoursesController {

    @FXML private ListView<Cours> coursListView;
    @FXML private Button addCoursBtn;

    @FXML private void initialize();
    @FXML private void onAddCours();
    @FXML private void onEditCours();

    /** Construit le badge Label pour un statut donné. */
    private Label buildBadge(CoursStatut statut);

    private void reloadCours();
}
```

### 7. Modifications `BackCoursesController`

La méthode `createCours()` existante est modifiée pour appeler `CoursWorkflowService.soumettre()` au lieu de `repository.createCours()` directement, afin de définir `statut=EN_ATTENTE` et `createdById=AppState.getUserId()`.

### 8. Modifications `FrontCoursesController`

`initialize()` remplace `repository.listCours("")` par `validationRepo.listCoursApprouves("")`. La recherche locale (`applyFilter`) continue de filtrer sur la liste déjà filtrée.

---

## Data Models

### Migration SQL

```sql
-- À exécuter via ensureValidationSchema() dans CoursValidationRepository
-- (pattern idempotent déjà utilisé dans CourseManagementRepository)

ALTER TABLE cours
    ADD COLUMN IF NOT EXISTS statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    ADD COLUMN IF NOT EXISTS commentaire_admin TEXT NULL,
    ADD COLUMN IF NOT EXISTS created_by_id INT NULL;
```

> MySQL < 8.0 ne supporte pas `ADD COLUMN IF NOT EXISTS`. Le code Java utilisera le pattern `columnExists()` déjà présent dans `CourseManagementRepository` pour rendre la migration idempotente.

### Schéma final de la table `cours`

| Colonne               | Type           | Contrainte                        |
|-----------------------|----------------|-----------------------------------|
| id                    | INT            | PK AUTO_INCREMENT                 |
| titre                 | VARCHAR(255)   | NOT NULL                          |
| description           | TEXT           |                                   |
| niveau                | VARCHAR(32)    |                                   |
| domaine               | VARCHAR(64)    |                                   |
| image                 | VARCHAR(255)   |                                   |
| date_creation         | DATETIME       | NOT NULL DEFAULT CURRENT_TIMESTAMP|
| nom_formateur         | VARCHAR(255)   |                                   |
| duree_totale_heures   | INT            | NOT NULL DEFAULT 0                |
| statut                | VARCHAR(20)    | NOT NULL DEFAULT 'EN_ATTENTE'     |
| commentaire_admin     | TEXT           | NULL                              |
| created_by_id         | INT            | NULL                              |

### Diagramme de classes (simplifié)

```
┌──────────────────────────────┐
│         CoursStatut          │
│  <<enum>>                    │
│  EN_ATTENTE                  │
│  APPROUVE                    │
│  REFUSE                      │
│  + libelle(): String         │
│  + badgeCssClass(): String   │
│  + fromString(String): self  │
└──────────────────────────────┘
           ▲
           │ uses
┌──────────┴───────────────────┐
│            Cours             │
│  id: int                     │
│  titre: String               │
│  description: String         │
│  niveau: String              │
│  domaine: String             │
│  image: String               │
│  dateCreation: String        │
│  nomFormateur: String        │
│  dureeTotaleHeures: int      │
│  chapitreCount: int          │
│  statut: CoursStatut         │  ← nouveau
│  commentaireAdmin: String    │  ← nouveau
│  createdById: int            │  ← nouveau
└──────────────────────────────┘
           ▲
           │ manipule
┌──────────┴───────────────────┐      ┌──────────────────────────────┐
│  CoursValidationRepository   │      │  CourseManagementRepository  │
│  + approuver(int)            │      │  + createCours(Cours)        │
│  + refuser(int, String)      │      │  + updateCours(Cours)        │
│  + reinitialiserStatut(int)  │      │  + listCours(String)         │
│  + listCoursEnAttente()      │      │  + ...                       │
│  + listCoursApprouves(String)│      └──────────────────────────────┘
│  + listCoursByEnseignant(int)│
│  + findById(int)             │
└──────────────────────────────┘
           ▲
           │ uses
┌──────────┴───────────────────┐
│     CoursWorkflowService     │
│  + soumettre(Cours, int)     │
│  + approuver(int, int)       │
│  + refuser(int, int, String) │
│  + reinitialiserPourMod(int) │
└──────────────────────────────┘
           ▲
           │ uses
┌──────────┴───────────────────┐
│  BackValidationController    │
│  TeacherCoursesController    │
│  BackCoursesController (mod) │
│  FrontCoursesController (mod)│
└──────────────────────────────┘
```

### Transitions de statut

```
         soumettre()
[INITIAL] ──────────► [EN_ATTENTE]
                           │
              approuver()  │  refuser(commentaire)
                    ┌──────┴──────┐
                    ▼             ▼
               [APPROUVE]     [REFUSE]
                                  │
                    reinitialiserPourModification()
                                  │
                                  ▼
                            [EN_ATTENTE]
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1 : Soumission initialise le statut EN_ATTENTE

*For any* cours valide soumis par un enseignant (AppState.isTeacher() == true), le cours persisté doit avoir `statut = EN_ATTENTE` et `createdById` égal à l'identifiant de l'enseignant fourni.

**Validates: Requirements 1.2, 2.1**

---

### Property 2 : Approbation transite vers APPROUVE

*For any* cours dont le statut est `EN_ATTENTE`, appeler `approuver()` doit résulter en `statut = APPROUVE`, sans modifier les autres champs du cours.

**Validates: Requirements 1.3, 3.2**

---

### Property 3 : Refus transite vers REFUSE avec commentaire persisté

*For any* cours dont le statut est `EN_ATTENTE` et *for any* chaîne de commentaire (y compris vide ou null), appeler `refuser(coursId, commentaire)` doit résulter en `statut = REFUSE` et `commentaireAdmin` égal au commentaire fourni.

**Validates: Requirements 1.4, 3.4**

---

### Property 4 : Transition invalide levée pour statut non EN_ATTENTE

*For any* cours dont le statut est `APPROUVE` ou `REFUSE`, appeler `approuver()` ou `refuser()` doit lever une `IllegalStateException` avec le message `"Transition de statut invalide."`.

**Validates: Requirements 1.5, 5.5, 5.6**

---

### Property 5 : Modification d'un cours REFUSE remet EN_ATTENTE

*For any* cours dont le statut est `REFUSE` avec *any* commentaire_admin, appeler `reinitialiserPourModification()` doit résulter en `statut = EN_ATTENTE` et `commentaireAdmin = null`.

**Validates: Requirements 2.5**

---

### Property 6 : Filtrage par enseignant retourne uniquement ses cours

*For any* identifiant d'enseignant et *for any* ensemble de cours en base avec des `createdById` variés, `listCoursByEnseignant(enseignantId)` doit retourner uniquement les cours dont `createdById == enseignantId`.

**Validates: Requirements 2.2**

---

### Property 7 : listCoursEnAttente retourne uniquement les EN_ATTENTE

*For any* ensemble de cours en base avec des statuts variés, `listCoursEnAttente()` doit retourner uniquement les cours dont `statut = EN_ATTENTE`.

**Validates: Requirements 3.1**

---

### Property 8 : listCoursApprouves retourne uniquement les APPROUVE

*For any* ensemble de cours en base avec des statuts variés et *for any* query de recherche, `listCoursApprouves(query)` doit retourner uniquement les cours dont `statut = APPROUVE`.

**Validates: Requirements 4.1, 4.2, 4.3**

---

### Property 9 : Contrôle d'accès — opérations admin refusées aux non-admins

*For any* configuration AppState où `isAdmin() == false`, appeler `approuver()` ou `refuser()` via `CoursWorkflowService` doit lever une `IllegalStateException` avec le message `"Action réservée à l'administrateur."`.

**Validates: Requirements 5.1**

---

### Property 10 : Contrôle d'accès — soumission refusée aux non-enseignants

*For any* configuration AppState où `isTeacher() == false`, appeler `soumettre()` via `CoursWorkflowService` doit lever une `IllegalStateException` avec le message `"Action réservée à l'enseignant."`.

**Validates: Requirements 5.2**

---

### Property 11 : Cours inexistant lève IllegalArgumentException

*For any* identifiant de cours qui n'existe pas en base de données, appeler `approuver()` ou `refuser()` doit lever une `IllegalArgumentException` avec le message `"Cours introuvable."`.

**Validates: Requirements 5.3, 5.4**

---

### Property 12 : Badge visuel correspond au statut

*For any* valeur de `CoursStatut`, la méthode `buildBadge(statut)` doit retourner un `Label` dont la classe CSS est `statut.badgeCssClass()` et le texte est `statut.libelle()`.

**Validates: Requirements 6.1, 6.2, 6.3**

---

## Error Handling

| Situation | Exception levée | Message |
|-----------|----------------|---------|
| Non-admin tente approuver/refuser | `IllegalStateException` | `"Action réservée à l'administrateur."` |
| Non-enseignant tente soumettre | `IllegalStateException` | `"Action réservée à l'enseignant."` |
| Cours inexistant | `IllegalArgumentException` | `"Cours introuvable."` |
| Statut != EN_ATTENTE lors d'une transition | `IllegalStateException` | `"Transition de statut invalide."` |
| Erreur JDBC | `IllegalStateException` | Message technique wrappé |

Les contrôleurs JavaFX capturent ces exceptions et les affichent via `Alert.AlertType.ERROR`. Les opérations réussies affichent une `Alert.AlertType.INFORMATION`.

---

## Testing Strategy

### Approche duale

Les tests sont organisés en deux catégories complémentaires :

1. **Tests unitaires (JUnit 5)** — exemples concrets, cas limites, comportements UI
2. **Tests de propriétés (jqwik)** — propriétés universelles sur la logique métier

### Bibliothèque PBT

**jqwik** (déjà compatible JUnit 5 / Maven) est utilisé pour les tests de propriétés.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.4</version>
    <scope>test</scope>
</dependency>
```

Chaque test de propriété est configuré avec `@Property(tries = 100)` minimum.

### Tests de propriétés (jqwik)

Chaque propriété du design correspond à un test annoté `@Property`. Le tag de référence est inclus en commentaire :

```java
// Feature: validation-contenu-cours, Property 1: Soumission initialise le statut EN_ATTENTE
@Property(tries = 100)
void soumettreInitialiseEnAttente(@ForAll("validCours") Cours cours, @ForAll int enseignantId) { ... }
```

| Test | Propriété validée |
|------|------------------|
| `soumettreInitialiseEnAttente` | Property 1 |
| `approuverTransiteVersApprouve` | Property 2 |
| `refuserTransiteVersRefuseAvecCommentaire` | Property 3 |
| `transitionInvalideLeveException` | Property 4 |
| `reinitialiserRemetEnAttente` | Property 5 |
| `filtragePareEnseignant` | Property 6 |
| `listEnAttenteRetourneSeulementEnAttente` | Property 7 |
| `listApprouveRetourneSeulementApprouve` | Property 8 |
| `nonAdminNeePeutPasApprouverRefuser` | Property 9 |
| `nonEnseignantNePeutPasSoumettre` | Property 10 |
| `coursInexistantLeveIllegalArgument` | Property 11 |
| `badgeCorrespondAuStatut` | Property 12 |

### Tests unitaires (JUnit 5)

- `CoursStatutTest` — `fromString()` avec valeurs connues et null
- `BackValidationControllerTest` — affichage du compteur, état vide, dialog commentaire
- `TeacherCoursesControllerTest` — affichage du commentaire_admin pour REFUSE, message info pour EN_ATTENTE/APPROUVE
- `FrontCoursesControllerTest` — état vide quand liste approuvés vide
- Tests de smoke (schéma) — vérification des colonnes `statut`, `commentaire_admin`, `created_by_id` via `information_schema`

### CSS — Badges de statut

```css
/* À ajouter dans styles/educompus.css */

.badge-en-attente {
    -fx-background-color: #f97316;   /* orange */
    -fx-text-fill: white;
    -fx-background-radius: 12px;
    -fx-padding: 2px 10px;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
}

.badge-approuve {
    -fx-background-color: #22c55e;   /* vert */
    -fx-text-fill: white;
    -fx-background-radius: 12px;
    -fx-padding: 2px 10px;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
}

.badge-refuse {
    -fx-background-color: #ef4444;   /* rouge */
    -fx-text-fill: white;
    -fx-background-radius: 12px;
    -fx-padding: 2px 10px;
    -fx-font-size: 11px;
    -fx-font-weight: bold;
}
```
