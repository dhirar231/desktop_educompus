-- Schema for Courses, Chapters, TDs, and Videos
-- Database: `educompus` (MySQL)

CREATE TABLE IF NOT EXISTS cours (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    niveau VARCHAR(32),
    domaine VARCHAR(64),
    image VARCHAR(255),
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nom_formateur VARCHAR(255),
    duree_totale_heures INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chapitre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    ordre INT NOT NULL DEFAULT 1,
    cours_id INT NOT NULL,
    fichier_c VARCHAR(512),
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS td (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    fichier VARCHAR(512),
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chapitre_id INT NOT NULL,
    FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS video_explicative (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    url_video VARCHAR(512),
    description TEXT,
    date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    chapitre_id INT NOT NULL,
    FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
);