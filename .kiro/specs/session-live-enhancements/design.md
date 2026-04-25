# Document de Conception Technique - Session Live Enhancements

## Vue d'ensemble

Le module "Session Live Enhancements" étend les fonctionnalités existantes du système de sessions live EduCompus avec un focus principal sur le **système de notifications automatiques**. Cette conception se concentre sur l'implémentation d'un système de notifications en arrière-plan qui complète les notifications Google Calendar existantes, en fournissant des alertes locales dans l'application JavaFX.

### Objectifs principaux

- **Système de notifications dual** : Complément aux notifications Google Calendar avec des alertes locales JavaFX
- **Notifications temporisées** : Alertes automatiques à 30 minutes et 5 minutes avant le début des sessions
- **Interface non-bloquante** : Exécution en arrière-plan sans impact sur l'interface utilisateur
- **Prévention des doublons** : Mécanisme robuste pour éviter les notifications multiples pour la même session
- **Intégration transparente** : Extension du module Live Session existant sans modification majeure

### Contraintes techniques

- Intégration avec l'infrastructure Live Session existante
- Utilisation du système de tâches en arrière-plan JavaFX (Timeline/ScheduledExecutorService)
- Notifications via JavaFX Alert/Notification API
- Persistance des états de notification dans MySQL
- Maintien de la cohérence avec l'architecture MVC existante

## Architecture

### Architecture générale du système de notifications

```
┌─────────────────────────────────────────────────────────────┐
│                    Couche Présentation                      │
├─────────────────────────────────────────────────────────────┤
│  NotificationPopupController   │  SessionLiveController     │
│  (Gestion des popups)          │  (Interface existante)     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Couche Service                           │
├─────────────────────────────────────────────────────────────┤
│  NotificationSchedulerService  │  NotificationService       │
│  (Planification automatique)   │  (Gestion des notifications)│
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Couche Données                           │
├─────────────────────────────────────────────────────────────┤
│  NotificationRepository        │  SessionLiveRepository     │
│  (États des notifications)     │  (Sessions existantes)     │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Base de Données MySQL                    │
├─────────────────────────────────────────────────────────────┤
│  notification_state            │  session_live (existante)  │
│  (Nouvelle table)              │                            │
└─────────────────────────────────────────────────────────────┘
```

### Flux de notifications

1. **Initialisation du planificateur**
   ```
   Application Startup → NotificationSchedulerService.start() → Timeline.play()
   ```

2. **Vérification périodique**
   ```
   Timeline (1 minute) → checkPendingNotifications() → NotificationRepository.findDue()
   ```

3. **Déclenchement de notification**
   ```
   NotificationService.trigger() → JavaFX Alert → NotificationRepository.markSent()
   ```

4. **Prévention des doublons**
   ```
   NotificationRepository.isAlreadySent() → Skip ou Send → Update state
   ```

### Intégration avec l'existant

Le système s'intègre avec les composants existants :
- **SessionLiveRepository** : Lecture des sessions programmées
- **SessionLive** : Modèle de données existant (pas de modification)
- **JavaFX Timeline** : Planification des tâches périodiques
- **Alert API** : Affichage des notifications popup

## Composants et Interfaces

### Services de notification

#### NotificationSchedulerService (Nouveau service principal)

