package com.educompus.repository;

import com.educompus.model.Avis;

import java.sql.*;
import java.util.ArrayList;

public class AvisRepository {

    public void insert(Avis a) throws SQLException {
        String sql = "INSERT INTO avis (user_id, produit_id, note, commentaire, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, a.getUserId());
            ps.setInt(2, a.getProduitId());
            ps.setInt(3, a.getNote());
            ps.setString(4, a.getCommentaire());
            ps.setTimestamp(5, Timestamp.valueOf(a.getCreatedAt()));
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void update(Avis a) throws SQLException {
        String sql = "UPDATE avis SET note=?, commentaire=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, a.getNote());
            ps.setString(2, a.getCommentaire());
            ps.setInt(3, a.getId());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM avis WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public ArrayList<Avis> findAll() throws SQLException {
        ArrayList<Avis> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM avis");
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public ArrayList<Avis> findByProduit(int produitId) throws SQLException {
        ArrayList<Avis> liste = new ArrayList<>();
        String sql = "SELECT * FROM avis WHERE produit_id=? ORDER BY created_at DESC";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    private Avis mapRow(ResultSet rs) throws SQLException {
        return new Avis(
                rs.getInt("id"), rs.getInt("user_id"), rs.getInt("produit_id"),
                rs.getInt("note"), rs.getString("commentaire"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
