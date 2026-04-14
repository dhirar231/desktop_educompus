-- Progression de lecture des chapitres par étudiant
CREATE TABLE IF NOT EXISTS chapitre_progress (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chapitre_id INT NOT NULL,
    student_id INT NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    completed_at DATETIME NULL,
    UNIQUE KEY uq_chapitre_student (chapitre_id, student_id)
);
