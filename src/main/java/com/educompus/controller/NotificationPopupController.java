package com.educompus.controller;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.service.JcefBrowserService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Contrôleur pour les popups de notification des sessions live.
 * Gère les interactions utilisateur avec les notifications automatiques.
 */
public final class NotificationPopupController {
    private static final Logger logger = Logger.getLogger(NotificationPopupController.class.getName());
    
    @FXML private VBox notificationContainer;
    @FXML private Label titleLabel;
    @FXML private Label courseLabel;
    @FXML private Label teacherLabel;
    @FXML private Label timeLabel;
    @FXML private Label messageLabel;
    @FXML private Button joinButton;
    @FXML private Button laterButton;
    @FXML private Button closeButton;
    
    private SessionLive session;
    private NotificationType notificationType;
    private Stage popupStage;
    private Timeline autoCloseTimeline;
    
    /**
     * Initialise le contrôleur après le chargement du FXML.
     */
    @FXML
    private void initialize() {
        // Configuration initiale des boutons
        if (joinButton != null) {
            joinButton.setOnAction(e -> handleJoinSession());
        }
        if (laterButton != null) {
            laterButton.setOnAction(e -> handleLater());
        }
        if (closeButton != null) {
            closeButton.setOnAction(e -> handleClose());
        }
    }
    
    /**
     * Configure la notification avec les données de session.
     * 
     * @param session Session concernée
     * @param type Type de notification
     * @param stage Stage du popup
     */
    public void setupNotification(SessionLive session, NotificationType type, Stage stage) {
        this.session = session;
        this.notificationType = type;
        this.popupStage = stage;
        
        updateUI();
        setupAutoClose();
        applyStyles();
    }
    
    /**
     * Met à jour l'interface utilisateur avec les données de session.
     */
    private void updateUI() {
        if (session == null) return;
        
        // Titre selon le type de notification
        if (titleLabel != null) {
            String title = switch (notificationType) {
                case THIRTY_MINUTES -> "Session dans 30 minutes";
                case FIVE_MINUTES -> "⚠️ Session dans 5 minutes !";
            };
            titleLabel.setText(title);
        }
        
        // Informations de la session
        if (courseLabel != null) {
            courseLabel.setText("📚 " + (session.getCoursTitre() != null ? session.getCoursTitre() : session.getNomCours()));
        }
        
        if (teacherLabel != null) {
            String enseignant = session.getEnseignantNom() != null ? session.getEnseignantNom() : "Enseignant";
            teacherLabel.setText("👨‍🏫 " + enseignant);
        }
        
        if (timeLabel != null) {
            String timeText = session.getDateDebut() != null ? 
                session.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")) :
                session.getDateHeureFormatee();
            timeLabel.setText("🕐 " + timeText);
        }
        
        // Message selon le type
        if (messageLabel != null) {
            String message = switch (notificationType) {
                case THIRTY_MINUTES -> "Préparez-vous pour la session. Vous recevrez une autre notification 5 minutes avant le début.";
                case FIVE_MINUTES -> "La session commence bientôt ! Cliquez sur 'Rejoindre maintenant' pour accéder directement à la session.";
            };
            messageLabel.setText(message);
        }
        
        // Configuration des boutons selon le type
        if (notificationType == NotificationType.THIRTY_MINUTES) {
            // Pour les notifications 30min, masquer le bouton "Plus tard"
            if (laterButton != null) {
                laterButton.setVisible(false);
                laterButton.setManaged(false);
            }
            if (joinButton != null) {
                joinButton.setText("OK");
            }
        } else {
            // Pour les notifications 5min, afficher les deux boutons
            if (laterButton != null) {
                laterButton.setVisible(true);
                laterButton.setManaged(true);
            }
            if (joinButton != null) {
                joinButton.setText("🔗 Rejoindre maintenant");
            }
        }
    }
    
    /**
     * Configure la fermeture automatique du popup.
     */
    private void setupAutoClose() {
        if (autoCloseTimeline != null) {
            autoCloseTimeline.stop();
        }
        
        // Durée d'affichage selon le type
        int seconds = switch (notificationType) {
            case THIRTY_MINUTES -> 10; // 10 secondes pour les notifications info
            case FIVE_MINUTES -> 0;    // Pas de fermeture auto pour les notifications urgentes
        };
        
        if (seconds > 0) {
            autoCloseTimeline = new Timeline(new KeyFrame(
                Duration.seconds(seconds),
                e -> handleClose()
            ));
            autoCloseTimeline.play();
        }
    }
    
