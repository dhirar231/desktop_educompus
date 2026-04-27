package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Génération d'images via Pollinations AI (gratuit, sans clé API).
 * Utilise https://image.pollinations.ai/prompt/{prompt}
 * qui retourne directement une image PNG.
 */
public class PollinationsImageService {

    private static final String BASE_URL = "https://image.pollinations.ai/prompt/";

    /**
     * Génère une image à partir d'un prompt et la sauvegarde localement.
     * @param prompt  description de l'image (ex: "educational book mathematics cover")
     * @param width   largeur en pixels (ex: 512)
     * @param height  hauteur en pixels (ex: 512)
     * @return fichier PNG téléchargé dans le dossier temporaire
     */
    public File genererImage(String prompt, int width, int height) throws Exception {
        // Construire l'URL avec paramètres
        String promptEncode = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        String urlStr = BASE_URL + promptEncode
                + "?width=" + width
                + "&height=" + height
                + "&nologo=true"
                + "&model=flux";

        System.out.println("[Pollinations] URL : " + urlStr);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000); // génération peut prendre du temps
        conn.setRequestProperty("User-Agent", "EduCampus/1.0");
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Pollinations erreur HTTP " + code);
        }

        // Sauvegarder l'image dans un fichier temporaire
        File dest = File.createTempFile("produit_ia_", ".png");
        dest.deleteOnExit();

        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
        }

        System.out.println("[Pollinations] Image sauvegardée : " + dest.getAbsolutePath()
                + " (" + dest.length() + " bytes)");
        return dest;
    }

    /**
     * Construit un prompt optimisé pour un produit éducatif.
     * Inclut la description pour que l'image soit cohérente avec le contenu.
     */
    public static String construirePrompt(String nomProduit, String type,
                                           String categorie, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("professional educational product cover image, ");
        sb.append(nomProduit).append(", ");
        sb.append(type).append(" about ").append(categorie).append(", ");

        // Extraire les mots-clés de la description (max 20 mots)
        if (description != null && !description.isBlank()) {
            String[] mots = description.trim().split("\\s+");
            int limite = Math.min(mots.length, 20);
            for (int i = 0; i < limite; i++) {
                String mot = mots[i].replaceAll("[^a-zA-ZÀ-ÿ]", "");
                if (mot.length() > 3) sb.append(mot).append(" ");
            }
        }

        sb.append(", clean modern flat illustration, ");
        sb.append("blue purple gradient, educational icons, ");
        sb.append("high quality, no text, no watermark, 4k");
        return sb.toString();
    }
}
