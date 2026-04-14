package com.educompus.repository;

import com.educompus.model.Produit;

import java.sql.*;
import java.util.ArrayList;

public class ProduitRepository {

    public void insert(Produit p) throws SQLException {
        String sql = "INSERT INTO produit (nom, description, prix, type, categorie, image, user_id, stock) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getType());
            ps.setString(5, p.getCategorie());
            ps.setString(6, p.getImage());
            ps.setInt(7, p.getUserId());
            ps.setInt(8, p.getStock());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void update(Produit p) throws SQLException {
        String sql = "UPDATE produit SET nom=?, description=?, prix=?, type=?, categorie=?, image=?, stock=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, p.getNom());
            ps.setString(2, p.getDescription());
            ps.setDouble(3, p.getPrix());
            ps.setString(4, p.getType());
            ps.setString(5, p.getCategorie());
            ps.setString(6, p.getImage());
            ps.setInt(7, p.getStock());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM produit WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public ArrayList<Produit> findAll() throws SQLException {
        ArrayList<Produit> liste = new ArrayList<>();
        String sql = "SELECT * FROM produit";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public Produit findById(int id) throws SQLException {
        String sql = "SELECT * FROM produit WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            try { return rs.next() ? mapRow(rs) : null; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    public boolean existsByNomTypeCategorie(String nom, String type, String categorie, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM produit WHERE LOWER(nom)=LOWER(?) AND LOWER(type)=LOWER(?) AND LOWER(categorie)=LOWER(?) AND id<>?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, nom.trim());
            ps.setString(2, type.trim());
            ps.setString(3, categorie.trim());
            ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            try { return rs.next() && rs.getInt(1) > 0; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    public void decrementeStock(int produitId, int quantite) throws SQLException {
        String sql = "UPDATE produit SET stock = stock - ? WHERE id=? AND stock >= ?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, quantite);
            ps.setInt(2, produitId);
            ps.setInt(3, quantite);
            if (ps.executeUpdate() == 0)
                throw new SQLException("Stock insuffisant pour le produit #" + produitId);
        } finally { ps.close(); conn.close(); }
    }

    private Produit mapRow(ResultSet rs) throws SQLException {
        return new Produit(
                rs.getInt("id"), rs.getString("nom"), rs.getString("description"),
                rs.getDouble("prix"), rs.getString("type"), rs.getString("categorie"),
                rs.getString("image"), rs.getInt("user_id"), rs.getInt("stock"));
    }
}
