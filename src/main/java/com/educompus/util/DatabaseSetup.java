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

        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la configuration de la base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
