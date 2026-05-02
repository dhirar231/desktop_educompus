package com.educompus.model;

/**
 * Énumération des types de notifications pour les sessions live.
 * Définit les différents moments de notification avant le début d'une session.
 */
public enum NotificationType {
    THIRTY_MINUTES("30min", "30 minutes avant"),
    FIVE_MINUTES("5min", "5 minutes avant");
    
    private final String code;
    private final String description;
    
    /**
     * Constructeur de l'énumération.
     * 
     * @param code Code court pour la base de données
     * @param description Description lisible du type de notification
     */
    NotificationType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * Retourne le code court du type de notification.
     * 
     * @return Code utilisé en base de données
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Retourne la description lisible du type de notification.
     * 
     * @return Description pour l'affichage utilisateur
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Trouve un type de notification par son code.
     * 
     * @param code Code à rechercher
     * @return Type de notification correspondant
     * @throws IllegalArgumentException si le code n'existe pas
     */
    public static NotificationType fromCode(String code) {
        for (NotificationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Code de notification invalide: " + code);
    }
    
    @Override
    public String toString() {
        return description;
    }
}