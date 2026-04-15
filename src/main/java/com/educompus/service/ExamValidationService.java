package com.educompus.service;

import com.educompus.model.Cours;

public final class ExamValidationService {
    private static final int TITLE_MAX = 200;
    private static final int DESCRIPTION_MAX = 2000;
    private static final int LEVEL_MAX = 100;
    private static final int DOMAIN_MAX = 100;

    private ExamValidationService() {}

    public static ValidationResult validateTitle(String title) {
        ValidationResult result = new ValidationResult();
        String value = safe(title);
        if (value.isBlank()) {
            result.addError("Le titre de l'examen est obligatoire.");
        } else if (value.length() > TITLE_MAX) {
            result.addError("Le titre ne doit pas dépasser " + TITLE_MAX + " caractères.");
        }
        return result;
    }

    public static ValidationResult validateDescription(String description) {
        ValidationResult result = new ValidationResult();
        String value = safe(description);
        if (value.isBlank()) {
            result.addError("La description est obligatoire.");
        } else if (value.length() > DESCRIPTION_MAX) {
            result.addError("La description ne doit pas dépasser " + DESCRIPTION_MAX + " caractères.");
        }
        return result;
    }

    public static ValidationResult validateLevel(String level) {
        ValidationResult result = new ValidationResult();
        String value = safe(level);
        if (value.isBlank()) {
            result.addError("Le niveau est obligatoire.");
        } else if (value.length() > LEVEL_MAX) {
            result.addError("Le niveau ne doit pas dépasser " + LEVEL_MAX + " caractères.");
        }
        return result;
    }

    public static ValidationResult validateDomain(String domain) {
        ValidationResult result = new ValidationResult();
        String value = safe(domain);
        if (value.isBlank()) {
            result.addError("Le domaine est obligatoire.");
        } else if (value.length() > DOMAIN_MAX) {
            result.addError("Le domaine ne doit pas dépasser " + DOMAIN_MAX + " caractères.");
        }
        return result;
    }

    public static ValidationResult validateCourse(Cours cours) {
        ValidationResult result = new ValidationResult();
        if (cours == null || cours.getId() <= 0) {
            result.addError("Sélectionnez un cours.");
        }
        return result;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
