package com.educompus.model;

import java.time.LocalDateTime;

public class Commande {
    private int id;
    private double total;
    private LocalDateTime dateCommande;
    private int userId;
    private String reference;

    public Commande() {}

    public Commande(int id, double total, LocalDateTime dateCommande, int userId) {
        this.id = id;
        this.total = total;
        this.dateCommande = dateCommande;
        this.userId = userId;
    }

    public Commande(int id, double total, LocalDateTime dateCommande, int userId, String reference) {
        this.id = id;
        this.total = total;
        this.dateCommande = dateCommande;
        this.userId = userId;
        this.reference = reference;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    /** Retourne la référence affichable : si `reference` absent, formate l'ID en 6 chiffres. */
    public String getDisplayReference() {
        if (reference != null && !reference.isBlank()) return reference;
        return String.format("%06d", Math.max(0, id));
    }

    @Override
    public String toString() {
        return "Commande{id=" + id + ", total=" + total + ", date=" + dateCommande + ", reference=" + reference + "}";
    }
}
