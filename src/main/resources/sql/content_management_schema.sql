-- ═══════════════════════════════════════════════════════════════════════════════════════
-- SCHÉMA DE BASE DE DONNÉES POUR LA GESTION DE CONTENU PÉDAGOGIQUE
-- Intégration Google Drive sélective
-- ═══════════════════════════════════════════════════════════════════════════════════════

-- Extension de la table cours
ALTER TABLE cours ADD COLUMN IF NOT EXISTS important BOOLEAN DEFAULT FALSE;
ALTER TABLE cours ADD COLUMN IF NOT EXISTS drive_link TEXT;

-- Extension de la table chapitres
ALTER TABLE chapitres ADD COLUMN IF NOT EXISTS important BOOLEAN DEFAULT FALSE;
ALTER TABLE chapitres ADD COLUMN IF NOT EXISTS drive_link TEXT;

-- Extension de la table tds
ALTER TABLE tds ADD COLUMN IF NOT EXISTS important BOOLEAN DEFAULT FALSE;
ALTER TABLE tds ADD COLUMN IF NOT EXISTS drive_link TEXT;

-- Extension de la table videos_explicatives
ALTER TABLE videos_explicatives ADD COLUMN IF NOT EXISTS important BOOLEAN DEFAULT FALSE;
ALTER TABLE videos_explicatives ADD COLUMN IF NOT EXISTS drive_link TEXT;

-- Table pour le suivi des uploads Google Drive
CREATE TABLE IF NOT EXISTS drive_uploads (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content_type TEXT NOT NULL, -- 'cours', 'chapitre', 'td', 'video'
    content_id INTEGER NOT NULL,
    drive_file_id TEXT NOT NULL,
    drive_link TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_size INTEGER,
    upload_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status TEXT DEFAULT 'uploaded', -- 'uploaded', 'deleted', 'error'
    
    UNIQUE(content_type, content_id)
);

-- Index pour optimiser les requêtes
CREATE INDEX IF NOT EXISTS idx_drive_uploads_content ON drive_uploads(content_type, content_id);
CREATE INDEX IF NOT EXISTS idx_drive_uploads_status ON drive_uploads(status);
CREATE INDEX IF NOT EXISTS idx_cours_important ON cours(important);
CREATE INDEX IF NOT EXISTS idx_chapitres_important ON chapitres(important);
CREATE INDEX IF NOT EXISTS idx_tds_important ON tds(important);
CREATE INDEX IF NOT EXISTS idx_videos_important ON videos_explicatives(important);

-- Table pour les statistiques de contenu
CREATE TABLE IF NOT EXISTS content_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_snapshot DATE DEFAULT CURRENT_DATE,
    total_cours INTEGER DEFAULT 0,
    cours_importants INTEGER DEFAULT 0,
    total_chapitres INTEGER DEFAULT 0,
    chapitres_importants INTEGER DEFAULT 0,
    total_tds INTEGER DEFAULT 0,
    tds_importants INTEGER DEFAULT 0,
    total_videos INTEGER DEFAULT 0,
    videos_importants INTEGER DEFAULT 0,
    drive_storage_used INTEGER DEFAULT 0,
    drive_files_count INTEGER DEFAULT 0,
    
    UNIQUE(date_snapshot)
);

