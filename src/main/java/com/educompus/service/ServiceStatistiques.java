package com.educompus.service;

import com.educompus.repository.EducompusDB;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ServiceStatistiques {

    // ── KPI globaux ───────────────────────────────────────────────────────────

    public int totalProduits() throws SQLException {
        return queryInt("SELECT COUNT(*) FROM produit");
    }

    public double valeurTotaleStock() throws SQLException {
        return queryDouble("SELECT COALESCE(SUM(prix * stock), 0) FROM produit");
    }

    public int produitsEnRupture() throws SQLException {
        return queryInt("SELECT COUNT(*) FROM produit WHERE stock = 0");
    }

    public double noteMoyenneGlobale() throws SQLException {
        return queryDouble("SELECT COALESCE(AVG(note), 0) FROM avis");
    }

    // ── Stats d'un produit spécifique ────────────────────────────────────────

    public static class ProduitStatDetail {
        public final double noteMoyenne;
        public final int    nbAvis;
        public final int    nbCommandes;   // fois commandé
        public final double caTotal;       // chiffre d'affaires généré

        public ProduitStatDetail(double noteMoyenne, int nbAvis, int nbCommandes, double caTotal) {
            this.noteMoyenne = noteMoyenne;
            this.nbAvis      = nbAvis;
            this.nbCommandes = nbCommandes;
            this.caTotal     = caTotal;
        }
    }

    public ProduitStatDetail statsProduit(int produitId) throws SQLException {
        // Note moyenne + nb avis
        double note = 0; int nbAvis = 0;
        String sqlAvis = "SELECT COALESCE(AVG(note),0), COUNT(*) FROM avis WHERE produit_id=?";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlAvis);
        try {
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();
            try { if (rs.next()) { note = rs.getDouble(1); nbAvis = rs.getInt(2); } }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }

        // Nb commandes + CA
        int nbCmd = 0; double ca = 0;
        String sqlCmd = "SELECT COUNT(*), COALESCE(SUM(prix_unitaire * quantite),0) FROM ligne_commande WHERE produit_id=?";
        conn = EducompusDB.getConnection();
        ps = conn.prepareStatement(sqlCmd);
        try {
            ps.setInt(1, produitId);
            ResultSet rs = ps.executeQuery();
            try { if (rs.next()) { nbCmd = rs.getInt(1); ca = rs.getDouble(2); } }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }

        return new ProduitStatDetail(note, nbAvis, nbCmd, ca);
    }

    // ── Répartition par catégorie ─────────────────────────────────────────────

    /** Retourne { catégorie → nombre de produits } */
    public Map<String, Integer> produitsByCategorie() throws SQLException {
        String sql = "SELECT categorie, COUNT(*) AS nb FROM produit GROUP BY categorie ORDER BY nb DESC";
        return queryStringInt(sql);
    }

    // ── Top 5 produits les mieux notés ────────────────────────────────────────

    public static class ProduitStat {
        public final String nom;
        public final String categorie;
        public final double noteMoyenne;
        public final int    nbAvis;

        public ProduitStat(String nom, String categorie, double noteMoyenne, int nbAvis) {
            this.nom        = nom;
            this.categorie  = categorie;
            this.noteMoyenne = noteMoyenne;
            this.nbAvis     = nbAvis;
        }
    }

    public java.util.List<ProduitStat> top5ProduitsNotes() throws SQLException {
        String sql = """
                SELECT p.nom, p.categorie,
                       AVG(a.note) AS moy,
                       COUNT(a.id) AS nb
                FROM produit p
                JOIN avis a ON a.produit_id = p.id
                GROUP BY p.id, p.nom, p.categorie
                ORDER BY moy DESC, nb DESC
                LIMIT 5
                """;
        java.util.List<ProduitStat> liste = new java.util.ArrayList<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    liste.add(new ProduitStat(
                            rs.getString("nom"),
                            rs.getString("categorie"),
                            rs.getDouble("moy"),
                            rs.getInt("nb")
                    ));
                }
            } finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return liste;
    }

    // ── Répartition des avis par note (1 à 5) ────────────────────────────────

    /** Retourne { "1★" → count, "2★" → count, … "5★" → count } */
    public Map<String, Integer> avisByNote() throws SQLException {
        String sql = "SELECT note, COUNT(*) AS nb FROM avis GROUP BY note ORDER BY note";
        Map<String, Integer> result = new LinkedHashMap<>();
        // Initialiser toutes les notes à 0
        for (int i = 1; i <= 5; i++) result.put(i + "★", 0);

        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    result.put(rs.getInt("note") + "★", rs.getInt("nb"));
                }
            } finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return result;
    }

    // ── CA par catégorie ──────────────────────────────────────────────────────

    /** Retourne { catégorie → chiffre d'affaires } depuis ligne_commande + produit */
    public Map<String, Double> chiffreAffairesByCategorie() throws SQLException {
        String sql = """
                SELECT p.categorie,
                       COALESCE(SUM(lc.prix_unitaire * lc.quantite), 0) AS ca
                FROM produit p
                LEFT JOIN ligne_commande lc ON lc.produit_id = p.id
                GROUP BY p.categorie
                ORDER BY ca DESC
                """;
        Map<String, Double> result = new LinkedHashMap<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) result.put(rs.getString("categorie"), rs.getDouble("ca"));
            } finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int queryInt(String sql) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try { return rs.next() ? rs.getInt(1) : 0; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    private double queryDouble(String sql) throws SQLException {
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try { return rs.next() ? rs.getDouble(1) : 0.0; }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
    }

    private Map<String, Integer> queryStringInt(String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        try {
            ResultSet rs = ps.executeQuery();
            try { while (rs.next()) map.put(rs.getString(1), rs.getInt(2)); }
            finally { rs.close(); }
        } finally { ps.close(); conn.close(); }
        return map;
    }
}
