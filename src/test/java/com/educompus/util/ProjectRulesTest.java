package com.educompus.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRulesTest {
    @TempDir
    Path tempDir;

    @Test
    void submissionResponseAcceptsLettersNumbersAndPunctuation() {
        assertTrue(ProjectRules.isSubmissionResponseValid("Projet 2026: version finale."));
    }

    @Test
    void submissionResponseRejectsTooShortText() {
        assertFalse(ProjectRules.isSubmissionResponseValid("A"));
    }

    @Test
    void kanbanTitleAcceptsShortAlphabeticTitle() {
        assertTrue(ProjectRules.isKanbanTitleValid("Sprint Plan"));
    }

    @Test
    void kanbanTitleRejectsLeadingDigit() {
        assertEquals("Le titre doit commencer par une lettre.", ProjectRules.kanbanTitleValidationError("1Task"));
    }

    @Test
    void extensionOfRejectsInvalidOrMissingExtension() {
        assertEquals("", ProjectRules.extensionOf("archive"));
        assertEquals("", ProjectRules.extensionOf("virus.bad-extension!"));
    }

    @Test
    void validateSubmissionFieldsAcceptsValidFiles() throws Exception {
        Path cahier = Files.createFile(tempDir.resolve("brief.pdf"));
        Path dossier = Files.createFile(tempDir.resolve("sources.zip"));

        assertNull(ProjectRules.validateSubmissionFields("Projet complet 2026", cahier.toString(), dossier.toString()));
    }

    @Test
    void validateSubmissionFieldsRejectsUnsupportedDossierType() throws Exception {
        Path cahier = Files.createFile(tempDir.resolve("brief.pdf"));
        Path dossier = Files.createFile(tempDir.resolve("sources.txt"));

        assertEquals("Dossier invalide (type/chemin).",
                ProjectRules.validateSubmissionFields("Projet complet 2026", cahier.toString(), dossier.toString()));
    }
}
