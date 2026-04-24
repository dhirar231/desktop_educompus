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

/**
 * Recommandation de produits via Groq API (llama-3.3-70b-versatile).
 * Groq est gratuit avec 14 400 requêtes/jour sur le free tier.
 */
public class GroqRecommandationService {

    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final String apiKey;

    public GroqRecommandationService() {
        this.apiKey = chargerCle();
    }

    // ── API publique ──────────────────────────────────────────────────────────

    public List<Produit> recommander(int userId, List<Produit> tousProduits) throws Exception {
        ProfilClient profil = construireProfil(userId);

        // Pas assez de données → retourner les mieux notés par défaut
        if (profil.achats.isEmpty() && profil.avis.isEmpty()) {
            return tousProduits.stream()
                    .filter(p -> p.getStock() > 0)
                    .limit(6)
                    .collect(Collectors.toList());
        }

        String prompt   = construirePrompt(profil, tousProduits);
        String reponse  = appellerGroq(prompt);
        List<Integer> ids = parserIds(reponse);

        Map<Integer, Produit> index = tousProduits.stream()
                .collect(Collectors.toMap(Produit::getId, p -> p));

        List<Produit> recommandes = ids.stream()
                .map(index::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStock() > 0)
                .limit(6)
                .collect(Collectors.toList());

        return recommandes.size() < 3
                ? tousProduits.stream().filter(p -> p.getStock() > 0).limit(6).collect(Collectors.toList())
                : recommandes;
    }

    // ── Chatbot assistant de vente ────────────────────────────────────────────

    /**
     * Répond à une question client sur un produit avec une offre proactive.
     * @param nomProduit   nom du produit consulté
     * @param nomClient    prénom du client connecté
     * @param nbConsultations nombre de fois que le client a consulté ce produit
     * @param question     question posée par le client
     */
    public String repondreAssistant(String nomProduit, String nomClient,
                                     int nbConsultations, String question) throws Exception {
        String prompt = "Agis en tant qu'assistant de vente intelligent pour la Marketplace éducative EduCampus.\n\n" +
                "Contexte : L'utilisateur(trice) nommé(e) '" + nomClient + "' a consulté la fiche du produit '" +
                nomProduit + "' " + nbConsultations + " fois sans l'acheter. " +
                "Il/Elle vient de poser cette question : \"" + question + "\"\n\n" +
                "Ta mission :\n" +
                "1. Réponds brièvement à sa curiosité de manière chaleureuse et encourageante.\n" +
                "2. Si nbConsultations >= 2, déclenche une offre proactive : propose le code promo STUDENT10 (-10%) valable 1 heure.\n" +
                "3. Utilise un ton amical, jeune et motivant (style Smart Campus).\n" +
                "4. Sois court et percutant — max 4 lignes, idéal pour une petite fenêtre de chat JavaFX.\n" +
                "5. L'offre ne doit pas paraître forcée, mais comme un coup de pouce naturel.\n\n" +
                "Réponds directement sans introduction ni explication.";

        return appellerGroq(prompt).trim();
    }

    /**
     * Génère une description marketing professionnelle pour un produit éducatif.
     * @param nom       nom du produit
     * @param type      type (Livre, Cours en ligne, etc.)
     * @param categorie catégorie (Mathématiques, Informatique, etc.)
     * @param motsCles  mots-clés supplémentaires saisis par l'admin (optionnel)
     * @return description générée (2-3 phrases, max 200 caractères)
     */
    public String genererDescription(String nom, String type, String categorie, String motsCles) throws Exception {
        String prompt = "Tu es un expert en marketing éducatif. " +
                "Génère une description professionnelle et captivante pour un produit éducatif. " +
                "La description doit faire 3 à 4 phrases maximum, être en français, " +
                "mettre en valeur les bénéfices pour l'apprenant, et ne pas dépasser 200 caractères.\n\n" +
                "Produit : " + nom + "\n" +
                "Type : " + type + "\n" +
                "Catégorie : " + categorie + "\n" +
                (motsCles != null && !motsCles.isBlank() ? "Mots-clés : " + motsCles + "\n" : "") +
                "\nRéponds UNIQUEMENT avec la description, sans guillemets ni introduction.";

        return appellerGroq(prompt).trim();
    }

    private static class ProfilClient {
        List<String> achats     = new ArrayList<>();
        List<String> categories = new ArrayList<>();
        List<String> avis       = new ArrayList<>();
        double noteMoyenne      = 0;
    }

