# Document de Conception Technique - Module Interaction en Session Live

## Vue d'ensemble

Le module "Interaction en session live" permet aux enseignants de créer et gérer des sessions live via des plateformes externes (Google Meet, Zoom), et aux étudiants de rejoindre ces sessions pour participer aux interactions en temps réel. L'application JavaFX sert d'interface de gestion et de redirection, sans implémenter de système de visioconférence interne.

### Objectifs principaux

- **Simplicité architecturale** : Délégation complète aux plateformes externes
- **Expérience utilisateur fluide** : Interface intuitive pour la gestion des sessions
- **Sécurité** : Validation et assainissement des liens de session
- **Intégration** : Cohérence avec l'architecture existante de l'application

### Contraintes techniques

- Aucune implémentation WebRTC ou streaming vidéo interne
- Utilisation exclusive des plateformes externes autorisées
- Maintien de la cohérence avec l'architecture JavaFX existante
- Persistance des données dans MySQL existant

## Architecture

### Architecture générale

Le module s'intègre dans l'architecture existante de l'application EduCompus en suivant le pattern MVC déjà établi :

```
┌─────────────────────────────────────────────────────────────┐
│                    Couche Présentation                      │
├─────────────────────────────────────────────────────────────┤
│  BackSessionLiveController  │  FrontSessionLiveController   │
│  (Gestion enseignant)       │  (Affichage étudiant)         │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Couche Service                           │
├─────────────────────────────────────────────────────────────┤
│  SessionLiveService         │  SessionLiveValidationService │
│  (Logique métier)           │  (Validation des données)     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Couche Données                           │
├─────────────────────────────────────────────────────────────┤
│  SessionLiveRepository      │  CourseManagementRepository   │
│  (Accès données sessions)   │  (Données cours existantes)   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Base de Données MySQL                    │
├─────────────────────────────────────────────────────────────┤
│  session_live               │  cours (existante)            │
│  (Nouvelle table)           │  chapitre (existante)         │
└─────────────────────────────────────────────────────────────┘
```

### Flux de données

1. **Création de session (Enseignant)**
   ```
   BackSessionLiveController → SessionLiveService → SessionLiveRepository → MySQL
   ```

2. **Affichage des sessions (Étudiant)**
   ```
   FrontSessionLiveController → SessionLiveService → SessionLiveRepository → MySQL
   ```

3. **Redirection vers plateforme externe**
   ```
   FrontSessionLiveController → Desktop.browse() → Navigateur externe
   ```

### Intégration avec l'existant

Le module réutilise les composants existants :
- **CourseManagementRepository** : Pour l'association avec les cours
- **Styles CSS** : Cohérence visuelle avec `educompus.css`
- **Patterns de validation** : Réutilisation de `FormValidator` et `ValidationResult`
- **Gestion des erreurs** : Utilisation de `Dialogs.error()` existant

## Composants et Interfaces

### Modèle de données

#### SessionLive (Nouveau modèle)

```java
public final class SessionLive {
    private int id;
    private String titre;
    private String description;
    private String lienSession;        // URL vers la plateforme externe
    private String plateforme;         // "Google Meet", "Zoom", etc.
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private SessionStatut statut;      // PROGRAMMEE, EN_COURS, TERMINEE, ANNULEE
    private int coursId;               // Association avec cours existant
    private int enseignantId;          // ID de l'enseignant créateur
    private String coursTitre;         // Dénormalisé pour l'affichage
    private String enseignantNom;      // Dénormalisé pour l'affichage
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Getters/Setters...
}
```

#### SessionStatut (Énumération)

```java
public enum SessionStatut {
    PROGRAMMEE("Programmée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");
    
    private final String libelle;
    // Constructor et getter...
}
```

### Services

#### SessionLiveService

