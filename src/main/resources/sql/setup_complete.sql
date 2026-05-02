-- ============================================
-- Script SQL complet pour EduCompus
-- ============================================

-- 1. Ajouter les colonnes de validation aux cours (si elles n'existent pas)
ALTER TABLE cours 
ADD COLUMN IF NOT EXISTS statut VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE',
ADD COLUMN IF NOT EXISTS commentaire_admin TEXT NULL,
ADD COLUMN IF NOT EXISTS created_by_id INT NOT NULL DEFAULT 0;

-- 2. Table de progression des chapitres
CREATE TABLE IF NOT EXISTS chapitre_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chapitre_id INT NOT NULL,
    student_id INT NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    completed_at DATETIME NULL,
    UNIQUE KEY uq_chapitre_student (chapitre_id, student_id),
    INDEX idx_student (student_id),
    INDEX idx_chapitre (chapitre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Table des favoris (si elle n'existe pas)
CREATE TABLE IF NOT EXISTS course_favorites (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    cours_id INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_cours (student_id, cours_id),
    INDEX idx_student (student_id),
    INDEX idx_cours (cours_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Vérifier que les tables principales existent
-- (Ces commandes ne feront rien si les tables existent déjà)

CREATE TABLE IF NOT EXISTS cours (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    niveau VARCHAR(50),
    domaine VARCHAR(100),
    image VARCHAR(500),
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    nom_formateur VARCHAR(255),
    duree_totale_heures INT DEFAULT 0,
    statut VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE',
    commentaire_admin TEXT NULL,
    created_by_id INT NOT NULL DEFAULT 0,
    INDEX idx_statut (statut),
    INDEX idx_created_by (created_by_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chapitre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    ordre INT NOT NULL DEFAULT 1,
    description TEXT,
    fichier_c VARCHAR(500),
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    cours_id INT NOT NULL,
    INDEX idx_cours (cours_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS td (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    fichier VARCHAR(500),
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    niveau VARCHAR(50),
    cours_id INT NOT NULL,
    chapitre_id INT,
    domaine VARCHAR(100),
    INDEX idx_cours (cours_id),
    INDEX idx_chapitre (chapitre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS video_explicative (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    url_video VARCHAR(500),
    description TEXT,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    niveau VARCHAR(50),
    cours_id INT NOT NULL,
    chapitre_id INT,
    domaine VARCHAR(100),
    INDEX idx_cours (cours_id),
    INDEX idx_chapitre (chapitre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Afficher un résumé
SELECT 'Setup terminé !' AS message;
SELECT COUNT(*) AS total_cours FROM cours;
SELECT COUNT(*) AS total_chapitres FROM chapitre;
SELECT COUNT(*) AS total_td FROM td;
SELECT COUNT(*) AS total_videos FROM video_explicative;