```java
@Component
public final class NotificationSchedulerService {
    private final NotificationService notificationService;
    private final SessionLiveRepository sessionRepository;
    private final NotificationRepository notificationRepository;
    private Timeline schedulerTimeline;
    private boolean isRunning = false;
    
    /**
     * Démarre le planificateur de notifications en arrière-plan
     */
    public void start() {
        if (isRunning) return;
        
        schedulerTimeline = new Timeline(new KeyFrame(
            Duration.minutes(1), // Vérification chaque minute
            e -> checkPendingNotifications()
        ));
        schedulerTimeline.setCycleCount(Timeline.INDEFINITE);
        schedulerTimeline.play();
        isRunning = true;
        
        logger.info("Notification scheduler started");
    }
    
    /**
     * Arrête le planificateur
     */
    public void stop() {
        if (schedulerTimeline != null) {
            schedulerTimeline.stop();
        }
        isRunning = false;
        logger.info("Notification scheduler stopped");
    }
    
    /**
     * Vérifie et déclenche les notifications dues
     */
    private void checkPendingNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<SessionLive> upcomingSessions = sessionRepository.findUpcomingSessions(
                now, now.plusHours(1) // Sessions dans la prochaine heure
            );
            
            for (SessionLive session : upcomingSessions) {
                processSessionNotifications(session, now);
            }
        } catch (Exception e) {
            logger.error("Error checking pending notifications", e);
        }
    }
    
    /**
     * Traite les notifications pour une session donnée
     */
    private void processSessionNotifications(SessionLive session, LocalDateTime now) {
        // Notification 30 minutes avant
        LocalDateTime thirtyMinBefore = session.getDateDebut().minusMinutes(30);
        if (shouldSendNotification(session.getId(), NotificationType.THIRTY_MINUTES, thirtyMinBefore, now)) {
            notificationService.sendInfoNotification(session, NotificationType.THIRTY_MINUTES);
            notificationRepository.markNotificationSent(session.getId(), NotificationType.THIRTY_MINUTES);
        }
        
        // Notification 5 minutes avant (urgente)
        LocalDateTime fiveMinBefore = session.getDateDebut().minusMinutes(5);
        if (shouldSendNotification(session.getId(), NotificationType.FIVE_MINUTES, fiveMinBefore, now)) {
            notificationService.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
            notificationRepository.markNotificationSent(session.getId(), NotificationType.FIVE_MINUTES);
        }
    }
    
    /**
     * Détermine si une notification doit être envoyée
     */
    private boolean shouldSendNotification(int sessionId, NotificationType type, 
                                         LocalDateTime scheduledTime, LocalDateTime now) {
        // Vérifier si c'est le bon moment (dans une fenêtre de 2 minutes)
        boolean isTimeToSend = now.isAfter(scheduledTime) && 
                              now.isBefore(scheduledTime.plusMinutes(2));
        
        // Vérifier si pas déjà envoyée
        boolean notAlreadySent = !notificationRepository.isNotificationSent(sessionId, type);
        
        return isTimeToSend && notAlreadySent;
    }
}
```

#### NotificationService (Nouveau service)

```java
@Component
public final class NotificationService {
    private final NotificationRepository repository;
    
    /**
     * Envoie une notification d'information (30 minutes avant)
     */
    public void sendInfoNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Live - Rappel");
            alert.setHeaderText("Session dans 30 minutes");
            alert.setContentText(buildNotificationMessage(session, type));
            
            // Configuration de l'apparence
            alert.getDialogPane().getStylesheets().add("/styles/educompus.css");
            alert.getDialogPane().getStyleClass().add("notification-info");
            
            // Affichage non-bloquant
            alert.show();
            
            // Auto-fermeture après 10 secondes
            Timeline autoClose = new Timeline(new KeyFrame(
                Duration.seconds(10), 
                e -> alert.close()
            ));
            autoClose.play();
            
            logger.info("Info notification sent for session {}", session.getId());
        });
    }
    
    /**
     * Envoie une notification urgente (5 minutes avant)
     */
    public void sendUrgentNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Session Live - URGENT");
            alert.setHeaderText("Session dans 5 minutes !");
            alert.setContentText(buildNotificationMessage(session, type));
            
            // Configuration urgente
            alert.getDialogPane().getStylesheets().add("/styles/educompus.css");
            alert.getDialogPane().getStyleClass().add("notification-urgent");
            
            // Bouton pour rejoindre directement
            ButtonType joinButton = new ButtonType("Rejoindre maintenant");
            ButtonType laterButton = new ButtonType("Plus tard");
            alert.getButtonTypes().setAll(joinButton, laterButton);
            
            // Gestion des actions
            alert.showAndWait().ifPresent(response -> {
                if (response == joinButton) {
                    joinSessionDirectly(session);
                }
            });
            
            logger.info("Urgent notification sent for session {}", session.getId());
        });
    }
    
    /**
     * Construit le message de notification
     */
    private String buildNotificationMessage(SessionLive session, NotificationType type) {
        StringBuilder message = new StringBuilder();
        message.append("Cours: ").append(session.getCoursTitre()).append("\n");
        message.append("Enseignant: ").append(session.getEnseignantNom()).append("\n");
        message.append("Heure: ").append(session.getDateDebut().format(
            DateTimeFormatter.ofPattern("HH:mm"))).append("\n");
        
        if (type == NotificationType.FIVE_MINUTES) {
            message.append("\n⚠️ La session commence bientôt !");
        } else {
            message.append("\nPréparez-vous pour la session.");
        }
        
        return message.toString();
    }
    
    /**
     * Rejoint directement la session depuis la notification
     */
    private void joinSessionDirectly(SessionLive session) {
        try {
            Desktop.getDesktop().browse(URI.create(session.getLienSession()));
        } catch (Exception e) {
            logger.error("Failed to open session link from notification", e);
            // Fallback: copier le lien dans le presse-papier
            Clipboard.getSystemClipboard().setContent(
                Collections.singletonMap(DataFormat.PLAIN_TEXT, session.getLienSession())
            );
        }
    }
}
```

