package com.educompus.service;

import com.educompus.model.Cours;

import java.util.List;


public final class CoursValidationService {

    private static final int TITRE_MIN = 3;
    private static final int TITRE_MAX = 255;
    private static final int DESC_MIN = 10;
    private static final int DESC_MAX = 2000;
    private static final int FORMATEUR_MIN = 3;
    private static final int DUREE_MIN = 1;
    private static final int DUREE_MAX = 1000;
    private static final int ORDRE_MIN = 1;
    private static final int ORDRE_MAX = 999;

    private static final List<String> NIVEAUX_VALIDES =
            List.of("1er", "2eme", "3eme", "4eme", "5eme");
    private static final List<String> DOMAINES_VALIDES = List.of(
            "Informatique", "Intelligence Artificielle", "Développement Web",
            "Développement Mobile", "Réseaux", "Cybersécurité",
            "Data Science", "Marketing", "Finance", "Comptabilité", "Design Graphique"
    );

    private CoursValidationService() {}

    //  Cours 

    public static ValidationResult validateCours(Cours cours) {
        ValidationResult r = new ValidationResult();
        if (cours == null) {
            r.addError("Le cours est null.");
            return r;
        }

        validateTitre(cours.getTitre(), "Titre du cours", r);
        validateDescription(cours.getDescription(), r);
        validateNiveau(cours.getNiveau(), r);
        validateDomaine(cours.getDomaine(), r);
        validateFormateur(cours.getNomFormateur(), r);
        validateDuree(cours.getDureeTotaleHeures(), r);

        return r;
    }

    //  Chapitre 

    public static ValidationResult validateChapitreTitre(String titre) {
        ValidationResult r = new ValidationResult();
        validateTitre(titre, "Titre du chapitre", r);
        return r;
    }

    public static ValidationResult validateChapitreOrdre(String ordreStr) {
        ValidationResult r = new ValidationResult();
        validateOrdre(ordreStr, r);
        return r;
    }

    public static ValidationResult validateChapitreCoursId(int coursId) {
        ValidationResult r = new ValidationResult();
        if (coursId <= 0) {
            r.addError("Veuillez sélectionner un cours.");
        }
        return r;
    }

    // TD 

    public static ValidationResult validateTdTitre(String titre) {
        ValidationResult r = new ValidationResult();
        validateTitre(titre, "Titre du TD", r);
        return r;
    }

    public static ValidationResult validateTdChapitreId(int chapitreId) {
        ValidationResult r = new ValidationResult();
        if (chapitreId <= 0) {
            r.addError("Veuillez sélectionner un chapitre.");
        }
        return r;
    }

    //  Vidéo 
    public static ValidationResult validateVideoTitre(String titre) {
        ValidationResult r = new ValidationResult();
        validateTitre(titre, "Titre de la vidéo", r);
        return r;
    }

