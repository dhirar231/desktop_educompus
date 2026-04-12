package com.educompus.model;

import java.time.LocalDateTime;

public class Livraison {
    private int id;
    private String adresse;
    private String ville;
    private LocalDateTime dateLivraison;
    private String statusLivraison;
    private Integer commandeId;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expedieeAt;
    private LocalDateTime livreeAt;
    private LocalDateTime annuleeAt;
    private String phoneNumber;

    public Livraison() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }
    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }
    public String getStatusLivraison() { return statusLivraison; }
    public void setStatusLivraison(String statusLivraison) { this.statusLivraison = statusLivraison; }
    public Integer getCommandeId() { return commandeId; }
    public void setCommandeId(Integer commandeId) { this.commandeId = commandeId; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getExpedieeAt() { return expedieeAt; }
    public void setExpedieeAt(LocalDateTime expedieeAt) { this.expedieeAt = expedieeAt; }
    public LocalDateTime getLivreeAt() { return livreeAt; }
    public void setLivreeAt(LocalDateTime livreeAt) { this.livreeAt = livreeAt; }
    public LocalDateTime getAnnuleeAt() { return annuleeAt; }
    public void setAnnuleeAt(LocalDateTime annuleeAt) { this.annuleeAt = annuleeAt; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    @Override
    public String toString() {
        return "Livraison{id=" + id + ", ville='" + ville + "', status='" + statusLivraison + "'}";
    }
}