```java
public final class SessionLiveService {
    private final SessionLiveRepository repository;
    private final CourseManagementRepository coursRepository;
    
    // Opérations CRUD
    public void creer(SessionLive session);
    public void modifier(SessionLive session);
    public void supprimer(int sessionId);
    public List<SessionLive> listerTous(String recherche);
    public List<SessionLive> listerParCours(int coursId);
    public List<SessionLive> listerParStatut(SessionStatut statut);
    
    // Logique métier
    public void demarrerSession(int sessionId);
    public void terminerSession(int sessionId);
    public boolean peutRejoindre(SessionLive session);
    public void valider(SessionLive session);
    
    // Gestion des plateformes
    public List<String> getPlateformesAutorisees();
    public boolean estPlateformeAutorisee(String url);
}
```

#### SessionLiveValidationService

```java
public final class SessionLiveValidationService {
    public static ValidationResult validateSession(SessionLive session);
    public static ValidationResult validateTitre(String titre);
    public static ValidationResult validateLienSession(String lien);
    public static ValidationResult validatePlateforme(String plateforme);
    public static ValidationResult validateDates(LocalDateTime debut, LocalDateTime fin);
    public static ValidationResult validateCoursAssociation(int coursId);
    
    // Validation des URLs de plateformes
    public static boolean isValidGoogleMeetUrl(String url);
    public static boolean isValidZoomUrl(String url);
    public static String sanitizeUrl(String url);
}
```

### Contrôleurs

#### BackSessionLiveController (Interface enseignant)

```java
public final class BackSessionLiveController {
    private final SessionLiveService service;
    private final ObservableList<SessionLive> sessionItems;
    
    @FXML private ListView<SessionLive> sessionListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<SessionStatut> statutFilter;
    @FXML private ComboBox<String> coursFilter;
    
    // Actions CRUD
    @FXML private void createSession();
    @FXML private void editSession();
    @FXML private void deleteSession();
    @FXML private void duplicateSession();
    
    // Gestion des sessions
    @FXML private void startSession();
    @FXML private void endSession();
    @FXML private void testSessionLink();
    
    // Filtres et recherche
    @FXML private void refreshSessions();
    private void applyFilters();
}
```

#### FrontSessionLiveController (Interface étudiant)

```java
public final class FrontSessionLiveController {
    private final SessionLiveService service;
    
    @FXML private VBox sessionsContainer;
    @FXML private Label noSessionsLabel;
    
    // Affichage des sessions
    public void loadSessionsForCourse(int coursId);
    private HBox buildSessionCard(SessionLive session);
    private void joinSession(SessionLive session);
    
    // Gestion des erreurs
    private void handleJoinError(Exception e, SessionLive session);
    private void showSessionUnavailable(SessionLive session);
}
```

### Repository

#### SessionLiveRepository

```java
public final class SessionLiveRepository {
    private final DatabaseConnection connection;
    
    // CRUD de base
    public void create(SessionLive session);
    public SessionLive findById(int id);
    public List<SessionLive> findAll(String searchTerm);
    public void update(SessionLive session);
    public void delete(int id);
    
    // Requêtes spécialisées
    public List<SessionLive> findByCourseId(int coursId);
    public List<SessionLive> findByStatus(SessionStatut statut);
    public List<SessionLive> findActiveSessionsForStudent();
    public List<SessionLive> findByDateRange(LocalDateTime debut, LocalDateTime fin);
    
    // Statistiques
    public int countSessionsByStatus(SessionStatut statut);
    public int countSessionsByCourse(int coursId);
}
```

## Modèles de données

### Schéma de base de données

#### Table session_live

```sql
CREATE TABLE session_live (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    lien_session VARCHAR(500) NOT NULL,
    plateforme VARCHAR(50) NOT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    statut ENUM('PROGRAMMEE', 'EN_COURS', 'TERMINEE', 'ANNULEE') DEFAULT 'PROGRAMMEE',
    cours_id INT NOT NULL,
    enseignant_id INT NOT NULL,
    cours_titre VARCHAR(255), -- Dénormalisé pour performance
    enseignant_nom VARCHAR(255), -- Dénormalisé pour performance
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE,
    INDEX idx_cours_id (cours_id),
    INDEX idx_statut (statut),
    INDEX idx_date_debut (date_debut),
    INDEX idx_enseignant_id (enseignant_id)
);
```

