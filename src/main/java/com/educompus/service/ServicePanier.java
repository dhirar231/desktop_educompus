package com.educompus.service;

import com.educompus.model.Panier;
import com.educompus.repository.PanierRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServicePanier {

    private final PanierRepository repo = new PanierRepository();

    /** Ajout depuis le front — si le produit existe déjà, incrémente la quantité */
    public void ajouter(Panier panier) throws SQLException {
        Panier existant = repo.findByUserAndProduit(panier.getUserId(), panier.getProduitId());
        if (existant != null) {
            existant.setQuantite(existant.getQuantite() + panier.getQuantite());
            repo.updateQuantite(existant);
        } else {
            repo.insert(panier);
        }
    }

    public void updateQuantite(Panier panier) throws SQLException {
        repo.updateQuantite(panier);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<Panier> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public ArrayList<Panier> afficherByUser(int userId) throws SQLException {
        return repo.findByUser(userId);
    }

    public void viderPanier(int userId) throws SQLException {
        repo.deleteByUser(userId);
    }
}
