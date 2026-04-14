package com.educompus.repository;

import com.educompus.model.LigneCommande;

import java.sql.*;
import java.util.ArrayList;

public class LigneCommandeRepository {

    public void insert(LigneCommande lc) throws SQLException {
        String sql = "INSERT INTO ligne_commande (commande_id, produit_id, nom_produit, image_produit, prix_unitaire, quantite) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, lc.getCommandeId());
            if (lc.getProduitId() != null) ps.setInt(2, lc.getProduitId());
            else ps.setNull(2, Types.INTEGER);
            ps.setString(3, lc.getNomProduit());
            ps.setString(4, lc.getImageProduit());
            ps.setDouble(5, lc.getPrixUnitaire());
            ps.setInt(6, lc.getQuantite());
            ps.executeUpdate();
        } finally { ps.close(); conn.close(); }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ligne_commande WHERE id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try { ps.setInt(1, id); ps.executeUpdate(); }
        finally { ps.close(); conn.close(); }
    }

    public ArrayList<LigneCommande> findAll() throws SQLException {
        ArrayList<LigneCommande> liste = new ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM ligne_commande");
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    public ArrayList<LigneCommande> findByCommande(int commandeId) throws SQLException {
        ArrayList<LigneCommande> liste = new ArrayList<>();
        String sql = "SELECT * FROM ligne_commande WHERE commande_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ps.setInt(1, commandeId);
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) liste.add(mapRow(rs)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    private LigneCommande mapRow(ResultSet rs) throws SQLException {
        int pid = rs.getInt("produit_id");
        return new LigneCommande(
                rs.getInt("id"), rs.getInt("commande_id"),
                rs.wasNull() ? null : pid,
                rs.getString("nom_produit"), rs.getString("image_produit"),
                rs.getDouble("prix_unitaire"), rs.getInt("quantite"));
    }
}
