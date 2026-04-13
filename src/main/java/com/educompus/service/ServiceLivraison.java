package com.educompus.service;

import com.educompus.model.Livraison;
import com.educompus.repository.LivraisonRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServiceLivraison {

    private final LivraisonRepository repo = new LivraisonRepository();

    public void ajouter(Livraison livraison) throws SQLException {
        repo.insert(livraison);
    }

    public void update(Livraison livraison) throws SQLException {
        repo.update(livraison);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<Livraison> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public Livraison findByCommande(int commandeId) throws SQLException {
        return repo.findByCommande(commandeId);
    }
}
