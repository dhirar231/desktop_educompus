-- ============================================================================
-- Script de Test Rapide - Module Engagement Étudiants
-- ============================================================================
-- Ce script ajoute des données de test pour voir le module en action
-- ============================================================================

USE educompus;

-- ============================================================================
-- ÉTAPE 1 : Vérifier les étudiants existants
-- ============================================================================
SELECT '=== ÉTUDIANTS EXISTANTS ===' as '';
SELECT id, nom, prenom, email, role 
FROM utilisateur 
WHERE role = 'STUDENT'
LIMIT 5;

-- ============================================================================
-- ÉTAPE 2 : Ajouter des logs d'activité (connexions)
-- ============================================================================
SELECT '=== AJOUT DES LOGS D''ACTIVITÉ ===' as '';

-- Supprimer les anciennes données de test
DELETE FROM user_activity_log WHERE user_id IN (
    SELECT id FROM utilisateur WHERE role = 'STUDENT' LIMIT 5
);

-- Récupérer les IDs des 5 premiers étudiants
SET @student1 = (SELECT id FROM utilisateur WHERE role = 'STUDENT' ORDER BY id LIMIT 1);
SET @student2 = (SELECT id FROM utilisateur WHERE role = 'STUDENT' ORDER BY id LIMIT 1 OFFSET 1);
SET @student3 = (SELECT id FROM utilisateur WHERE role = 'STUDENT' ORDER BY id LIMIT 1 OFFSET 2);
SET @student4 = (SELECT id FROM utilisateur WHERE role = 'STUDENT' ORDER BY id LIMIT 1 OFFSET 3);
SET @student5 = (SELECT id FROM utilisateur WHERE role = 'STUDENT' ORDER BY id LIMIT 1 OFFSET 4);

-- Étudiant 1 : ACTIF (connecté hier et aujourd'hui)
INSERT INTO user_activity_log (user_id, date_connexion, ip_address) 
VALUES 
(@student1, NOW() - INTERVAL 1 DAY, '192.168.1.101'),
(@student1, NOW() - INTERVAL 2 HOUR, '192.168.1.101');

-- Étudiant 2 : ACTIF (connecté il y a 3 jours)
INSERT INTO user_activity_log (user_id, date_connexion, ip_address) 
VALUES 
(@student2, NOW() - INTERVAL 3 DAY, '192.168.1.102');

-- Étudiant 3 : À SURVEILLER (connecté il y a 8 jours)
INSERT INTO user_activity_log (user_id, date_connexion, ip_address) 
VALUES 
(@student3, NOW() - INTERVAL 8 DAY, '192.168.1.103');

-- Étudiant 4 : DÉSENGAGÉ (connecté il y a 15 jours)
INSERT INTO user_activity_log (user_id, date_connexion, ip_address) 
VALUES 
(@student4, NOW() - INTERVAL 15 DAY, '192.168.1.104');

-- Étudiant 5 : DÉSENGAGÉ (jamais connecté - pas d'entrée)

SELECT 'Logs d''activité ajoutés !' as '';

-- ============================================================================
-- ÉTAPE 3 : Ajouter des téléchargements PDF
-- ============================================================================
SELECT '=== AJOUT DES TÉLÉCHARGEMENTS PDF ===' as '';

-- Supprimer les anciennes données de test
DELETE FROM pdf_download_log WHERE student_id IN (
    SELECT id FROM utilisateur WHERE role = 'STUDENT' LIMIT 5
);

-- Récupérer l'ID du premier cours
SET @course1 = (SELECT id FROM cours ORDER BY id LIMIT 1);

-- Étudiant 1 : A téléchargé plusieurs PDFs (ACTIF)
INSERT INTO pdf_download_log (student_id, course_id, chapter_id, pdf_type) 
VALUES 
(@student1, @course1, 1, 'CHAPTER'),
(@student1, @course1, 2, 'CHAPTER'),
(@student1, @course1, 3, 'CHAPTER');

-- Étudiant 2 : A téléchargé 1 PDF
INSERT INTO pdf_download_log (student_id, course_id, chapter_id, pdf_type) 
VALUES 
(@student2, @course1, 1, 'CHAPTER');

-- Étudiants 3, 4, 5 : N'ont rien téléchargé (pas d'entrée)

SELECT 'Téléchargements PDF ajoutés !' as '';

-- ============================================================================
-- ÉTAPE 4 : Vérifier les données créées
-- ============================================================================
SELECT '=== VÉRIFICATION DES DONNÉES ===' as '';

SELECT 
    u.id,
    u.nom,
    u.prenom,
    COUNT(DISTINCT ual.id) as nb_connexions,
    MAX(ual.date_connexion) as derniere_connexion,
    DATEDIFF(NOW(), MAX(ual.date_connexion)) as jours_depuis_connexion,
    COUNT(DISTINCT pdl.id) as nb_telechargements
FROM utilisateur u
LEFT JOIN user_activity_log ual ON ual.user_id = u.id
LEFT JOIN pdf_download_log pdl ON pdl.student_id = u.id
WHERE u.role = 'STUDENT'
GROUP BY u.id, u.nom, u.prenom
ORDER BY u.id
LIMIT 5;

-- ============================================================================
-- RÉSUMÉ ATTENDU
-- ============================================================================
SELECT '=== RÉSUMÉ ATTENDU ===' as '';
SELECT 
    'Étudiant 1' as etudiant,
    'ACTIF' as niveau,
    '0-30' as score_attendu,
    'Connecté récemment, a téléchargé des PDFs' as raison;

SELECT 
    'Étudiant 2' as etudiant,
    'ACTIF' as niveau,
    '0-30' as score_attendu,
    'Connecté il y a 3 jours' as raison;

SELECT 
    'Étudiant 3' as etudiant,
    'À SURVEILLER' as niveau,
    '31-60' as score_attendu,
    'Connecté il y a 8 jours (+25), pas de téléchargements (+15)' as raison;

SELECT 
    'Étudiant 4' as etudiant,
    'DÉSENGAGÉ' as niveau,
    '61+' as score_attendu,
    'Connecté il y a 15 jours (+40), pas de téléchargements (+15)' as raison;

SELECT 
    'Étudiant 5' as etudiant,
    'DÉSENGAGÉ' as niveau,
    '61+' as score_attendu,
    'Jamais connecté (+40), pas de téléchargements (+15)' as raison;

-- ============================================================================
-- FIN DU SCRIPT
-- ============================================================================
SELECT '=== DONNÉES DE TEST CRÉÉES AVEC SUCCÈS ! ===' as '';
SELECT 'Vous pouvez maintenant tester le module Engagement dans l''application' as '';
