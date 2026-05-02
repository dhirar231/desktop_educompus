package com.educompus.service;

import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;
import com.educompus.repository.NotificationRepository;
import com.educompus.repository.SessionLiveRepository;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Service de planification automatique des notifications pour les sessions live.
 * Utilise JavaFX Timeline pour l'exécution en arrière-plan sans bloquer l'UI.
 */
public final class NotificationSchedulerService {
    private static final Logger logger = Logger.getLogger(NotificationSchedulerService.class.getName());
    
    private final NotificationService notificationService;
    private final SessionLiveRepository sessionRepository;
    private final NotificationRepository notificationRepository;
    
    private Timeline schedulerTimeline;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    // Configuration
    private static final int CHECK_INTERVAL_MINUTES = 1; // Vérification chaque minute
    private static final int NOTIFICATION_WINDOW_MINUTES = 2; // Fenêtre d'envoi de 2 minutes
    
    /**
     * Constructeur avec injection de dépendances.
     * 
     * @param notificationService Service de notification
     * @param sessionRepository Repository des sessions
     * @param notificationRepository Repository des notifications
     */
    public NotificationSchedulerService(NotificationService notificationService,
                                      SessionLiveRepository sessionRepository,
                                      NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.sessionRepository = sessionRepository;
        this.notificationRepository = notificationRepository;
    }
    
    /**
     * Démarre le planificateur de notifications en arrière-plan.
     */
    public void start() {
        if (isRunning.get()) {
            logger.warning("Le planificateur de notifications est déjà en cours d'exécution");
            return;
        }
        
        try {
            schedulerTimeline = new Timeline(new KeyFrame(
                Duration.minutes(CHECK_INTERVAL_MINUTES),
                e -> checkPendingNotifications()
            ));
            schedulerTimeline.setCycleCount(Timeline.INDEFINITE);
            schedulerTimeline.play();
            
            isRunning.set(true);
            logger.info("Planificateur de notifications démarré (vérification toutes les " + 
                       CHECK_INTERVAL_MINUTES + " minute(s))");
            
            // Vérification immédiate au démarrage
            checkPendingNotifications();
            
        } catch (Exception e) {
            logger.severe("Erreur lors du démarrage du planificateur: " + e.getMessage());
            isRunning.set(false);
        }
    }
    
    /**
     * Arrête le planificateur de notifications.
     */
    public void stop() {
        if (!isRunning.get()) {
            logger.info("Le planificateur de notifications n'est pas en cours d'exécution");
            return;
        }
        
        try {
            if (schedulerTimeline != null) {
                schedulerTimeline.stop();
                schedulerTimeline = null;
            }
            
            isRunning.set(false);
            logger.info("Planificateur de notifications arrêté");
            
        } catch (Exception e) {
            logger.severe("Erreur lors de l'arrêt du planificateur: " + e.getMessage());
        }
    }
    
    /**
     * Redémarre le planificateur (arrêt puis démarrage).
     */
    public void restart() {
        logger.info("Redémarrage du planificateur de notifications");
        stop();
        start();
    }
    
    /**
     * Vérifie si le planificateur est en cours d'exécution.
     * 
     * @return true si le planificateur est actif
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Vérifie et déclenche les notifications dues.
     * Méthode principale appelée périodiquement par le Timeline.
     */
    private void checkPendingNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // Rechercher les sessions à venir dans la prochaine heure
            List<SessionLive> upcomingSessions = sessionRepository.findUpcomingSessions(
                now, now.plusHours(1)
            );
            
            logger.fine(String.format("Vérification des notifications: %d sessions à venir", 
                                    upcomingSessions.size()));
            
            // Traiter chaque session
            for (SessionLive session : upcomingSessions) {
                processSessionNotifications(session, now);
            }
            
