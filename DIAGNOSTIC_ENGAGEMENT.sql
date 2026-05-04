-- ═══════════════════════════════════════════════════════════════
-- 🔍 SCRIPT DE DIAGNOSTIC - MODULE ENGAGEMENT
-- ═══════════════════════════════════════════════════════════════
-- Exécutez ces requêtes dans MySQL pour diagnostiquer le problème
-- ═══════════════════════════════════════════════════════════════

-- 1️⃣ Vérifier les étudiants dans la base
SELECT '=== 1. LISTE DES ÉTUDIANTS ===' as '';
SELECT id, nom, prenom, email, role 
FROM utilisateur 
WHERE role = 'STUDENT'
ORDER BY nom, prenom;

-- 2️⃣ Vérifier les cours disponibles
SELECT '=== 2. LISTE DES COURS ===' as '';
SELECT id, titre, niveau, domaine 
FROM cours 
ORDER BY id;

-- 3️⃣ Vérifier les chapitres du cours "analyse" (ou autre)
SELECT '=== 3. CHAPITRES DU COURS ===' as '';
SELECT c.id, c.ordre, c.titre, c.cours_id, co.titre as cours_titre
FROM chapitre c
JOIN cours co ON co.id = c.cours_id
WHERE co.titre LIKE '%analyse%'  -- Remplacer par le nom de votre cours
ORDER BY c.ordre;

-- 4️⃣ Vérifier les chapitres marqués comme lus
SELECT '=== 4. CHAPITRES MARQUÉS COMME LUS ===' as '';
SELECT 
    cp.id,
    cp.student_id,
    u.nom,
    u.prenom,
    cp.chapitre_id,
    ch.titre as chapitre_titre,
    ch.cours_id,
    co.titre as cours_titre,
    cp.completed,
    cp.completed_at
FROM chapitre_progress cp
JOIN utilisateur u ON u.id = cp.student_id
JOIN chapitre ch ON ch.id = cp.chapitre_id
JOIN cours co ON co.id = ch.cours_id
WHERE cp.completed = 1
ORDER BY cp.completed_at DESC;

-- 5️⃣ Compter les chapitres par étudiant et par cours
SELECT '=== 5. STATISTIQUES PAR ÉTUDIANT ===' as '';
SELECT 
    u.id as student_id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    co.id as cours_id,
    co.titre as cours,
    COUNT(DISTINCT cp.chapitre_id) as chapitres_lus,
    (SELECT COUNT(*) FROM chapitre WHERE cours_id = co.id) as total_chapitres
FROM utilisateur u
CROSS JOIN cours co
LEFT JOIN chapitre ch ON ch.cours_id = co.id
LEFT JOIN chapitre_progress cp ON cp.chapitre_id = ch.id AND cp.student_id = u.id AND cp.completed = 1
WHERE u.role = 'STUDENT'
GROUP BY u.id, co.id
HAVING chapitres_lus > 0
ORDER BY co.titre, u.nom;

-- 6️⃣ Vérifier la structure de la table chapitre_progress
SELECT '=== 6. STRUCTURE DE LA TABLE ===' as '';
DESCRIBE chapitre_progress;

-- 7️⃣ Vérifier toutes les données de chapitre_progress
SELECT '=== 7. TOUTES LES DONNÉES CHAPITRE_PROGRESS ===' as '';
SELECT * FROM chapitre_progress ORDER BY completed_at DESC LIMIT 20;

-- 8️⃣ Vérifier les téléchargements de PDF
SELECT '=== 8. TÉLÉCHARGEMENTS PDF ===' as '';
SELECT 
    pdl.student_id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    pdl.course_id,
    co.titre as cours,
    pdl.pdf_type,
    pdl.downloaded_at
FROM pdf_download_log pdl
JOIN utilisateur u ON u.id = pdl.student_id
JOIN cours co ON co.id = pdl.course_id
ORDER BY pdl.downloaded_at DESC
LIMIT 20;

-- 9️⃣ Vérifier les connexions des étudiants
SELECT '=== 9. DERNIÈRES CONNEXIONS ===' as '';
SELECT 
    ual.user_id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    MAX(ual.date_connexion) as derniere_connexion,
    DATEDIFF(NOW(), MAX(ual.date_connexion)) as jours_depuis
FROM user_activity_log ual
JOIN utilisateur u ON u.id = ual.user_id
WHERE u.role = 'STUDENT'
GROUP BY ual.user_id
ORDER BY derniere_connexion DESC;

-- 🔟 Test de la requête du service (version simplifiée)
SELECT '=== 10. TEST REQUÊTE SERVICE ===' as '';
SELECT 
    u.id,
    u.nom,
    u.prenom,
    u.email,
    (SELECT COUNT(*) FROM chapitre WHERE cours_id = 1) as total_chapitres,
    (SELECT COUNT(DISTINCT cp.chapitre_id) 
     FROM chapitre_progress cp
     JOIN chapitre c ON c.id = cp.chapitre_id
     WHERE cp.student_id = u.id 
     AND c.cours_id = 1 
     AND cp.completed = TRUE) as chapitres_lus
FROM utilisateur u
WHERE u.role = 'STUDENT'
ORDER BY u.nom;

-- ═══════════════════════════════════════════════════════════════
-- 📋 INSTRUCTIONS
-- ═══════════════════════════════════════════════════════════════
-- 1. Copiez tout ce fichier
-- 2. Ouvrez MySQL Workbench ou phpMyAdmin
-- 3. Sélectionnez la base de données 'educompus'
-- 4. Collez et exécutez toutes les requêtes
-- 5. Envoyez-moi les résultats des requêtes 4, 5 et 10
-- ═══════════════════════════════════════════════════════════════
