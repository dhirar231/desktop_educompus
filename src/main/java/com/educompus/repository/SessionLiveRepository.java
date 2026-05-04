package com.educompus.repository;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des sessions live de visioconférence.
 * Gère les opérations CRUD et les requêtes spécialisées pour les sessions live.
 */
public final class SessionLiveRepository {

    public SessionLiveRepository() {
        ensureSessionLiveSchema();
    }

    /**
     * Ajoute une nouvelle session live à la base de données.
     * @param session La session à ajouter
     * @return L'ID généré pour la nouvelle session
     */
    public int ajouterSession(SessionLive session) {
        String sql = """
                INSERT INTO session_live (nom_cours, lien, date, heure, statut, cours_id, google_event_id, date_creation)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, session.getNomCours());
            ps.setString(2, session.getLien());
            ps.setDate(3, session.getDate() != null ? Date.valueOf(session.getDate()) : null);
            ps.setTime(4, session.getHeure() != null ? Time.valueOf(session.getHeure()) : null);
            ps.setString(5, session.getStatut().name());
            ps.setObject(6, session.getCoursId() > 0 ? session.getCoursId() : null);
            ps.setString(7, session.getGoogleEventId());
            
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    session.setId(generatedId);
                    return generatedId;
                }
            }
            return 0;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter la session live: " + safeMsg(e), e);
        }
    }

    /**
     * Modifie une session live existante.
     * @param session La session avec les nouvelles données
     */
    public void modifierSession(SessionLive session) {
        String sql = """
                UPDATE session_live 
                SET nom_cours = ?, lien = ?, date = ?, heure = ?, statut = ?, cours_id = ?,
                    google_event_id = ?, date_modification = NOW()
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, session.getNomCours());
            ps.setString(2, session.getLien());
            ps.setDate(3, session.getDate() != null ? Date.valueOf(session.getDate()) : null);
            ps.setTime(4, session.getHeure() != null ? Time.valueOf(session.getHeure()) : null);
            ps.setString(5, session.getStatut().name());
            ps.setObject(6, session.getCoursId() > 0 ? session.getCoursId() : null); // cours_id optionnel
            ps.setString(7, session.getGoogleEventId());
            ps.setInt(8, session.getId());
            
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("Aucune session trouvée avec l'ID: " + session.getId());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier la session live: " + safeMsg(e), e);
        }
    }

    /**
     * Supprime une session live par son ID.
     * @param id L'ID de la session à supprimer
     */
    public void supprimerSession(int id) {
        executeDelete("DELETE FROM session_live WHERE id = ?", id, "Impossible de supprimer la session live");
    }

    /**
     * Récupère toutes les sessions live.
     * @return Liste de toutes les sessions
     */
    public List<SessionLive> getAllSessions() {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                ORDER BY sl.date DESC, sl.heure DESC, sl.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            List<SessionLive> sessions = new ArrayList<>();
            while (rs.next()) {
                sessions.add(mapSessionLive(rs));
            }
            return sessions;
        } catch (Exception e) {
            System.err.println("[SessionLive] getAllSessions: " + safeMsg(e));
            return new ArrayList<>();
        }
    }

    /**
     * Récupère une session live par son ID.
     * @param id L'ID de la session recherchée
     * @return La session trouvée, ou null si non trouvée
     */
    public SessionLive getSessionById(int id) {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE sl.id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSessionLive(rs);
                }
                return null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger la session live: " + safeMsg(e), e);
        }
    }

    /**
     * Récupère les sessions live par statut.
     * @param statut Le statut recherché
     * @return Liste des sessions avec le statut spécifié
     */
    public List<SessionLive> getSessionsByStatut(SessionStatut statut) {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE sl.statut = ?
                ORDER BY sl.date DESC, sl.heure DESC, sl.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, statut.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionLive> sessions = new ArrayList<>();
                while (rs.next()) {
                    sessions.add(mapSessionLive(rs));
                }
                return sessions;
            }
        } catch (Exception e) {
            System.err.println("[SessionLive] getSessionsByStatut: " + safeMsg(e));
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les sessions live d'un cours spécifique.
     * @param coursId L'ID du cours
     * @return Liste des sessions du cours
     */
    public List<SessionLive> getSessionsByCoursId(int coursId) {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE sl.cours_id = ?
                ORDER BY sl.date DESC, sl.heure DESC, sl.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionLive> sessions = new ArrayList<>();
                while (rs.next()) {
                    sessions.add(mapSessionLive(rs));
                }
                return sessions;
            }
        } catch (Exception e) {
            // Retourner une liste vide si la table n'existe pas encore
            System.err.println("[SessionLive] getSessionsByCoursId: " + safeMsg(e));
            return new ArrayList<>();
        }
    }

    /**
     * Récupère les sessions live d'une date spécifique.
     * @param date La date recherchée
     * @return Liste des sessions de la date
     */
    public List<SessionLive> getSessionsByDate(LocalDate date) {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE sl.date = ?
                ORDER BY sl.heure ASC, sl.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionLive> sessions = new ArrayList<>();
                while (rs.next()) {
                    sessions.add(mapSessionLive(rs));
                }
                return sessions;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les sessions de la date: " + safeMsg(e), e);
        }
    }

    /**
     * Compte le nombre de sessions par statut.
     * @param statut Le statut à compter
     * @return Le nombre de sessions avec ce statut
     */
    public int countSessionsByStatut(SessionStatut statut) {
        String sql = "SELECT COUNT(*) FROM session_live WHERE statut = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, statut.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de compter les sessions: " + safeMsg(e), e);
        }
    }

    /**
     * Recherche des sessions par nom de cours (recherche partielle).
     * @param query Le terme de recherche
     * @return Liste des sessions correspondantes
     */
    public List<SessionLive> searchSessions(String query) {
        String q = safe(query);
        if (q.isBlank()) {
            return getAllSessions();
        }

        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE sl.nom_cours LIKE ? OR c.titre LIKE ?
                ORDER BY sl.date DESC, sl.heure DESC, sl.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            String like = "%" + q + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionLive> sessions = new ArrayList<>();
                while (rs.next()) {
                    sessions.add(mapSessionLive(rs));
                }
                return sessions;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de rechercher les sessions: " + safeMsg(e), e);
        }
    }

    /**
     * Met à jour uniquement l'identifiant Google Calendar d'une session.
     */
    public void updateGoogleEventId(int sessionId, String googleEventId) {
        String sql = "UPDATE session_live SET google_event_id = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, googleEventId);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[SessionLive] updateGoogleEventId: " + safeMsg(e));
        }
    }

    /**
     * Trouve les sessions à venir dans une plage de temps donnée.
     * Utilisé par le système de notifications automatiques.
     * 
     * @param from Début de la plage temporelle
     * @param to Fin de la plage temporelle
     * @return Liste des sessions dans la plage spécifiée
     */
    public List<SessionLive> findUpcomingSessions(java.time.LocalDateTime from, java.time.LocalDateTime to) {
        String sql = """
                SELECT sl.id, sl.nom_cours, sl.lien, sl.date, sl.heure, sl.statut, sl.cours_id,
                       sl.date_creation, sl.date_modification, c.titre AS cours_titre
                FROM session_live sl
                LEFT JOIN cours c ON c.id = sl.cours_id
                WHERE TIMESTAMP(sl.date, sl.heure) BETWEEN ? AND ?
                  AND sl.statut = 'PLANIFIEE'
                ORDER BY sl.date ASC, sl.heure ASC
                """;
        
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(from));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(to));
            
            try (ResultSet rs = ps.executeQuery()) {
                List<SessionLive> sessions = new ArrayList<>();
                while (rs.next()) {
                    SessionLive session = mapSessionLive(rs);
                    // Convertir les champs date/heure séparés en LocalDateTime pour compatibilité
                    if (session.getDate() != null && session.getHeure() != null) {
                        java.time.LocalDateTime dateDebut = java.time.LocalDateTime.of(
                            session.getDate(), session.getHeure()
                        );
                        // Stocker dans un champ temporaire pour le système de notifications
                        session.setDateDebut(dateDebut);
                        session.setDateFin(dateDebut.plusHours(1)); // Durée par défaut d'1 heure
                    }
                    sessions.add(session);
                }
                return sessions;
            }
        } catch (Exception e) {
            System.err.println("[SessionLive] findUpcomingSessions: " + safeMsg(e));
            return new ArrayList<>();
        }
    }

    /**
     * Trouve une session par ID avec mapping complet pour le système de notifications.
     * 
     * @param id ID de la session
     * @return Session avec tous les champs mappés, ou null si non trouvée
     */
    public SessionLive findById(int id) {
        SessionLive session = getSessionById(id);
        if (session != null) {
            // Mapper les champs pour compatibilité avec le système de notifications
            if (session.getDate() != null && session.getHeure() != null) {
                java.time.LocalDateTime dateDebut = java.time.LocalDateTime.of(
                    session.getDate(), session.getHeure()
                );
                session.setDateDebut(dateDebut);
                session.setDateFin(dateDebut.plusHours(1)); // Durée par défaut
                session.setTitre(session.getNomCours());
                session.setLienSession(session.getLien());
                // Mapper le statut vers le nouveau système
                session.setStatutNotification(mapToNotificationStatus(session.getStatut()));
            }
        }
        return session;
    }

    /**
     * Mappe le statut de l'ancien système vers le nouveau système de notifications.
     */
    private com.educompus.model.SessionStatutNotification mapToNotificationStatus(SessionStatut oldStatus) {
        return com.educompus.model.SessionStatutNotification.fromOldStatus(oldStatus);
    }

    // Méthodes privées utilitaires

    private void ensureSessionLiveSchema() {
        try (Connection conn = EducompusDB.getConnection()) {
            // Étape 1 : créer la table sans FK (toujours réussit)
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS session_live (
                        id               INT AUTO_INCREMENT PRIMARY KEY,
                        nom_cours        VARCHAR(255) NOT NULL,
                        lien             VARCHAR(512) NOT NULL,
                        date             DATE         NOT NULL,
                        heure            TIME         NOT NULL,
                        statut           ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE')
                                             NOT NULL DEFAULT 'PLANIFIEE',
                        cours_id         INT          NULL,
                        date_creation    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        date_modification DATETIME    NULL ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """);

            // Étape 2 : ajouter les index s'ils n'existent pas encore
            executeIgnore(conn, "ALTER TABLE session_live ADD INDEX idx_sl_date (date)");
            executeIgnore(conn, "ALTER TABLE session_live ADD INDEX idx_sl_statut (statut)");
            executeIgnore(conn, "ALTER TABLE session_live ADD INDEX idx_sl_cours_id (cours_id)");
            // Colonne google_event_id (ajout si absente)
            executeIgnore(conn, "ALTER TABLE session_live ADD COLUMN google_event_id VARCHAR(255) NULL");

            // Étape 3 : ajouter la FK vers cours si la table cours existe
            executeIgnore(conn, """
                    ALTER TABLE session_live
                    ADD CONSTRAINT fk_sl_cours
                    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE SET NULL
                    """);

        } catch (Exception ignored) {
            // L'application reste utilisable même si la migration échoue
        }
    }

    private static void executeIgnore(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {
            // Ignore schema creation failures
        }
    }

    private void executeDelete(String sql, int id, String label) {
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new IllegalStateException("Aucune session trouvée avec l'ID: " + id);
            }
        } catch (Exception e) {
            throw new IllegalStateException(label + ": " + safeMsg(e), e);
        }
    }

    private static SessionLive mapSessionLive(ResultSet rs) throws Exception {
        SessionLive session = new SessionLive();
        session.setId(rs.getInt("id"));
        session.setNomCours(rs.getString("nom_cours"));
        session.setLien(rs.getString("lien"));
        
        // Mapping des types temporels
        Date sqlDate = rs.getDate("date");
        if (sqlDate != null) {
            session.setDate(sqlDate.toLocalDate());
        }
        
        Time sqlTime = rs.getTime("heure");
        if (sqlTime != null) {
            session.setHeure(sqlTime.toLocalTime());
        }
        
        // Mapping de l'énumération
        String statutStr = rs.getString("statut");
        session.setStatut(SessionStatut.fromString(statutStr));

        // Stocker le cours_id
        int coursId = rs.getInt("cours_id");
        if (!rs.wasNull()) {
            session.setCoursId(coursId);
        }

        // Google Calendar event ID
        try {
            session.setGoogleEventId(rs.getString("google_event_id"));
        } catch (Exception ignored) {}

        return session;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeMsg(Exception e) {
        if (e == null) {
            return "erreur inconnue";
        }
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 220) {
            msg = msg.substring(0, 220) + "...";
        }
        return msg;
    }
}