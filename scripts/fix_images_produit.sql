-- ============================================================
-- Nettoyage des chemins d'images corrompus dans la table produit
-- Problème : JavaFX stockait file:/C:/... au lieu du nom de fichier
-- ============================================================

-- Voir les produits affectés avant de corriger
SELECT id, nom, image FROM produit
WHERE image LIKE 'file:%' OR image LIKE 'C:%' OR image LIKE 'D:%';

-- Extraire uniquement le nom du fichier (partie après le dernier '/' ou '\')
UPDATE produit
SET image = SUBSTRING_INDEX(REPLACE(image, '\\', '/'), '/', -1)
WHERE image LIKE 'file:%' OR image LIKE 'C:%' OR image LIKE 'D:%';

-- Décoder les %20 et autres caractères URL encodés (ex: télé... → télé...)
-- Note : MySQL ne décode pas l'URL nativement, mais le nom sera propre après le SUBSTRING_INDEX

-- Vérifier le résultat
SELECT id, nom, image FROM produit ORDER BY id DESC LIMIT 20;
