package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.VideoExplicative;
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
        service = new VideoExplicatifService();
        coursService = new CoursService();
        chapitreService = new ChapitreService();

        // Cours support
        Cours cours = new Cours();
        cours.setTitre("Cours Support Video Test");
        cours.setDescription("Cours temporaire pour tests vidéos.");
        cours.setNiveau("1er");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Formateur Test");
        cours.setDureeTotaleHeures(5);
        coursService.creer(cours);

        idCoursSupport = coursService.listerTous("").stream()
            .filter(c -> c.getTitre().equals("Cours Support Video Test"))
            .mapToInt(Cours::getId)
            .findFirst()
            .orElse(0);

        // Chapitre support
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre("Chapitre Support Video Test");
        chapitre.setDescription("Chapitre temporaire pour tests vidéos.");
        chapitre.setOrdre(1);
        chapitre.setCoursId(idCoursSupport);
        chapitre.setFichierC("support.pdf");
        chapitre.setNiveau("1er");
        chapitre.setDomaine("Informatique");
        chapitreService.creer(chapitre);

        idChapitreSupport = chapitreService.listerTous("").stream()
            .filter(ch -> ch.getTitre().equals("Chapitre Support Video Test"))
            .mapToInt(Chapitre::getId)
            .findFirst()
            .orElse(0);

        System.out.println("Support créé — Cours ID: " + idCoursSupport + ", Chapitre ID: " + idChapitreSupport);
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- État après test ---");
        List<VideoExplicative> liste = service.listerToutes("");
        liste.forEach(v -> System.out.println("  Vidéo #" + v.getId() + " : " + v.getTitre()));
    }

    // ── Test 1 : Ajouter ────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testAjouterVideo() {
        assertTrue(idCoursSupport > 0, "Un cours support doit exister.");
        assertTrue(idChapitreSupport > 0, "Un chapitre support doit exister.");

        VideoExplicative video = new VideoExplicative();
        video.setTitre("Video Test JUnit");
        video.setDescription("Description de la vidéo de test suffisamment longue.");
        video.setUrlVideo("https://www.youtube.com/watch?v=test123");
        video.setCoursId(idCoursSupport);
        video.setChapitreId(idChapitreSupport);
        video.setNiveau("1er");
        video.setDomaine("Informatique");

        service.creer(video);

        List<VideoExplicative> liste = service.listerToutes("");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout.");
        assertTrue(
            liste.stream().anyMatch(v -> v.getTitre().equals("Video Test JUnit")),
            "La vidéo ajoutée doit être présente dans la liste."
        );

        idVideoTest = liste.stream()
            .filter(v -> v.getTitre().equals("Video Test JUnit"))
            .mapToInt(VideoExplicative::getId)
            .findFirst()
            .orElse(0);

        assertTrue(idVideoTest > 0, "L'ID de la vidéo ajoutée doit être positif.");
        System.out.println("Vidéo ajoutée avec ID : " + idVideoTest);
    }

    // ── Test 2 : Afficher ────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testAfficherVideos() {
        List<VideoExplicative> liste = service.listerToutes("");
        assertNotNull(liste, "La liste ne doit pas être null.");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins une vidéo.");
        liste.forEach(v -> assertNotNull(v.getTitre(), "Le titre ne doit pas être null."));
    }

    // ── Test 3 : Modifier ────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testModifierVideo() {
        List<VideoExplicative> liste = service.listerToutes("");
        VideoExplicative cible = liste.stream()
            .filter(v -> v.getTitre().equals("Video Test JUnit") || v.getTitre().equals("Video Modifiee JUnit"))
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

    // ── Test 4 : Supprimer ───────────────────────────────────────────────────

    @Test
    @Order(4)
    void testSupprimerVideo() {
        List<VideoExplicative> liste = service.listerToutes("");
        VideoExplicative cible = liste.stream()
            .filter(v -> v.getTitre().equals("Video Modifiee JUnit") || v.getTitre().equals("Video Test JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "La vidéo de test doit exister pour être supprimée.");
        int id = cible.getId();

        service.supprimer(id);

        List<VideoExplicative> apres = service.listerToutes("");
        boolean existe = apres.stream().anyMatch(v -> v.getId() == id);
        assertFalse(existe, "La vidéo supprimée ne doit plus être dans la liste.");

        // Nettoyage
        if (idChapitreSupport > 0) chapitreService.supprimer(idChapitreSupport);
        if (idCoursSupport > 0) coursService.supprimer(idCoursSupport);
    }

    // ── Test 5 : Validation — URL invalide ───────────────────────────────────

    @Test
    @Order(5)
    void testValidationUrlInvalide() {
        VideoExplicative invalide = new VideoExplicative();
        invalide.setTitre("Video Invalide");
        invalide.setUrlVideo("pas-une-url");   // URL sans http://
        invalide.setChapitreId(0);             // invalide
        invalide.setCoursId(0);                // invalide

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Une vidéo avec URL invalide ne doit pas passer la validation.");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("http")),
            "Une erreur sur l'URL doit être présente.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }

    // ── Test 6 : Validation — titre trop court ───────────────────────────────

    @Test
    @Order(6)
    void testValidationTitreTropCourt() {
        VideoExplicative invalide = new VideoExplicative();
        invalide.setTitre("AB");                              // trop court
        invalide.setUrlVideo("https://youtube.com/watch?v=x");
        invalide.setChapitreId(1);
        invalide.setCoursId(1);

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Un titre trop court doit échouer la validation.");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("caractères")),
            "Une erreur sur la longueur du titre doit être présente.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }

    // ── Test 7 : Validation — vidéo valide ───────────────────────────────────

    @Test
    @Order(7)
    void testValidationVideoValide() {
        VideoExplicative valide = new VideoExplicative();
        valide.setTitre("Introduction à Java");
        valide.setUrlVideo("https://www.youtube.com/watch?v=abc123");
        valide.setChapitreId(1);
        valide.setCoursId(1);

        ValidationResult result = service.validerSansException(valide);
        assertTrue(result.isValid(), "Une vidéo valide doit passer la validation. Erreurs : " + result.allErrors());
    }
}