#### Contraintes et règles métier

- **Titre** : Obligatoire, 3-255 caractères
- **Lien session** : URL valide d'une plateforme autorisée
- **Dates** : Date de fin > Date de début, Date de début >= maintenant (pour nouvelles sessions)
- **Plateforme** : Doit être dans la liste des plateformes autorisées
- **Cours** : Doit exister dans la table cours
- **Statut** : Transitions contrôlées (PROGRAMMEE → EN_COURS → TERMINEE)

### Relations avec l'existant

```
cours (1) ←→ (N) session_live
```

La session live est toujours associée à un cours existant, permettant :
- Affichage des sessions dans le détail du cours
- Filtrage des sessions par cours
- Suppression en cascade si le cours est supprimé

## Correctness Properties

*Une propriété est une caractéristique ou un comportement qui doit être vrai pour toutes les exécutions valides d'un système - essentiellement, une déclaration formelle sur ce que le système doit faire. Les propriétés servent de pont entre les spécifications lisibles par l'homme et les garanties de correction vérifiables par machine.*

### Property 1: Validation d'URL universelle

*Pour toute* URL fournie comme lien de session, la validation doit correctement identifier si l'URL est valide et provient d'une plateforme autorisée

**Validates: Requirements 1.5, 5.4, 5.5**

### Property 2: Affichage complet des informations de session

*Pour toute* session live, l'interface doit afficher toutes les informations de base requises (titre, horaire, enseignant)

**Validates: Requirements 2.5**

### Property 3: Bouton de rejoindre universel

*Pour toute* session live active, l'interface doit fournir un bouton "Rejoindre session" accessible

**Validates: Requirements 3.1**

### Property 4: Gestion d'erreur pour liens invalides

*Pour tout* lien de session invalide, l'application doit afficher un message d'erreur explicite et approprié

**Validates: Requirements 3.4**

### Property 5: Association cours obligatoire

*Pour toute* session live créée, elle doit être associée à un cours existant et valide

**Validates: Requirements 6.2**

### Property 6: Affichage du statut universel

*Pour toute* session live, l'interface doit afficher correctement son statut actuel (programmée, en cours, terminée)

**Validates: Requirements 7.2**

### Property 7: Responsivité de l'interface

*Pour toute* taille d'écran dans la plage supportée, l'interface des sessions doit s'adapter et rester utilisable

**Validates: Requirements 7.5**

### Property 8: Logging des tentatives de connexion

*Pour toute* tentative de connexion à une session, l'événement doit être enregistré dans les logs pour le débogage

**Validates: Requirements 8.4**

### Property 9: Gestion d'erreur de connexion

*Pour tout* échec de connexion à une plateforme externe, l'application doit afficher un message d'erreur descriptif approprié au type d'erreur

**Validates: Requirements 8.1**

## Gestion des erreurs

### Stratégie de gestion d'erreurs

#### Validation des données

```java
public class SessionLiveValidationService {
    public static ValidationResult validateLienSession(String lien) {
        ValidationResult result = new ValidationResult();
        
        if (lien == null || lien.trim().isEmpty()) {
            result.addError("Le lien de session est obligatoire");
            return result;
        }
        
        // Validation format URL
        try {
            URL url = new URL(lien);
            if (!url.getProtocol().equals("https")) {
                result.addError("Le lien doit utiliser HTTPS");
            }
        } catch (MalformedURLException e) {
            result.addError("Format d'URL invalide");
            return result;
        }
        
        // Validation plateforme autorisée
        if (!isPlateformeAutorisee(lien)) {
            result.addError("Plateforme non autorisée. Utilisez Google Meet, Zoom ou Teams");
        }
        
        return result;
    }
    
    private static boolean isPlateformeAutorisee(String url) {
        return url.contains("meet.google.com") || 
               url.contains("zoom.us") || 
               url.contains("teams.microsoft.com");
    }
}
```

#### Gestion des erreurs de connexion

