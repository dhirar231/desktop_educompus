package com.educompus.service;

/**
 * Validation logique pour les tâches Kanban.
 */
public final class KanbanValidationService {

    private static final int TITRE_MIN = 2;
    private static final int TITRE_MAX = 255;
    private static final int DESC_MAX = 1000;

    private KanbanValidationService() {}

    public static ValidationResult validateTache(String titre, String description) {
        ValidationResult r = new ValidationResult();
        validateTitreTache(titre, r);
        validateDescriptionTache(description, r);
        return r;
    }

    public static ValidationResult validateTitreTache(String titre) {
        ValidationResult r = new ValidationResult();
        validateTitreTache(titre, r);
        return r;
    }

    private static void validateTitreTache(String titre, ValidationResult r) {
        if (titre == null || titre.isBlank()) {
            r.addError("Le titre de la tâche est obligatoire.");
            return;
        }
        String t = titre.trim();
        if (t.length() < TITRE_MIN) {
            r.addError("Le titre doit contenir au moins " + TITRE_MIN + " caractères.");
        }
        if (t.length() > TITRE_MAX) {
            r.addError("Le titre ne doit pas dépasser " + TITRE_MAX + " caractères.");
        }
        if (isAllSpecialChars(t)) {
            r.addError("Le titre ne peut pas être composé uniquement de caractères spéciaux.");
        }
        if (isAllDigits(t)) {
            r.addError("Le titre ne peut pas être composé uniquement de chiffres.");
        }
    }

    private static void validateDescriptionTache(String desc, ValidationResult r) {
        if (desc == null || desc.isBlank()) return;
        if (desc.trim().length() > DESC_MAX) {
            r.addError("La description ne doit pas dépasser " + DESC_MAX + " caractères.");
        }
    }

    private static boolean isAllDigits(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    private static boolean isAllSpecialChars(String s) {
        return s.chars().noneMatch(c -> Character.isLetterOrDigit(c));
    }
}
