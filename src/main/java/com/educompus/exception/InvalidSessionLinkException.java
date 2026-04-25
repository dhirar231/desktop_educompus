package com.educompus.exception;

/**
 * Exception levée quand le lien d'une session est invalide ou non reconnu.
 */
public class InvalidSessionLinkException extends RuntimeException {
    private final String lien;

    public InvalidSessionLinkException(String lien) {
        super("Lien de session invalide : " + lien);
        this.lien = lien;
    }

    public InvalidSessionLinkException(String lien, String raison) {
        super("Lien de session invalide (" + raison + ") : " + lien);
        this.lien = lien;
    }

    public String getLien() { return lien; }
}
