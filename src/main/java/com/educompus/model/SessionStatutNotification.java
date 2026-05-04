package com.educompus.model;

/**
 * Énumération des statuts de session pour le système de notifications.
 * Utilisée spécifiquement par le système de notifications automatiques.
 */
public enum SessionStatutNotification {
    PROGRAMMEE("Programmée"),
    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");
    
    private final String libelle;
    
    /**
     * Constructeur de l'énumération.
     * 
     * @param libelle Libellé français du statut
     */
    SessionStatutNotification(String libelle) {
        this.libelle = libelle;
    }
    
    /**
     * Retourne le libellé français du statut.
     * 
     * @return Libellé pour l'affichage
     */
    public String getLibelle() {
        return libelle;
    }
    
    /**
     * Convertit depuis l'ancien système de statuts.
     * 
     * @param oldStatus Ancien statut
     * @return Nouveau statut équivalent
     */
    public static SessionStatutNotification fromOldStatus(SessionStatut oldStatus) {
        return switch (oldStatus) {
            case PLANIFIEE -> PROGRAMMEE;
            case EN_COURS -> EN_COURS;
            case TERMINEE -> TERMINEE;
            case ANNULEE -> ANNULEE;
        };
    }
    
    /**
     * Convertit vers l'ancien système de statuts.
     * 
     * @return Ancien statut équivalent
     */
    public SessionStatut toOldStatus() {
        return switch (this) {
            case PROGRAMMEE -> SessionStatut.PLANIFIEE;
            case EN_COURS -> SessionStatut.EN_COURS;
            case TERMINEE -> SessionStatut.TERMINEE;
            case ANNULEE -> SessionStatut.ANNULEE;
        };
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}