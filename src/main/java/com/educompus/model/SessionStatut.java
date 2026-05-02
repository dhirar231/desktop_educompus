package com.educompus.model;

/**
 * Énumération représentant les différents statuts d'une session live.
 * Utilisée pour gérer l'état des sessions de visioconférence dans l'application.
 */
public enum SessionStatut {
    PLANIFIEE,
    EN_COURS,
    TERMINEE,
    ANNULEE;

    /**
     * Retourne le libellé français du statut pour l'affichage dans l'interface utilisateur.
     * @return Le libellé localisé du statut
     */
    public String libelle() {
        return switch (this) {
            case PLANIFIEE -> "Planifiée";
            case EN_COURS  -> "En cours";
            case TERMINEE  -> "Terminée";
            case ANNULEE   -> "Annulée";
        };
    }

    /**
     * Retourne la classe CSS appropriée pour l'affichage du badge de statut.
     * @return La classe CSS pour le style du badge
     */
    public String badgeCssClass() {
        return switch (this) {
            case PLANIFIEE -> "badge-planifiee";
            case EN_COURS  -> "badge-en-cours";
            case TERMINEE  -> "badge-terminee";
            case ANNULEE   -> "badge-annulee";
        };
    }

    /**
     * Retourne l'icône appropriée pour l'affichage du statut.
     * @return L'icône Unicode ou emoji représentant le statut
     */
    public String icone() {
        return switch (this) {
            case PLANIFIEE -> "📅";
            case EN_COURS  -> "🔴";
            case TERMINEE  -> "✅";
            case ANNULEE   -> "❌";
        };
    }

    /**
     * Convertit une chaîne de caractères en SessionStatut.
     * @param value La valeur string à convertir
     * @return Le SessionStatut correspondant, ou PLANIFIEE par défaut
     */
    public static SessionStatut fromString(String value) {
        if (value == null) return PLANIFIEE;
        return switch (value.toUpperCase().trim()) {
            case "EN_COURS", "EN COURS" -> EN_COURS;
            case "TERMINEE", "TERMINÉE" -> TERMINEE;
            case "ANNULEE", "ANNULÉE"   -> ANNULEE;
            default                     -> PLANIFIEE;
        };
    }

    /**
     * Vérifie si une transition de statut est valide.
     * @param nouveauStatut Le nouveau statut souhaité
     * @return true si la transition est autorisée, false sinon
     */
    public boolean peutTransitionnerVers(SessionStatut nouveauStatut) {
        return switch (this) {
            case PLANIFIEE -> nouveauStatut == EN_COURS || nouveauStatut == ANNULEE;
            case EN_COURS  -> nouveauStatut == TERMINEE || nouveauStatut == ANNULEE;
            case TERMINEE  -> false; // Une session terminée ne peut plus changer
            case ANNULEE   -> false; // Une session annulée ne peut plus changer
        };
    }

    /**
     * Vérifie si le statut permet aux étudiants de rejoindre la session.
     * @return true si les étudiants peuvent rejoindre, false sinon
     */
    public boolean peutRejoindre() {
        return this == EN_COURS;
    }

    /**
     * Vérifie si le statut indique une session active.
     * @return true si la session est active, false sinon
     */
    public boolean estActive() {
        return this == EN_COURS;
    }
}