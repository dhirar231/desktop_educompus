package com.educompus.service;

import com.educompus.model.Livraison;
import com.educompus.repository.EducompusDB;

import java.sql.*;
import java.util.ArrayList;

public class ServiceLivraison {

    public void ajouter(Livraison livraison) throws SQLException {
        String sql = "INSERT INTO livraison (adresse, ville, date_livraison, status_livraison, commande_id, tracking_number, created_at, updated_at, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, livraison.getAdresse());
            ps.setString(2, livraison.getVille());
            setNullableTimestamp(ps, 3, livraison.getDateLivraison());
            ps.setString(4, livraison.getStatusLivraison());
            if (livraison.getCommandeId() != null) ps.setInt(5, livraison.getCommandeId());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, livraison.getTrackingNumber());
            ps.setTimestamp(7, Timestamp.valueOf(livraison.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(livraison.getUpdatedAt()));
            ps.setString(9, livraison.getPhoneNumber());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void update(Livraison livraison) throws SQLException {
        String sql = "UPDATE livraison SET adresse=?, ville=?, date_livraison=?, status_livraison=?, tracking_number=?, updated_at=?, expediee_at=?, livree_at=?, annulee_at=?, phone_number=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, livraison.getAdresse());
            ps.setString(2, livraison.getVille());
            setNullableTimestamp(ps, 3, livraison.getDateLivraison());
            ps.setString(4, livraison.getStatusLivraison());
            ps.setString(5, livraison.getTrackingNumber());
            ps.setTimestamp(6, Timestamp.valueOf(livraison.getUpdatedAt()));
            setNullableTimestamp(ps, 7, livraison.getExpedieeAt());
            setNullableTimestamp(ps, 8, livraison.getLivreeAt());
            setNullableTimestamp(ps, 9, livraison.getAnnuleeAt());
            ps.setString(10, livraison.getPhoneNumber());
            ps.setInt(11, livraison.getId());
            ps.executeUpdate();
        } finally {
            ps.close();
            conn.close();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM livraison WHERE id=?";
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

    public ArrayList<Livraison> afficherAll() throws SQLException {
        ArrayList<Livraison> liste = new ArrayList<>();
        String sql = "SELECT * FROM livraison";
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

    public Livraison findByCommande(int commandeId) throws SQLException {
        String sql = "SELECT * FROM livraison WHERE commande_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, commandeId);
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

    private void setNullableTimestamp(PreparedStatement ps, int index, java.time.LocalDateTime ldt) throws SQLException {
        if (ldt != null) ps.setTimestamp(index, Timestamp.valueOf(ldt));
        else ps.setNull(index, Types.TIMESTAMP);
    }

    private Livraison mapRow(ResultSet rs) throws SQLException {
        Livraison l = new Livraison();
        l.setId(rs.getInt("id"));
        l.setAdresse(rs.getString("adresse"));
        l.setVille(rs.getString("ville"));
        l.setDateLivraison(toLocalDateTime(rs.getTimestamp("date_livraison")));
        l.setStatusLivraison(rs.getString("status_livraison"));
        int cid = rs.getInt("commande_id");
        l.setCommandeId(rs.wasNull() ? null : cid);
        l.setTrackingNumber(rs.getString("tracking_number"));
        l.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        l.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        l.setExpedieeAt(toLocalDateTime(rs.getTimestamp("expediee_at")));
        l.setLivreeAt(toLocalDateTime(rs.getTimestamp("livree_at")));
        l.setAnnuleeAt(toLocalDateTime(rs.getTimestamp("annulee_at")));
        l.setPhoneNumber(rs.getString("phone_number"));
        return l;
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
