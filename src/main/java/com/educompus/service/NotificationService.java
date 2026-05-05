package com.educompus.service;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.repository.NotificationRepository;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.util.Duration;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Service de gestion des notifications pour les sessions live.
 * Gère l'affichage des popups JavaFX avec différenciation visuelle.
 */
public final class NotificationService {
    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());
    private final NotificationRepository repository;
    
    /**
     * Constructeur par défaut.
     */
    public NotificationService() {
        this.repository = new NotificationRepository();
    }
    
    /**
     * Constructeur avec injection de dépendance.
     * 
     * @param repository Repository des notifications
     */
    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Envoie une notification d'information (30 minutes avant).
     * 
     * @param session Session concernée
     * @param type Type de notification
     */
    public void sendInfoNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Session Live - Rappel");
                alert.setHeaderText("Session dans 30 minutes");
                alert.setContentText(buildNotificationMessage(session, type));
                
                applyNotificationStyles(alert);
                alert.getDialogPane().getStyleClass().add("notification-info");
                
                // Affichage non-bloquant
                alert.show();
                
                // Auto-fermeture après 10 secondes
                Timeline autoClose = new Timeline(new KeyFrame(
                    Duration.seconds(10), 
                    e -> alert.close()
                ));
                autoClose.play();
                
                logger.info(String.format("Notification info envoyée pour la session %d", session.getId()));
                
            } catch (Exception e) {
                logger.severe("Erreur lors de l'affichage de la notification info: " + e.getMessage());
                handleDisplayError(e, session, type);
            }
        });
    }
    
    /**
     * Envoie une notification urgente (5 minutes avant).
     * 
     * @param session Session concernée
     * @param type Type de notification
     */
    public void sendUrgentNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Session Live - URGENT");
                alert.setHeaderText("Session dans 5 minutes !");
                alert.setContentText(buildNotificationMessage(session, type));
                
                applyNotificationStyles(alert);
                alert.getDialogPane().getStyleClass().add("notification-urgent");
                
                // Boutons d'action
                ButtonType joinButton = new ButtonType("Rejoindre maintenant");
                ButtonType laterButton = new ButtonType("Plus tard");
                alert.getButtonTypes().setAll(joinButton, laterButton);
                
                // Gestion des actions
                alert.showAndWait().ifPresent(response -> {
                    if (response == joinButton) {
                        joinSessionDirectly(session);
                    }
                });
                
                logger.info(String.format("Notification urgente envoyée pour la session %d", session.getId()));
                
            } catch (Exception e) {
                logger.severe("Erreur lors de l'affichage de la notification urgente: " + e.getMessage());
                handleDisplayError(e, session, type);
            }
        });
    }
    
    /**
     * Envoie une notification de rattrapage pour les notifications manquées.
     * 
     * @param session Session concernée
     * @param type Type de notification manquée
     */
    public void sendCatchupNotification(SessionLive session, NotificationType type) {
        Platform.runLater(() -> {
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Session Live - Rattrapage");
                alert.setHeaderText("Notification manquée");
                alert.setContentText(buildCatchupMessage(session, type));
                
                applyNotificationStyles(alert);
                alert.getDialogPane().getStyleClass().add("notification-info");
                
                // Bouton pour rejoindre
                ButtonType joinButton = new ButtonType("Rejoindre");
                ButtonType closeButton = new ButtonType("Fermer");
                alert.getButtonTypes().setAll(joinButton, closeButton);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == joinButton) {
                        joinSessionDirectly(session);
                    }
                });
                
                logger.info(String.format("Notification de rattrapage envoyée pour la session %d", session.getId()));
                
            } catch (Exception e) {
                logger.severe("Erreur lors de l'affichage de la notification de rattrapage: " + e.getMessage());
            }
        });
    }
    
    /**
     * Construit le message de notification.
     * 
     * @param session Session concernée
     * @param type Type de notification
     * @return Message formaté
     */
    public String buildNotificationMessage(SessionLive session, NotificationType type) {
        StringBuilder message = new StringBuilder();
        message.append("Cours: ").append(session.getCoursTitre()).append("\n");
        message.append("Titre: ").append(session.getTitre()).append("\n");
        message.append("Enseignant: ").append(session.getEnseignantNom()).append("\n");
        message.append("Heure: ").append(session.getDateDebut().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))).append("\n");
        
        if (type == NotificationType.FIVE_MINUTES) {
            message.append("\n⚠️ La session commence bientôt !");
            message.append("\nCliquez sur 'Rejoindre maintenant' pour accéder directement à la session.");
        } else {
            message.append("\nPréparez-vous pour la session.");
            message.append("\nVous recevrez une autre notification 5 minutes avant le début.");
        }
        
        return message.toString();
    }
    
    /**
     * Construit le message de notification de rattrapage.
     * 
     * @param session Session concernée
     * @param type Type de notification manquée
     * @return Message formaté
     */
    private String buildCatchupMessage(SessionLive session, NotificationType type) {
        StringBuilder message = new StringBuilder();
        message.append("Une notification a été manquée pour:\n\n");
        message.append("Cours: ").append(session.getCoursTitre()).append("\n");
        message.append("Titre: ").append(session.getTitre()).append("\n");
        message.append("Enseignant: ").append(session.getEnseignantNom()).append("\n");
        message.append("Heure: ").append(session.getDateDebut().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))).append("\n");
        
        message.append("\nNotification manquée: ").append(type.getDescription());
        
        if (session.getDateDebut().isAfter(java.time.LocalDateTime.now())) {
            message.append("\n\nLa session n'a pas encore commencé.");
        }
        
        return message.toString();
    }
    
    /**
     * Rejoint directement la session depuis la notification.
     * 
     * @param session Session à rejoindre
     */
    public void joinSessionDirectly(SessionLive session) {
        try {
            if (session.getLienSession() == null || session.getLienSession().trim().isEmpty()) {
                showErrorAlert("Lien de session manquant", 
                             "Le lien de la session n'est pas disponible.");
                return;
            }
            
            JcefBrowserService.getInstance().openMeetingDialog("Session - " + safe(session.getNomCours()), session.getLienSession());
            
            logger.info(String.format("Session %d ouverte depuis la notification", session.getId()));
            
        } catch (Exception e) {
            logger.warning(String.format("Impossible d'ouvrir la session %d: %s", 
                                       session.getId(), e.getMessage()));
            
            // Fallback: copier le lien dans le presse-papier
            copyLinkToClipboard(session.getLienSession());
            
            showErrorAlert("Ouverture automatique impossible", 
                         "Le lien a été copié dans le presse-papier.\n" +
                         "Collez-le dans votre navigateur pour rejoindre la session.\n\n" +
                         "Lien: " + session.getLienSession());
        }
    }
    
    /**
     * Copie un lien dans le presse-papier.
     * 
     * @param link Lien à copier
     */
    private void copyLinkToClipboard(String link) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(link);
            Clipboard.getSystemClipboard().setContent(content);
            
            logger.info("Lien copié dans le presse-papier: " + link);
        } catch (Exception e) {
            logger.warning("Impossible de copier le lien dans le presse-papier: " + e.getMessage());
        }
    }
    
    /**
     * Affiche une alerte d'erreur.
     * 
     * @param title Titre de l'alerte
     * @param message Message d'erreur
     */
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Erreur");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void applyNotificationStyles(Alert alert) {
        if (alert == null || alert.getDialogPane() == null) {
            return;
        }
        try {
            var resource = getClass().getResource("/styles/educompus.css");
            if (resource != null) {
                alert.getDialogPane().getStylesheets().add(resource.toExternalForm());
                return;
            }
            File css = new File("styles/educompus.css");
            if (!css.exists()) {
                css = new File("eduCompus-javafx/styles/educompus.css");
            }
            if (!css.exists()) {
                css = new File(new File("..", "eduCompus-javafx"), "styles/educompus.css");
            }
            if (css.exists()) {
                alert.getDialogPane().getStylesheets().add(css.toURI().toString());
            }
        } catch (Exception ignored) {
        }
    }
    
    /**
     * Gère les erreurs d'affichage de notification.
     * 
     * @param e Exception survenue
     * @param session Session concernée
     * @param type Type de notification
     */
    private void handleDisplayError(Exception e, SessionLive session, NotificationType type) {
        logger.severe(String.format("Échec d'affichage de notification pour session %d: %s", 
                                   session.getId(), e.getMessage()));
        
        // Tentative de fallback vers notification système
        try {
            if (java.awt.SystemTray.isSupported()) {
                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
                java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(
                    java.awt.Toolkit.getDefaultToolkit().createImage("icon.png")
                );
                
                tray.add(trayIcon);
                trayIcon.displayMessage(
                    "Session Live - Rappel",
                    buildNotificationMessage(session, type),
                    java.awt.TrayIcon.MessageType.INFO
                );
                
                logger.info("Notification système utilisée comme fallback");
            }
        } catch (Exception fallbackException) {
            logger.severe("Échec du fallback notification système: " + fallbackException.getMessage());
            
            // Dernier recours : log détaillé pour debug
            logger.warning(String.format("NOTIFICATION MANQUÉE - Session: %s à %s", 
                                       session.getTitre(), session.getDateDebut()));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