### Modèles de données

#### NotificationType (Nouvelle énumération)

```java
public enum NotificationType {
    THIRTY_MINUTES("30min", "30 minutes avant"),
    FIVE_MINUTES("5min", "5 minutes avant");
    
    private final String code;
    private final String description;
    
    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() { return code; }
    public String getDescription() { return description; }
}
```

#### NotificationState (Nouveau modèle)

```java
public final class NotificationState {
    private int id;
    private int sessionId;
    private NotificationType type;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private boolean sent;
    private LocalDateTime createdAt;
    
    // Constructeurs
    public NotificationState() {}
    
    public NotificationState(int sessionId, NotificationType type, LocalDateTime scheduledTime) {
        this.sessionId = sessionId;
        this.type = type;
        this.scheduledTime = scheduledTime;
        this.sent = false;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters et setters...
}
```

### Repository

#### NotificationRepository (Nouveau repository)

```java
@Repository
public final class NotificationRepository {
    private final DatabaseConnection connection;
    
    /**
     * Vérifie si une notification a déjà été envoyée
     */
    public boolean isNotificationSent(int sessionId, NotificationType type) {
        String sql = "SELECT sent FROM notification_state WHERE session_id = ? AND type = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("sent");
        } catch (SQLException e) {
            logger.error("Error checking notification state", e);
            return false; // En cas d'erreur, permettre l'envoi
        }
    }
    
    /**
     * Marque une notification comme envoyée
     */
    public void markNotificationSent(int sessionId, NotificationType type) {
        String sql = """
            INSERT INTO notification_state (session_id, type, scheduled_time, sent_time, sent, created_at)
            VALUES (?, ?, ?, ?, true, ?)
            ON DUPLICATE KEY UPDATE sent_time = ?, sent = true
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            stmt.setTimestamp(3, Timestamp.valueOf(calculateScheduledTime(sessionId, type)));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error marking notification as sent", e);
        }
    }
    
    /**
     * Crée les états de notification pour une nouvelle session
     */
    public void createNotificationStatesForSession(SessionLive session) {
        LocalDateTime sessionStart = session.getDateDebut();
        
        // État pour notification 30 minutes avant
        createNotificationState(session.getId(), NotificationType.THIRTY_MINUTES, 
                               sessionStart.minusMinutes(30));
        
        // État pour notification 5 minutes avant
        createNotificationState(session.getId(), NotificationType.FIVE_MINUTES, 
                               sessionStart.minusMinutes(5));
    }
    
    /**
     * Crée un état de notification
     */
    private void createNotificationState(int sessionId, NotificationType type, 
                                       LocalDateTime scheduledTime) {
        String sql = """
            INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
            VALUES (?, ?, ?, false, ?)
            ON DUPLICATE KEY UPDATE scheduled_time = ?
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            stmt.setTimestamp(3, Timestamp.valueOf(scheduledTime));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(scheduledTime));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error creating notification state", e);
        }
    }
    
    /**
     * Nettoie les anciens états de notification (plus de 7 jours)
     */
    public void cleanupOldNotificationStates() {
        String sql = "DELETE FROM notification_state WHERE created_at < ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(7)));
            int deleted = stmt.executeUpdate();
            logger.info("Cleaned up {} old notification states", deleted);
        } catch (SQLException e) {
            logger.error("Error cleaning up notification states", e);
        }
    }
}
```

### Extension du SessionLiveRepository

```java
// Ajout de méthodes au repository existant
public final class SessionLiveRepository {
    // ... méthodes existantes ...
    
    /**
     * Trouve les sessions à venir dans une plage de temps donnée
     */
    public List<SessionLive> findUpcomingSessions(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT * FROM session_live 
            WHERE date_debut BETWEEN ? AND ? 
            AND statut = 'PROGRAMMEE'
            ORDER BY date_debut ASC
            """;
        
        List<SessionLive> sessions = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(from));
            stmt.setTimestamp(2, Timestamp.valueOf(to));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(mapResultSetToSessionLive(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding upcoming sessions", e);
        }
        
        return sessions;
    }
}
```