```java
public class SessionLiveService {
    public void rejoindreSession(SessionLive session) {
        try {
            // Vérifications préalables
            if (session.getStatut() != SessionStatut.EN_COURS) {
                throw new SessionNotActiveException("La session n'est pas active");
            }
            
            if (!isLienValide(session.getLienSession())) {
                throw new InvalidSessionLinkException("Lien de session invalide ou expiré");
            }
            
            // Tentative d'ouverture
            Desktop.getDesktop().browse(URI.create(session.getLienSession()));
            
            // Log de la tentative
            logger.info("Connexion à la session {} par l'utilisateur {}", 
                       session.getId(), getCurrentUserId());
                       
        } catch (IOException e) {
            logger.error("Erreur ouverture navigateur pour session {}", session.getId(), e);
            throw new BrowserOpenException("Impossible d'ouvrir le navigateur", e);
        } catch (UnsupportedOperationException e) {
            logger.error("Ouverture navigateur non supportée", e);
            throw new BrowserNotSupportedException("Ouverture automatique non supportée", e);
        }
    }
}
```

#### Messages d'erreur utilisateur

```java
public class SessionLiveErrorHandler {
    public static void handleJoinError(Exception e, SessionLive session, Window parent) {
        String title = "Erreur de connexion";
        String message;
        String details = null;
        
        if (e instanceof SessionNotActiveException) {
            message = "Cette session n'est pas encore active ou est terminée.";
            details = "Vérifiez l'horaire de la session ou contactez l'enseignant.";
        } else if (e instanceof InvalidSessionLinkException) {
            message = "Le lien de session est invalide ou a expiré.";
            details = "Contactez l'enseignant pour obtenir un nouveau lien.";
        } else if (e instanceof BrowserOpenException) {
            message = "Impossible d'ouvrir automatiquement le navigateur.";
            details = "Copiez le lien manuellement : " + session.getLienSession();
        } else {
            message = "Une erreur inattendue s'est produite.";
            details = "Réessayez ou contactez le support technique.";
        }
        
        Dialogs.error(title, message, details, parent);
    }
}
```

### Types d'exceptions personnalisées

```java
public class SessionNotActiveException extends RuntimeException {
    public SessionNotActiveException(String message) { super(message); }
}

public class InvalidSessionLinkException extends RuntimeException {
    public InvalidSessionLinkException(String message) { super(message); }
}

public class BrowserOpenException extends RuntimeException {
    public BrowserOpenException(String message, Throwable cause) { super(message, cause); }
}

public class BrowserNotSupportedException extends RuntimeException {
    public BrowserNotSupportedException(String message, Throwable cause) { super(message, cause); }
}
```

## Stratégie de test

### Approche de test double

Le module utilise une approche de test combinant :
- **Tests unitaires** : Pour les exemples spécifiques et cas d'erreur
- **Tests basés sur les propriétés** : Pour les propriétés universelles identifiées
- **Tests d'intégration** : Pour les interactions avec les plateformes externes

### Tests unitaires

#### Tests de validation

```java
@Test
void testValidationLienSessionValide() {
    // Arrange
    String lienValide = "https://meet.google.com/abc-defg-hij";
    
    // Act
    ValidationResult result = SessionLiveValidationService.validateLienSession(lienValide);
    
    // Assert
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
}

@Test
void testValidationLienSessionInvalide() {
    // Arrange
    String lienInvalide = "http://malicious-site.com/fake-meeting";
    
    // Act
    ValidationResult result = SessionLiveValidationService.validateLienSession(lienInvalide);
    
    // Assert
    assertFalse(result.isValid());
    assertTrue(result.getErrors().contains("Plateforme non autorisée"));
}
```

#### Tests de service