-- Vue pour les statistiques en temps réel
CREATE VIEW IF NOT EXISTS v_content_statistics AS
SELECT 
    -- Statistiques des cours
    (SELECT COUNT(*) FROM cours) as total_cours,
    (SELECT COUNT(*) FROM cours WHERE important = TRUE) as cours_importants,
    (SELECT COUNT(*) FROM cours WHERE drive_link IS NOT NULL) as cours_sur_drive,
    
    -- Statistiques des chapitres
    (SELECT COUNT(*) FROM chapitres) as total_chapitres,
    (SELECT COUNT(*) FROM chapitres WHERE important = TRUE) as chapitres_importants,
    (SELECT COUNT(*) FROM chapitres WHERE drive_link IS NOT NULL) as chapitres_sur_drive,
    
    -- Statistiques des TDs
    (SELECT COUNT(*) FROM tds) as total_tds,
    (SELECT COUNT(*) FROM tds WHERE important = TRUE) as tds_importants,
    (SELECT COUNT(*) FROM tds WHERE drive_link IS NOT NULL) as tds_sur_drive,
    
    -- Statistiques des vidéos
    (SELECT COUNT(*) FROM videos_explicatives) as total_videos,
    (SELECT COUNT(*) FROM videos_explicatives WHERE important = TRUE) as videos_importants,
    (SELECT COUNT(*) FROM videos_explicatives WHERE drive_link IS NOT NULL) as videos_sur_drive,
    
    -- Statistiques Google Drive
    (SELECT COUNT(*) FROM drive_uploads WHERE status = 'uploaded') as total_fichiers_drive,
    (SELECT COALESCE(SUM(file_size), 0) FROM drive_uploads WHERE status = 'uploaded') as taille_totale_drive;

-- Vue pour le contenu important non uploadé
CREATE VIEW IF NOT EXISTS v_content_important_non_uploade AS
SELECT 
    'cours' as type_contenu,
    id,
    titre,
    important,
    drive_link,
    date_creation
FROM cours 
WHERE important = TRUE AND (drive_link IS NULL OR drive_link = '')

UNION ALL

SELECT 
    'chapitre' as type_contenu,
    id,
    titre,
    important,
    drive_link,
    date_creation
FROM chapitres 
WHERE important = TRUE AND (drive_link IS NULL OR drive_link = '')

UNION ALL

SELECT 
    'td' as type_contenu,
    id,
    titre,
    important,
    drive_link,
    date_creation
FROM tds 
WHERE important = TRUE AND (drive_link IS NULL OR drive_link = '')

UNION ALL

SELECT 
    'video' as type_contenu,
    id,
    titre,
    important,
    drive_link,
    date_creation
FROM videos_explicatives 
WHERE important = TRUE AND (drive_link IS NULL OR drive_link = '');

-- Procédure pour nettoyer les anciens uploads (simulée avec des commentaires)
-- Cette logique sera implémentée dans le service Java

-- Trigger pour mettre à jour les statistiques (optionnel, peut être fait via le service)
-- CREATE TRIGGER IF NOT EXISTS update_content_stats_on_insert
-- AFTER INSERT ON cours
-- BEGIN
--     INSERT OR REPLACE INTO content_stats (date_snapshot, total_cours, cours_importants)
--     SELECT CURRENT_DATE, COUNT(*), COUNT(CASE WHEN important THEN 1 END)
--     FROM cours;
-- END;

-- Données de test (optionnel)
-- INSERT OR IGNORE INTO cours (titre, description, niveau, domaine, important) VALUES
-- ('Cours Test Important', 'Description du cours test', 'Débutant', 'Informatique', TRUE),
-- ('Cours Test Normal', 'Description du cours normal', 'Intermédiaire', 'Mathématiques', FALSE);

-- Vérification de l'intégrité
-- SELECT 'Vérification terminée' as status;

-- ═══════════════════════════════════════════════════════════════════════════════════════
-- REQUÊTES UTILES POUR LA GESTION DE CONTENU
-- ═══════════════════════════════════════════════════════════════════════════════════════

-- Lister tout le contenu important
-- SELECT * FROM v_content_important_non_uploade ORDER BY date_creation DESC;

-- Statistiques complètes
-- SELECT * FROM v_content_statistics;

-- Contenu récemment uploadé sur Drive
-- SELECT * FROM drive_uploads WHERE upload_date >= date('now', '-7 days') ORDER BY upload_date DESC;

-- Espace utilisé par type de contenu
-- SELECT 
--     content_type,
--     COUNT(*) as nombre_fichiers,
--     SUM(file_size) as taille_totale,
--     AVG(file_size) as taille_moyenne
-- FROM drive_uploads 
-- WHERE status = 'uploaded'
-- GROUP BY content_type;