## Modèles de données

### Schéma de base de données

#### Table notification_state (Nouvelle table)

```sql
CREATE TABLE notification_state (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    type VARCHAR(20) NOT NULL, -- '30min' ou '5min'
    scheduled_time DATETIME NOT NULL, -- Heure prévue d'envoi
    sent_time DATETIME NULL, -- Heure réelle d'envoi
    sent BOOLEAN DEFAULT FALSE, -- État d'envoi
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,
    UNIQUE KEY unique_session_type (session_id, type),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_sent (sent),
    INDEX idx_session_id (session_id)
);
```

#### Contraintes et règles métier

- **session_id** : Doit référencer une session existante
- **type** : Uniquement '30min' ou '5min'
- **scheduled_time** : Calculé automatiquement (date_debut - 30min ou 5min)
- **sent** : Booléen pour éviter les doublons
- **Clé unique** : (session_id, type) pour éviter les doublons d'états

### Relations avec l'existant

```
session_live (1) ←→ (N) notification_state
```

Chaque session live peut avoir plusieurs états de notification (30min, 5min), mais chaque type est unique par session.

## Correctness Properties

*Une propriété est une caractéristique ou un comportement qui doit être vrai pour toutes les exécutions valides d'un système - essentiellement, une déclaration formelle sur ce que le système doit faire. Les propriétés servent de pont entre les spécifications lisibles par l'homme et les garanties de correction vérifiables par machine.*

### Property 1: Notification automatique universelle

*Pour toute* session programmée, le système doit automatiquement planifier et envoyer des notifications aux moments appropriés (30 minutes et 5 minutes avant le début)

**Validates: Requirements 3.1, 5.2**

### Property 2: Notification d'événements de session

*Pour tout* changement d'état de session (création, modification, annulation), le système doit déclencher les notifications appropriées à tous les participants concernés

**Validates: Requirements 3.2**

### Property 3: Respect des préférences utilisateur

*Pour toute* configuration de préférences de notification définie par un utilisateur, le système doit respecter ces préférences lors de l'envoi des notifications

**Validates: Requirements 3.3, 5.1**

### Property 4: Différenciation par niveau d'urgence

*Pour toute* session avec un niveau d'urgence défini, le système doit adapter le comportement des notifications (fréquence, style, contenu) en fonction de ce niveau

**Validates: Requirements 3.4, 3.5, 5.4**

### Property 5: Gestion sélective des notifications

*Pour toute* session avec des paramètres de notification spécifiques (activées/désactivées), le système doit respecter ces paramètres sans affecter les autres sessions

**Validates: Requirements 3.6**

### Property 6: Historique complet des notifications

*Pour toute* notification envoyée par le système, un enregistrement complet doit être maintenu dans l'historique avec tous les détails pertinents

**Validates: Requirements 3.7**

### Property 7: Mécanisme de fallback email

*Pour tout* scénario où l'utilisateur n'est pas disponible pour recevoir les notifications locales, le système doit activer automatiquement les notifications email de secours

**Validates: Requirements 3.8**

### Property 8: Contenu complet des notifications

*Pour toute* notification de rappel envoyée, elle doit inclure toutes les informations essentielles de la session (titre, lien, horaire) de manière lisible

**Validates: Requirements 5.3**

### Property 9: Fonctionnalité snooze

*Pour toute* notification avec option de report (snooze), le système doit correctement reprogrammer la notification pour le délai spécifié (5 ou 15 minutes)

**Validates: Requirements 5.5**

### Property 10: Gestion d'état des rappels

*Pour tout* rappel depuis lequel l'utilisateur rejoint une session, le système doit marquer automatiquement ce rappel comme traité pour éviter les notifications redondantes

**Validates: Requirements 5.6**

### Property 11: Adaptation aux fuseaux horaires

*Pour tout* utilisateur avec un fuseau horaire configuré, les notifications doivent être planifiées et affichées selon ce fuseau horaire local

**Validates: Requirements 5.7**

## Gestion des erreurs

### Stratégie de gestion d'erreurs pour les notifications

#### Gestion des erreurs de planification

