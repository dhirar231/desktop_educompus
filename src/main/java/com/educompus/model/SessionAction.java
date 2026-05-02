package com.educompus.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente une action/événement survenu pendant une session live.
 * Constitue le journal d'audit complet de la session.
 */
public final class SessionAction {

    private int id;
    private int sessionId;
    private int etudiantId;       // 0 si action système (start/end)
    private String nomEtudiant;   // pour affichage
    private ActionType type;
    private LocalDateTime timestamp;
    private String details;       // informations complémentaires optionnelles

    public SessionAction() {}

    public SessionAction(int sessionId, int etudiantId, String nomEtudiant, ActionType type) {
        this.sessionId   = sessionId;
        this.etudiantId  = etudiantId;
        this.nomEtudiant = nomEtudiant;
        this.type        = type;
        this.timestamp   = LocalDateTime.now();
    }

    public SessionAction(int sessionId, int etudiantId, String nomEtudiant,
                         ActionType type, String details) {
        this(sessionId, etudiantId, nomEtudiant, type);
        this.details = details;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public int getEtudiantId() { return etudiantId; }
    public void setEtudiantId(int etudiantId) { this.etudiantId = etudiantId; }

    public String getNomEtudiant() { return nomEtudiant; }
    public void setNomEtudiant(String nomEtudiant) { this.nomEtudiant = nomEtudiant; }

    public ActionType getType() { return type; }
    public void setType(ActionType type) { this.type = type; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    // ── Méthodes utilitaires ──────────────────────────────────────────────────

    public String getTimestampFormate() {
        if (timestamp == null) return "-";
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /** Résumé lisible pour l'affichage dans le journal. */
    public String getResume() {
        String icone = type != null ? type.icone() + " " : "";
        String libelle = type != null ? type.libelle() : "Action inconnue";
        String acteur = (nomEtudiant != null && !nomEtudiant.isBlank()) ? nomEtudiant : "Système";
        String det = (details != null && !details.isBlank()) ? " — " + details : "";
        return icone + "[" + getTimestampFormate() + "] " + acteur + " : " + libelle + det;
    }

    @Override
    public String toString() {
        return getResume();
    }
}
