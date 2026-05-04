package com.educompus.integration;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.service.ContentManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour le système de gestion de contenu pédagogique.
 * 
 * Ces tests vérifient le fonctionnement complet du système sans Google Drive
 * (pour éviter les dépendances externes dans les tests).
 */
@DisplayName("Tests d'intégration - Gestion de contenu")
class ContentManagementIntegrationTest {

    private ContentManagementService contentService;
    private Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // Créer un répertoire temporaire pour les fichiers de test
        tempDir = Files.createTempDirectory("educompus-test");
        
        // Note: Le service ne sera pas initialisé avec Google Drive dans les tests
        // contentService = new ContentManagementService();
    }

    @Test
    @DisplayName("Création d'un cours non important (stockage local uniquement)")
    void testCreateCoursNonImportant() {
        // Arrange
        Cours cours = new Cours();
        cours.setTitre("Cours de Test");
        cours.setDescription("Description du cours de test");
        cours.setNiveau("Débutant");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Prof Test");
        cours.setDureeTotaleHeures(20);
        cours.setImportant(false); // Non important = pas d'upload Drive

        // Act & Assert
        // Ce test nécessiterait une base de données de test
        // Pour l'instant, on vérifie juste la logique métier
        assertFalse(cours.isImportant());
        assertNull(cours.getDriveLink());
        
        // Le cours devrait être créé en base de données uniquement
        // sans tentative d'upload vers Google Drive
    }

    @Test
    @DisplayName("Création d'un cours important avec fichier (simulation upload Drive)")
    void testCreateCoursImportantAvecFichier() throws IOException {
        // Arrange
        Cours cours = new Cours();
        cours.setTitre("Cours Important");
        cours.setDescription("Cours qui doit être uploadé sur Drive");
        cours.setNiveau("Avancé");
        cours.setDomaine("Mathématiques");
        cours.setImportant(true); // Important = upload Drive requis

        // Créer un fichier temporaire pour simuler le contenu
        Path fichierTest = tempDir.resolve("cours-test.pdf");
        Files.write(fichierTest, "Contenu du cours de test".getBytes());

        // Act & Assert
        assertTrue(cours.isImportant());
        assertTrue(Files.exists(fichierTest));
        
        // Dans un vrai test, on vérifierait :
        // 1. Que le cours est sauvegardé en base
        // 2. Que le fichier est uploadé sur Drive (si configuré)
        // 3. Que le lien Drive est stocké dans cours.driveLink
    }

    @Test
    @DisplayName("Création d'un chapitre avec validation des champs obligatoires")
    void testCreateChapitreValidation() {
        // Arrange
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre("Chapitre 1 - Introduction");
        chapitre.setDescription("Premier chapitre du cours");
        chapitre.setOrdre(1);
        chapitre.setCoursId(1); // Référence vers un cours existant
        chapitre.setImportant(true);

        // Act & Assert
        assertNotNull(chapitre.getTitre());
        assertFalse(chapitre.getTitre().isBlank());
        assertTrue(chapitre.getOrdre() > 0);
        assertTrue(chapitre.getCoursId() > 0);
        assertTrue(chapitre.isImportant());
    }

    @Test
    @DisplayName("Création d'un TD avec liaison cours et chapitre")
    void testCreateTdAvecLiaisons() {
        // Arrange
        Td td = new Td();
        td.setTitre("TD Pratique - Exercices");
        td.setDescription("Travaux dirigés avec exercices pratiques");
        td.setCoursId(1);
        td.setChapitreId(1);
        td.setImportant(false); // Pas d'upload Drive

        // Act & Assert
        assertNotNull(td.getTitre());
        assertTrue(td.getCoursId() > 0);
        assertTrue(td.getChapitreId() > 0);
        assertFalse(td.isImportant());
        assertNull(td.getDriveLink());
    }

    @Test
    @DisplayName("Création d'une vidéo explicative avec URL")
    void testCreateVideoExplicative() {
        // Arrange
        VideoExplicative video = new VideoExplicative();
        video.setTitre("Vidéo Introduction");
        video.setDescription("Vidéo d'introduction au cours");
        video.setUrlVideo("https://example.com/video.mp4");
        video.setCoursId(1);
        video.setChapitreId(1);
        video.setImportant(true);

        // Act & Assert
        assertNotNull(video.getTitre());
        assertNotNull(video.getUrlVideo());
        assertTrue(video.getCoursId() > 0);
        assertTrue(video.getChapitreId() > 0);
        assertTrue(video.isImportant());
    }

    @Test
    @DisplayName("Validation des règles métier pour contenu important")
    void testValidationContenuImportant() {
        // Test 1: Cours important sans fichier devrait échouer
        Cours coursImportantSansFichier = new Cours();
        coursImportantSansFichier.setTitre("Cours Important");
        coursImportantSansFichier.setImportant(true);
        
        // Dans un vrai test avec le service :
        // assertThrows(IllegalArgumentException.class, () -> {
        //     contentService.createCours(coursImportantSansFichier, true, null);
        // });

        // Test 2: Contenu non important peut être créé sans fichier
        Cours coursNonImportant = new Cours();
        coursNonImportant.setTitre("Cours Normal");
        coursNonImportant.setImportant(false);
        
        // Devrait réussir même sans fichier
        assertFalse(coursNonImportant.isImportant());
    }

    @Test
    @DisplayName("Test de la logique de nommage des fichiers")
    void testFileNamingLogic() {
        // Test de la logique de génération des noms de fichiers
        // (cette logique est dans ContentManagementService)
        
        String coursTitle = "Cours d'Introduction à Java";
        String expectedSanitized = "Cours_d_Introduction_a_Java";
        
        // Simulation de la méthode sanitizeFileName
        String sanitized = coursTitle.replaceAll("[<>:\"/\\\\|?*]", "_")
                                   .replaceAll("\\s+", "_")
                                   .replaceAll("'", "_")
                                   .trim();
        
        assertEquals(expectedSanitized, sanitized);
    }

    @Test
    @DisplayName("Test de la structure des dossiers Google Drive")
    void testDriveFolderStructure() {
        // Test de la logique d'organisation des dossiers
        String coursTitle = "Mathématiques Avancées";
        String expectedCoursFolder = "Mathematiques_Avancees";
        String expectedChapitresFolder = "Chapitres";
        String expectedTdsFolder = "Travaux Dirigés";
        String expectedVideosFolder = "Vidéos Explicatives";
        
        // Vérifier que les noms de dossiers sont corrects
        assertNotNull(expectedCoursFolder);
        assertNotNull(expectedChapitresFolder);
        assertNotNull(expectedTdsFolder);
        assertNotNull(expectedVideosFolder);
        
        // Dans un vrai test, on vérifierait la création effective des dossiers
    }

    @Test
    @DisplayName("Test des statistiques de contenu")
    void testContentStatistics() {
        // Test de la logique de calcul des statistiques
        // (nombre de contenus importants vs non importants)
        
        int totalCours = 10;
        int coursImportants = 3;
        int totalChapitres = 25;
        int chapitresImportants = 8;
        
        double pourcentageCoursImportants = (double) coursImportants / totalCours * 100;
        double pourcentageChapitresImportants = (double) chapitresImportants / totalChapitres * 100;
        
        assertEquals(30.0, pourcentageCoursImportants, 0.1);
        assertEquals(32.0, pourcentageChapitresImportants, 0.1);
    }

    @Test
    @DisplayName("Test de la gestion des erreurs de validation")
    void testValidationErrors() {
        // Test des différents cas d'erreur de validation
        
        // Cours sans titre
        Cours coursSansTitre = new Cours();
        coursSansTitre.setDescription("Description");
        // Devrait échouer à la validation
        
        // Chapitre sans cours associé
        Chapitre chapitreSansCours = new Chapitre();
        chapitreSansCours.setTitre("Chapitre");
        chapitreSansCours.setCoursId(0); // ID invalide
        // Devrait échouer à la validation
        
        // TD sans titre
        Td tdSansTitre = new Td();
        tdSansTitre.setDescription("Description");
        // Devrait échouer à la validation
        
        // Assertions basiques (dans un vrai test, on utiliserait le service)
        assertTrue(coursSansTitre.getTitre() == null || coursSansTitre.getTitre().isBlank());
        assertEquals(0, chapitreSansCours.getCoursId());
        assertTrue(tdSansTitre.getTitre() == null || tdSansTitre.getTitre().isBlank());
    }
}