package com.educompus.service;

import com.educompus.app.AppState;
import com.educompus.model.Cours;
import com.educompus.model.CoursStatut;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoursServiceTest {

    private static CoursService service;
    private static CoursWorkflowService workflowService;

    @BeforeAll
    static void setUp() {
        service = new CoursService();
        workflowService = new CoursWorkflowService();
    }

    @AfterEach
    void afficherEtat() {
        System.out.println("--- Etat apres test ---");
        List<Cours> liste = service.listerTous("");
        liste.forEach(c -> System.out.println("  Cours #" + c.getId() + " : " + c.getTitre()));
    }

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
        assertFalse(liste.isEmpty(), "La liste ne doit pas etre vide apres ajout.");
        assertTrue(
                liste.stream().anyMatch(cours -> cours.getTitre().equals("Cours Test JUnit")),
                "Le cours ajoute doit etre present dans la liste."
        );
    }

    @Test
    @Order(2)
    void testAfficherCours() {
        List<Cours> liste = service.listerTous("");
        assertNotNull(liste, "La liste ne doit pas etre null.");
        assertFalse(liste.isEmpty(), "La liste doit contenir au moins un cours.");
        liste.forEach(c -> assertNotNull(c.getTitre(), "Le titre ne doit pas etre null."));
    }

    @Test
    @Order(3)
    void testModifierCours() {
        List<Cours> liste = service.listerTous("");
        Cours cible = liste.stream()
                .filter(c -> c.getTitre().equals("Cours Test JUnit") || c.getTitre().equals("Cours Modifie JUnit"))
                .findFirst()
                .orElse(null);

        assertNotNull(cible, "Le cours de test doit exister pour etre modifie.");

        cible.setTitre("Cours Modifie JUnit");
        cible.setDescription("Description modifiee suffisamment longue.");
        cible.setNomFormateur("Formateur Modifie");
        cible.setDureeTotaleHeures(20);

        service.modifier(cible);

        List<Cours> apres = service.listerTous("");
        boolean trouve = apres.stream().anyMatch(c -> c.getTitre().equals("Cours Modifie JUnit"));
        assertTrue(trouve, "Le cours modifie doit apparaitre dans la liste.");
    }

    @Test
    @Order(4)
    void testSupprimerCours() {
        List<Cours> liste = service.listerTous("");
        Cours cible = liste.stream()
                .filter(c -> c.getTitre().equals("Cours Modifie JUnit") || c.getTitre().equals("Cours Test JUnit"))
                .findFirst()
                .orElse(null);

        assertNotNull(cible, "Le cours de test doit exister pour etre supprime.");
        int id = cible.getId();

        service.supprimer(id);

        List<Cours> apres = service.listerTous("");
        boolean existe = apres.stream().anyMatch(c -> c.getId() == id);
        assertFalse(existe, "Le cours supprime ne doit plus etre dans la liste.");
    }

    @Test
    @Order(5)
    void testValidationCoursInvalide() {
        Cours invalide = new Cours();
        invalide.setTitre("AB");
        invalide.setDescription("Court");
        invalide.setNiveau("1er");
        invalide.setDomaine("Informatique");
        invalide.setNomFormateur("Test");
        invalide.setDureeTotaleHeures(0);

        ValidationResult result = service.validerSansException(invalide);
        assertFalse(result.isValid(), "Un cours invalide ne doit pas passer la validation.");
        assertFalse(result.getErrors().isEmpty(), "Des erreurs doivent etre retournees.");
    }

    @Test
    @Order(6)
    void testWorkflowValidationCours() {
        AppState.Role ancienRole = AppState.getRole();
        int ancienUserId = AppState.getUserId();

        Cours cours = new Cours();
        cours.setTitre("Cours Workflow Validation");
        cours.setDescription("Description de workflow suffisamment longue.");
        cours.setNiveau("2eme");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Enseignant Workflow");
        cours.setDureeTotaleHeures(8);

        try {
            AppState.setRole(AppState.Role.TEACHER);
            AppState.setUserId(101);
            workflowService.soumettre(cours, AppState.getUserId());

            Cours cree = service.listerTous("").stream()
                    .filter(item -> "Cours Workflow Validation".equals(item.getTitre()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(cree, "Le cours soumis doit exister.");
            assertEquals(CoursStatut.EN_ATTENTE, cree.getStatut(), "Un cours cree par un enseignant doit etre en attente.");
            assertEquals(101, cree.getCreatedById(), "L'auteur du cours doit etre memorise.");

            AppState.setRole(AppState.Role.ADMIN);
            AppState.setUserId(1);
            workflowService.approuver(cree.getId(), AppState.getUserId());

            Cours approuve = service.listerTous("").stream()
                    .filter(item -> item.getId() == cree.getId())
                    .findFirst()
                    .orElse(null);

            assertNotNull(approuve, "Le cours approuve doit rester accessible en back-office.");
            assertEquals(CoursStatut.APPROUVE, approuve.getStatut(), "Le statut doit passer a approuve.");

            service.supprimer(approuve.getId());
        } finally {
            AppState.setRole(ancienRole);
            AppState.setUserId(ancienUserId);
        }
    }
}
