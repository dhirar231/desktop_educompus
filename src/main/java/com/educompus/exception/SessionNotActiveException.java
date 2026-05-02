package com.educompus.exception;

/**
 * Exception levée quand un étudiant tente de rejoindre une session non active.
 */
public class SessionNotActiveException extends RuntimeException {
    private final int sessionId;

    public SessionNotActiveException(int sessionId) {
        super("La session " + sessionId + " n'est pas active.");
        this.sessionId = sessionId;
    }

    public SessionNotActiveException(int sessionId, String statut) {
        super("La session " + sessionId + " n'est pas active (statut : " + statut + ").");
        this.sessionId = sessionId;
    }

    public int getSessionId() { return sessionId; }
}
