package com.educompus.model;

import java.time.LocalDateTime;

/**
 * Modèle représentant l'état d'une notification pour une session live.
 * Utilisé pour suivre les notifications envoyées et éviter les doublons.
 */
public final class NotificationState {
    private int id;
    private int sessionId;
    private NotificationType type;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private boolean sent;
    private LocalDateTime createdAt;
    
    /**
     * Constructeur par défaut.
     */
    public NotificationState() {
        this.sent = false;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Constructeur avec paramètres principaux.
     * 
     * @param sessionId ID de la session concernée
     * @param type Type de notification
     * @param scheduledTime Heure prévue d'envoi
     */
    public NotificationState(int sessionId, NotificationType type, LocalDateTime scheduledTime) {
        this();
        this.sessionId = sessionId;
        this.type = type;
        this.scheduledTime = scheduledTime;
    }
    
    /**
     * Constructeur complet.
     * 
     * @param id ID de l'état de notification
     * @param sessionId ID de la session concernée
     * @param type Type de notification
     * @param scheduledTime Heure prévue d'envoi
     * @param sentTime Heure réelle d'envoi
     * @param sent État d'envoi
     * @param createdAt Date de création
     */
    public NotificationState(int id, int sessionId, NotificationType type, 
                           LocalDateTime scheduledTime, LocalDateTime sentTime, 
                           boolean sent, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.type = type;
        this.scheduledTime = scheduledTime;
        this.sentTime = sentTime;
        this.sent = sent;
        this.createdAt = createdAt;
    }
    
    // Getters
    public int getId() {
        return id;
    }
    
    public int getSessionId() {
        return sessionId;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
    
    public LocalDateTime getSentTime() {
        return sentTime;
    }
    
    public boolean isSent() {
        return sent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // Setters
    public void setId(int id) {
        this.id = id;
    }
    
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }
    
    public void setSent(boolean sent) {
        this.sent = sent;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Marque la notification comme envoyée avec l'heure actuelle.
     */
    public void markAsSent() {
        this.sent = true;
        this.sentTime = LocalDateTime.now();
    }
    
    /**
     * Vérifie si la notification est due (heure programmée dépassée).
     * 
     * @return true si la notification doit être envoyée
     */
    public boolean isDue() {
        return scheduledTime != null && LocalDateTime.now().isAfter(scheduledTime);
    }
    
    /**
     * Vérifie si la notification est dans la fenêtre d'envoi (2 minutes après l'heure prévue).
     * 
     * @return true si dans la fenêtre d'envoi
     */
    public boolean isInSendingWindow() {
        if (scheduledTime == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(scheduledTime) && now.isBefore(scheduledTime.plusMinutes(2));
    }
    
    @Override
    public String toString() {
        return String.format("NotificationState{id=%d, sessionId=%d, type=%s, scheduledTime=%s, sent=%s}", 
                           id, sessionId, type, scheduledTime, sent);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        NotificationState that = (NotificationState) obj;
        return sessionId == that.sessionId && type == that.type;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(sessionId, type);
    }
}