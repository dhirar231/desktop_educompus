package com.educompus.repository;

import com.educompus.model.Commande;

import java.sql.*;
import java.util.ArrayList;

public class CommandeRepository {

    public void insert(Commande c) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        try { insert(conn, c); }
        finally { conn.close(); }
    }

    /** Version transactionnelle — connexion externe */
    public void insert(Connection conn, Commande c) throws SQLException {
        String sql = "INSERT INTO commande (total, date_commande, user_id) VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            ps.setDouble(1, c.getTotal());
            ps.setTimestamp(2, Timestamp.valueOf(c.getDateCommande()));
            ps.setInt(3, c.getUserId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            try { if (keys.next()) c.setId(keys.getInt(1)); }
            finally { keys.close(); }
        } finally { ps.close(); }
    }

    public void updateDate(Commande c) throws SQLException {
        String sql = "UPDATE commande SET date_commande=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setTimestamp(1, Timestamp.valueOf(c.getDateCommande()));
            ps.setInt(2, c.getId());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM commande WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public ArrayList<Commande> findAll() throws SQLException {
        ArrayList<Commande> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM commande");
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public ArrayList<Commande> findByUser(int userId) throws SQLException {
        ArrayList<Commande> liste = new ArrayList<>();
        String sql = "SELECT * FROM commande WHERE user_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    private Commande mapRow(ResultSet rs) throws SQLException {
        return new Commande(
                rs.getInt("id"), rs.getDouble("total"),
                rs.getTimestamp("date_commande").toLocalDateTime(),
                rs.getInt("user_id"));
    }
}