    public static ValidationResult validateVideoUrl(String url) {
        ValidationResult r = new ValidationResult();
        if (url == null || url.isBlank()) {
            r.addError("L'URL de la vidéo est obligatoire.");
            return r;
        }
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            r.addError("L'URL doit commencer par http:// ou https://");
        }
        if (u.length() < 10) {
            r.addError("L'URL semble trop courte pour être valide.");
        }
        if (u.contains(" ")) {
            r.addError("L'URL ne doit pas contenir d'espaces.");
        }
        return r;
    }

    public static ValidationResult validateVideoChapitreId(int chapitreId) {
        ValidationResult r = new ValidationResult();
        if (chapitreId <= 0) {
            r.addError("Veuillez sélectionner un chapitre.");
        }
        return r;
    }

    //  Règles communes

    private static void validateTitre(String titre, String label, ValidationResult r) {
        if (titre == null || titre.isBlank()) {
            r.addError(label + " est obligatoire.");
            return;
        }
        String t = titre.trim();
        if (t.length() < TITRE_MIN) {
            r.addError(label + " doit contenir au moins " + TITRE_MIN + " caractères.");
        }
        if (t.length() > TITRE_MAX) {
            r.addError(label + " ne doit pas dépasser " + TITRE_MAX + " caractères.");
        }
        if (isAllDigits(t)) {
            r.addError(label + " ne peut pas être composé uniquement de chiffres.");
        }
        if (isAllSpecialChars(t)) {
            r.addError(label + " ne peut pas être composé uniquement de caractères spéciaux.");
        }
    }

    private static void validateDescription(String desc, ValidationResult r) {
        if (desc == null || desc.isBlank()) {
            r.addError("La description est obligatoire.");
            return;
        }
        String d = desc.trim();
        if (d.length() < DESC_MIN) {
            r.addError("La description doit contenir au moins " + DESC_MIN + " caractères.");
        }
        if (d.length() > DESC_MAX) {
            r.addError("La description ne doit pas dépasser " + DESC_MAX + " caractères.");
        }
    }

    private static void validateNiveau(String niveau, ValidationResult r) {
        if (niveau == null || niveau.isBlank()) {
            r.addError("Le niveau est obligatoire.");
            return;
        }
        if (!NIVEAUX_VALIDES.contains(niveau.trim())) {
            r.addError("Niveau invalide. Valeurs acceptées : " + String.join(", ", NIVEAUX_VALIDES));
        }
    }

    private static void validateDomaine(String domaine, ValidationResult r) {
        if (domaine == null || domaine.isBlank()) {
            r.addError("Le domaine est obligatoire.");
            return;
        }
        if (!DOMAINES_VALIDES.contains(domaine.trim())) {
            r.addError("Domaine invalide. Valeurs acceptées : " + String.join(", ", DOMAINES_VALIDES));
        }
    }

    private static void validateFormateur(String nom, ValidationResult r) {
        if (nom == null || nom.isBlank()) {
            r.addError("Le nom du formateur est obligatoire.");
            return;
        }
        String n = nom.trim();
        if (n.length() < FORMATEUR_MIN) {
            r.addError("Le nom du formateur doit contenir au moins " + FORMATEUR_MIN + " caractères.");
        }
        if (containsDigit(n)) {
            r.addError("Le nom du formateur ne doit pas contenir de chiffres.");
        }
        if (isAllSpecialChars(n)) {
            r.addError("Le nom du formateur ne peut pas être composé uniquement de caractères spéciaux.");
        }
    }

    private static void validateDuree(int duree, ValidationResult r) {
        if (duree < DUREE_MIN) {
            r.addError("La durée doit être d'au moins " + DUREE_MIN + " heure(s).");
        }
        if (duree > DUREE_MAX) {
            r.addError("La durée ne peut pas dépasser " + DUREE_MAX + " heures.");
        }
    }

    public static ValidationResult validateDureeStr(String dureeStr) {
        ValidationResult r = new ValidationResult();
        if (dureeStr == null || dureeStr.isBlank()) {
            r.addError("La durée est obligatoire.");
            return r;
        }
        try {
            int val = Integer.parseInt(dureeStr.trim());
            validateDuree(val, r);
        } catch (NumberFormatException e) {
            r.addError("La durée doit être un nombre entier (ex: 12).");
        }
        return r;
    }

    private static void validateOrdre(String ordreStr, ValidationResult r) {
        if (ordreStr == null || ordreStr.isBlank()) {
            r.addError("L'ordre est obligatoire.");
            return;
        }
        try {
            int val = Integer.parseInt(ordreStr.trim());
            if (val < ORDRE_MIN) {
                r.addError("L'ordre doit être au moins " + ORDRE_MIN + ".");
            }
            if (val > ORDRE_MAX) {
                r.addError("L'ordre ne peut pas dépasser " + ORDRE_MAX + ".");
            }
        } catch (NumberFormatException e) {
            r.addError("L'ordre doit être un nombre entier positif (ex: 1, 2, 3...).");
        }
    }

    // Helpers

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
