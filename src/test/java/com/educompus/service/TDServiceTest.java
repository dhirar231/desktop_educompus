package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TDService.
 * Nécessite une connexion MySQL active (base educompus).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TDServiceTest {

    private static TDService service;
    private static CoursService coursService;
    private static ChapitreService chapitreService;
    private static int idTdTest;
    private static int idCoursSupport;
    private static int idChapitreSupport;

    @BeforeAll
    static void setUp() {
        service = new TDService();
        coursService = new CoursService();
        chapitreService = new ChapitreService();

        // Cours support
        Cours cours = new Cours();
        cours.setTitre("Cours Support TD Test");
        cours.setDescription("Cours temporaire pour tests TD.");
        cours.setNiveau("1er");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Formateur Test");
        cours.setDureeTotaleHeures(5);
        coursService.creer(cours);

        idCoursSupport = coursService.listerTous("").stream()
            .filter(c -> c.getTitre().equals("Cours Support TD Test"))
            .mapToInt(Cours::getId)
            .findFirst()
            .orElse(0);

        // Chapitre support
        Chapitre chapitre = new Chapitre();
        chapitre.setTitre("Chapitre Support TD Test");
        chapitre.setDescription("Chapitre temporaire pour tests TD.");
        chapitre.setOrdre(1);
        chapitre.setCoursId(idCoursSupport);
        chapitre.setFichierC("support.pdf");
        chapitre.setNiveau("1er");
        chapitre.setDomaine("Informatique");
        chapitreService.creer(chapitre);

        idChapitreSupport = chapitreService.listerTous("").stream()
            .filter(ch -> ch.getTitre().equals("Chapitre Support TD Test"))
            .mapToInt(Chapitre::getId)
            .findFirst()
            .orElse(0);

        System.out.println("Support créé — Cours ID: " + idCoursSupport + ", Chapitre ID: " + idChapitreSupport);
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- État après test ---");
        List<Td> liste = service.listerTous("");
        liste.forEach(td -> System.out.println("  TD #" + td.getId() + " : " + td.getTitre()));
    }

    // ── Test 1 : Ajouter ────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testAjouterTd() {
        assertTrue(idCoursSupport > 0, "Un cours support doit exister.");
        assertTrue(idChapitreSupport > 0, "Un chapitre support doit exister.");

        Td td = new Td();
        td.setTitre("TD Test JUnit");
        td.setDescription("Description du TD de test suffisamment longue.");
        td.setFichier("td_test.pdf");
        td.setCoursId(idCoursSupport);
        td.setChapitreId(idChapitreSupport);
        td.setNiveau("1er");
        td.setDomaine("Informatique");

        service.creer(td);

        List<Td> liste = service.listerTous("");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout.");
        assertTrue(
            liste.stream().anyMatch(t -> t.getTitre().equals("TD Test JUnit")),
            "Le TD ajouté doit être présent dans la liste."
        );

        idTdTest = liste.stream()
            .filter(t -> t.getTitre().equals("TD Test JUnit"))
            .mapToInt(Td::getId)
            .findFirst()
            .orElse(0);

        assertTrue(idTdTest > 0, "L'ID du TD ajouté doit être positif.");
        System.out.println("TD ajouté avec ID : " + idTdTest);
    }

    // ── Test 2 : Afficher ────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testAfficherTds() {
        List<Td> liste = service.listerTous("");
        assertNotNull(liste, "La liste ne doit pas être null.");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins un TD.");
        liste.forEach(td -> assertNotNull(td.getTitre(), "Le titre ne doit pas être null."));
    }

    // ── Test 3 : Modifier ────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testModifierTd() {
        List<Td> liste = service.listerTous("");
        Td cible = liste.stream()
            .filter(t -> t.getTitre().equals("TD Test JUnit") || t.getTitre().equals("TD Modifie JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le TD de test doit exister pour être modifié.");

        cible.setTitre("TD Modifie JUnit");
        cible.setDescription("Description modifiée du TD suffisamment longue.");

        service.modifier(cible);

        List<Td> apres = service.listerTous("");
        boolean trouve = apres.stream().anyMatch(t -> t.getTitre().equals("TD Modifie JUnit"));
        assertTrue(trouve, "Le TD modifié doit apparaître dans la liste.");
    }

    // ── Test 4 : Supprimer ───────────────────────────────────────────────────

    @Test
    @Order(4)
    void testSupprimerTd() {
        List<Td> liste = service.listerTous("");
        Td cible = liste.stream()
            .filter(t -> t.getTitre().equals("TD Modifie JUnit") || t.getTitre().equals("TD Test JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le TD de test doit exister pour être supprimé.");
        int id = cible.getId();

        service.supprimer(id);

        List<Td> apres = service.listerTous("");
        boolean existe = apres.stream().anyMatch(t -> t.getId() == id);
        assertFalse(existe, "Le TD supprimé ne doit plus être dans la liste.");

        // Nettoyage
        if (idChapitreSupport > 0) chapitreService.supprimer(idChapitreSupport);
        if (idCoursSupport > 0) coursService.supprimer(idCoursSupport);
    }

    // ── Test 5 : Validation logique ──────────────────────────────────────────

    @Test
    @Order(5)
    void testValidationTdInvalide() {
        Td invalide = new Td();
        invalide.setTitre("T");          // trop court
        invalide.setChapitreId(0);       // invalide
        invalide.setCoursId(0);          // invalide
        invalide.setFichier("cours.docx"); // pas un PDF

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Un TD invalide ne doit pas passer la validation.");
        assertTrue(result.getErrors().size() >= 3, "Au moins 3 erreurs attendues.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }
}
