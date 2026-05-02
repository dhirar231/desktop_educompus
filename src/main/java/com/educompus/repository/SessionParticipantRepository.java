package com.educompus.repository;

import com.educompus.model.SessionParticipant;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des participants aux sessions live.
 */
public final class SessionParticipantRepository {

    public SessionParticipantRepository() {
        ensureSchema();
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    /**
     * Enregistre l'arrivée d'un participant (JOIN).
     * Si le participant était déjà présent (reconnexion), remet heure_leave à NULL.
     */
    public void join(SessionParticipant p) {
        String sql = """
                INSERT INTO session_participants
                    (session_id, etudiant_id, nom_etudiant, heure_join,
                     main_levee, demande_parole, parole_accordee)
                VALUES (?, ?, ?, ?, 0, 0, 0)
                ON DUPLICATE KEY UPDATE
                    heure_join      = VALUES(heure_join),
                    heure_leave     = NULL,
                    main_levee      = 0,
                    demande_parole  = 0,
                    parole_accordee = 0
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getSessionId());
            ps.setInt(2, p.getEtudiantId());
            ps.setString(3, p.getNomEtudiant());
            ps.setTimestamp(4, Timestamp.valueOf(
                    p.getHeureJoin() != null ? p.getHeureJoin() : LocalDateTime.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer le join: " + safeMsg(e), e);
        }
    }

    /**
     * Enregistre le départ d'un participant (LEAVE).
     */
    public void leave(int sessionId, int etudiantId) {
        String sql = """
                UPDATE session_participants
                SET heure_leave = NOW()
                WHERE session_id = ? AND etudiant_id = ? AND heure_leave IS NULL
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, etudiantId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer le leave: " + safeMsg(e), e);
        }
    }

    /**
     * Met à jour l'état interactif d'un participant (main levée, demande parole, parole accordée).
     */
    public void updateEtatInteractif(int sessionId, int etudiantId,
                                     boolean mainLevee, boolean demandeParole, boolean paroleAccordee) {
        String sql = """
                UPDATE session_participants
                SET main_levee = ?, demande_parole = ?, parole_accordee = ?
                WHERE session_id = ? AND etudiant_id = ? AND heure_leave IS NULL
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, mainLevee);
            ps.setBoolean(2, demandeParole);
            ps.setBoolean(3, paroleAccordee);
            ps.setInt(4, sessionId);
            ps.setInt(5, etudiantId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de mettre à jour l'état: " + safeMsg(e), e);
        }
    }

    /** Marque tous les participants encore présents comme partis (fin de session). */
    public void leaveAll(int sessionId) {
        String sql = """
                UPDATE session_participants
                SET heure_leave = NOW()
                WHERE session_id = ? AND heure_leave IS NULL
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de fermer les participants: " + safeMsg(e), e);
        }
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    /** Participants actuellement présents dans la session. */
    public List<SessionParticipant> getPresents(int sessionId) {
        return query("""
                SELECT * FROM session_participants
                WHERE session_id = ? AND heure_leave IS NULL
                ORDER BY heure_join ASC
                """, sessionId);
    }

    /** Tous les participants (présents + partis) d'une session. */
    public List<SessionParticipant> getTous(int sessionId) {
        return query("""
                SELECT * FROM session_participants
                WHERE session_id = ?
                ORDER BY heure_join ASC
                """, sessionId);
    }

    /** Participants avec la main levée. */
    public List<SessionParticipant> getMainsLevees(int sessionId) {
        return query("""
                SELECT * FROM session_participants
                WHERE session_id = ? AND main_levee = 1 AND heure_leave IS NULL
                ORDER BY heure_join ASC
                """, sessionId);
    }

    /** Participants ayant demandé la parole. */
    public List<SessionParticipant> getDemandesParole(int sessionId) {
        return query("""
                SELECT * FROM session_participants
                WHERE session_id = ? AND demande_parole = 1 AND heure_leave IS NULL
                ORDER BY heure_join ASC
                """, sessionId);
    }

    /** Nombre de participants actuellement présents. */
    public int countPresents(int sessionId) {
        String sql = "SELECT COUNT(*) FROM session_participants WHERE session_id = ? AND heure_leave IS NULL";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** Vérifie si un étudiant est présent dans la session. */
    public boolean estPresent(int sessionId, int etudiantId) {
        String sql = """
                SELECT 1 FROM session_participants
                WHERE session_id = ? AND etudiant_id = ? AND heure_leave IS NULL
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, etudiantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ── Utilitaires privés ────────────────────────────────────────────────────

    private List<SessionParticipant> query(String sql, int sessionId) {
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionParticipant> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture participants: " + safeMsg(e), e);
        }
    }

    private static SessionParticipant map(ResultSet rs) throws Exception {
        SessionParticipant p = new SessionParticipant();
        p.setId(rs.getInt("id"));
        p.setSessionId(rs.getInt("session_id"));
        p.setEtudiantId(rs.getInt("etudiant_id"));
        p.setNomEtudiant(rs.getString("nom_etudiant"));
        Timestamp join = rs.getTimestamp("heure_join");
        if (join != null) p.setHeureJoin(join.toLocalDateTime());
        Timestamp leave = rs.getTimestamp("heure_leave");
        if (leave != null) p.setHeureLeave(leave.toLocalDateTime());
        p.setMainLevee(rs.getBoolean("main_levee"));
        p.setDemandeParole(rs.getBoolean("demande_parole"));
        p.setParoleAccordee(rs.getBoolean("parole_accordee"));
        return p;
    }

    private void ensureSchema() {
        try (Connection conn = EducompusDB.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS session_participants (
                        id              INT AUTO_INCREMENT PRIMARY KEY,
                        session_id      INT          NOT NULL,
                        etudiant_id     INT          NOT NULL,
                        nom_etudiant    VARCHAR(255) NOT NULL,
                        heure_join      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        heure_leave     DATETIME     NULL,
                        main_levee      TINYINT(1)   NOT NULL DEFAULT 0,
                        demande_parole  TINYINT(1)   NOT NULL DEFAULT 0,
                        parole_accordee TINYINT(1)   NOT NULL DEFAULT 0,
                        UNIQUE KEY uq_session_etudiant (session_id, etudiant_id),
                        INDEX idx_sp_session (session_id),
                        INDEX idx_sp_present (session_id, heure_leave)
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
