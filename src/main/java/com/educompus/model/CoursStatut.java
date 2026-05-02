package com.educompus.model;

public enum CoursStatut {
    EN_ATTENTE,
    APPROUVE,
    REFUSE;

    public String libelle() {
        return switch (this) {
            case EN_ATTENTE -> "En attente";
            case APPROUVE   -> "Approuvé";
            case REFUSE     -> "Refusé";
        };
    }

    public String badgeCssClass() {
        return switch (this) {
            case EN_ATTENTE -> "badge-en-attente";
            case APPROUVE   -> "badge-approuve";
            case REFUSE     -> "badge-refuse";
        };
    }

    public static CoursStatut fromString(String value) {
        if (value == null) return EN_ATTENTE;
        return switch (value.toUpperCase().trim()) {
            case "APPROUVE"  -> APPROUVE;
            case "REFUSE"    -> REFUSE;
            default          -> EN_ATTENTE;
        };
    }
}
