-- Nettoyage des notifications liées aux sessions live
-- Date: 2026-05-08
-- Description: Supprime toutes les notifications existantes pour les sessions live

-- Supprimer toutes les notifications de type session live
DELETE FROM notification_states 
WHERE session_live_id IS NOT NULL;

-- Vérification
SELECT 'Notifications session live supprimées' AS message;
SELECT COUNT(*) AS notifications_restantes FROM notification_states;
