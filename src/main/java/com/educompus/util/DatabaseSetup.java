package com.educompus.util;

import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Utilitaire pour créer automatiquement les tables manquantes au démarrage.
 */
public final class DatabaseSetup {

    private DatabaseSetup() {}

    /**
     * Crée toutes les tables nécessaires si elles n'existent pas.
     * Appelé automatiquement au démarrage de l'application.
     */
    public static void ensureTablesExist() {
        try (Connection conn = EducompusDB.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("🔧 Vérification de la base de données...");

            // 1. Ajouter les colonnes de validation aux cours
            try {
                stmt.execute("ALTER TABLE cours ADD COLUMN statut VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE'");
                System.out.println("✓ Colonne 'statut' ajoutée à la table cours");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            try {
                stmt.execute("ALTER TABLE cours ADD COLUMN commentaire_admin TEXT NULL");
                System.out.println("✓ Colonne 'commentaire_admin' ajoutée à la table cours");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            try {
                stmt.execute("ALTER TABLE cours ADD COLUMN created_by_id INT NOT NULL DEFAULT 0");
                System.out.println("✓ Colonne 'created_by_id' ajoutée à la table cours");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            // 2. Créer la table chapitre_progress
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chapitre_progress (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    chapitre_id INT NOT NULL,
                    student_id INT NOT NULL,
                    completed TINYINT(1) NOT NULL DEFAULT 0,
                    completed_at DATETIME NULL,
                    UNIQUE KEY uq_chapitre_student (chapitre_id, student_id),
                    INDEX idx_student (student_id),
                    INDEX idx_chapitre (chapitre_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            System.out.println("✓ Table 'chapitre_progress' créée");

            // 3. Créer la table course_favorites
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS course_favorites (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    student_id INT NOT NULL,
                    cours_id INT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uq_student_cours (student_id, cours_id),
                    INDEX idx_student (student_id),
                    INDEX idx_cours (cours_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            System.out.println("✓ Table 'course_favorites' créée");

            // 4. Ajouter les colonnes pour les vidéos AI
            try {
                stmt.execute("ALTER TABLE video_explicative ADD COLUMN is_ai_generated TINYINT(1) NOT NULL DEFAULT 0");
                System.out.println("✓ Colonne 'is_ai_generated' ajoutée à la table video_explicative");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            try {
                stmt.execute("ALTER TABLE video_explicative ADD COLUMN ai_script TEXT NULL");
                System.out.println("✓ Colonne 'ai_script' ajoutée à la table video_explicative");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            try {
                stmt.execute("ALTER TABLE video_explicative ADD COLUMN generation_status VARCHAR(20) DEFAULT 'COMPLETED'");
                System.out.println("✓ Colonne 'generation_status' ajoutée à la table video_explicative");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            try {
                stmt.execute("ALTER TABLE video_explicative ADD COLUMN did_video_id VARCHAR(100) NULL");
                System.out.println("✓ Colonne 'did_video_id' ajoutée à la table video_explicative");
            } catch (Exception e) {
                // Colonne existe déjà
            }

            System.out.println("✅ Base de données prête !");

            // 5. Tables Session Live
            ensureSessionLiveTables(stmt);
            
            // 6. Tables de notifications automatiques
            ensureNotificationTables(stmt);

        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la configuration de la base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureSessionLiveTables(Statement stmt) {
        try {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS session_live (
                    id               INT AUTO_INCREMENT PRIMARY KEY,
                    nom_cours        VARCHAR(255) NOT NULL,
                    lien             VARCHAR(512) NOT NULL,
                    date             DATE         NOT NULL,
                    heure            TIME         NOT NULL,
                    statut           ENUM('PLANIFIEE','EN_COURS','TERMINEE','ANNULEE') NOT NULL DEFAULT 'PLANIFIEE',
                    cours_id         INT          NULL,
                    google_event_id  VARCHAR(255) NULL,
                    date_creation    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    date_modification DATETIME    NULL ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_sl_date    (date),
                    INDEX idx_sl_statut  (statut),
                    INDEX idx_sl_cours   (cours_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            System.out.println("✓ Table 'session_live' créée");
        } catch (Exception e) { /* existe déjà */ }

        try {
            stmt.execute("""
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
                    INDEX idx_sp_session (session_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            System.out.println("✓ Table 'session_participants' créée");
        } catch (Exception e) { /* existe déjà */ }

        try {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS session_actions (
                    id           INT AUTO_INCREMENT PRIMARY KEY,
                    session_id   INT          NOT NULL,
                    etudiant_id  INT          NOT NULL DEFAULT 0,
                    nom_etudiant VARCHAR(255) NOT NULL DEFAULT 'Système',
                    type         VARCHAR(32)  NOT NULL,
                    timestamp    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    details      TEXT         NULL,
                    INDEX idx_sa_session   (session_id),
                    INDEX idx_sa_timestamp (session_id, timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
            System.out.println("✓ Table 'session_actions' créée");
        } catch (Exception e) { /* existe déjà */ }

        // Ajouter google_event_id si absent
        try {
            stmt.execute("ALTER TABLE session_live ADD COLUMN google_event_id VARCHAR(255) NULL");
        } catch (Exception e) { /* existe déjà */ }
    }
    
    /**
     * Crée les tables nécessaires pour le système de notifications automatiques.
     */
    private static void ensureNotificationTables(Statement stmt) {
        try {
            // Table des états de notification
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notification_state (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    session_id INT NOT NULL,
                    type VARCHAR(20) NOT NULL COMMENT 'Type de notification: 30min ou 5min',
                    scheduled_time DATETIME NOT NULL COMMENT 'Heure prévue d\\'envoi de la notification',
                    sent_time DATETIME NULL COMMENT 'Heure réelle d\\'envoi (NULL si pas encore envoyée)',
                    sent BOOLEAN DEFAULT FALSE COMMENT 'Indique si la notification a été envoyée',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création de l\\'état',
                    
                    -- Contraintes
                    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,
                    UNIQUE KEY unique_session_type (session_id, type) COMMENT 'Une seule notification par type et par session',
                    
                    -- Index pour optimiser les performances
                    INDEX idx_scheduled_time (scheduled_time) COMMENT 'Index pour les requêtes de planification',
                    INDEX idx_sent (sent) COMMENT 'Index pour filtrer les notifications non envoyées',
                    INDEX idx_session_id (session_id) COMMENT 'Index pour les requêtes par session',
                    INDEX idx_notification_cleanup (created_at, sent) COMMENT 'Index pour le nettoyage automatique'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
                COMMENT='États des notifications pour les sessions live'
            """);
            System.out.println("✓ Table 'notification_state' créée");
            
            // Vue pour les notifications dues
            stmt.execute("""
                CREATE OR REPLACE VIEW v_notifications_due AS
                SELECT 
                    ns.id,
                    ns.session_id,
                    ns.type,
                    ns.scheduled_time,
                    ns.sent,
                    sl.nom_cours as session_titre,
                    sl.nom_cours as cours_titre,
                    'Enseignant' as enseignant_nom,
                    TIMESTAMP(sl.date, sl.heure) as date_debut,
                    sl.lien as lien_session,
                    sl.statut as session_statut
                FROM notification_state ns
                INNER JOIN session_live sl ON ns.session_id = sl.id
                WHERE ns.sent = FALSE
                  AND ns.scheduled_time <= NOW()
                  AND ns.scheduled_time >= DATE_SUB(NOW(), INTERVAL 2 MINUTE)
                  AND sl.statut = 'PLANIFIEE'
                  AND TIMESTAMP(sl.date, sl.heure) > NOW()
                ORDER BY ns.scheduled_time ASC
            """);
            System.out.println("✓ Vue 'v_notifications_due' créée");
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la création des tables de notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
}