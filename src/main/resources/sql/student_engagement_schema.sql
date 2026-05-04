-- ============================================================================
-- Script SQL pour le module de Détection des Étudiants Désengagés
-- ============================================================================

-- Table pour enregistrer les présences aux sessions live
CREATE TABLE IF NOT EXISTS session_attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    student_id INT NOT NULL,
    attended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    UNIQUE KEY unique_attendance (session_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table pour enregistrer l'activité des utilisateurs (connexions)
CREATE TABLE IF NOT EXISTS user_activity_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    date_connexion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (user_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_user_date (user_id, date_connexion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table pour enregistrer les téléchargements de PDF
CREATE TABLE IF NOT EXISTS pdf_download_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    chapter_id INT,
    pdf_type VARCHAR(50) NOT NULL, -- 'CHAPTER', 'TD', 'VIDEO'
    downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (course_id) REFERENCES cours(id) ON DELETE CASCADE,
    FOREIGN KEY (chapter_id) REFERENCES chapitre(id) ON DELETE SET NULL,
    INDEX idx_student_course (student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Vues utiles pour l'analyse
-- ============================================================================

-- Vue pour obtenir le dernier accès de chaque étudiant
CREATE OR REPLACE VIEW v_student_last_activity AS
SELECT 
    user_id,
    MAX(date_connexion) as last_connection,
    DATEDIFF(NOW(), MAX(date_connexion)) as days_since_last_connection
FROM user_activity_log
GROUP BY user_id;

-- Vue pour compter les absences par étudiant et par cours
CREATE OR REPLACE VIEW v_student_absences AS
SELECT 
    u.id as student_id,
    sl.cours_id,
    COUNT(sl.id) as total_absences
FROM utilisateur u
CROSS JOIN session_live sl
LEFT JOIN session_attendance sa ON sa.session_id = sl.id AND sa.student_id = u.id
WHERE u.role = 'STUDENT'
  AND sl.statut = 'TERMINEE'
  AND sa.id IS NULL
GROUP BY u.id, sl.cours_id;

-- Vue pour compter les téléchargements par étudiant et par cours
CREATE OR REPLACE VIEW v_student_downloads AS
SELECT 
    student_id,
    course_id,
    COUNT(*) as total_downloads,
    COUNT(DISTINCT chapter_id) as chapters_downloaded
FROM pdf_download_log
GROUP BY student_id, course_id;

-- ============================================================================
-- Données de test (optionnel)
-- ============================================================================

-- Exemple d'insertion de logs d'activité pour tester
-- INSERT INTO user_activity_log (user_id, date_connexion, ip_address) VALUES
-- (1, NOW() - INTERVAL 2 DAY, '192.168.1.1'),
-- (2, NOW() - INTERVAL 8 DAY, '192.168.1.2'),
-- (3, NOW() - INTERVAL 15 DAY, '192.168.1.3');

-- ============================================================================
-- Procédures stockées utiles
-- ============================================================================

-- Procédure pour enregistrer une connexion
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_log_user_activity(
    IN p_user_id INT,
    IN p_ip_address VARCHAR(45),
    IN p_user_agent TEXT
)
BEGIN
    INSERT INTO user_activity_log (user_id, date_connexion, ip_address, user_agent)
    VALUES (p_user_id, NOW(), p_ip_address, p_user_agent);
END //
DELIMITER ;

-- Procédure pour enregistrer un téléchargement de PDF
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS sp_log_pdf_download(
    IN p_student_id INT,
    IN p_course_id INT,
    IN p_chapter_id INT,
    IN p_pdf_type VARCHAR(50)
)
BEGIN
    INSERT INTO pdf_download_log (student_id, course_id, chapter_id, pdf_type, downloaded_at)
    VALUES (p_student_id, p_course_id, p_chapter_id, p_pdf_type, NOW());
END //
DELIMITER ;

-- ============================================================================
-- Index pour optimiser les performances
-- ============================================================================

-- Index pour les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_activity_user_date ON user_activity_log(user_id, date_connexion DESC);
CREATE INDEX IF NOT EXISTS idx_download_student_course ON pdf_download_log(student_id, course_id);
CREATE INDEX IF NOT EXISTS idx_attendance_session ON session_attendance(session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON session_attendance(student_id);

-- ============================================================================
-- Fin du script
-- ============================================================================
