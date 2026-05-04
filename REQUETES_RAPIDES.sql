-- ═══════════════════════════════════════════════════════════════
-- 🔍 REQUÊTES RAPIDES - Copier-Coller dans MySQL
-- ═══════════════════════════════════════════════════════════════

-- 1️⃣ Vérifier si les chapitres sont marqués comme lus
SELECT 
    cp.student_id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    cp.chapitre_id,
    ch.titre as chapitre_titre,
    co.titre as cours_titre,
    cp.completed,
    cp.completed_at
FROM chapitre_progress cp
JOIN utilisateur u ON u.id = cp.student_id
JOIN chapitre ch ON ch.id = cp.chapitre_id
JOIN cours co ON co.id = ch.cours_id
WHERE cp.completed = 1
ORDER BY cp.completed_at DESC;

-- ═══════════════════════════════════════════════════════════════

-- 2️⃣ Statistiques par étudiant et par cours
SELECT 
    u.id as student_id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    co.id as cours_id,
    co.titre as cours,
    COUNT(DISTINCT CASE WHEN cp.completed = 1 THEN cp.chapitre_id END) as chapitres_lus,
    (SELECT COUNT(*) FROM chapitre WHERE cours_id = co.id) as total_chapitres,
    ROUND(
        COUNT(DISTINCT CASE WHEN cp.completed = 1 THEN cp.chapitre_id END) * 100.0 / 
        NULLIF((SELECT COUNT(*) FROM chapitre WHERE cours_id = co.id), 0),
        0
    ) as pourcentage
FROM utilisateur u
CROSS JOIN cours co
LEFT JOIN chapitre ch ON ch.cours_id = co.id
LEFT JOIN chapitre_progress cp ON cp.chapitre_id = ch.id AND cp.student_id = u.id
WHERE u.role = 'STUDENT'
GROUP BY u.id, co.id
HAVING chapitres_lus > 0
ORDER BY co.titre, u.nom;

-- ═══════════════════════════════════════════════════════════════

-- 3️⃣ Test de la requête du service (celle qui est utilisée dans le code)
SELECT 
    u.id,
    CONCAT(u.prenom, ' ', u.nom) as etudiant,
    u.email,
    (SELECT COUNT(*) FROM chapitre WHERE cours_id = 1) as total_chapitres,
    (SELECT COUNT(DISTINCT cp.chapitre_id) 
     FROM chapitre_progress cp
     JOIN chapitre c ON c.id = cp.chapitre_id
     WHERE cp.student_id = u.id 
     AND c.cours_id = 1 
     AND cp.completed = TRUE) as chapitres_lus,
    ROUND(
        (SELECT COUNT(DISTINCT cp.chapitre_id) 
         FROM chapitre_progress cp
         JOIN chapitre c ON c.id = cp.chapitre_id
         WHERE cp.student_id = u.id 
         AND c.cours_id = 1 
         AND cp.completed = TRUE) * 100.0 /
        NULLIF((SELECT COUNT(*) FROM chapitre WHERE cours_id = 1), 0),
        0
    ) as pourcentage
FROM utilisateur u
WHERE u.role = 'STUDENT'
ORDER BY u.nom;

-- ⚠️ IMPORTANT : Remplacer "cours_id = 1" par l'ID de votre cours !
-- Pour trouver l'ID de votre cours :
SELECT id, titre FROM cours WHERE titre LIKE '%analyse%';

-- ═══════════════════════════════════════════════════════════════
-- 📋 INSTRUCTIONS
-- ═══════════════════════════════════════════════════════════════
-- 1. Ouvrir MySQL Workbench ou phpMyAdmin
-- 2. Sélectionner la base de données 'educompus'
-- 3. Copier-coller chaque requête une par une
-- 4. Exécuter et noter les résultats
-- ═══════════════════════════════════════════════════════════════
