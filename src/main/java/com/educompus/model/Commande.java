package com.educompus.model;

import java.time.LocalDateTime;

public class Commande {
    private int id;
    private double total;
    private LocalDateTime dateCommande;
    private int userId;

    public Commande() {}

    public Commande(int id, double total, LocalDateTime dateCommande, int userId) {
        this.id = id;
        this.total = total;
        this.dateCommande = dateCommande;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Commande{id=" + id + ", total=" + total + ", date=" + dateCommande + "}";
    }
}
