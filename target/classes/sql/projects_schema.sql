-- Minimal schema for the Projects + Submissions + Kanban feature
-- Database: `educompus` (MySQL)

CREATE TABLE IF NOT EXISTS project (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    deadline VARCHAR(64) NULL,
    deliverables TEXT NULL,
    created_by_id INT NULL,
    is_published TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_submission (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    student_id INT NOT NULL,
    text_response TEXT NULL,
    cahier_path VARCHAR(512) NULL,
    dossier_path VARCHAR(512) NULL,
    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_project_student (project_id, student_id)
);

CREATE TABLE IF NOT EXISTS kanban_task (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    position INT NOT NULL DEFAULT 1,
    project_id INT NOT NULL,
    student_id INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_kanban_scope (project_id, student_id, status, position)
);

