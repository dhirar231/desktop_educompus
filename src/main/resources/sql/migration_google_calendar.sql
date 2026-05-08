-- ============================================================
-- Migration : Ajout de la synchronisation Google Calendar
-- Date : Mai 2026
-- Description : Ajoute la colonne google_event_id à la table session_live
-- ============================================================

-- Vérifier si la colonne existe déjà avant de l'ajouter
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'session_live'
      AND COLUMN_NAME = 'google_event_id'
);

-- Ajouter la colonne si elle n'existe pas
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE session_live ADD COLUMN google_event_id VARCHAR(255) NULL COMMENT ''ID événement Google Calendar (NULL si non synchronisé)'' AFTER cours_id',
    'SELECT ''La colonne google_event_id existe déjà'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ajouter un index pour améliorer les performances
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'session_live'
      AND INDEX_NAME = 'idx_sl_google_event'
);

SET @sql_index = IF(
    @index_exists = 0,
    'CREATE INDEX idx_sl_google_event ON session_live(google_event_id)',
    'SELECT ''L''index idx_sl_google_event existe déjà'' AS message'
);

PREPARE stmt_index FROM @sql_index;
EXECUTE stmt_index;
DEALLOCATE PREPARE stmt_index;

-- Afficher un message de confirmation
SELECT 
    'Migration Google Calendar terminée avec succès' AS status,
    NOW() AS timestamp,
    (SELECT COUNT(*) FROM session_live) AS total_sessions,
    (SELECT COUNT(*) FROM session_live WHERE google_event_id IS NOT NULL) AS sessions_synchronisees;
