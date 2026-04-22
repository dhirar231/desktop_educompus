package com.educompus.repository;

import com.educompus.model.Panier;

import java.sql.*;
import java.util.ArrayList;

public class PanierRepository {

    public void insert(Panier p) throws SQLException {
        String sql = "INSERT INTO panier (quantite, user_id, produit_id) VALUES (?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, p.getQuantite());
            if (p.getUserId() != null) ps.setInt(2, p.getUserId());
            else ps.setNull(2, Types.INTEGER);
            ps.setInt(3, p.getProduitId());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void updateQuantite(Panier p) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("UPDATE panier SET quantite=? WHERE id=?");
        try { ps.setInt(1, p.getQuantite()); ps.setInt(2, p.getId()); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM panier WHERE id=?");
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public void deleteByUser(int userId) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        try { deleteByUser(conn, userId); }
        finally { conn.close(); }
    }

    /** Version transactionnelle */
    public void deleteByUser(Connection conn, int userId) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM panier WHERE user_id=?");
        try { ps.setInt(1, userId); ps.executeUpdate(); }
        finally { ps.close(); }
    }

    public ArrayList<Panier> findAll() throws SQLException {
        ArrayList<Panier> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM panier");
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public ArrayList<Panier> findByUser(int userId) throws SQLException {
        ArrayList<Panier> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM panier WHERE user_id=?");
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public Panier findByUserAndProduit(Integer userId, int produitId) throws SQLException {
        String sql = "SELECT * FROM panier WHERE user_id=? AND produit_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            if (userId != null) ps.setInt(1, userId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, produitId);
            ResultSet rs = ps.executeQuery();
            try { return rs.next() ? mapRow(rs) : null; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    private Panier mapRow(ResultSet rs) throws SQLException {
        int uid = rs.getInt("user_id");
        return new Panier(rs.getInt("id"), rs.getInt("quantite"),
                rs.wasNull() ? null : uid, rs.getInt("produit_id"));
    }
}
