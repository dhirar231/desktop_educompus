package com.educompus.service;

import com.educompus.model.Produit;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceProduitTest {

    static ServiceProduit service;

    // Id du produit inséré — partagé entre tous les tests ordonnés
    static int    idProduitTest  = -1;
    static String nomTest        = "ProduitTest_JUnit_" + System.currentTimeMillis();
    static String nomModifie     = "ProduitTest_Modifie_" + System.currentTimeMillis();

    @BeforeAll
    static void setup() {
        service = new ServiceProduit();
    }

    // Nettoyage global : si un test a échoué avant la suppression explicite
    @AfterAll
    static void teardown() throws SQLException {
        if (idProduitTest > 0) {
            try { service.delete(idProduitTest); } catch (Exception ignored) {}
        }
    }

    // ── Test 1 : Ajouter ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    void testAjouterProduit() throws SQLException {
        Produit p = new Produit();
        p.setNom(nomTest);
        p.setDescription("Description de test unitaire suffisamment longue");
        p.setPrix(19.99);
        p.setStock(10);
        p.setType("Livre");
        p.setCategorie("Informatique");
        p.setImage(null);
        p.setUserId(1);

        service.ajouter(p);

        List<Produit> liste = service.afficherAll();
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide après ajout");

        Produit insere = liste.stream()
                .filter(pr -> nomTest.equals(pr.getNom()))
                .findFirst()
                .orElse(null);

        assertNotNull(insere, "Le produit inséré doit être trouvable");
        assertEquals("Livre",        insere.getType());
        assertEquals("Informatique", insere.getCategorie());
        assertEquals(19.99,          insere.getPrix(), 0.001);
        assertEquals(10,             insere.getStock());

        idProduitTest = insere.getId();
    }

    // ── Test 2 : findById ────────────────────────────────────────────────────

    @Test
    @Order(2)
    void testFindById() throws SQLException {
        assumeIdValide();

        Produit p = service.findById(idProduitTest);

        assertNotNull(p, "findById doit retourner le produit");
        assertEquals(nomTest,       p.getNom());
        assertEquals(idProduitTest, p.getId());
    }

    // ── Test 3 : Modifier ────────────────────────────────────────────────────

    @Test
    @Order(3)
    void testModifierProduit() throws SQLException {
        assumeIdValide();

        Produit p = service.findById(idProduitTest);
        assertNotNull(p, "Le produit doit exister avant modification");

        p.setNom(nomModifie);
        p.setPrix(29.99);
        p.setStock(5);
        service.update(p);

        Produit modifie = service.findById(idProduitTest);
        assertNotNull(modifie);
        assertEquals(nomModifie, modifie.getNom());
        assertEquals(29.99, modifie.getPrix(), 0.001);
        assertEquals(5,     modifie.getStock());
    }

    // ── Test 4 : Unicité — doublon doit être rejeté ──────────────────────────

    @Test
    @Order(4)
    void testUniciteDoublon() {
        assumeIdValide();

        Produit doublon = new Produit();
        doublon.setNom(nomModifie);  // même nom que le produit modifié
        doublon.setDescription("Autre description suffisamment longue pour le test");
        doublon.setPrix(9.99);
        doublon.setStock(3);
        doublon.setType("Livre");             // même type
        doublon.setCategorie("Informatique"); // même catégorie
        doublon.setUserId(1);

        assertThrows(SQLException.class, () -> service.ajouter(doublon),
                "L'ajout d'un doublon (même nom+type+catégorie) doit lever une SQLException");
    }

    // ── Test 5 : Décrémenter le stock ────────────────────────────────────────

    @Test
    @Order(5)
    void testDecrementerStock() throws SQLException {
        assumeIdValide();

        Produit avant = service.findById(idProduitTest);
        assertNotNull(avant);
        int stockAvant = avant.getStock(); // 5 après la modif

        service.decrementeStock(idProduitTest, 2);

        Produit apres = service.findById(idProduitTest);
        assertNotNull(apres);
        assertEquals(stockAvant - 2, apres.getStock(),
                "Le stock doit diminuer de la quantité commandée");
    }

    // ── Test 6 : Stock insuffisant doit lever une exception ──────────────────

    @Test
    @Order(6)
    void testDecrementerStockInsuffisant() {
        assumeIdValide();

        assertThrows(SQLException.class,
                () -> service.decrementeStock(idProduitTest, 9999),
                "Une quantité supérieure au stock doit lever une SQLException");
    }

    // ── Test 7 : Supprimer ───────────────────────────────────────────────────

    @Test
    @Order(7)
    void testSupprimerProduit() throws SQLException {
        assumeIdValide();

        service.delete(idProduitTest);

        Produit supprime = service.findById(idProduitTest);
        assertNull(supprime, "Le produit supprimé ne doit plus exister en base");

        idProduitTest = -1; // nettoyé, @AfterAll n'a rien à faire
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void assumeIdValide() {
        assertTrue(idProduitTest > 0,
                "L'id du produit test doit être valide (test précédent a dû réussir)");
    }
}
