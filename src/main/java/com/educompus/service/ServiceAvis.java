package com.educompus.service;

import com.educompus.model.Avis;
import com.educompus.repository.EducompusDB;

import java.sql.*;
import java.util.ArrayList;

public class ServiceAvis {

    public void ajouter(Avis avis) throws SQLException {
        String sql = "INSERT INTO avis (user_id, produit_id, note, commentaire, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, avis.getUserId());
            ps.setInt(2, avis.getProduitId());
            ps.setInt(3, avis.getNote());
            ps.setString(4, avis.getCommentaire());
            ps.setTimestamp(5, Timestamp.valueOf(avis.getCreatedAt()));
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void update(Avis avis) throws SQLException {
        String sql = "UPDATE avis SET note=?, commentaire=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, avis.getNote());
            ps.setString(2, avis.getCommentaire());
            ps.setInt(3, avis.getId());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM avis WHERE id=?";
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

    public ArrayList<Avis> afficherAll() throws SQLException {
        ArrayList<Avis> liste = new ArrayList<>();
        String sql = "SELECT * FROM avis";
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

    // Récupérer les avis d'un produit (affichage sous le produit)
    public ArrayList<Avis> afficherByProduit(int produitId) throws SQLException {
        ArrayList<Avis> liste = new ArrayList<>();
        String sql = "SELECT * FROM avis WHERE produit_id=? ORDER BY created_at DESC";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, produitId);
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

    private Avis mapRow(ResultSet rs) throws SQLException {
        return new Avis(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("produit_id"),
                rs.getInt("note"),
                rs.getString("commentaire"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
