package com.educompus.repository;

import com.educompus.model.AppNotification;
import com.educompus.model.AuthUser;
import com.educompus.model.NotificationState;
import com.educompus.model.NotificationType;
import com.educompus.model.SessionLive;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Repository pour la gestion des états de notification des sessions live.
 * Gère la persistance et la prévention des doublons de notifications.
 */
public final class NotificationRepository {
    private static final Logger logger = Logger.getLogger(NotificationRepository.class.getName());
    
    /**
     * Constructeur par défaut.
     */
    public NotificationRepository() {
        ensureAppNotificationTable();
    }

    public int countUnreadForUser(int userId) {
        if (userId <= 0) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM app_notification WHERE recipient_user_id = ? AND is_read = 0";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.warning("Erreur count unread app notifications: " + e.getMessage());
            return 0;
        }
    }

    public List<AppNotification> listRecentForUser(int userId, int limit) {
        if (userId <= 0) {
            return List.of();
        }
        String sql = """
                SELECT id, recipient_user_id, type, title, message, source_entity_type, source_entity_id, is_read, created_at
                FROM app_notification
                WHERE recipient_user_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                """;
        List<AppNotification> out = new ArrayList<>();
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, Math.max(1, Math.min(50, limit)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapAppNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.warning("Erreur list app notifications: " + e.getMessage());
        }
        return out;
    }

    public void markAllAsRead(int userId) {
        if (userId <= 0) {
            return;
        }
        String sql = "UPDATE app_notification SET is_read = 1 WHERE recipient_user_id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Erreur mark app notifications read: " + e.getMessage());
        }
    }

    public void createProjectSubmissionNotifications(int projectId, int studentId, String studentEmail, String projectTitle) {
        String title = "Nouvelle soumission projet";
        String message = safe(studentEmail) + " a soumis le projet \"" + safe(projectTitle) + "\".";
        try {
            AuthUserRepository users = new AuthUserRepository();
            for (AuthUser user : users.findAll()) {
                if (user != null && (user.admin() || user.teacher())) {
                    createAppNotification(user.id(), "PROJECT_SUBMISSION", title, message, "project", projectId);
                }
            }
        } catch (Exception e) {
            logger.warning("Erreur création notifications projet: " + e.getMessage());
        }
    }

    private void createAppNotification(int recipientUserId, String type, String title, String message,
                                       String sourceEntityType, int sourceEntityId) {
        if (recipientUserId <= 0) {
            return;
        }
        String sql = """
                INSERT INTO app_notification
                    (recipient_user_id, type, title, message, source_entity_type, source_entity_id, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, NOW())
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recipientUserId);
            ps.setString(2, safe(type));
            ps.setString(3, safe(title));
            ps.setString(4, safe(message));
            ps.setString(5, safe(sourceEntityType));
            ps.setInt(6, sourceEntityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Erreur insert app notification: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si une notification a déjà été envoyée.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     * @return true si la notification a été envoyée
     */
    public boolean isNotificationSent(int sessionId, NotificationType type) {
        String sql = "SELECT sent FROM notification_state WHERE session_id = ? AND type = ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getBoolean("sent");
        } catch (SQLException e) {
            logger.severe("Erreur lors de la vérification de l'état de notification: " + e.getMessage());
            return false; // En cas d'erreur, permettre l'envoi pour éviter les notifications manquées
        }
    }
    
    /**
     * Vérifie si une notification est planifiée pour une session.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     * @return true si la notification est planifiée
     */
    public boolean isNotificationScheduled(int sessionId, NotificationType type) {
        String sql = "SELECT COUNT(*) FROM notification_state WHERE session_id = ? AND type = ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            logger.severe("Erreur lors de la vérification de planification: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Marque une notification comme envoyée.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     */
    public void markNotificationSent(int sessionId, NotificationType type) {
        String sql = """
            INSERT INTO notification_state (session_id, type, scheduled_time, sent_time, sent, created_at)
            VALUES (?, ?, ?, ?, true, ?)
            ON DUPLICATE KEY UPDATE 
                sent_time = VALUES(sent_time), 
                sent = true
            """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            stmt.setTimestamp(3, Timestamp.valueOf(calculateScheduledTime(sessionId, type)));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.info(String.format("Notification marquée comme envoyée: session=%d, type=%s", 
                                        sessionId, type.getCode()));
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors du marquage de notification envoyée: " + e.getMessage());
        }
    }
    
    /**
     * Crée les états de notification pour une nouvelle session.
     * 
     * @param session Session pour laquelle créer les notifications
     */
    public void createNotificationStatesForSession(SessionLive session) {
        LocalDateTime sessionStart = session.getDateDebut();
        
        // État pour notification 30 minutes avant
        createNotificationState(session.getId(), NotificationType.THIRTY_MINUTES, 
                               sessionStart.minusMinutes(30));
        
        // État pour notification 5 minutes avant
        createNotificationState(session.getId(), NotificationType.FIVE_MINUTES, 
                               sessionStart.minusMinutes(5));
        
        logger.info(String.format("États de notification créés pour la session %d", session.getId()));
    }
    
    /**
     * Met à jour les heures de notification pour une session modifiée.
     * 
     * @param session Session avec nouvelles heures
     */
    public void updateNotificationTimesForSession(SessionLive session) {
        String sql = """
            UPDATE notification_state 
            SET scheduled_time = ?, sent = false, sent_time = null
            WHERE session_id = ? AND type = ? AND sent = false
            """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime sessionStart = session.getDateDebut();
            
            // Mettre à jour notification 30 minutes
            stmt.setTimestamp(1, Timestamp.valueOf(sessionStart.minusMinutes(30)));
            stmt.setInt(2, session.getId());
            stmt.setString(3, NotificationType.THIRTY_MINUTES.getCode());
            stmt.executeUpdate();
            
            // Mettre à jour notification 5 minutes
            stmt.setTimestamp(1, Timestamp.valueOf(sessionStart.minusMinutes(5)));
            stmt.setInt(2, session.getId());
            stmt.setString(3, NotificationType.FIVE_MINUTES.getCode());
            stmt.executeUpdate();
            
            logger.info(String.format("Heures de notification mises à jour pour la session %d", session.getId()));
        } catch (SQLException e) {
            logger.severe("Erreur lors de la mise à jour des heures de notification: " + e.getMessage());
        }
    }
    
    /**
     * Trouve les notifications manquées dans une plage de temps.
     * 
     * @param from Début de la plage
     * @param to Fin de la plage
     * @return Liste des notifications manquées
     */
    public List<NotificationState> findMissedNotifications(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT id, session_id, type, scheduled_time, sent_time, sent, created_at
            FROM notification_state 
            WHERE scheduled_time BETWEEN ? AND ? 
              AND sent = false
            ORDER BY scheduled_time ASC
            """;
        
        List<NotificationState> missedNotifications = new ArrayList<>();
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(from));
            stmt.setTimestamp(2, Timestamp.valueOf(to));
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                missedNotifications.add(mapResultSetToNotificationState(rs));
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la recherche de notifications manquées: " + e.getMessage());
        }
        
        return missedNotifications;
    }
    
    /**
     * Trouve les notifications dues maintenant.
     * 
     * @return Liste des notifications à envoyer
     */
    public List<NotificationState> findDueNotifications() {
        String sql = """
            SELECT id, session_id, type, scheduled_time, sent_time, sent, created_at
            FROM v_notifications_due
            """;
        
        List<NotificationState> dueNotifications = new ArrayList<>();
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                dueNotifications.add(mapResultSetToNotificationState(rs));
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la recherche de notifications dues: " + e.getMessage());
        }
        
        return dueNotifications;
    }
    
    /**
     * Nettoie les anciens états de notification (plus de 7 jours).
     * 
     * @return Nombre d'états supprimés
     */
    public int cleanupOldNotificationStates() {
        String sql = "DELETE FROM notification_state WHERE created_at < ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(7)));
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                logger.info(String.format("Nettoyage terminé: %d anciens états de notification supprimés", deleted));
            }
            
            return deleted;
        } catch (SQLException e) {
            logger.severe("Erreur lors du nettoyage des états de notification: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Supprime tous les états de notification pour une session.
     * 
     * @param sessionId ID de la session
     */
    public void deleteNotificationStatesForSession(int sessionId) {
        String sql = "DELETE FROM notification_state WHERE session_id = ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                logger.info(String.format("États de notification supprimés pour la session %d: %d états", 
                                        sessionId, deleted));
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors de la suppression des états de notification: " + e.getMessage());
        }
    }
    
    /**
     * Crée un état de notification.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     * @param scheduledTime Heure programmée
     */
    private void createNotificationState(int sessionId, NotificationType type, LocalDateTime scheduledTime) {
        String sql = """
            INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
            VALUES (?, ?, ?, false, ?)
            ON DUPLICATE KEY UPDATE 
                scheduled_time = VALUES(scheduled_time),
                sent = false,
                sent_time = null
            """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            stmt.setInt(1, sessionId);
            stmt.setString(2, type.getCode());
            stmt.setTimestamp(3, Timestamp.valueOf(scheduledTime));
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe(String.format("Erreur lors de la création de l'état de notification: session=%d, type=%s - %s", 
                                      sessionId, type.getCode(), e.getMessage()));
        }
    }
    
    /**
     * Calcule l'heure programmée pour une notification.
     * 
     * @param sessionId ID de la session
     * @param type Type de notification
     * @return Heure programmée calculée
     */
    private LocalDateTime calculateScheduledTime(int sessionId, NotificationType type) {
        String sql = "SELECT date, heure FROM session_live WHERE id = ?";
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Convertir date et heure séparées en LocalDateTime
                java.sql.Date sqlDate = rs.getDate("date");
                java.sql.Time sqlTime = rs.getTime("heure");
                
                if (sqlDate != null && sqlTime != null) {
                    LocalDateTime sessionStart = LocalDateTime.of(
                        sqlDate.toLocalDate(), 
                        sqlTime.toLocalTime()
                    );
                    return switch (type) {
                        case THIRTY_MINUTES -> sessionStart.minusMinutes(30);
                        case FIVE_MINUTES -> sessionStart.minusMinutes(5);
                    };
                }
            }
        } catch (SQLException e) {
            logger.severe("Erreur lors du calcul de l'heure programmée: " + e.getMessage());
        }
        
        return LocalDateTime.now(); // Fallback
    }
    
    /**
     * Mappe un ResultSet vers un objet NotificationState.
     * 
     * @param rs ResultSet à mapper
     * @return Objet NotificationState
     * @throws SQLException En cas d'erreur SQL
     */
    private NotificationState mapResultSetToNotificationState(ResultSet rs) throws SQLException {
        NotificationState state = new NotificationState();
        state.setId(rs.getInt("id"));
        state.setSessionId(rs.getInt("session_id"));
        state.setType(NotificationType.fromCode(rs.getString("type")));
        state.setScheduledTime(rs.getTimestamp("scheduled_time").toLocalDateTime());
        
        Timestamp sentTime = rs.getTimestamp("sent_time");
        if (sentTime != null) {
            state.setSentTime(sentTime.toLocalDateTime());
        }
        
        state.setSent(rs.getBoolean("sent"));
        state.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return state;
    }

    private AppNotification mapAppNotification(ResultSet rs) throws SQLException {
        AppNotification notification = new AppNotification();
        notification.setId(rs.getInt("id"));
        notification.setRecipientUserId(rs.getInt("recipient_user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setSourceEntityType(rs.getString("source_entity_type"));
        notification.setSourceEntityId(rs.getInt("source_entity_id"));
        notification.setRead(rs.getBoolean("is_read"));
        Timestamp created = rs.getTimestamp("created_at");
        notification.setCreatedAt(created == null ? "" : created.toString());
        return notification;
    }

    private void ensureAppNotificationTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS app_notification (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    recipient_user_id INT NOT NULL,
                    type VARCHAR(80) NOT NULL,
                    title VARCHAR(180) NOT NULL,
                    message TEXT NOT NULL,
                    source_entity_type VARCHAR(80) NULL,
                    source_entity_id INT NULL,
                    is_read TINYINT(1) NOT NULL DEFAULT 0,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_app_notification_recipient_read (recipient_user_id, is_read),
                    INDEX idx_app_notification_created (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
        try (Connection conn = EducompusDB.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            logger.warning("Erreur création app_notification: " + e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
