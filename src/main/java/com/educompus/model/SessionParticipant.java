package com.educompus.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Représente un participant à une session live.
 * Suit la présence et l'état interactif de chaque étudiant.
 */
public final class SessionParticipant {

    private int id;
    private int sessionId;
    private int etudiantId;
    private String nomEtudiant;
    private LocalDateTime heureJoin;
    private LocalDateTime heureLeave;   // null si encore présent
    private boolean mainLevee;
    private boolean demandeParole;
    private boolean paroleAccordee;

    public SessionParticipant() {}

    public SessionParticipant(int sessionId, int etudiantId, String nomEtudiant) {
        this.sessionId    = sessionId;
        this.etudiantId   = etudiantId;
        this.nomEtudiant  = nomEtudiant;
        this.heureJoin    = LocalDateTime.now();
        this.mainLevee    = false;
        this.demandeParole = false;
        this.paroleAccordee = false;
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

    public LocalDateTime getHeureJoin() { return heureJoin; }
    public void setHeureJoin(LocalDateTime heureJoin) { this.heureJoin = heureJoin; }

    public LocalDateTime getHeureLeave() { return heureLeave; }
    public void setHeureLeave(LocalDateTime heureLeave) { this.heureLeave = heureLeave; }

    public boolean isMainLevee() { return mainLevee; }
    public void setMainLevee(boolean mainLevee) { this.mainLevee = mainLevee; }

    public boolean isDemandeParole() { return demandeParole; }
    public void setDemandeParole(boolean demandeParole) { this.demandeParole = demandeParole; }

    public boolean isParoleAccordee() { return paroleAccordee; }
    public void setParoleAccordee(boolean paroleAccordee) { this.paroleAccordee = paroleAccordee; }

    // ── Méthodes utilitaires ──────────────────────────────────────────────────

    /** Vrai si l'étudiant est encore dans la session. */
    public boolean estPresent() {
        return heureLeave == null;
    }

    /** Durée de présence en minutes (0 si encore présent ou données manquantes). */
    public long getDureePresenceMinutes() {
        if (heureJoin == null) return 0;
        LocalDateTime fin = heureLeave != null ? heureLeave : LocalDateTime.now();
        return java.time.Duration.between(heureJoin, fin).toMinutes();
    }

    public String getHeureJoinFormatee() {
        if (heureJoin == null) return "-";
        return heureJoin.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getHeureLeaveFormatee() {
        if (heureLeave == null) return "En cours";
        return heureLeave.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Override
    public String toString() {
        return nomEtudiant + " (session=" + sessionId + ", présent=" + estPresent() + ")";
    }
}
