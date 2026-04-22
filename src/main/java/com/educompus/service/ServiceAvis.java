package com.educompus.service;

import com.educompus.model.Avis;
import com.educompus.repository.AvisRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServiceAvis {

    private final AvisRepository repo = new AvisRepository();

    public void ajouter(Avis avis) throws SQLException {
        repo.insert(avis);
    }

    public void update(Avis avis) throws SQLException {
        repo.update(avis);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<Avis> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public ArrayList<Avis> afficherByProduit(int produitId) throws SQLException {
        return repo.findByProduit(produitId);
    }
}
