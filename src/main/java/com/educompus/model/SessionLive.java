package com.educompus.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle représentant une session live de visioconférence.
 * Une session live permet aux étudiants de participer à des cours interactifs
 * via des plateformes externes comme Google Meet, Zoom, etc.
 */
public final class SessionLive {
    private int id;
    private String nomCours;
    private String lien;
    private LocalDate date;
    private LocalTime heure;
    private SessionStatut statut = SessionStatut.PLANIFIEE;
    private int coursId; // ID du cours associé

    // Constructeurs
    
    /**
     * Constructeur par défaut.
     */
    public SessionLive() {
    }

    /**
     * Constructeur avec tous les paramètres principaux.
     * 
     * @param nomCours Le nom du cours associé à la session
     * @param lien Le lien vers la plateforme de visioconférence
     * @param date La date de la session
     * @param heure L'heure de début de la session
     */
    public SessionLive(String nomCours, String lien, LocalDate date, LocalTime heure) {
        this.nomCours = nomCours;
        this.lien = lien;
        this.date = date;
        this.heure = heure;
        this.statut = SessionStatut.PLANIFIEE;
    }

    /**
     * Constructeur complet avec statut.
     * 
     * @param id L'identifiant unique de la session
     * @param nomCours Le nom du cours associé à la session
     * @param lien Le lien vers la plateforme de visioconférence
     * @param date La date de la session
     * @param heure L'heure de début de la session
     * @param statut Le statut actuel de la session
     */
    public SessionLive(int id, String nomCours, String lien, LocalDate date, LocalTime heure, SessionStatut statut) {
        this.id = id;
        this.nomCours = nomCours;
        this.lien = lien;
        this.date = date;
        this.heure = heure;
        this.statut = statut == null ? SessionStatut.PLANIFIEE : statut;
    }

    // Getters et Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNomCours() {
        return nomCours;
    }

    public void setNomCours(String nomCours) {
        this.nomCours = nomCours;
    }

    public String getLien() {
        return lien;
    }

    public void setLien(String lien) {
        this.lien = lien;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getHeure() {
        return heure;
    }

    public void setHeure(LocalTime heure) {
        this.heure = heure;
    }

    public SessionStatut getStatut() {
        return statut;
    }

    public void setStatut(SessionStatut statut) {
        this.statut = statut == null ? SessionStatut.PLANIFIEE : statut;
    }

    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }

    // Méthodes utilitaires

    /**
     * Vérifie si la session peut être rejointe par les étudiants.
     * @return true si la session est en cours, false sinon
     */
    public boolean peutEtreRejointe() {
        return statut != null && statut.peutRejoindre();
    }

    /**
     * Vérifie si la session est actuellement active.
     * @return true si la session est en cours, false sinon
     */
    public boolean estActive() {
        return statut != null && statut.estActive();
    }

    /**
     * Retourne une représentation formatée de la date et heure.
     * @return La date et heure formatées pour l'affichage
     */
    public String getDateHeureFormatee() {
        if (date == null || heure == null) {
            return "Date non définie";
        }
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return date.format(dateFormatter) + " à " + heure.format(timeFormatter);
    }

    /**
     * Retourne l'icône du statut pour l'affichage dans l'interface.
     * @return L'icône correspondant au statut actuel
     */
    public String getIconeStatut() {
        return statut != null ? statut.icone() : SessionStatut.PLANIFIEE.icone();
    }

    /**
     * Retourne le libellé du statut pour l'affichage dans l'interface.
     * @return Le libellé localisé du statut actuel
     */
    public String getLibelleStatut() {
        return statut != null ? statut.libelle() : SessionStatut.PLANIFIEE.libelle();
    }

    /**
     * Retourne la classe CSS pour le badge de statut.
     * @return La classe CSS appropriée pour le styling
     */
    public String getCssClasseStatut() {
        return statut != null ? statut.badgeCssClass() : SessionStatut.PLANIFIEE.badgeCssClass();
    }

    /**
     * Vérifie si le lien de session est valide (non null et non vide).
     * @return true si le lien est valide, false sinon
     */
    public boolean aLienValide() {
        return lien != null && !lien.trim().isEmpty();
    }

    /**
     * Vérifie si la session a toutes les informations requises.
     * @return true si la session est complète, false sinon
     */
    public boolean estComplete() {
        return nomCours != null && !nomCours.trim().isEmpty() &&
               lien != null && !lien.trim().isEmpty() &&
               date != null &&
               heure != null &&
               statut != null;
    }

    @Override
    public String toString() {
        if (nomCours == null || nomCours.isBlank()) {
            return "Session #" + id;
        }
        
        String dateHeure = getDateHeureFormatee();
        String statutInfo = statut != null ? " (" + statut.libelle() + ")" : "";
        
        return nomCours + " - " + dateHeure + statutInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SessionLive that = (SessionLive) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}