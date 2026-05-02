package com.educompus.model;

/**
 * Types d'actions possibles dans une session live.
 * Représente tous les événements qui peuvent se produire pendant une session.
 */
public enum ActionType {
    // Actions de participation
    JOIN,           // Étudiant rejoint la session
    LEAVE,          // Étudiant quitte la session

    // Actions d'interaction étudiant
    RAISE_HAND,     // Étudiant lève la main
    LOWER_HAND,     // Étudiant baisse la main (ou enseignant l'a accordé)
    REQUEST_SPEAK,  // Étudiant demande la parole

    // Actions enseignant
    GRANT_SPEAK,    // Enseignant accorde la parole
    REVOKE_SPEAK,   // Enseignant retire la parole

    // Actions de session
    SESSION_START,  // Session démarrée
    SESSION_END;    // Session terminée

    /**
     * Libellé français de l'action pour l'affichage.
     */
    public String libelle() {
        return switch (this) {
            case JOIN          -> "A rejoint";
            case LEAVE         -> "A quitté";
            case RAISE_HAND    -> "Lève la main";
            case LOWER_HAND    -> "Baisse la main";
            case REQUEST_SPEAK -> "Demande la parole";
            case GRANT_SPEAK   -> "Parole accordée";
            case REVOKE_SPEAK  -> "Parole retirée";
            case SESSION_START -> "Session démarrée";
            case SESSION_END   -> "Session terminée";
        };
    }

    /**
     * Icône associée à l'action.
     */
    public String icone() {
        return switch (this) {
            case JOIN          -> "🟢";
            case LEAVE         -> "🔴";
            case RAISE_HAND    -> "✋";
            case LOWER_HAND    -> "👇";
            case REQUEST_SPEAK -> "🎤";
            case GRANT_SPEAK   -> "✅";
            case REVOKE_SPEAK  -> "🚫";
            case SESSION_START -> "▶️";
            case SESSION_END   -> "⏹️";
        };
    }

    public static ActionType fromString(String value) {
        if (value == null) return JOIN;
        try {
            return ActionType.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return JOIN;
        }
    }
}
