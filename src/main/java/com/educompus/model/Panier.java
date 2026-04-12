package com.educompus.model;

public class Panier {
    private int id;
    private int quantite;
    private Integer userId;
    private int produitId;

    public Panier() {}

    public Panier(int id, int quantite, Integer userId, int produitId) {
        this.id = id;
        this.quantite = quantite;
        this.userId = userId;
        this.produitId = produitId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public int getProduitId() { return produitId; }
    public void setProduitId(int produitId) { this.produitId = produitId; }

    @Override
    public String toString() {
        return "Panier{id=" + id + ", produitId=" + produitId + ", quantite=" + quantite + "}";
    }
}