```java
public class NotificationSchedulerService {
    private void handleSchedulingError(Exception e, SessionLive session) {
        logger.error("Failed to schedule notification for session {}", session.getId(), e);
        
        // Tentative de replanification avec délai
        Timeline retryTimeline = new Timeline(new KeyFrame(
            Duration.minutes(5), 
            event -> {
                try {
                    processSessionNotifications(session, LocalDateTime.now());
                } catch (Exception retryException) {
                    logger.error("Retry failed for session {}", session.getId(), retryException);
                    // Marquer comme échec définitif après 3 tentatives
                    notificationRepository.markNotificationFailed(session.getId());
                }
            }
        ));
        retryTimeline.play();
    }
}
```

#### Gestion des erreurs d'affichage

```java
public class NotificationService {
    private void handleDisplayError(Exception e, SessionLive session, NotificationType type) {
        logger.error("Failed to display notification for session {}", session.getId(), e);
        
        // Fallback vers notification système si JavaFX Alert échoue
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon trayIcon = new TrayIcon(loadNotificationIcon());
                trayIcon.displayMessage(
                    "Session Live - Rappel",
                    buildNotificationMessage(session, type),
                    TrayIcon.MessageType.INFO
                );
            }
        } catch (Exception fallbackException) {
            logger.error("System tray fallback failed", fallbackException);
            // Dernier recours : log détaillé pour debug
            logger.warn("NOTIFICATION MISSED - Session: {} at {}", 
                       session.getTitre(), session.getDateDebut());
        }
    }
}
```

#### Types d'exceptions personnalisées

```java
public class NotificationSchedulingException extends RuntimeException {
    public NotificationSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class NotificationDisplayException extends RuntimeException {
    public NotificationDisplayException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class DuplicateNotificationException extends RuntimeException {
    public DuplicateNotificationException(String message) {
        super(message);
    }
}
```

### Mécanismes de récupération

#### Récupération automatique après redémarrage

```java
@Component
public class NotificationRecoveryService {
    
    /**
     * Récupère les notifications manquées après un redémarrage
     */
    public void recoverMissedNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // Trouver les notifications qui auraient dû être envoyées
        List<NotificationState> missedNotifications = 
            notificationRepository.findMissedNotifications(oneHourAgo, now);
        
        for (NotificationState missed : missedNotifications) {
            SessionLive session = sessionRepository.findById(missed.getSessionId());
            if (session != null && session.getDateDebut().isAfter(now)) {
                // Session pas encore commencée, envoyer notification de rattrapage
                notificationService.sendCatchupNotification(session, missed.getType());
                notificationRepository.markNotificationSent(missed.getSessionId(), missed.getType());
            }
        }
    }
}
```

## Stratégie de test

### Approche de test double

Le système de notifications utilise une approche de test combinant :
- **Tests unitaires** : Pour les exemples spécifiques et cas d'erreur
- **Tests basés sur les propriétés** : Pour les propriétés universelles identifiées
- **Tests d'intégration** : Pour les interactions avec JavaFX et la base de données
- **Tests de timing** : Pour vérifier la précision des notifications temporisées

### Tests unitaires

#### Tests de planification

```java
@Test
void testNotificationSchedulingForNewSession() {
    // Arrange
    SessionLive session = createTestSession(LocalDateTime.now().plusHours(1));
    
    // Act
    notificationRepository.createNotificationStatesForSession(session);
    
    // Assert
    assertTrue(notificationRepository.isNotificationScheduled(session.getId(), NotificationType.THIRTY_MINUTES));
    assertTrue(notificationRepository.isNotificationScheduled(session.getId(), NotificationType.FIVE_MINUTES));
}

@Test
void testDuplicateNotificationPrevention() {
    // Arrange
    SessionLive session = createTestSession(LocalDateTime.now().plusMinutes(10));
    notificationRepository.markNotificationSent(session.getId(), NotificationType.FIVE_MINUTES);
    
    // Act
    boolean shouldSend = schedulerService.shouldSendNotification(
        session.getId(), NotificationType.FIVE_MINUTES, 
        LocalDateTime.now().minusMinutes(5), LocalDateTime.now()
    );
    
    // Assert
    assertFalse(shouldSend, "Ne doit pas renvoyer une notification déjà envoyée");
}
```

#### Tests de service