            // Nettoyage périodique (une fois par heure)
            if (now.getMinute() == 0) {
                performPeriodicCleanup();
            }
            
        } catch (Exception e) {
            logger.severe("Erreur lors de la vérification des notifications: " + e.getMessage());
        }
    }
    
    /**
     * Traite les notifications pour une session donnée.
     * 
     * @param session Session à traiter
     * @param now Heure actuelle
     */
    private void processSessionNotifications(SessionLive session, LocalDateTime now) {
        try {
            // Notification 30 minutes avant
            LocalDateTime thirtyMinBefore = session.getDateDebut().minusMinutes(30);
            if (shouldSendNotification(session.getId(), NotificationType.THIRTY_MINUTES, thirtyMinBefore, now)) {
                notificationService.sendInfoNotification(session, NotificationType.THIRTY_MINUTES);
                notificationRepository.markNotificationSent(session.getId(), NotificationType.THIRTY_MINUTES);
                
                logger.info(String.format("Notification 30min envoyée pour session %d: %s", 
                                         session.getId(), session.getTitre()));
            }
            
            // Notification 5 minutes avant (urgente)
            LocalDateTime fiveMinBefore = session.getDateDebut().minusMinutes(5);
            if (shouldSendNotification(session.getId(), NotificationType.FIVE_MINUTES, fiveMinBefore, now)) {
                notificationService.sendUrgentNotification(session, NotificationType.FIVE_MINUTES);
                notificationRepository.markNotificationSent(session.getId(), NotificationType.FIVE_MINUTES);
                
                logger.info(String.format("Notification 5min envoyée pour session %d: %s", 
                                         session.getId(), session.getTitre()));
            }
            
        } catch (Exception e) {
            logger.severe(String.format("Erreur lors du traitement des notifications pour session %d: %s", 
                                       session.getId(), e.getMessage()));
        }
    }
    
    /**
     * Détermine si une notification doit être envoyée.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     * @param scheduledTime Heure programmée
     * @param now Heure actuelle
     * @return true si la notification doit être envoyée
     */
    public boolean shouldSendNotification(int sessionId, NotificationType type, 
                                        LocalDateTime scheduledTime, LocalDateTime now) {
        try {
            // Vérifier si c'est le bon moment (dans la fenêtre d'envoi)
            boolean isTimeToSend = now.isAfter(scheduledTime) && 
                                  now.isBefore(scheduledTime.plusMinutes(NOTIFICATION_WINDOW_MINUTES));
            
            // Vérifier si pas déjà envoyée
            boolean notAlreadySent = !notificationRepository.isNotificationSent(sessionId, type);
            
            boolean shouldSend = isTimeToSend && notAlreadySent;
            
            if (shouldSend) {
                logger.fine(String.format("Notification à envoyer: session=%d, type=%s, programmée=%s", 
                                         sessionId, type.getCode(), scheduledTime));
            }
            
            return shouldSend;
            
        } catch (Exception e) {
            logger.warning(String.format("Erreur lors de la vérification d'envoi: session=%d, type=%s - %s", 
                                        sessionId, type.getCode(), e.getMessage()));
            return false; // En cas d'erreur, ne pas envoyer pour éviter les doublons
        }
    }
    
    /**
     * Force la vérification immédiate des notifications.
     * Utile pour les tests ou après modification de sessions.
     */
    public void forceCheck() {
        logger.info("Vérification forcée des notifications");
        checkPendingNotifications();
    }
    
    /**
     * Planifie les notifications pour une nouvelle session.
     * 
     * @param session Nouvelle session
     */
    public void scheduleNotificationsForSession(SessionLive session) {
        try {
            notificationRepository.createNotificationStatesForSession(session);
            logger.info(String.format("Notifications planifiées pour la session %d: %s", 
                                     session.getId(), session.getTitre()));
        } catch (Exception e) {
            logger.severe(String.format("Erreur lors de la planification pour session %d: %s", 
                                       session.getId(), e.getMessage()));
        }
    }
    
    /**
     * Met à jour les notifications pour une session modifiée.
     * 
     * @param session Session modifiée
     */
    public void updateNotificationsForSession(SessionLive session) {
        try {
            notificationRepository.updateNotificationTimesForSession(session);
            logger.info(String.format("Notifications mises à jour pour la session %d: %s", 
                                     session.getId(), session.getTitre()));
        } catch (Exception e) {
            logger.severe(String.format("Erreur lors de la mise à jour pour session %d: %s", 
                                       session.getId(), e.getMessage()));
        }
    }
    
    /**
     * Supprime les notifications pour une session supprimée.
     * 
     * @param sessionId ID de la session supprimée
     */
    public void removeNotificationsForSession(int sessionId) {
        try {
            notificationRepository.deleteNotificationStatesForSession(sessionId);
            logger.info(String.format("Notifications supprimées pour la session %d", sessionId));
        } catch (Exception e) {
            logger.severe(String.format("Erreur lors de la suppression pour session %d: %s", 
                                       sessionId, e.getMessage()));
        }
    }
    
    /**
     * Effectue un nettoyage périodique des anciennes données.
     */
    private void performPeriodicCleanup() {
        try {
            int cleaned = notificationRepository.cleanupOldNotificationStates();
            if (cleaned > 0) {
                logger.info(String.format("Nettoyage périodique: %d anciens états supprimés", cleaned));
            }
        } catch (Exception e) {
            logger.warning("Erreur lors du nettoyage périodique: " + e.getMessage());
        }
    }
    
    /**
     * Récupère les statistiques du planificateur.
     * 
     * @return Chaîne avec les statistiques
     */
    public String getStatistics() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<SessionLive> upcomingSessions = sessionRepository.findUpcomingSessions(
                now, now.plusDays(1)
            );
            
            long notificationsDues = upcomingSessions.stream()
                .mapToLong(session -> {
                    long count = 0;
                    if (shouldSendNotification(session.getId(), NotificationType.THIRTY_MINUTES, 
                                             session.getDateDebut().minusMinutes(30), now)) {
                        count++;
                    }
                    if (shouldSendNotification(session.getId(), NotificationType.FIVE_MINUTES, 
                                             session.getDateDebut().minusMinutes(5), now)) {
                        count++;
                    }
                    return count;
                })
                .sum();
            
            return String.format("Planificateur: %s | Sessions à venir (24h): %d | Notifications dues: %d", 
                               isRunning.get() ? "ACTIF" : "ARRÊTÉ", 
                               upcomingSessions.size(), 
                               notificationsDues);
                               
        } catch (Exception e) {
            return "Erreur lors du calcul des statistiques: " + e.getMessage();
        }
    }
}