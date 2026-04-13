package com.educompus.model;

public class LigneCommande {
    private int id;
    private int commandeId;
    private Integer produitId; // nullable
    private String nomProduit;
    private String imageProduit;
    private double prixUnitaire;
    private int quantite;

    public LigneCommande() {}

    public LigneCommande(int id, int commandeId, Integer produitId, String nomProduit,
                         String imageProduit, double prixUnitaire, int quantite) {
        this.id = id;
        this.commandeId = commandeId;
        this.produitId = produitId;
        this.nomProduit = nomProduit;
        this.imageProduit = imageProduit;
        this.prixUnitaire = prixUnitaire;
        this.quantite = quantite;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCommandeId() { return commandeId; }
    public void setCommandeId(int commandeId) { this.commandeId = commandeId; }
    public Integer getProduitId() { return produitId; }
    public void setProduitId(Integer produitId) { this.produitId = produitId; }
    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }
    public String getImageProduit() { return imageProduit; }
    public void setImageProduit(String imageProduit) { this.imageProduit = imageProduit; }
    public double getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(double prixUnitaire) { this.prixUnitaire = prixUnitaire; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    @Override
    public String toString() {
        return "LigneCommande{id=" + id + ", nomProduit='" + nomProduit + "', quantite=" + quantite + "}";
    }
}