```java
@Test
void testNotificationContentGeneration() {
    // Arrange
    SessionLive session = createTestSession("Cours de Java", "Prof. Martin");
    
    // Act
    String message = notificationService.buildNotificationMessage(session, NotificationType.THIRTY_MINUTES);
    
    // Assert
    assertThat(message).contains("Cours de Java");
    assertThat(message).contains("Prof. Martin");
    assertThat(message).contains("Préparez-vous pour la session");
}

@Test
void testUrgentNotificationBehavior() {
    // Arrange
    SessionLive urgentSession = createTestSession(LocalDateTime.now().plusMinutes(5));
    
    // Act & Assert
    assertDoesNotThrow(() -> notificationService.sendUrgentNotification(urgentSession, NotificationType.FIVE_MINUTES));
    
    // Vérifier que la notification urgente a des caractéristiques spéciales
    verify(alertMock).setAlertType(Alert.AlertType.WARNING);
    verify(alertMock).setHeaderText(contains("URGENT"));
}
```

### Tests basés sur les propriétés

Configuration avec JQwik pour Java :

```java
@Property(tries = 100)
@Label("Feature: session-live-enhancements, Property 1: Notification automatique universelle")
void notificationSchedulingProperty(@ForAll("futureSessions") SessionLive session) {
    // Pour toute session future
    assumeThat(session.getDateDebut()).isAfter(LocalDateTime.now());
    
    // Créer les états de notification
    notificationRepository.createNotificationStatesForSession(session);
    
    // Les notifications doivent être planifiées
    assertTrue(notificationRepository.isNotificationScheduled(session.getId(), NotificationType.THIRTY_MINUTES),
              "Notification 30min doit être planifiée pour session " + session.getId());
    assertTrue(notificationRepository.isNotificationScheduled(session.getId(), NotificationType.FIVE_MINUTES),
              "Notification 5min doit être planifiée pour session " + session.getId());
}

@Property(tries = 100)
@Label("Feature: session-live-enhancements, Property 5: Gestion sélective des notifications")
void selectiveNotificationProperty(@ForAll("sessions") SessionLive session, 
                                 @ForAll boolean notificationsEnabled) {
    // Pour toute session avec paramètres de notification
    session.setNotificationsEnabled(notificationsEnabled);
    
    // Traiter les notifications
    schedulerService.processSessionNotifications(session, LocalDateTime.now());
    
    // Vérifier que les paramètres sont respectés
    boolean notificationSent = notificationRepository.isNotificationSent(session.getId(), NotificationType.THIRTY_MINUTES);
    assertEquals(notificationsEnabled && isTimeForNotification(session), notificationSent,
                "État de notification doit correspondre aux paramètres");
}

@Property(tries = 100)
@Label("Feature: session-live-enhancements, Property 8: Contenu complet des notifications")
void notificationContentProperty(@ForAll("sessions") SessionLive session) {
    // Pour toute session
    String message = notificationService.buildNotificationMessage(session, NotificationType.THIRTY_MINUTES);
    
    // Le message doit contenir toutes les informations essentielles
    assertThat(message).contains(session.getCoursTitre());
    assertThat(message).contains(session.getEnseignantNom());
    assertThat(message).contains(session.getDateDebut().format(DateTimeFormatter.ofPattern("HH:mm")));
}

@Provide
Arbitrary<SessionLive> futureSessions() {
    return Arbitraries.create(() -> {
        SessionLive session = new SessionLive();
        session.setId(Arbitraries.integers().between(1, 1000).sample());
        session.setTitre(Arbitraries.strings().withCharRange('A', 'Z').withLengthRange(5, 50).sample());
        session.setCoursTitre(Arbitraries.strings().withCharRange('A', 'Z').withLengthRange(5, 30).sample());
        session.setEnseignantNom(Arbitraries.strings().withCharRange('A', 'Z').withLengthRange(5, 20).sample());
        
        // Date future aléatoire (entre 1 heure et 7 jours)
        LocalDateTime futureDate = LocalDateTime.now()
            .plusHours(Arbitraries.integers().between(1, 168).sample());
        session.setDateDebut(futureDate);
        session.setDateFin(futureDate.plusHours(1));
        
        session.setStatut(SessionStatut.PROGRAMMEE);
        session.setLienSession("https://meet.google.com/test-" + session.getId());
        
        return session;
    });
}

@Provide
Arbitrary<SessionLive> sessions() {
    return futureSessions(); // Réutilise le générateur de sessions futures
}
```

### Tests d'intégration

#### Tests de timing précis

