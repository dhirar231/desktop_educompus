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
     */
    public static String construirePrompt(String nomProduit, String type, String categorie) {
        String base = "professional educational product cover image, ";
        base += nomProduit + ", ";
        base += type + " for " + categorie + ", ";
        base += "clean modern design, flat illustration, blue and white colors, ";
        base += "high quality, no text, no watermark";
        return base;
    }
}