    /**
     * Applique les styles CSS selon le type de notification.
     */
    private void applyStyles() {
        if (notificationContainer == null) return;
        
        // Supprimer les anciennes classes
        notificationContainer.getStyleClass().removeAll("notification-info", "notification-urgent");
        
        // Ajouter la classe appropriée
        String styleClass = switch (notificationType) {
            case THIRTY_MINUTES -> "notification-info";
            case FIVE_MINUTES -> "notification-urgent";
        };
        notificationContainer.getStyleClass().add(styleClass);
    }
    
    /**
     * Gère l'action "Rejoindre la session".
     */
    @FXML
    private void handleJoinSession() {
        if (session == null) {
            handleClose();
            return;
        }
        
        try {
            String lien = session.getLienSession() != null ? session.getLienSession() : session.getLien();
            
            if (lien == null || lien.trim().isEmpty()) {
                showError("Lien de session manquant", "Le lien de la session n'est pas disponible.");
                return;
            }
            
            JcefBrowserService.getInstance().openMeetingDialog("Session - " + safe(session.getNomCours()), lien);
            
            logger.info(String.format("Session %d ouverte depuis la notification popup", session.getId()));
            
            // Fermer le popup après ouverture réussie
            handleClose();
            
        } catch (Exception e) {
            logger.warning(String.format("Impossible d'ouvrir la session %d: %s", session.getId(), e.getMessage()));
            
            // Fallback: copier le lien dans le presse-papier
            copyLinkToClipboard();
            
            showError("Ouverture automatique impossible", 
                     "Le lien a été copié dans le presse-papier.\n" +
                     "Collez-le dans votre navigateur pour rejoindre la session.");
        }
    }
    
    /**
     * Gère l'action "Plus tard" (snooze).
     */
    @FXML
    private void handleLater() {
        // Pour l'instant, fermer simplement le popup
        // Dans une version future, on pourrait implémenter un vrai snooze
        logger.info("Notification reportée par l'utilisateur");
        handleClose();
    }
    
    /**
     * Gère la fermeture du popup.
     */
    @FXML
    private void handleClose() {
        try {
            if (autoCloseTimeline != null) {
                autoCloseTimeline.stop();
            }
            
            if (popupStage != null) {
                popupStage.close();
            }
            
        } catch (Exception e) {
            logger.warning("Erreur lors de la fermeture du popup: " + e.getMessage());
        }
    }
    
    /**
     * Copie le lien de session dans le presse-papier.
     */
    private void copyLinkToClipboard() {
        try {
            if (session == null) return;
            
            String lien = session.getLienSession() != null ? session.getLienSession() : session.getLien();
            if (lien == null || lien.trim().isEmpty()) return;
            
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(lien);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            
            logger.info("Lien de session copié dans le presse-papier");
            
        } catch (Exception e) {
            logger.warning("Impossible de copier le lien dans le presse-papier: " + e.getMessage());
        }
    }
    
    /**
     * Affiche un message d'erreur à l'utilisateur.
     * 
     * @param title Titre de l'erreur
     * @param message Message d'erreur
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR
                );
                alert.setTitle(title);
                alert.setHeaderText("Erreur");
                alert.setContentText(message);
                alert.showAndWait();
                
            } catch (Exception e) {
                logger.severe("Erreur lors de l'affichage du message d'erreur: " + e.getMessage());
            }
        });
    }
    
    /**
     * Méthode utilitaire pour tester le popup.
     * 
     * @param session Session de test
     * @param type Type de notification
     */
    public static void showTestNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            try {
                // Créer un popup simple pour les tests
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
                );
                
                String title = switch (type) {
                    case THIRTY_MINUTES -> "Test - Session dans 30 minutes";
                    case FIVE_MINUTES -> "Test - Session dans 5 minutes";
                };
                
                alert.setTitle(title);
                alert.setHeaderText("Test de notification");
                alert.setContentText(String.format(
                    "Session: %s\nHeure: %s\nType: %s",
                    session.getTitre(),
                    session.getDateHeureFormatee(),
                    type.getDescription()
                ));
                
                alert.showAndWait();
                
            } catch (Exception e) {
                Logger.getLogger(NotificationPopupController.class.getName())
                      .severe("Erreur lors du test de notification: " + e.getMessage());
            }
        });
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