```java
@Test
void testNotificationTimingAccuracy() throws InterruptedException {
    // Arrange
    LocalDateTime sessionStart = LocalDateTime.now().plusMinutes(31); // 31 minutes dans le futur
    SessionLive session = createTestSession(sessionStart);
    
    // Démarrer le planificateur
    schedulerService.start();
    
    // Attendre que la notification 30min soit due
    Thread.sleep(65000); // Attendre 65 secondes (fenêtre de 2 minutes)
    
    // Assert
    assertTrue(notificationRepository.isNotificationSent(session.getId(), NotificationType.THIRTY_MINUTES),
              "Notification 30min doit être envoyée dans les temps");
}
```

#### Tests de récupération

```java
@Test
void testNotificationRecoveryAfterRestart() {
    // Arrange - Simuler des notifications manquées
    SessionLive session = createTestSession(LocalDateTime.now().plusMinutes(10));
    LocalDateTime missedTime = LocalDateTime.now().minusMinutes(5);
    
    // Créer un état de notification manquée
    NotificationState missedState = new NotificationState(session.getId(), NotificationType.THIRTY_MINUTES, missedTime);
    notificationRepository.save(missedState);
    
    // Act
    recoveryService.recoverMissedNotifications();
    
    // Assert
    assertTrue(notificationRepository.isNotificationSent(session.getId(), NotificationType.THIRTY_MINUTES),
              "Notification manquée doit être récupérée");
}
```

### Configuration des tests

#### Minimum 100 itérations pour les tests de propriétés

```java
@Property(tries = 100)
@Label("Feature: session-live-enhancements, Property 11: Adaptation aux fuseaux horaires")
void timezoneAdaptationProperty(@ForAll("timezones") ZoneId userTimezone, 
                               @ForAll("futureSessions") SessionLive session) {
    // Configuration pour 100 itérations minimum
    UserPreferences prefs = new UserPreferences();
    prefs.setTimezone(userTimezone);
    
    // Calculer l'heure de notification dans le fuseau utilisateur
    LocalDateTime notificationTime = schedulerService.calculateNotificationTime(session, prefs, NotificationType.THIRTY_MINUTES);
    
    // Vérifier que l'heure est correctement adaptée au fuseau
    ZonedDateTime sessionInUserTz = session.getDateDebut().atZone(ZoneId.systemDefault()).withZoneSameInstant(userTimezone);
    ZonedDateTime expectedNotificationTime = sessionInUserTz.minusMinutes(30);
    
    assertEquals(expectedNotificationTime.toLocalDateTime(), notificationTime,
                "Notification doit être planifiée selon le fuseau utilisateur");
}

@Provide
Arbitrary<ZoneId> timezones() {
    return Arbitraries.of(
        ZoneId.of("Europe/Paris"),
        ZoneId.of("America/New_York"), 
        ZoneId.of("Asia/Tokyo"),
        ZoneId.of("UTC"),
        ZoneId.of("America/Los_Angeles")
    );
}
```

#### Couverture de test

- **Tests unitaires** : 85%+ de couverture du code de notification
- **Tests de propriétés** : Toutes les 11 propriétés identifiées
- **Tests d'intégration** : Scénarios critiques de timing et récupération
- **Tests de performance** : Vérification que le planificateur n'impacte pas les performances UI

La stratégie de test garantit la fiabilité du système de notifications tout en maintenant des performances optimales grâce à l'utilisation de mocks pour les composants JavaFX et de bases de données en mémoire pour les tests.

## Intégration avec l'existant

### Modifications minimales requises

#### Extension du SessionLiveService existant

```java
// Ajout de méthodes au service existant
public final class SessionLiveService {
    private final NotificationRepository notificationRepository; // Nouvelle dépendance
    
    // ... méthodes existantes ...
    
    /**
     * Crée une session avec planification automatique des notifications
     */
    public void creer(SessionLive session) {
        // Logique existante
        repository.create(session);
        
        // Nouvelle logique : planifier les notifications
        notificationRepository.createNotificationStatesForSession(session);
        
        logger.info("Session créée avec notifications planifiées: {}", session.getId());
    }
    
    /**
     * Modifie une session et met à jour les notifications si nécessaire
     */
    public void modifier(SessionLive session) {
        SessionLive existing = repository.findById(session.getId());
        
        // Logique existante
        repository.update(session);
        
        // Nouvelle logique : replanifier si les dates ont changé
        if (!existing.getDateDebut().equals(session.getDateDebut())) {
            notificationRepository.updateNotificationTimesForSession(session);
        }
        
        logger.info("Session modifiée avec notifications mises à jour: {}", session.getId());
    }
}
```

