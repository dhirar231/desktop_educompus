package com.educompus.model;

public class Produit {
    private int id;
    private String nom;
    private String description;
    private double prix;
    private String type;
    private String categorie;
    private String image;
    private int userId;
    private int stock;

    public Produit() {}

    public Produit(int id, String nom, String description, double prix,
                   String type, String categorie, String image, int userId, int stock) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.type = type;
        this.categorie = categorie;
        this.image = image;
        this.userId = userId;
        this.stock = stock;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    @Override
    public String toString() {
        return "Produit{id=" + id + ", nom='" + nom + "', prix=" + prix + ", stock=" + stock + "}";
    }
}
