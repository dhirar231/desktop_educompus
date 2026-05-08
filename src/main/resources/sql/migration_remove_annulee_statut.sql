-- Migration: Suppression du statut ANNULEE
-- Date: 2026-05-08
-- Description: Supprime le statut ANNULEE de l'énumération session_live.statut
--              Les sessions annulées existantes seront converties en TERMINEE

-- Étape 1: Convertir toutes les sessions ANNULEE en TERMINEE
UPDATE session_live 
SET statut = 'TERMINEE' 
WHERE statut = 'ANNULEE';

-- Étape 2: Modifier la colonne pour supprimer ANNULEE de l'énumération
ALTER TABLE session_live 
MODIFY COLUMN statut ENUM('PLANIFIEE','EN_COURS','TERMINEE') NOT NULL DEFAULT 'PLANIFIEE';

-- Vérification
SELECT 'Migration terminée: statut ANNULEE supprimé' AS message;
SELECT COUNT(*) AS sessions_total FROM session_live;
SELECT statut, COUNT(*) AS nombre FROM session_live GROUP BY statut;
