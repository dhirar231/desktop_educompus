package com.educompus.service;

import com.educompus.model.Produit;
import com.educompus.repository.ProduitRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServiceProduit {

    private final ProduitRepository repo = new ProduitRepository();

    public void ajouter(Produit produit) throws SQLException {
        if (repo.existsByNomTypeCategorie(produit.getNom(), produit.getType(), produit.getCategorie(), -1))
            throw new SQLException("Un produit avec le même nom, type et catégorie existe déjà.");
        repo.insert(produit);
    }

    public void update(Produit produit) throws SQLException {
        if (repo.existsByNomTypeCategorie(produit.getNom(), produit.getType(), produit.getCategorie(), produit.getId()))
            throw new SQLException("Un autre produit avec le même nom, type et catégorie existe déjà.");
        repo.update(produit);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<Produit> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public Produit findById(int id) throws SQLException {
        return repo.findById(id);
    }

    public void decrementeStock(int produitId, int quantite) throws SQLException {
        repo.decrementeStock(produitId, quantite);
    }
}
