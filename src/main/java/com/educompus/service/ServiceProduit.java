package com.educompus.service;

import com.educompus.model.Produit;
import com.educompus.repository.EducompusDB;

import java.sql.*;
import java.util.ArrayList;

public class ServiceProduit {

    public void ajouter(Produit produit) throws SQLException {
        String sql = "INSERT INTO produit (nom, description, prix, type, categorie, image, user_id, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getDescription());
            ps.setDouble(3, produit.getPrix());
            ps.setString(4, produit.getType());
            ps.setString(5, produit.getCategorie());
            ps.setString(6, produit.getImage());
            ps.setInt(7, produit.getUserId());
            ps.setInt(8, produit.getStock());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void update(Produit produit) throws SQLException {
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, type=?, categorie=?, image=?, stock=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, produit.getNom());
            ps.setString(2, produit.getDescription());
            ps.setDouble(3, produit.getPrix());
            ps.setString(4, produit.getType());
            ps.setString(5, produit.getCategorie());
            ps.setString(6, produit.getImage());
            ps.setInt(7, produit.getStock());
            ps.setInt(8, produit.getId());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id=?";
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

    public ArrayList<Produit> afficherAll() throws SQLException {
        ArrayList<Produit> liste = new ArrayList<>();
        String sql = "SELECT * FROM produit";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    liste.add(mapRow(rs));
                }
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
        return liste;
    }

    public Produit findById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, id);
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

    public void decrementeStock(int produitId, int quantite) throws SQLException {
        String sql = "UPDATE produit SET stock = stock - ? WHERE id = ? AND stock >= ?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, quantite);
            ps.setInt(2, produitId);
            ps.setInt(3, quantite);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Stock insuffisant pour le produit #" + produitId);
            }
        } finally {
            ps.close();
            conn.close();
        }
    }

    private Produit mapRow(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getString("description"),
                rs.getDouble("prix"),
                rs.getString("type"),
                rs.getString("categorie"),
                rs.getString("image"),
                rs.getInt("user_id"),
                rs.getInt("stock")
        );
    }
}
