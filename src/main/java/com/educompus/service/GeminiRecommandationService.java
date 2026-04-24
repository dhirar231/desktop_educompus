package com.educompus.service;

import com.educompus.model.Produit;
import com.educompus.repository.EducompusDB;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class GeminiRecommandationService {
private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-8b:generateContent?key=";
    private final String apiKey;
    
    public GeminiRecommandationService() {
        this.apiKey = chargerCle();
    }

    public List<Produit> recommander(int userId, List<Produit> tous) throws Exception {
        ProfilClient p = construireProfil(userId);
        if (p.achats.isEmpty() && p.avis.isEmpty()) {
            return tous.stream().filter(x -> x.getStock() > 0).limit(6).collect(Collectors.toList());
        }
        
        List<Integer> ids = parserIds(appellerGemini(construirePrompt(p, tous)));
        Map<Integer, Produit> idx = tous.stream().collect(Collectors.toMap(Produit::getId, x -> x));
        List<Produit> res = ids.stream()
            .map(idx::get)
            .filter(Objects::nonNull)
            .filter(x -> x.getStock() > 0)
            .limit(6)
            .collect(Collectors.toList());
            
        return res.size() < 3 ? tous.stream().filter(x -> x.getStock() > 0).limit(6).collect(Collectors.toList()) : res;
    }

    private static class ProfilClient {
        List<String> achats = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> avis = new ArrayList<>();
        double noteMoyenne = 0;
    }

    private ProfilClient construireProfil(int userId) throws SQLException {
        ProfilClient p = new ProfilClient();
        Connection conn = EducompusDB.getConnection();
        
        // Récupérer achats et catégories
        PreparedStatement ps = conn.prepareStatement(
            "SELECT DISTINCT lc.nom_produit, pr.categorie FROM ligne_commande lc " +
            "JOIN commande c ON c.id=lc.commande_id " +
            "LEFT JOIN produit pr ON pr.id=lc.produit_id " +
            "WHERE c.user_id=? LIMIT 20"
        );
        
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    p.achats.add(rs.getString("nom_produit"));
                    String cat = rs.getString("categorie");
                    if (cat != null && !p.categories.contains(cat)) {
                        p.categories.add(cat);
                    }
                }
            } finally {
                rs.close();
            }
        } finally {
            ps.close();
            conn.close();
        }
        
        // Récupérer avis
        conn = EducompusDB.getConnection();
        ps = conn.prepareStatement(
            "SELECT commentaire, note FROM avis WHERE user_id=? ORDER BY created_at DESC LIMIT 10"
        );
        
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            double total = 0;
            int count = 0;
            try {
                while (rs.next()) {
                    String c = rs.getString("commentaire");
                    if (c != null && !c.isBlank()) {
                        p.avis.add(c);
                    }
                    total += rs.getInt("note");
                    count++;
                }
            } finally {
                rs.close();
            }
            if (count > 0) {
                p.noteMoyenne = total / count;
            }
        } finally {
            ps.close();
            conn.close();
        }
        
        return p;
    }

    private String construirePrompt(ProfilClient p, List<Produit> cat) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un système de recommandation. Retourne 6 IDs de produits pertinents séparés par des virgules.\n");
        sb.append("Réponds UNIQUEMENT avec les IDs numériques.\n\n");
        sb.append("PROFIL:\n");
        sb.append("- Achats: ").append(String.join(", ", p.achats)).append("\n");
        sb.append("- Catégories: ").append(String.join(", ", p.categories)).append("\n");
        
        sb.append("\nCATALOGUE (id|nom|categorie|prix|stock):\n");
        for (Produit x : cat) {
            if (x.getStock() > 0 && !p.achats.contains(x.getNom())) {
                sb.append(x.getId()).append("|")
                  .append(x.getNom()).append("|")
                  .append(x.getCategorie()).append("|")
                  .append(x.getPrix()).append("|")
                  .append(x.getStock()).append("\n");
            }
        }
        
        sb.append("\nRéponds avec 6 IDs (ex: 3,7,12,5,9,1)");
        return sb.toString();
    }

    private String appellerGemini(String prompt) throws Exception {
        String safe = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + safe + "\"}]}]," +
                     "\"generationConfig\":{\"temperature\":0.3,\"maxOutputTokens\":100}}";
        
        HttpURLConnection conn = (HttpURLConnection) new URL(GEMINI_URL + apiKey).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Content-Type", "application/json");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String rep = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        
        if (code != 200) {
            throw new IOException("Gemini " + code + ": " + rep);
        }
        
        int i = rep.indexOf("\"text\":");
        if (i == -1) {
            throw new IOException("Réponse inattendue");
        }
        
        int d = rep.indexOf('"', i + 7) + 1;
        int f = rep.indexOf('"', d);
        return rep.substring(d, f).trim();
    }

    private List<Integer> parserIds(String t) {
        List<Integer> ids = new ArrayList<>();
        for (String s : t.split("[,\\s]+")) {
            try {
                ids.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    private static String chargerCle() {
        Properties props = new Properties();
        try (InputStream is = GeminiRecommandationService.class.getResourceAsStream("/gemini/gemini.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ignored) {
        }
        
        String v = props.getProperty("GEMINI_API_KEY");
        if (v == null || v.isBlank()) {
            v = System.getenv("GEMINI_API_KEY");
        }
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Clé Gemini manquante");
        }
        return v.trim();
    }
}