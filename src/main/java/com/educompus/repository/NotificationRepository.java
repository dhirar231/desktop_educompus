package com.educompus.repository;

import com.educompus.model.AppNotification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class NotificationRepository {
    private static volatile boolean schemaReady = false;

    public NotificationRepository() {
        ensureSchema();
    }

    public void createProjectSubmissionNotifications(int projectId, int studentId, String studentEmail, String projectTitle) {
        ensureSchema();
        String recipientSql = """
                SELECT DISTINCT id
                FROM `user`
                WHERE roles LIKE '%ROLE_ADMIN%'
                   OR roles LIKE '%ROLE_TEACHER%'
                """;
        String insertSql = """
                INSERT INTO app_notification
                    (recipient_user_id, type, title, message, source_entity_type, source_entity_id, is_read, created_at)
                VALUES
                    (?, ?, ?, ?, ?, ?, 0, NOW())
                """;
        String safeProjectTitle = safe(projectTitle);
        String safeStudentEmail = safe(studentEmail);
        String title = "Nouvelle soumission projet";
        String message = safeStudentEmail.isBlank()
                ? ("Un etudiant a soumis le projet \"" + safeProjectTitle + "\".")
                : (safeStudentEmail + " a soumis le projet \"" + safeProjectTitle + "\".");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement recipientsPs = conn.prepareStatement(recipientSql);
             ResultSet rs = recipientsPs.executeQuery();
             PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            while (rs.next()) {
                int recipientId = rs.getInt("id");
                if (recipientId <= 0 || recipientId == studentId) {
                    continue;
                }
                insertPs.setInt(1, recipientId);
                insertPs.setString(2, "PROJECT_SUBMISSION");
                insertPs.setString(3, title);
                insertPs.setString(4, message);
                insertPs.setString(5, "project");
                insertPs.setInt(6, projectId);
                insertPs.addBatch();
            }
            insertPs.executeBatch();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create notifications: " + safeMsg(e), e);
        }
    }

    public List<AppNotification> listRecentForUser(int userId, int limit) {
        ensureSchema();
        int uid = Math.max(0, userId);
        if (uid <= 0) {
            return List.of();
        }
        int max = limit <= 0 ? 10 : Math.min(limit, 50);
        String sql = """
                SELECT id,
                       recipient_user_id,
                       type,
                       title,
                       message,
                       source_entity_type,
                       source_entity_id,
                       is_read,
                       created_at
                FROM app_notification
                WHERE recipient_user_id = ?
                ORDER BY is_read ASC, created_at DESC, id DESC
                LIMIT ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.setInt(2, max);
            try (ResultSet rs = ps.executeQuery()) {
                List<AppNotification> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list notifications: " + safeMsg(e), e);
        }
    }

    public int countUnreadForUser(int userId) {
        ensureSchema();
        int uid = Math.max(0, userId);
        if (uid <= 0) {
            return 0;
        }
        String sql = "SELECT COUNT(*) FROM app_notification WHERE recipient_user_id = ? AND is_read = 0";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, uid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count unread notifications: " + safeMsg(e), e);
        }
    }

    public void markAllAsRead(int userId) {
        ensureSchema();
        int uid = Math.max(0, userId);
        if (uid <= 0) {
            return;
        }
        String sql = "UPDATE app_notification SET is_read = 1 WHERE recipient_user_id = ? AND is_read = 0";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, uid);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to mark notifications as read: " + safeMsg(e), e);
        }
    }

    private static AppNotification map(ResultSet rs) throws Exception {
        AppNotification notification = new AppNotification();
        notification.setId(rs.getInt("id"));
        notification.setRecipientUserId(rs.getInt("recipient_user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setSourceEntityType(rs.getString("source_entity_type"));
        notification.setSourceEntityId(rs.getInt("source_entity_id"));
        notification.setRead(rs.getBoolean("is_read"));
        notification.setCreatedAt(rs.getString("created_at"));
        return notification;
    }

    private static void ensureSchema() {
        if (schemaReady) {
            return;
        }
        synchronized (NotificationRepository.class) {
            if (schemaReady) {
                return;
            }
            String sql = """
                    CREATE TABLE IF NOT EXISTS app_notification (
                        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        recipient_user_id INT NOT NULL,
                        type VARCHAR(80) NOT NULL,
                        title VARCHAR(160) NOT NULL,
                        message VARCHAR(500) NOT NULL,
                        source_entity_type VARCHAR(80) NOT NULL,
                        source_entity_id INT NOT NULL DEFAULT 0,
                        is_read TINYINT(1) NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_app_notification_recipient_read_created (recipient_user_id, is_read, created_at),
                        CONSTRAINT fk_app_notification_user
                            FOREIGN KEY (recipient_user_id) REFERENCES `user`(id)
                            ON DELETE CASCADE
                    )
                    """;
            try (Connection conn = EducompusDB.getConnection();
                 Statement st = conn.createStatement()) {
                st.execute(sql);
                schemaReady = true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize notification table: " + safeMsg(e), e);
            }
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeMsg(Exception e) {
        if (e == null) {
            return "unknown error";
        }
        String msg = String.valueOf(e.getMessage());
        msg = msg.replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 180) {
            msg = msg.substring(0, 180) + "...";
        }
        return msg;
    }
}