#### Initialisation du planificateur dans l'application principale

```java
public class EduCompusApplication extends Application {
    private NotificationSchedulerService notificationScheduler;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialisation existante...
        
        // Nouvelle initialisation : démarrer le planificateur de notifications
        notificationScheduler = new NotificationSchedulerService(
            new NotificationService(new NotificationRepository()),
            new SessionLiveRepository(),
            new NotificationRepository()
        );
        notificationScheduler.start();
        
        logger.info("Application démarrée avec système de notifications");
    }
    
    @Override
    public void stop() throws Exception {
        // Arrêt propre du planificateur
        if (notificationScheduler != null) {
            notificationScheduler.stop();
        }
        
        // Logique d'arrêt existante...
        super.stop();
    }
}
```

### Styles CSS pour les notifications

#### Ajout dans educompus.css

```css
/* Styles pour les notifications d'information (30 minutes avant) */
.notification-info .dialog-pane {
    -fx-background-color: #e3f2fd;
    -fx-border-color: #2196f3;
    -fx-border-width: 2px;
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
}

.notification-info .header-panel {
    -fx-background-color: #2196f3;
    -fx-text-fill: white;
}

.notification-info .header-panel .label {
    -fx-text-fill: white;
    -fx-font-weight: bold;
}

/* Styles pour les notifications urgentes (5 minutes avant) */
.notification-urgent .dialog-pane {
    -fx-background-color: #fff3e0;
    -fx-border-color: #ff9800;
    -fx-border-width: 3px;
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
    -fx-effect: dropshadow(gaussian, rgba(255, 152, 0, 0.4), 10, 0, 0, 0);
}

.notification-urgent .header-panel {
    -fx-background-color: #ff9800;
    -fx-text-fill: white;
}

.notification-urgent .header-panel .label {
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 14px;
}

/* Animation pour les notifications urgentes */
.notification-urgent {
    -fx-effect: dropshadow(gaussian, rgba(255, 152, 0, 0.6), 15, 0, 0, 0);
}

/* Boutons dans les notifications */
.notification-info .button,
.notification-urgent .button {
    -fx-background-radius: 6px;
    -fx-border-radius: 6px;
    -fx-padding: 8px 16px;
    -fx-font-weight: bold;
}

.notification-urgent .button:default {
    -fx-background-color: #ff9800;
    -fx-text-fill: white;
}

.notification-urgent .button:default:hover {
    -fx-background-color: #f57c00;
}
```

### Configuration et déploiement

#### Script de mise à jour de base de données

```sql
-- Migration pour ajouter le support des notifications
-- Fichier: src/main/resources/sql/notification_migration.sql

-- Créer la table des états de notification
CREATE TABLE IF NOT EXISTS notification_state (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    scheduled_time DATETIME NOT NULL,
    sent_time DATETIME NULL,
    sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,
    UNIQUE KEY unique_session_type (session_id, type),
    INDEX idx_scheduled_time (scheduled_time),
    INDEX idx_sent (sent),
    INDEX idx_session_id (session_id)
);

-- Créer les états de notification pour les sessions existantes
INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
SELECT 
    id as session_id,
    '30min' as type,
    DATE_SUB(date_debut, INTERVAL 30 MINUTE) as scheduled_time,
    CASE 
        WHEN date_debut <= NOW() THEN TRUE 
        ELSE FALSE 
    END as sent,
    NOW() as created_at
FROM session_live 
WHERE statut = 'PROGRAMMEE' AND date_debut > NOW()
ON DUPLICATE KEY UPDATE scheduled_time = VALUES(scheduled_time);

INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
SELECT 
    id as session_id,
    '5min' as type,
    DATE_SUB(date_debut, INTERVAL 5 MINUTE) as scheduled_time,
    CASE 
        WHEN date_debut <= NOW() THEN TRUE 
        ELSE FALSE 
    END as sent,
    NOW() as created_at
FROM session_live 
WHERE statut = 'PROGRAMMEE' AND date_debut > NOW()
ON DUPLICATE KEY UPDATE scheduled_time = VALUES(scheduled_time);

-- Ajouter un index pour optimiser les requêtes de nettoyage
CREATE INDEX idx_notification_cleanup ON notification_state (created_at, sent);
```

Cette conception fournit une architecture complète et robuste pour le système de notifications automatiques, s'intégrant harmonieusement avec l'infrastructure existante tout en apportant les fonctionnalités demandées de manière non-intrusive.