package com.educompus.service;

import com.educompus.model.Project;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validation logique pour les projets et soumissions.
 */
public final class ProjectValidationService {

    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 255;
    private static final int DESC_MIN = 10;
    private static final int DESC_MAX = 3000;
    private static final int DELIVERABLES_MAX = 2000;

    private ProjectValidationService() {}

    // ── Projet complet ───────────────────────────────────────────────────────

    public static ValidationResult validateProject(Project p) {
        ValidationResult r = new ValidationResult();
        if (p == null) {
            r.addError("Le projet est null.");
            return r;
        }

        validateTitreProjet(p.getTitle(), r);
        validateDescriptionProjet(p.getDescription(), r);
        validateDeadline(p.getDeadline(), r);
        validateDeliverables(p.getDeliverables(), r);

        return r;
    }

    // ── Champs individuels (pour validation en temps réel) ───────────────────

    public static ValidationResult validateTitreProjet(String titre) {
        ValidationResult r = new ValidationResult();
        validateTitreProjet(titre, r);
        return r;
    }

    public static ValidationResult validateDeadlineStr(String deadline) {
        ValidationResult r = new ValidationResult();
        validateDeadline(deadline, r);
        return r;
    }

    public static ValidationResult validateDeliverablesStr(String deliverables) {
        ValidationResult r = new ValidationResult();
        validateDeliverables(deliverables, r);
        return r;
    }

    // ── Règles internes ──────────────────────────────────────────────────────

    private static void validateTitreProjet(String titre, ValidationResult r) {
        if (titre == null || titre.isBlank()) {
            r.addError("Le titre du projet est obligatoire.");
            return;
        }
        String t = titre.trim();
        if (t.length() < TITRE_MIN) {
            r.addError("Le titre doit contenir au moins " + TITRE_MIN + " caractères.");
        }
        if (t.length() > TITRE_MAX) {
            r.addError("Le titre ne doit pas dépasser " + TITRE_MAX + " caractères.");
        }
        if (isAllDigits(t)) {
            r.addError("Le titre ne peut pas être composé uniquement de chiffres.");
        }
        if (isAllSpecialChars(t)) {
            r.addError("Le titre ne peut pas être composé uniquement de caractères spéciaux.");
        }
        if (containsDigit(t)) {
            r.addError("Le titre ne doit pas contenir de chiffres.");
        }
    }

    private static void validateDescriptionProjet(String desc, ValidationResult r) {
        if (desc == null || desc.isBlank()) {
            // description optionnelle pour les projets
            return;
        }
        String d = desc.trim();
        if (d.length() < DESC_MIN) {
            r.addError("La description doit contenir au moins " + DESC_MIN + " caractères si elle est renseignée.");
        }
        if (d.length() > DESC_MAX) {
            r.addError("La description ne doit pas dépasser " + DESC_MAX + " caractères.");
        }
    }

    private static void validateDeadline(String deadline, ValidationResult r) {
        if (deadline == null || deadline.isBlank()) {
            r.addError("La deadline est obligatoire.");
            return;
        }
        String dl = deadline.trim();
        // Séparer date et heure (ex: "2026-06-30 17:00" ou "5/8/2026 17.00:00")
        String[] parts = dl.split("\\s+");
        String datePart = parts.length > 0 ? parts[0] : dl;
        String timePart = parts.length > 1 ? parts[1].trim() : null;

        // Essayer plusieurs formats de date courants
        LocalDate date = null;
        DateTimeFormatter[] dateFormats = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        };
        for (DateTimeFormatter fmt : dateFormats) {
            try {
                date = LocalDate.parse(datePart, fmt);
                break;
            } catch (DateTimeParseException ignored) {
            }
        }
        if (date == null) {
            r.addError("Format de deadline invalide. Attendu : YYYY-MM-DD ou d/M/yyyy (ex: 2026-06-30 ou 30/06/2026). ");
            return;
        }
        if (date.isBefore(LocalDate.now())) {
            r.addError("La deadline ne peut pas être dans le passé.");
        }
        if (date.isAfter(LocalDate.now().plusYears(5))) {
            r.addError("La deadline semble trop lointaine (plus de 5 ans).");
        }
        // Valider la partie heure si présente
        if (timePart != null && !timePart.isBlank()) {
            // Normaliser séparateurs courants (ex: 17.00 -> 17:00)
            String normalized = timePart.replace('.', ':');
            if (!normalized.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
                r.addError("Format d'heure invalide. Attendu : HH:MM ou HH:MM:SS.");
            }
        }
    }

    private static void validateDeliverables(String deliverables, ValidationResult r) {
        if (deliverables == null || deliverables.isBlank()) {
            return; // optionnel
        }
        if (deliverables.trim().length() > DELIVERABLES_MAX) {
            r.addError("Les livrables ne doivent pas dépasser " + DELIVERABLES_MAX + " caractères.");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static boolean isAllDigits(String s) {
        return s.chars().allMatch(Character::isDigit);
    }

    private static boolean containsDigit(String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    private static boolean isAllSpecialChars(String s) {
        return s.chars().noneMatch(c -> Character.isLetterOrDigit(c));
    }
}
