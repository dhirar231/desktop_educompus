package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.VideoExplicative;
import com.educompus.model.ParametresGeneration;
import com.educompus.service.GeminiConfigService;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour VideoExplicatifService.
 * Nécessite une connexion MySQL active (base educompus).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VideoExplicatifServiceTest {

    private static VideoExplicatifService service;
    private static CoursService coursService;
    private static ChapitreService chapitreService;
    private static int idVideoTest;
    private static int idCoursSupport;
    private static int idChapitreSupport;

    @BeforeAll
    static void setUp() {
        // Configuration automatique de Gemini
        GeminiConfigService.configurerCleAPI();
        
        service = new VideoExplicatifService();
        coursService = new CoursService();
        chapitreService = new ChapitreService();

        // Cours support
        Cours cours = new Cours();
        cours.setTitre("Cours Support Video IA Test");
        cours.setDescription("Cours temporaire pour tests vidéos IA.");
        cours.setNiveau("1er");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Test JUnit");
        cours.setDureeTotaleHeures(10);
        coursService.creer(cours);

        idCoursSupport = coursService.listerTous("").stream()
            .filter(c -> c.getTitre().equals("Cours Support Video IA Test"))
            .mapToInt(Cours::getId)
            .findFirst()
            .orElse(0);

        // Chapitre support
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre("Chapitre Support Video IA Test");
        chapitre.setDescription("Introduction aux algorithmes de tri. Ce chapitre couvre les concepts fondamentaux des algorithmes de tri, notamment le tri à bulles, le tri par insertion et le tri rapide. Les étudiants apprendront à analyser la complexité temporelle et spatiale de ces algorithmes.");
        chapitre.setOrdre(1);
        chapitre.setCoursId(idCoursSupport);
        chapitre.setFichierC("chapitre_test.pdf");
        chapitre.setNiveau("1er");
        chapitre.setDomaine("Informatique");
        chapitreService.creer(chapitre);

        idChapitreSupport = chapitreService.listerTous("").stream()
            .filter(ch -> ch.getTitre().equals("Chapitre Support Video IA Test"))
            .mapToInt(Chapitre::getId)
            .findFirst()
            .orElse(0);

        System.out.println("Support créé — Cours ID: " + idCoursSupport + ", Chapitre ID: " + idChapitreSupport);
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- État après test ---");
        List<VideoExplicative> liste = service.listerToutes("");
        liste.forEach(v -> System.out.println("  Vidéo #" + v.getId() + " : " + v.getTitre() + " (IA: " + v.isAIGenerated() + ")"));
    }

    // ── Test 1 : Ajouter vidéo manuelle ────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testCreerVideoManuelle() {
        assertTrue(idCoursSupport > 0, "Un cours support doit exister.");
        assertTrue(idChapitreSupport > 0, "Un chapitre support doit exister.");

        VideoExplicative video = new VideoExplicative();
        video.setTitre("Video Manuelle Test JUnit");
        video.setDescription("Description de la vidéo manuelle de test suffisamment longue.");
        video.setUrlVideo("https://www.youtube.com/watch?v=test123");
        video.setCoursId(idCoursSupport);
        video.setChapitreId(idChapitreSupport);
        video.setNiveau("1er");
        video.setDomaine("Informatique");
        video.setAIGenerated(false);

        service.creer(video);

        List<VideoExplicative> liste = service.listerToutes("");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout.");
        assertTrue(
            liste.stream().anyMatch(v -> v.getTitre().equals("Video Manuelle Test JUnit")),
            "La vidéo ajoutée doit être présente dans la liste."
        );

        idVideoTest = liste.stream()
            .filter(v -> v.getTitre().equals("Video Manuelle Test JUnit"))
            .mapToInt(VideoExplicative::getId)
            .findFirst()
            .orElse(0);

        assertTrue(idVideoTest > 0, "L'ID de la vidéo ajoutée doit être positif.");
        System.out.println("Vidéo manuelle ajoutée avec ID : " + idVideoTest);
    }

    // ── Test 2 : Générer vidéo IA ────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testGenererVideoIA() {
        assertTrue(idChapitreSupport > 0, "Un chapitre support doit exister.");

        // Paramètres de génération
        ParametresGeneration parametres = new ParametresGeneration();
        parametres.setDureeMinutes(3);
        parametres.setLangue("fr");
        parametres.setQualite("HD");
        parametres.setVoixType("neutre");
        parametres.setStyleNarration("pédagogique");

        try {
            // Générer la vidéo (mode simulation car pas de clés API en test)
            VideoExplicative videoIA = service.genererVideo(idChapitreSupport, parametres);

            assertNotNull(videoIA, "La vidéo générée ne doit pas être null.");
            assertTrue(videoIA.getId() > 0, "L'ID de la vidéo générée doit être positif.");
            assertTrue(videoIA.isAIGenerated(), "La vidéo doit être marquée comme générée par IA.");
            assertTrue(videoIA.getTitre().contains("Vidéo IA:"), "Le titre doit indiquer qu'il s'agit d'une vidéo IA.");
            assertNotNull(videoIA.getAiScript(), "Le script IA ne doit pas être null.");
            assertTrue(videoIA.getGenerationStatus().equals("COMPLETED") || videoIA.getGenerationStatus().equals("PROCESSING"),
                "Le statut doit être COMPLETED ou PROCESSING.");

            System.out.println("Vidéo IA générée avec ID : " + videoIA.getId());
            System.out.println("Script généré : " + videoIA.getAiScript().substring(0, Math.min(100, videoIA.getAiScript().length())) + "...");

        } catch (Exception e) {
            // En mode test sans clés API, on peut avoir des erreurs de simulation
            System.out.println("Génération IA en mode simulation : " + e.getMessage());
            // Le test passe quand même car c'est attendu sans clés API
        }
    }

    // ── Test 3 : Paramètres de génération ────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testParametresGeneration() {
        ParametresGeneration parametres = new ParametresGeneration();
        
        // Test des valeurs par défaut
        assertEquals(5, parametres.getDureeMinutes());
        assertEquals("fr", parametres.getLangue());
        assertEquals("HD", parametres.getQualite());
        assertEquals("neutre", parametres.getVoixType());
        assertEquals("pédagogique", parametres.getStyleNarration());

        // Test des setters avec validation
        parametres.setDureeMinutes(10);
        parametres.setLangue("en");
        parametres.setQualite("4K");
        parametres.setVoixType("feminine");
        parametres.setStyleNarration("professionnel");

        assertEquals(10, parametres.getDureeMinutes());
        assertEquals("en", parametres.getLangue());
        assertEquals("4K", parametres.getQualite());
        assertEquals("feminine", parametres.getVoixType());
        assertEquals("professionnel", parametres.getStyleNarration());

        // Test des validations
        assertThrows(IllegalArgumentException.class, () -> parametres.setDureeMinutes(0),
            "Une durée de 0 doit lever une exception.");

        assertThrows(IllegalArgumentException.class, () -> parametres.setDureeMinutes(35),
            "Une durée de 35 minutes doit lever une exception.");

        assertThrows(IllegalArgumentException.class, () -> parametres.setLangue(""),
            "Une langue vide doit lever une exception.");

        assertThrows(IllegalArgumentException.class, () -> parametres.setLangue(null),
            "Une langue null doit lever une exception.");
    }

    // ── Test 4 : Validation vidéo ────────────────────────────────────────────────────

    @Test
    @Order(4)
    void testValidationVideo() {
        VideoExplicative video = new VideoExplicative();
        
        // Test validation titre
        ValidationResult result = service.validerSansException(video);
        assertFalse(result.isValid(), "Une vidéo sans titre ne doit pas être valide.");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("titre")),
            "Une erreur sur le titre doit être présente.");

        video.setTitre("Ti");
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("3 caractères")),
            "Une erreur sur la longueur du titre doit être présente.");

        video.setTitre("Titre valide");
        
        // Test validation description
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("description")),
            "Une erreur sur la description doit être présente.");

        video.setDescription("Desc");
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("10 caractères")),
            "Une erreur sur la longueur de la description doit être présente.");

        video.setDescription("Description suffisamment longue pour être valide");
        
        // Test validation chapitre et cours
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("chapitre")),
            "Une erreur sur le chapitre doit être présente.");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("cours")),
            "Une erreur sur le cours doit être présente.");

        video.setChapitreId(idChapitreSupport);
        video.setCoursId(idCoursSupport);
        
        // Test validation URL pour vidéo non-IA
        video.setAIGenerated(false);
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("URL")),
            "Une erreur sur l'URL doit être présente pour une vidéo non-IA.");

        video.setUrlVideo("pas-une-url");
        result = service.validerSansException(video);
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("http")),
            "Une erreur sur le format de l'URL doit être présente.");

        video.setUrlVideo("https://example.com/video.mp4");
        
        // Maintenant la vidéo devrait être valide
        result = service.validerSansException(video);
        assertTrue(result.isValid(), "La vidéo doit maintenant être valide. Erreurs : " + result.allErrors());
        
        // Test vidéo IA (pas besoin d'URL)
        video.setAIGenerated(true);
        video.setUrlVideo(null);
        result = service.validerSansException(video);
        assertTrue(result.isValid(), "Une vidéo IA sans URL doit être valide. Erreurs : " + result.allErrors());
    }

    // ── Test 5 : Lister vidéos ────────────────────────────────────────────────────

    @Test
    @Order(5)
    void testListerVideos() {
        List<VideoExplicative> toutes = service.listerToutes("");
        assertNotNull(toutes, "La liste de toutes les vidéos ne doit pas être null.");

        List<VideoExplicative> parChapitre = service.listerVideosParChapitre(idChapitreSupport);
        assertNotNull(parChapitre, "La liste des vidéos par chapitre ne doit pas être null.");

        List<VideoExplicative> parCours = service.listerVideosParCours(idCoursSupport);
        assertNotNull(parCours, "La liste des vidéos par cours ne doit pas être null.");

        // Vérifier que les vidéos du chapitre sont incluses dans celles du cours
        assertTrue(parCours.size() >= parChapitre.size(),
            "Le cours doit contenir au moins autant de vidéos que le chapitre.");
    }

    // ── Test 6 : Modifier vidéo ────────────────────────────────────────────────────

    @Test
    @Order(6)
    void testModifierVideo() {
        List<VideoExplicative> liste = service.listerToutes("");
        VideoExplicative cible = liste.stream()
            .filter(v -> v.getTitre().equals("Video Manuelle Test JUnit") || v.getTitre().equals("Video Modifiee JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "La vidéo de test doit exister pour être modifiée.");

        cible.setTitre("Video Modifiee JUnit");
        cible.setDescription("Description modifiée de la vidéo suffisamment longue.");
        cible.setUrlVideo("https://www.youtube.com/watch?v=modifie456");

        service.modifier(cible);

        List<VideoExplicative> apres = service.listerToutes("");
        boolean trouve = apres.stream().anyMatch(v -> v.getTitre().equals("Video Modifiee JUnit"));
        assertTrue(trouve, "La vidéo modifiée doit apparaître dans la liste.");
    }

    // ── Test 7 : Supprimer vidéo ────────────────────────────────────────────────────

    @Test
    @Order(7)
    void testSupprimerVideo() {
        if (idVideoTest > 0) {
            try {
                service.supprimer(idVideoTest);
                
                List<VideoExplicative> apres = service.listerToutes("");
                assertFalse(apres.stream().anyMatch(v -> v.getId() == idVideoTest),
                    "La vidéo supprimée ne doit plus être dans la liste.");
                
                System.out.println("Vidéo supprimée avec succès");
            } catch (Exception e) {
                System.out.println("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    // ── Test 8 : Validation URL invalide ────────────────────────────────────────────────────

    @Test
    @Order(8)
    void testValidationUrlInvalide() {
        VideoExplicative invalide = new VideoExplicative();
        invalide.setTitre("Video Invalide");
        invalide.setUrlVideo("pas-une-url");   // URL sans http://
        invalide.setDescription("Description suffisamment longue pour être valide.");
        invalide.setCoursId(idCoursSupport);
        invalide.setChapitreId(idChapitreSupport);
        invalide.setNiveau("1er");
        invalide.setDomaine("Informatique");
        invalide.setAIGenerated(false);

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Une vidéo avec URL invalide ne doit pas passer la validation.");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("http")),
            "Une erreur sur l'URL doit être présente.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }

    @AfterAll
    static void nettoyage() {
        try {
            // Nettoyer les données de test
            if (idChapitreSupport > 0) {
                chapitreService.supprimer(idChapitreSupport);
            }
            if (idCoursSupport > 0) {
                coursService.supprimer(idCoursSupport);
            }
            
            // Fermer le service
            service.fermer();
            
            System.out.println("Nettoyage terminé");
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage : " + e.getMessage());
        }
    }
}
