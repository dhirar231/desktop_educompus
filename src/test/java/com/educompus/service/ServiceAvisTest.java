package com.educompus.service;

import com.educompus.model.Avis;
import com.educompus.model.Produit;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServiceAvisTest {

    static ServiceAvis    service;
    static ServiceProduit serviceProduit;

    // Produit créé pour les tests — supprimé en @AfterAll
    static int produitIdTest = -1;
    static final int USER_ID_TEST = 1;

    // Id de l'avis inséré — partagé entre les méthodes
    static int idAvisTest = -1;

    // ── Setup / Teardown global ───────────────────────────────────────────────

    @BeforeAll
    static void setup() throws SQLException {
        service        = new ServiceAvis();
        serviceProduit = new ServiceProduit();

        // Créer un produit de test pour satisfaire la FK avis.produit_id
        Produit p = new Produit();
        p.setNom("ProduitAvisTest_JUnit");
        p.setDescription("Produit temporaire pour les tests d'avis unitaires");
        p.setPrix(1.0);
        p.setStock(99);
        p.setType("Autre");
        p.setCategorie("Autre");
        p.setUserId(1);
        serviceProduit.ajouter(p);

        // Récupérer l'id généré
        produitIdTest = serviceProduit.afficherAll().stream()
                .filter(pr -> pr.getNom().equals("ProduitAvisTest_JUnit"))
                .mapToInt(Produit::getId)
                .findFirst()
                .orElse(-1);

        assertTrue(produitIdTest > 0, "Le produit de test doit être créé avant les tests d'avis");
    }

    @AfterAll
    static void teardown() throws SQLException {
        // Supprimer le produit de test (les avis sont supprimés en cascade)
        if (produitIdTest > 0) {
            try { serviceProduit.delete(produitIdTest); } catch (Exception ignored) {}
        }
    }

    // ── Test 1 : Ajouter un avis ─────────────────────────────────────────────

    @Test
    @Order(1)
    void testAjouterAvis() throws SQLException {
        Avis a = new Avis();
        a.setUserId(USER_ID_TEST);
        a.setProduitId(produitIdTest);
        a.setNote(4);
        a.setCommentaire("Très bon produit, je recommande !");
        a.setCreatedAt(LocalDateTime.now());

        service.ajouter(a);

        List<Avis> liste = service.afficherByProduit(produitIdTest);
        assertFalse(liste.isEmpty(), "La liste d'avis ne doit pas être vide après ajout");

        Avis insere = liste.stream()
                .filter(av -> "Très bon produit, je recommande !".equals(av.getCommentaire()))
                .findFirst()
                .orElse(null);

        assertNotNull(insere, "L'avis inséré doit être retrouvable");
        assertEquals(4,             insere.getNote());
        assertEquals(USER_ID_TEST,  insere.getUserId());
        assertEquals(produitIdTest, insere.getProduitId());

        idAvisTest = insere.getId();
    }

    // ── Test 2 : Afficher les avis d'un produit ──────────────────────────────

    @Test
    @Order(2)
    void testAfficherByProduit() throws SQLException {
        assumeIdValide();

        List<Avis> liste = service.afficherByProduit(produitIdTest);

        assertNotNull(liste, "La liste ne doit pas être null");
        assertFalse(liste.isEmpty(), "Il doit y avoir au moins un avis");

        boolean trouve = liste.stream().anyMatch(a -> a.getId() == idAvisTest);
        assertTrue(trouve, "L'avis inséré doit apparaître dans la liste du produit");
    }

    // ── Test 3 : Modifier un avis ────────────────────────────────────────────

    @Test
    @Order(3)
    void testModifierAvis() throws SQLException {
        assumeIdValide();

        Avis avis = service.afficherAll().stream()
                .filter(a -> a.getId() == idAvisTest)
                .findFirst()
                .orElse(null);
        assertNotNull(avis, "L'avis doit exister avant modification");

        avis.setNote(5);
        avis.setCommentaire("Commentaire modifié par le test JUnit");
        service.update(avis);

        Avis modifie = service.afficherAll().stream()
                .filter(a -> a.getId() == idAvisTest)
                .findFirst()
                .orElse(null);

        assertNotNull(modifie);
        assertEquals(5, modifie.getNote(), "La note doit être mise à jour à 5");
        assertEquals("Commentaire modifié par le test JUnit", modifie.getCommentaire());
    }

    // ── Test 4 : Note doit être entre 1 et 5 ─────────────────────────────────

    @Test
    @Order(4)
    void testNoteValide() throws SQLException {
        assumeIdValide();

        Avis avis = service.afficherAll().stream()
                .filter(a -> a.getId() == idAvisTest)
                .findFirst()
                .orElse(null);

        assertNotNull(avis);
        assertTrue(avis.getNote() >= 1 && avis.getNote() <= 5,
                "La note doit être comprise entre 1 et 5");
    }

    // ── Test 5 : Supprimer un avis ───────────────────────────────────────────

    @Test
    @Order(5)
    void testSupprimerAvis() throws SQLException {
        assumeIdValide();

        service.delete(idAvisTest);

        boolean existe = service.afficherAll().stream()
                .anyMatch(a -> a.getId() == idAvisTest);

        assertFalse(existe, "L'avis supprimé ne doit plus exister en base");
        idAvisTest = -1;
    }

    // ── Nettoyage après chaque test (si échec) ────────────────────────────────

    @AfterEach
    void cleanUp() {
        // Pas de nettoyage inter-test : idAvisTest est partagé entre les @Order
        // Le nettoyage global est géré par @AfterAll via la suppression du produit (CASCADE)
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private void assumeIdValide() {
        assertTrue(idAvisTest > 0,
                "L'id de l'avis test doit être valide (test précédent a dû réussir)");
    }
}
