package com.educompus.model;

import java.time.LocalDateTime;

public class Avis {
    private int id;
    private int userId;
    private int produitId;
    private int note;
    private String commentaire;
    private LocalDateTime createdAt;

    public Avis() {}

    public Avis(int id, int userId, int produitId, int note, String commentaire, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.produitId = produitId;
        this.note = note;
        this.commentaire = commentaire;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }
    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }
    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Avis{id=" + id + ", note=" + note + ", produitId=" + produitId + "}";
    }
}
