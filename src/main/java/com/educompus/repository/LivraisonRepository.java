package com.educompus.repository;

import com.educompus.model.Livraison;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class LivraisonRepository {

    public void insert(Livraison l) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        try { insert(conn, l); }
        finally { conn.close(); }
    }

    /** Version transactionnelle */
    public void insert(Connection conn, Livraison l) throws SQLException {
        String sql = "INSERT INTO livraison (adresse, ville, date_livraison, status_livraison, commande_id, tracking_number, created_at, updated_at, phone_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, l.getAdresse());
            ps.setString(2, l.getVille());
            setTs(ps, 3, l.getDateLivraison());
            ps.setString(4, l.getStatusLivraison());
            if (l.getCommandeId() != null) ps.setInt(5, l.getCommandeId());
            else ps.setNull(5, Types.INTEGER);
            ps.setString(6, l.getTrackingNumber());
            ps.setTimestamp(7, Timestamp.valueOf(l.getCreatedAt()));
            ps.setTimestamp(8, Timestamp.valueOf(l.getUpdatedAt()));
            ps.setString(9, l.getPhoneNumber());
            ps.executeUpdate();
        } finally { ps.close(); }
    }

    public void update(Livraison l) throws SQLException {
        String sql = "UPDATE livraison SET adresse=?, ville=?, date_livraison=?, status_livraison=?, tracking_number=?, updated_at=?, expediee_at=?, livree_at=?, annulee_at=?, phone_number=? WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setString(1, l.getAdresse());
            ps.setString(2, l.getVille());
            setTs(ps, 3, l.getDateLivraison());
            ps.setString(4, l.getStatusLivraison());
            ps.setString(5, l.getTrackingNumber());
            ps.setTimestamp(6, Timestamp.valueOf(l.getUpdatedAt()));
            setTs(ps, 7, l.getExpedieeAt());
            setTs(ps, 8, l.getLivreeAt());
            setTs(ps, 9, l.getAnnuleeAt());
            ps.setString(10, l.getPhoneNumber());
            ps.setInt(11, l.getId());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM livraison WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public ArrayList<Livraison> findAll() throws SQLException {
        ArrayList<Livraison> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM livraison");
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public Livraison findByCommande(int commandeId) throws SQLException {
        String sql = "SELECT * FROM livraison WHERE commande_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, commandeId);
            ResultSet rs = ps.executeQuery();
            try { return rs.next() ? mapRow(rs) : null; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    private void setTs(PreparedStatement ps, int i, LocalDateTime ldt) throws SQLException {
        if (ldt != null) ps.setTimestamp(i, Timestamp.valueOf(ldt));
        else ps.setNull(i, Types.TIMESTAMP);
    }

    private Livraison mapRow(ResultSet rs) throws SQLException {
        Livraison l = new Livraison();
        l.setId(rs.getInt("id"));
        l.setAdresse(rs.getString("adresse"));
        l.setVille(rs.getString("ville"));
        l.setDateLivraison(toL(rs.getTimestamp("date_livraison")));
        l.setStatusLivraison(rs.getString("status_livraison"));
        int cid = rs.getInt("commande_id");
        l.setCommandeId(rs.wasNull() ? null : cid);
        l.setTrackingNumber(rs.getString("tracking_number"));
        l.setCreatedAt(toL(rs.getTimestamp("created_at")));
        l.setUpdatedAt(toL(rs.getTimestamp("updated_at")));
        l.setExpedieeAt(toL(rs.getTimestamp("expediee_at")));
        l.setLivreeAt(toL(rs.getTimestamp("livree_at")));
        l.setAnnuleeAt(toL(rs.getTimestamp("annulee_at")));
        l.setPhoneNumber(rs.getString("phone_number"));
        return l;
    }

    private LocalDateTime toL(Timestamp ts) { return ts != null ? ts.toLocalDateTime() : null; }
}
