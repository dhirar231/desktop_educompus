package com.educompus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le service de résumé de chapitre.
 */
class ResumeChapterServiceTest {

    @Test
    @DisplayName("Les enums TypeResume doivent avoir les bonnes valeurs")
    void testTypeResumeEnum() {
        assertEquals(3, ResumeChapterService.TypeResume.values().length);
        assertNotNull(ResumeChapterService.TypeResume.COURT);
        assertNotNull(ResumeChapterService.TypeResume.DETAILLE);
        assertNotNull(ResumeChapterService.TypeResume.POINTS_CLES);
    }

    @Test
    @DisplayName("Les enums LangueResume doivent avoir les bonnes valeurs")
    void testLangueResumeEnum() {
        assertEquals(3, ResumeChapterService.LangueResume.values().length);
        assertNotNull(ResumeChapterService.LangueResume.FR);
        assertNotNull(ResumeChapterService.LangueResume.AR);
        assertNotNull(ResumeChapterService.LangueResume.EN);
    }

    @Test
    @DisplayName("ResultatResume.succes doit créer un résultat valide")
    void testResultatResumeSucces() {
        String texte = "Ceci est un résumé test";
        ResumeChapterService.TypeResume type = ResumeChapterService.TypeResume.COURT;
        ResumeChapterService.LangueResume langue = ResumeChapterService.LangueResume.FR;

        ResumeChapterService.ResultatResume resultat = 
            ResumeChapterService.ResultatResume.succes(texte, type, langue);

        assertTrue(resultat.succes);
        assertEquals(texte, resultat.texte);
        assertEquals(type, resultat.type);
        assertEquals(langue, resultat.langue);
        assertNull(resultat.erreur);
    }

    @Test
    @DisplayName("ResultatResume.erreur doit créer un résultat d'erreur")
    void testResultatResumeErreur() {
        String messageErreur = "Erreur de test";

        ResumeChapterService.ResultatResume resultat = 
            ResumeChapterService.ResultatResume.erreur(messageErreur);

        assertFalse(resultat.succes);
        assertEquals(messageErreur, resultat.erreur);
        assertNull(resultat.texte);
        assertNull(resultat.type);
        assertNull(resultat.langue);
    }

    @Test
    @DisplayName("genererResume doit retourner une erreur pour un fichier inexistant")
    void testGenererResumeAvecFichierInexistant() {
        String cheminInvalide = "/chemin/inexistant/fichier.pdf";

        ResumeChapterService.ResultatResume resultat = ResumeChapterService.genererResume(
            cheminInvalide,
            ResumeChapterService.TypeResume.COURT,
            ResumeChapterService.LangueResume.FR
        );

        assertFalse(resultat.succes);
        assertNotNull(resultat.erreur);
        assertTrue(resultat.erreur.contains("Impossible d'extraire le texte du PDF"));
    }

    @Test
    @DisplayName("Les labels des types de résumé doivent être définis")
    void testTypeResumeLabels() {
        assertNotNull(ResumeChapterService.TypeResume.COURT.label);
        assertNotNull(ResumeChapterService.TypeResume.DETAILLE.label);
        assertNotNull(ResumeChapterService.TypeResume.POINTS_CLES.label);
        
        assertFalse(ResumeChapterService.TypeResume.COURT.label.isBlank());
        assertFalse(ResumeChapterService.TypeResume.DETAILLE.label.isBlank());
        assertFalse(ResumeChapterService.TypeResume.POINTS_CLES.label.isBlank());
    }

    @Test
    @DisplayName("Les codes des langues doivent être définis")
    void testLangueResumeCodes() {
        assertEquals("fr", ResumeChapterService.LangueResume.FR.code);
        assertEquals("ar", ResumeChapterService.LangueResume.AR.code);
        assertEquals("en", ResumeChapterService.LangueResume.EN.code);
    }

    @Test
    @DisplayName("Les labels des langues doivent être définis")
    void testLangueResumeLabels() {
        assertEquals("Français", ResumeChapterService.LangueResume.FR.label);
        assertEquals("العربية", ResumeChapterService.LangueResume.AR.label);
        assertEquals("English", ResumeChapterService.LangueResume.EN.label);
    }

    @Test
    @DisplayName("genererResume ne doit pas accepter un chemin null")
    void testGenererResumeAvecCheminNull() {
        ResumeChapterService.ResultatResume resultat = ResumeChapterService.genererResume(
            null,
            ResumeChapterService.TypeResume.COURT,
            ResumeChapterService.LangueResume.FR
        );

        assertFalse(resultat.succes);
        assertNotNull(resultat.erreur);
    }

    @Test
    @DisplayName("genererResume ne doit pas accepter un chemin vide")
    void testGenererResumeAvecCheminVide() {
        ResumeChapterService.ResultatResume resultat = ResumeChapterService.genererResume(
            "",
            ResumeChapterService.TypeResume.COURT,
            ResumeChapterService.LangueResume.FR
        );

        assertFalse(resultat.succes);
        assertNotNull(resultat.erreur);
    }
}
