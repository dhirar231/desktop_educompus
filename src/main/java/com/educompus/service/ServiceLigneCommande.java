package com.educompus.service;

import com.educompus.model.LigneCommande;
import com.educompus.repository.LigneCommandeRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServiceLigneCommande {

    private final LigneCommandeRepository repo = new LigneCommandeRepository();

    public void ajouter(LigneCommande lc) throws SQLException {
        repo.insert(lc);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<LigneCommande> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public ArrayList<LigneCommande> afficherByCommande(int commandeId) throws SQLException {
        return repo.findByCommande(commandeId);
    }
}