```java
@Test
void testCreationSessionAvecCoursValide() {
    // Arrange
    SessionLive session = new SessionLive();
    session.setTitre("Session Test");
    session.setCoursId(1); // Cours existant
    session.setLienSession("https://meet.google.com/test");
    
    // Act & Assert
    assertDoesNotThrow(() -> service.creer(session));
    verify(repository).create(session);
}

@Test
void testSuppressionSessionInexistante() {
    // Arrange
    int sessionId = 999;
    when(repository.findById(sessionId)).thenReturn(null);
    
    // Act & Assert
    assertThrows(SessionNotFoundException.class, 
                () -> service.supprimer(sessionId));
}
```

### Tests basés sur les propriétés

Configuration avec JQwik pour Java :

```java
@Property
@Label("Feature: interaction-session-live, Property 1: Validation d'URL universelle")
void validateUrlProperty(@ForAll("validUrls") String url) {
    // Pour toute URL valide d'une plateforme autorisée
    ValidationResult result = SessionLiveValidationService.validateLienSession(url);
    
    // La validation doit réussir
    assertTrue(result.isValid(), 
              "URL valide doit passer la validation: " + url);
}

@Property
@Label("Feature: interaction-session-live, Property 4: Gestion d'erreur pour liens invalides")
void invalidUrlErrorProperty(@ForAll("invalidUrls") String url) {
    // Pour tout lien invalide
    ValidationResult result = SessionLiveValidationService.validateLienSession(url);
    
    // Un message d'erreur approprié doit être fourni
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());
    assertTrue(result.getErrors().stream()
              .anyMatch(error -> error.contains("invalide") || 
                               error.contains("autorisée") || 
                               error.contains("format")));
}

@Provide
Arbitrary<String> validUrls() {
    return Arbitraries.oneOf(
        Arbitraries.strings().withCharRange('a', 'z')
                   .withLengthRange(5, 15)
                   .map(s -> "https://meet.google.com/" + s),
        Arbitraries.strings().withCharRange('0', '9')
                   .withLengthRange(9, 11)
                   .map(s -> "https://zoom.us/j/" + s)
    );
}

@Provide
Arbitrary<String> invalidUrls() {
    return Arbitraries.oneOf(
        Arbitraries.strings().withCharRange('a', 'z')
                   .map(s -> "http://malicious.com/" + s), // HTTP au lieu de HTTPS
        Arbitraries.strings().withCharRange('a', 'z')
                   .map(s -> "https://unknown-platform.com/" + s), // Plateforme non autorisée
        Arbitraries.just("not-a-url"), // Format invalide
        Arbitraries.just("") // URL vide
    );
}
```

### Tests d'intégration

```java
@Test
void testIntegrationAvecPlateformeExterne() {
    // Test avec une vraie URL de test Google Meet
    String testUrl = "https://meet.google.com/test-room";
    
    // Vérifier que l'URL est accessible (sans rejoindre)
    assertDoesNotThrow(() -> {
        HttpURLConnection connection = (HttpURLConnection) 
            new URL(testUrl).openConnection();
        connection.setRequestMethod("HEAD");
        int responseCode = connection.getResponseCode();
        assertTrue(responseCode < 400, "URL doit être accessible");
    });
}
```

### Configuration des tests

#### Minimum 100 itérations pour les tests de propriétés

```java
@Property(tries = 100)
@Label("Feature: interaction-session-live, Property 2: Affichage complet des informations")
void sessionDisplayProperty(@ForAll("sessions") SessionLive session) {
    // Configuration pour 100 itérations minimum
    HBox sessionCard = controller.buildSessionCard(session);
    
    // Vérifier que toutes les informations requises sont présentes
    assertTrue(containsText(sessionCard, session.getTitre()));
    assertTrue(containsText(sessionCard, session.getEnseignantNom()));
    assertTrue(containsDateInfo(sessionCard, session.getDateDebut()));
}
```

#### Couverture de test

- **Tests unitaires** : 80%+ de couverture du code
- **Tests de propriétés** : Toutes les propriétés identifiées
- **Tests d'intégration** : Scénarios critiques de bout en bout
- **Tests de régression** : Cas d'erreur connus

La stratégie de test garantit la robustesse du module tout en maintenant des temps d'exécution raisonnables grâce à l'utilisation judicieuse de mocks pour les dépendances externes.