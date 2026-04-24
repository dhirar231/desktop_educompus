package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Properties;

/**
 * Upload d'images vers Cloudinary via l'API REST (sans SDK).
 * Utilise une signature HMAC-SHA1 pour l'authentification.
 */
public class CloudinaryService {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryService() {
        Properties props = chargerProperties();
        this.cloudName = lire(props, "CLOUDINARY_CLOUD_NAME");
        this.apiKey    = lire(props, "CLOUDINARY_API_KEY");
        this.apiSecret = lire(props, "CLOUDINARY_API_SECRET");
    }

    /**
     * Upload un fichier image vers Cloudinary.
     * @param fichier  fichier PNG/JPG local
     * @param dossier  dossier Cloudinary (ex: "factures")
     * @return URL publique sécurisée (https://res.cloudinary.com/...)
     */
    public String uploader(File fichier, String dossier) throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String publicId  = dossier + "/" + fichier.getName().replaceAll("\\.[^.]+$", "");

        // Signature Cloudinary : paramètres triés alphabétiquement + secret
        // Format : "folder=...&public_id=...&timestamp=...{secret}"
        String toSign = "folder=" + dossier
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp
                + apiSecret;
        String signature = sha1Hex(toSign);

        // Construire la requête multipart
        String boundary = "----EduCampusBoundary" + System.currentTimeMillis();
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream out = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true)) {

            // Champs texte
            ajouterChamp(writer, out, boundary, "api_key",   apiKey);
            ajouterChamp(writer, out, boundary, "timestamp", timestamp);
            ajouterChamp(writer, out, boundary, "signature", signature);
            ajouterChamp(writer, out, boundary, "folder",    dossier);
            ajouterChamp(writer, out, boundary, "public_id", publicId);

            // Fichier image
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                  .append(fichier.getName()).append("\"").append("\r\n");
            writer.append("Content-Type: image/png").append("\r\n\r\n");
            writer.flush();
            out.write(Files.readAllBytes(fichier.toPath()));
            out.flush();
            writer.append("\r\n");

            // Fin
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.flush();
        }

        int code = conn.getResponseCode();
        String reponse = lireReponse(code < 400 ? conn.getInputStream() : conn.getErrorStream());
        System.out.println("[Cloudinary] HTTP " + code + " → " + reponse);

        if (code != 200) {
            throw new IOException("Cloudinary erreur " + code + " : " + reponse);
        }

        // Extraire secure_url du JSON (sans librairie JSON)
        return extraireSecureUrl(reponse);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void ajouterChamp(PrintWriter writer, OutputStream out,
                               String boundary, String nom, String valeur) {
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"").append(nom).append("\"").append("\r\n\r\n");
        writer.append(valeur).append("\r\n");
        writer.flush();
    }

    private String lireReponse(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String extraireSecureUrl(String json) {
        // Cherche "secure_url":"https://..."
        int idx = json.indexOf("\"secure_url\"");
        if (idx == -1) throw new IllegalStateException("secure_url absent de la réponse : " + json);
        int debut = json.indexOf('"', idx + 13) + 1;
        int fin   = json.indexOf('"', debut);
        return json.substring(debut, fin);
    }

    private static String sha1Hex(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static Properties chargerProperties() {
        Properties props = new Properties();
        try (InputStream is = CloudinaryService.class.getResourceAsStream("/cloudinary/cloudinary.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        return props;
    }

    private static String lire(Properties props, String key) {
        String val = props.getProperty(key);
        if (val == null || val.isBlank() || val.startsWith("REMPLACE")) val = System.getenv(key);
        if (val == null || val.isBlank())
            throw new IllegalStateException("Clé Cloudinary manquante : " + key);
        return val.trim();
    }
}
