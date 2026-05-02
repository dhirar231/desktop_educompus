package com.educompus.service;

import com.educompus.model.Cours;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseFavoriteServiceTest {

    private static CoursService coursService;
    private static CourseFavoriteService favoriteService;
    private static int coursId;

    @BeforeAll
    static void setUp() {
        coursService = new CoursService();
        favoriteService = new CourseFavoriteService();

        Cours cours = new Cours();
        cours.setTitre("Cours Favori Test");
        cours.setDescription("Cours temporaire pour tester les favoris.");
        cours.setNiveau("1er");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Formateur Favori");
        cours.setDureeTotaleHeures(4);
        coursService.creer(cours);

        coursId = coursService.listerTous("").stream()
                .filter(item -> "Cours Favori Test".equals(item.getTitre()))
                .mapToInt(Cours::getId)
                .findFirst()
                .orElse(0);
    }

    @AfterAll
    static void tearDown() {
        if (coursId > 0) {
            coursService.supprimer(coursId);
        }
    }

    @Test
    void testAjouterEtRetirerFavori() {
        int studentId = 9991;

        favoriteService.retirer(studentId, coursId);
        assertFalse(favoriteService.estFavori(studentId, coursId), "Le cours ne doit pas etre favori au debut.");

        favoriteService.ajouter(studentId, coursId);
        assertTrue(favoriteService.estFavori(studentId, coursId), "Le cours doit etre ajoute aux favoris.");

        Set<Integer> favoris = favoriteService.listerIdsFavoris(studentId);
        assertTrue(favoris.contains(coursId), "La liste des favoris doit contenir le cours ajoute.");

        favoriteService.retirer(studentId, coursId);
        assertFalse(favoriteService.estFavori(studentId, coursId), "Le cours doit pouvoir etre retire des favoris.");
    }
}
