package com.educompus.exception;

/**
 * Exception levée quand une session est introuvable en base.
 */
public class SessionNotFoundException extends RuntimeException {
    private final int sessionId;

    public SessionNotFoundException(int sessionId) {
        super("Session introuvable : id=" + sessionId);
        this.sessionId = sessionId;
    }

    public int getSessionId() { return sessionId; }
}
