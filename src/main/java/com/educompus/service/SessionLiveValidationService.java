package com.educompus.service;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Service de validation pour les sessions live.
 * Valide les données avant toute opération CRUD et vérifie
 * les URLs des plateformes de visioconférence autorisées.
 */
public final class SessionLiveValidationService {

    private static final int NOM_COURS_MIN = 3;
    private static final int NOM_COURS_MAX = 255;
    private static final int LIEN_MAX = 512;

    /** Plateformes de visioconférence autorisées. */
    private static final Set<String> DOMAINES_AUTORISES = Set.of(
            "meet.google.com",
            "zoom.us",
            "teams.microsoft.com",
            "webex.com",
            "whereby.com",
            "jitsi.org",
            "meet.jit.si",
            "discord.com",
            "skype.com"
    );

    private SessionLiveValidationService() {}

    /**
     * Valide une session live complète avant ajout ou modification.
     * @param session La session à valider
     * @return Le résultat de validation avec les erreurs éventuelles
     */
    public static ValidationResult validerSession(SessionLive session) {
        ValidationResult r = new ValidationResult();
        if (session == null) {
            r.addError("La session est null.");
            return r;
        }
        validerNomCours(session.getNomCours(), r);
        validerLien(session.getLien(), r);
        validerDate(session.getDate(), r);
        validerHeure(session.getHeure(), r);
        validerStatut(session.getStatut(), r);
        return r;
    }

    /**
     * Valide uniquement le nom du cours.
     */
    public static ValidationResult validerNomCours(String nomCours) {
        ValidationResult r = new ValidationResult();
        validerNomCours(nomCours, r);
        return r;
    }

    /**
     * Valide uniquement le lien de la session.
     */
    public static ValidationResult validerLien(String lien) {
        ValidationResult r = new ValidationResult();
        validerLien(lien, r);
        return r;
    }

    /**
     * Valide uniquement la date de la session.
     */
    public static ValidationResult validerDate(LocalDate date) {
        ValidationResult r = new ValidationResult();
        validerDate(date, r);
        return r;
    }

    /**
     * Valide uniquement l'heure de la session.
     */
    public static ValidationResult validerHeure(LocalTime heure) {
        ValidationResult r = new ValidationResult();
        validerHeure(heure, r);
        return r;
    }

    /**
     * Vérifie si une URL appartient à une plateforme de visioconférence autorisée.
     * @param url L'URL à vérifier
     * @return true si la plateforme est autorisée, false sinon
     */
    public static boolean estPlateformeAutorisee(String url) {
        if (url == null || url.isBlank()) return false;
        String u = url.trim().toLowerCase();
        // Retirer le protocole pour l'analyse du domaine
        if (u.startsWith("https://")) u = u.substring(8);
        else if (u.startsWith("http://")) u = u.substring(7);
        // Extraire le domaine (avant le premier '/')
        String domaine = u.contains("/") ? u.substring(0, u.indexOf('/')) : u;
        // Retirer le 'www.' éventuel
        if (domaine.startsWith("www.")) domaine = domaine.substring(4);
        final String d = domaine;
        return DOMAINES_AUTORISES.stream().anyMatch(autorise -> d.equals(autorise) || d.endsWith("." + autorise));
    }

    /**
     * Retourne la liste des plateformes autorisées pour affichage.
     */
    public static List<String> getPlateformesAutorisees() {
        return List.of(
                "Google Meet (meet.google.com)",
                "Zoom (zoom.us)",
                "Microsoft Teams (teams.microsoft.com)",
                "Cisco Webex (webex.com)",
                "Whereby (whereby.com)",
                "Jitsi Meet (meet.jit.si)",
                "Discord (discord.com)"
        );
    }

    // ── Règles privées ────────────────────────────────────────────────────────

    private static void validerNomCours(String nomCours, ValidationResult r) {
        if (nomCours == null || nomCours.isBlank()) {
            r.addError("Le nom du cours est obligatoire.");
            return;
        }
        String n = nomCours.trim();
        if (n.length() < NOM_COURS_MIN) {
            r.addError("Le nom du cours doit contenir au moins " + NOM_COURS_MIN + " caractères.");
        }
        if (n.length() > NOM_COURS_MAX) {
            r.addError("Le nom du cours ne doit pas dépasser " + NOM_COURS_MAX + " caractères.");
        }
        if (isAllSpecialChars(n)) {
            r.addError("Le nom du cours ne peut pas être composé uniquement de caractères spéciaux.");
        }
    }

    private static void validerLien(String lien, ValidationResult r) {
        if (lien == null || lien.isBlank()) {
            r.addError("Le lien de la session est obligatoire.");
            return;
        }
        String l = lien.trim();
        if (l.length() > LIEN_MAX) {
            r.addError("Le lien ne doit pas dépasser " + LIEN_MAX + " caractères.");
        }
        if (!l.startsWith("http://") && !l.startsWith("https://")) {
            r.addError("Le lien doit commencer par http:// ou https://");
            return;
        }
        if (l.contains(" ")) {
            r.addError("Le lien ne doit pas contenir d'espaces.");
        }
        if (!estPlateformeAutorisee(l)) {
            r.addError("Plateforme non reconnue. Utilisez : Google Meet, Zoom, Teams, Webex, Whereby ou Jitsi.");
        }
    }

    private static void validerDate(LocalDate date, ValidationResult r) {
        if (date == null) {
            r.addError("La date de la session est obligatoire.");
        }
    }

    private static void validerHeure(LocalTime heure, ValidationResult r) {
        if (heure == null) {
            r.addError("L'heure de la session est obligatoire.");
        }
    }

    private static void validerStatut(SessionStatut statut, ValidationResult r) {
        if (statut == null) {
            r.addError("Le statut de la session est obligatoire.");
        }
    }

    private static boolean isAllSpecialChars(String s) {
        return s.chars().noneMatch(Character::isLetterOrDigit);
    }
}
