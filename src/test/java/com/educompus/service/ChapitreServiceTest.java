package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ChapitreService.
 * Nécessite une connexion MySQL active (base educompus) et au moins un cours existant.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChapitreServiceTest {

    private static ChapitreService service;
    private static CoursService coursService;
    private static int idChapitreTest;
    private static int idCoursSupport; // cours temporaire pour les tests

    @BeforeAll
    static void setUp() {
        service = new ChapitreService();
        coursService = new CoursService();

        // Créer un cours support pour les tests de chapitre
        Cours support = new Cours();
        support.setTitre("Cours Support Chapitre Test");
        support.setDescription("Cours temporaire pour tests chapitres.");
        support.setNiveau("1er");
        support.setDomaine("Informatique");
        support.setNomFormateur("Formateur Test");
        support.setDureeTotaleHeures(5);
        coursService.creer(support);

        idCoursSupport = coursService.listerTous("").stream()
            .filter(c -> c.getTitre().equals("Cours Support Chapitre Test"))
            .mapToInt(Cours::getId)
            .findFirst()
            .orElse(0);

        System.out.println("Cours support créé avec ID : " + idCoursSupport);
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- État après test ---");
        List<Chapitre> liste = service.listerTous("");
        liste.forEach(ch -> System.out.println("  Chapitre #" + ch.getId() + " : " + ch.getTitre()));
    }

    // ── Test 1 : Ajouter ────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testAjouterChapitre() {
        assertTrue(idCoursSupport > 0, "Un cours support doit exister.");

        Chapitre ch = new Chapitre();
        ch.setTitre("Chapitre Test JUnit");
        ch.setDescription("Description du chapitre de test.");
        ch.setOrdre(1);
        ch.setCoursId(idCoursSupport);
        ch.setFichierC("test_chapitre.pdf");
        ch.setNiveau("1er");
        ch.setDomaine("Informatique");

        service.creer(ch);

        List<Chapitre> liste = service.listerTous("");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout.");
        assertTrue(
            liste.stream().anyMatch(c -> c.getTitre().equals("Chapitre Test JUnit")),
            "Le chapitre ajouté doit être présent dans la liste."
        );

        idChapitreTest = liste.stream()
            .filter(c -> c.getTitre().equals("Chapitre Test JUnit"))
            .mapToInt(Chapitre::getId)
            .findFirst()
            .orElse(0);

        assertTrue(idChapitreTest > 0, "L'ID du chapitre ajouté doit être positif.");
        System.out.println("Chapitre ajouté avec ID : " + idChapitreTest);
    }

    // ── Test 2 : Afficher ────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testAfficherChapitres() {
        List<Chapitre> liste = service.listerTous("");
        assertNotNull(liste, "La liste ne doit pas être null.");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins un chapitre.");
        liste.forEach(ch -> assertNotNull(ch.getTitre(), "Le titre ne doit pas être null."));
    }

    // ── Test 3 : Modifier ────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testModifierChapitre() {
        List<Chapitre> liste = service.listerTous("");
        Chapitre cible = liste.stream()
            .filter(ch -> ch.getTitre().equals("Chapitre Test JUnit") || ch.getTitre().equals("Chapitre Modifie JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le chapitre de test doit exister pour être modifié.");

        cible.setTitre("Chapitre Modifie JUnit");
        cible.setDescription("Description modifiée du chapitre.");
        cible.setOrdre(2);

        service.modifier(cible);

        List<Chapitre> apres = service.listerTous("");
        boolean trouve = apres.stream().anyMatch(ch -> ch.getTitre().equals("Chapitre Modifie JUnit"));
        assertTrue(trouve, "Le chapitre modifié doit apparaître dans la liste.");
    }

    // ── Test 4 : Supprimer ───────────────────────────────────────────────────

    @Test
    @Order(4)
    void testSupprimerChapitre() {
        List<Chapitre> liste = service.listerTous("");
        Chapitre cible = liste.stream()
            .filter(ch -> ch.getTitre().equals("Chapitre Modifie JUnit") || ch.getTitre().equals("Chapitre Test JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le chapitre de test doit exister pour être supprimé.");
        int id = cible.getId();

        service.supprimer(id);

        List<Chapitre> apres = service.listerTous("");
        boolean existe = apres.stream().anyMatch(ch -> ch.getId() == id);
        assertFalse(existe, "Le chapitre supprimé ne doit plus être dans la liste.");

        // Nettoyage : supprimer le cours support
        if (idCoursSupport > 0) {
            coursService.supprimer(idCoursSupport);
        }
    }

    // ── Test 5 : Validation logique ──────────────────────────────────────────

    @Test
    @Order(5)
    void testValidationChapitreInvalide() {
        Chapitre invalide = new Chapitre();
        invalide.setTitre("AB");       // trop court
        invalide.setOrdre(0);          // invalide
        invalide.setCoursId(0);        // invalide
        invalide.setFichierC("doc.docx"); // pas un PDF

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Un chapitre invalide ne doit pas passer la validation.");
        assertTrue(result.getErrors().size() >= 3, "Au moins 3 erreurs attendues.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }
}
