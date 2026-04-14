package com.educompus.service;

import com.educompus.model.Commande;
import com.educompus.repository.CommandeRepository;

import java.sql.SQLException;
import java.util.ArrayList;

public class ServiceCommande {

    private final CommandeRepository repo = new CommandeRepository();

    public void ajouter(Commande commande) throws SQLException {
        repo.insert(commande);
    }

    /** Back-office : l'admin peut modifier la date */
    public void updateDate(Commande commande) throws SQLException {
        repo.updateDate(commande);
    }

    public void delete(int id) throws SQLException {
        repo.delete(id);
    }

    public ArrayList<Commande> afficherAll() throws SQLException {
        return repo.findAll();
    }

    public ArrayList<Commande> afficherByUser(int userId) throws SQLException {
        return repo.findByUser(userId);
    }
}
