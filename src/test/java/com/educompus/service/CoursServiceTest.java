package com.educompus.service;

import com.educompus.model.Cours;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursServiceTest {

    private static CoursService service;
    private static int idCoursTest;

    @BeforeAll
    static void setUp() {
        service = new CoursService();
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- État après test ---");
        List<Cours> liste = service.listerTous("");
        liste.forEach(c -> System.out.println("  Cours #" + c.getId() + " : " + c.getTitre()));
    }

    // Test 1 : Ajouter

    @Test
    @Order(1)
    void testAjouterCours() {
        Cours c = new Cours();
        c.setTitre("Cours Test JUnit");
        c.setDescription("Description de test suffisamment longue.");
        c.setNiveau("1er");
        c.setDomaine("Informatique");
        c.setNomFormateur("Formateur Test");
        c.setDureeTotaleHeures(10);

        service.creer(c);

        List<Cours> liste = service.listerTous("");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout.");
        assertTrue(
            liste.stream().anyMatch(cours -> cours.getTitre().equals("Cours Test JUnit")),
            "Le cours ajouté doit être présent dans la liste."
        );

        // Récupérer l'ID pour les tests suivants
        idCoursTest = liste.stream()
            .filter(cours -> cours.getTitre().equals("Cours Test JUnit"))
            .mapToInt(Cours::getId)
            .findFirst()
            .orElse(0);

        assertTrue(idCoursTest > 0, "L'ID du cours ajouté doit être positif.");
        System.out.println("Cours ajouté avec ID : " + idCoursTest);
    }

    //  Test 2 : Afficher

    @Test
    @Order(2)
    void testAfficherCours() {
        List<Cours> liste = service.listerTous("");
        assertNotNull(liste, "La liste ne doit pas être null.");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins un cours.");
        liste.forEach(c -> assertNotNull(c.getTitre(), "Le titre ne doit pas être null."));
    }

    //  Test 3 : Modifier

    @Test
    @Order(3)
    void testModifierCours() {
        // Récupérer l'ID du cours de test
        List<Cours> liste = service.listerTous("");
        Cours cible = liste.stream()
            .filter(c -> c.getTitre().equals("Cours Test JUnit") || c.getTitre().equals("Cours Modifie JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le cours de test doit exister pour être modifié.");

        cible.setTitre("Cours Modifie JUnit");
        cible.setDescription("Description modifiée suffisamment longue.");
        cible.setNomFormateur("Formateur Modifie");
        cible.setDureeTotaleHeures(20);

        service.modifier(cible);

        List<Cours> apres = service.listerTous("");
        boolean trouve = apres.stream().anyMatch(c -> c.getTitre().equals("Cours Modifie JUnit"));
        assertTrue(trouve, "Le cours modifié doit apparaître dans la liste.");
    }

    //  Test 4 : Supprimer

    @Test
    @Order(4)
    void testSupprimerCours() {
        List<Cours> liste = service.listerTous("");
        Cours cible = liste.stream()
            .filter(c -> c.getTitre().equals("Cours Modifie JUnit") || c.getTitre().equals("Cours Test JUnit"))
            .findFirst()
            .orElse(null);

        assertNotNull(cible, "Le cours de test doit exister pour être supprimé.");
        int id = cible.getId();

        service.supprimer(id);

        List<Cours> apres = service.listerTous("");
        boolean existe = apres.stream().anyMatch(c -> c.getId() == id);
        assertFalse(existe, "Le cours supprimé ne doit plus être dans la liste.");
    }

    //  Test 5 :

    @Test
    @Order(5)
    void testValidationCoursInvalide() {
        Cours invalide = new Cours();
        invalide.setTitre("AB");           // trop court
        invalide.setDescription("Court"); // trop court
        invalide.setNiveau("1er");
        invalide.setDomaine("Informatique");
        invalide.setNomFormateur("Test");
        invalide.setDureeTotaleHeures(0); // invalide

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Un cours invalide ne doit pas passer la validation.");
        assertFalse(result.getErrors().isEmpty(), "Des erreurs doivent être retournées.");
        System.out.println("Erreurs détectées : " + result.allErrors());
    }
}