    private ProfilClient construireProfil(int userId) throws SQLException {
        ProfilClient profil = new ProfilClient();

        String sqlAchats = "SELECT DISTINCT lc.nom_produit, pr.categorie " +
                "FROM ligne_commande lc JOIN commande c ON c.id=lc.commande_id " +
                "LEFT JOIN produit pr ON pr.id=lc.produit_id WHERE c.user_id=? LIMIT 20";
        Connection conn = EducompusDB.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlAchats);
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    profil.achats.add(rs.getString("nom_produit"));
                    String cat = rs.getString("categorie");
                    if (cat != null && !profil.categories.contains(cat)) profil.categories.add(cat);
                }
            } finally { rs.close(); }
        } finally { ps.close(); conn.close(); }

        String sqlAvis = "SELECT commentaire, note FROM avis WHERE user_id=? " +
                "ORDER BY created_at DESC LIMIT 10";
        conn = EducompusDB.getConnection();
        ps = conn.prepareStatement(sqlAvis);
        try {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            double total = 0; int nb = 0;
            try {
                while (rs.next()) {
                    String c = rs.getString("commentaire");
                    if (c != null && !c.isBlank()) profil.avis.add(c);
                    total += rs.getInt("note"); nb++;
                }
            } finally { rs.close(); }
            if (nb > 0) profil.noteMoyenne = total / nb;
        } finally { ps.close(); conn.close(); }

        return profil;
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private String construirePrompt(ProfilClient profil, List<Produit> catalogue) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un système de recommandation de produits éducatifs. ");
        sb.append("Analyse le profil client et retourne UNIQUEMENT les IDs des 6 produits ");
        sb.append("les plus pertinents séparés par des virgules. Aucun autre texte.\n\n");
        sb.append("PROFIL CLIENT:\n");
        sb.append("- Achats: ").append(String.join(", ", profil.achats)).append("\n");
        sb.append("- Catégories préférées: ").append(String.join(", ", profil.categories)).append("\n");
        if (!profil.avis.isEmpty())
            sb.append("- Avis: ").append(String.join(" | ",
                    profil.avis.subList(0, Math.min(3, profil.avis.size())))).append("\n");
        if (profil.noteMoyenne > 0)
            sb.append("- Note moyenne donnée: ").append(String.format("%.1f/5", profil.noteMoyenne)).append("\n");

        sb.append("\nCATALOGUE DISPONIBLE (id|nom|categorie|prix|stock):\n");
        for (Produit p : catalogue) {
            if (p.getStock() > 0 && !profil.achats.contains(p.getNom())) {
                sb.append(p.getId()).append("|").append(p.getNom()).append("|")
                  .append(p.getCategorie()).append("|")
                  .append(String.format("%.2f", p.getPrix())).append("|")
                  .append(p.getStock()).append("\n");
            }
        }
        sb.append("\nRéponds avec exactement 6 IDs séparés par des virgules (ex: 3,7,12,5,9,1)");
        return sb.toString();
    }

    // ── Appel API Groq (compatible OpenAI) ───────────────────────────────────

    private String appellerGroq(String prompt) throws Exception {
        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonStr(prompt) + "}],"
                + "\"temperature\":0.3,"
                + "\"max_tokens\":50"
                + "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(GROQ_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(20_000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code < 400 ? conn.getInputStream() : conn.getErrorStream();
        String rep = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        if (code != 200) throw new IOException("Groq " + code + ": " + rep);

        // Parser la réponse OpenAI : {"choices":[{"message":{"content":"3,7,12,5,9,1"}}]}
        int idx = rep.indexOf("\"content\":");
        if (idx == -1) throw new IOException("Réponse Groq inattendue: " + rep);
        int d = rep.indexOf('"', idx + 10) + 1;
        int f = rep.indexOf('"', d);
        return rep.substring(d, f).trim();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<Integer> parserIds(String texte) {
        List<Integer> ids = new ArrayList<>();
        for (String s : texte.split("[,\\s]+")) {
            try { ids.add(Integer.parseInt(s.trim())); }
            catch (NumberFormatException ignored) {}
        }
        return ids;
    }

    private String jsonStr(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private static String chargerCle() {
        Properties props = new Properties();
        try (InputStream is = GroqRecommandationService.class
                .getResourceAsStream("/groq/groq.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        String v = props.getProperty("GROQ_API_KEY");
        if (v == null || v.isBlank() || v.startsWith("REMPLACE")) v = System.getenv("GROQ_API_KEY");
        if (v == null || v.isBlank())
            throw new IllegalStateException("Clé Groq manquante dans groq.properties");
        return v.trim();
    }
}
