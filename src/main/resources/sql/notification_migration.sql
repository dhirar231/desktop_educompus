-- Migration pour ajouter le support des notifications automatiques
-- Système de notifications pour les sessions live EduCompus
-- Version: 1.0
-- Date: 2026-04-25

-- Créer la table des états de notification
CREATE TABLE IF NOT EXISTS notification_state (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'Type de notification: 30min ou 5min',
    scheduled_time DATETIME NOT NULL COMMENT 'Heure prévue d\'envoi de la notification',
    sent_time DATETIME NULL COMMENT 'Heure réelle d\'envoi (NULL si pas encore envoyée)',
    sent BOOLEAN DEFAULT FALSE COMMENT 'Indique si la notification a été envoyée',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Date de création de l\'état',
    
    -- Contraintes
    FOREIGN KEY (session_id) REFERENCES session_live(id) ON DELETE CASCADE,
    UNIQUE KEY unique_session_type (session_id, type) COMMENT 'Une seule notification par type et par session',
    
    -- Index pour optimiser les performances
    INDEX idx_scheduled_time (scheduled_time) COMMENT 'Index pour les requêtes de planification',
    INDEX idx_sent (sent) COMMENT 'Index pour filtrer les notifications non envoyées',
    INDEX idx_session_id (session_id) COMMENT 'Index pour les requêtes par session',
    INDEX idx_notification_cleanup (created_at, sent) COMMENT 'Index pour le nettoyage automatique'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='États des notifications pour les sessions live';

-- Créer les états de notification pour les sessions existantes (30 minutes avant)
INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
SELECT 
    id as session_id,
    '30min' as type,
    DATE_SUB(date_debut, INTERVAL 30 MINUTE) as scheduled_time,
    CASE 
        WHEN date_debut <= NOW() THEN TRUE 
        ELSE FALSE 
    END as sent,
    NOW() as created_at
FROM session_live 
WHERE statut = 'PROGRAMMEE' 
  AND date_debut > NOW()
  AND NOT EXISTS (
      SELECT 1 FROM notification_state ns 
      WHERE ns.session_id = session_live.id 
        AND ns.type = '30min'
  )
ON DUPLICATE KEY UPDATE 
    scheduled_time = VALUES(scheduled_time),
    sent = CASE 
        WHEN VALUES(scheduled_time) <= NOW() THEN TRUE 
        ELSE sent 
    END;

-- Créer les états de notification pour les sessions existantes (5 minutes avant)
INSERT INTO notification_state (session_id, type, scheduled_time, sent, created_at)
SELECT 
    id as session_id,
    '5min' as type,
    DATE_SUB(date_debut, INTERVAL 5 MINUTE) as scheduled_time,
    CASE 
        WHEN date_debut <= NOW() THEN TRUE 
        ELSE FALSE 
    END as sent,
    NOW() as created_at
FROM session_live 
WHERE statut = 'PROGRAMMEE' 
  AND date_debut > NOW()
  AND NOT EXISTS (
      SELECT 1 FROM notification_state ns 
      WHERE ns.session_id = session_live.id 
        AND ns.type = '5min'
  )
ON DUPLICATE KEY UPDATE 
    scheduled_time = VALUES(scheduled_time),
    sent = CASE 
        WHEN VALUES(scheduled_time) <= NOW() THEN TRUE 
        ELSE sent 
    END;

-- Créer une vue pour faciliter les requêtes de notifications dues
CREATE OR REPLACE VIEW v_notifications_due AS
SELECT 
    ns.id,
    ns.session_id,
    ns.type,
    ns.scheduled_time,
    ns.sent,
    sl.titre as session_titre,
    sl.cours_titre,
    sl.enseignant_nom,
    sl.date_debut,
    sl.lien_session,
    sl.statut as session_statut
FROM notification_state ns
INNER JOIN session_live sl ON ns.session_id = sl.id
WHERE ns.sent = FALSE
  AND ns.scheduled_time <= NOW()
  AND ns.scheduled_time >= DATE_SUB(NOW(), INTERVAL 2 MINUTE)
  AND sl.statut = 'PROGRAMMEE'
  AND sl.date_debut > NOW()
ORDER BY ns.scheduled_time ASC;

-- Créer une procédure stockée pour le nettoyage automatique
DELIMITER //

CREATE PROCEDURE CleanupOldNotificationStates()
BEGIN
    DECLARE cleaned_count INT DEFAULT 0;
    
    -- Supprimer les états de notification anciens (plus de 7 jours)
    DELETE FROM notification_state 
    WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY);
    
    SET cleaned_count = ROW_COUNT();
    
    -- Log du nettoyage (optionnel, nécessite une table de logs)
    -- INSERT INTO system_logs (action, details, created_at) 
    -- VALUES ('notification_cleanup', CONCAT('Cleaned ', cleaned_count, ' old notification states'), NOW());
    
    SELECT CONCAT('Nettoyage terminé: ', cleaned_count, ' états supprimés') as result;
END //

DELIMITER ;

-- Créer un événement pour le nettoyage automatique (exécuté quotidiennement à 2h du matin)
-- Note: Nécessite que l'event scheduler soit activé (SET GLOBAL event_scheduler = ON;)
CREATE EVENT IF NOT EXISTS evt_cleanup_notifications
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '02:00:00')
DO
  CALL CleanupOldNotificationStates();

-- Vérifications post-migration
SELECT 
    'Migration terminée' as status,
    (SELECT COUNT(*) FROM notification_state) as total_notification_states,
    (SELECT COUNT(*) FROM notification_state WHERE sent = FALSE) as pending_notifications,
    (SELECT COUNT(DISTINCT session_id) FROM notification_state) as sessions_with_notifications;

-- Afficher les prochaines notifications dues (pour vérification)
SELECT 
    session_titre,
    type,
    scheduled_time,
    TIMESTAMPDIFF(MINUTE, NOW(), scheduled_time) as minutes_until_due
FROM v_notifications_due
WHERE scheduled_time > NOW()
ORDER BY scheduled_time
LIMIT 10;