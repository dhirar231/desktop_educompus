package com.educompus.repository;

import com.educompus.model.ActionType;
import com.educompus.model.SessionAction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour le journal des actions/événements des sessions live.
 */
public final class SessionActionRepository {

    public SessionActionRepository() {
        ensureSchema();
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    /**
     * Enregistre une action dans le journal.
     */
    public void enregistrer(SessionAction action) {
        String sql = """
                INSERT INTO session_actions (session_id, etudiant_id, nom_etudiant, type, timestamp, details)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, action.getSessionId());
            ps.setInt(2, action.getEtudiantId());
            ps.setString(3, action.getNomEtudiant() != null ? action.getNomEtudiant() : "Système");
            ps.setString(4, action.getType().name());
            ps.setTimestamp(5, action.getTimestamp() != null
                    ? Timestamp.valueOf(action.getTimestamp())
                    : new Timestamp(System.currentTimeMillis()));
            ps.setString(6, action.getDetails());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) action.setId(keys.getInt(1));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer l'action: " + safeMsg(e), e);
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    /** Journal complet d'une session, ordre chronologique. */
    public List<SessionAction> getJournal(int sessionId) {
        return query("""
                SELECT * FROM session_actions
                WHERE session_id = ?
                ORDER BY timestamp ASC, id ASC
                """, sessionId);
    }

    /** Actions récentes d'une session (les N dernières). */
    public List<SessionAction> getActionsRecentes(int sessionId, int limit) {
        String sql = """
                SELECT * FROM session_actions
                WHERE session_id = ?
                ORDER BY timestamp DESC, id DESC
                LIMIT ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionAction> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture actions: " + safeMsg(e), e);
        }
    }

    /** Actions d'un type spécifique pour une session. */
    public List<SessionAction> getParType(int sessionId, ActionType type) {
        String sql = """
                SELECT * FROM session_actions
                WHERE session_id = ? AND type = ?
                ORDER BY timestamp ASC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionAction> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture actions par type: " + safeMsg(e), e);
        }
    }

    /** Actions d'un étudiant dans une session. */
    public List<SessionAction> getParEtudiant(int sessionId, int etudiantId) {
        String sql = """
                SELECT * FROM session_actions
                WHERE session_id = ? AND etudiant_id = ?
                ORDER BY timestamp ASC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionAction> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture actions étudiant: " + safeMsg(e), e);
        }
    }

    /** Nombre d'actions d'un type donné dans une session. */
    public int count(int sessionId, ActionType type) {
        String sql = "SELECT COUNT(*) FROM session_actions WHERE session_id = ? AND type = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private List<SessionAction> query(String sql, int sessionId) {
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionAction> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture journal: " + safeMsg(e), e);
        }
    }

    private static SessionAction map(ResultSet rs) throws Exception {
        SessionAction a = new SessionAction();
        a.setId(rs.getInt("id"));
        a.setSessionId(rs.getInt("session_id"));
        a.setEtudiantId(rs.getInt("etudiant_id"));
        a.setNomEtudiant(rs.getString("nom_etudiant"));
        a.setType(ActionType.fromString(rs.getString("type")));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) a.setTimestamp(ts.toLocalDateTime());
        a.setDetails(rs.getString("details"));
        return a;
    }

    private void ensureSchema() {
        try (Connection conn = EducompusDB.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS session_actions (
                        id           INT AUTO_INCREMENT PRIMARY KEY,
                        session_id   INT          NOT NULL,
                        etudiant_id  INT          NOT NULL DEFAULT 0,
                        nom_etudiant VARCHAR(255) NOT NULL DEFAULT 'Système',
                        type         VARCHAR(32)  NOT NULL,
                        timestamp    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        details      TEXT         NULL,
                        INDEX idx_sa_session   (session_id),
                        INDEX idx_sa_type      (session_id, type),
                        INDEX idx_sa_timestamp (session_id, timestamp)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);
        } catch (Exception ignored) {}
    }

    private static String safeMsg(Exception e) {
        if (e == null) return "erreur inconnue";
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').trim();
        return msg.length() > 200 ? msg.substring(0, 200) + "..." : msg;
    }
}
