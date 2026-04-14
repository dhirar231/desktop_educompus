package com.educompus.service;

import com.educompus.model.Panier;
import com.educompus.repository.EducompusDB;

import java.sql.*;
import java.util.ArrayList;

public class ServicePanier {

    // Ajout depuis le front uniquement — si le produit existe déjà, incrémente la quantité
    public void ajouter(Panier panier) throws SQLException {
        // Chercher une ligne existante pour ce user + produit
        Panier existant = findByUserAndProduit(panier.getUserId(), panier.getProduitId());
        if (existant != null) {
            existant.setQuantite(existant.getQuantite() + panier.getQuantite());
            updateQuantite(existant);
            return;
        }
        String sql = "INSERT INTO panier (quantite, user_id, produit_id) VALUES (?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, panier.getQuantite());
            if (panier.getUserId() != null) ps.setInt(2, panier.getUserId());
            else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, panier.getProduitId());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    /** Retourne la ligne panier existante pour un user + produit, ou null. */
    public Panier findByUserAndProduit(Integer userId, int produitId) throws SQLException {
        String sql = "SELECT * FROM panier WHERE user_id=? AND produit_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            if (userId != null) ps.setInt(1, userId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, produitId);
            ResultSet rs = ps.executeQuery();
            try {
                if (rs.next()) return mapRow(rs);
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
        return null;
    }

    // Le client peut modifier la quantité
    public void updateQuantite(Panier panier) throws SQLException {
        String sql = "UPDATE panier SET quantite=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, panier.getQuantite());
            ps.setInt(2, panier.getId());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    // Le client peut supprimer un article
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM panier WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public ArrayList<Panier> afficherAll() throws SQLException {
        ArrayList<Panier> liste = new ArrayList<>();
        String sql = "SELECT * FROM panier";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) liste.add(mapRow(rs));
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
        return liste;
    }

    public ArrayList<Panier> afficherByUser(int userId) throws SQLException {
        ArrayList<Panier> liste = new ArrayList<>();
        String sql = "SELECT * FROM panier WHERE user_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) liste.add(mapRow(rs));
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
        return liste;
    }

    public void viderPanier(int userId) throws SQLException {
        String sql = "DELETE FROM panier WHERE user_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    private Panier mapRow(ResultSet rs) throws SQLException {
        int uid = rs.getInt("user_id");
        return new Panier(
                rs.getInt("id"),
                rs.getInt("quantite"),
                rs.wasNull() ? null : uid,
                rs.getInt("produit_id")
        );
    }
}
