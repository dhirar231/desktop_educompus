package com.educompus.service;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Copie les images générées par JavaFX dans le dossier public/uploads/
 * du projet Symfony, pour qu'elles soient visibles dans la marketplace web.
 *
 * La colonne `image` en base doit contenir uniquement le nom du fichier
 * (ex: produit_ia_1234567890.png), pas un chemin complet.
 * Symfony affiche : {{ asset('uploads/' ~ produit.image) }}
 */
public class SymfonyUploadsService {

    private static final String CONFIG_FILE = "/configMarketplace.properties";
    private static final String KEY = "SYMFONY_UPLOADS_PATH";

    private final String uploadsPath;

    public SymfonyUploadsService() {
        this.uploadsPath = chargerChemin();
    }

    /**
     * Copie le fichier image vers public/uploads/ de Symfony.
     * @param source fichier image source (généré par PollinationsImageService ou choisi par l'utilisateur)
     * @return le nom du fichier (à stocker dans la colonne image de la base)
     * @throws IOException si la copie échoue ou si le dossier Symfony n'est pas configuré
     */
    public String copierVersSymfony(File source) throws IOException {
        if (uploadsPath == null || uploadsPath.isBlank()) {
            throw new IOException(
                "SYMFONY_UPLOADS_PATH non configuré dans configMarketplace.properties.\n" +
                "Ajoutez : SYMFONY_UPLOADS_PATH=C:/wamp64/www/votre-projet/public/uploads"
            );
        }

        Path dest = Paths.get(uploadsPath);
        if (!Files.exists(dest)) {
            throw new IOException(
                "Dossier Symfony introuvable : " + uploadsPath + "\n" +
                "Vérifiez SYMFONY_UPLOADS_PATH dans configMarketplace.properties."
            );
        }

        String fileName = source.getName();
        Path cible = dest.resolve(fileName);
        Files.copy(source.toPath(), cible, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("[SymfonyUploads] Image copiée vers : " + cible.toAbsolutePath());
        return fileName;
    }

    /**
     * Vérifie si le dossier Symfony uploads est accessible.
     */
    public boolean estDisponible() {
        if (uploadsPath == null || uploadsPath.isBlank()) return false;
        return Files.exists(Paths.get(uploadsPath)) && Files.isWritable(Paths.get(uploadsPath));
    }

    public String getUploadsPath() { return uploadsPath; }

    private String chargerChemin() {
        try (InputStream is = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) return null;
            Properties props = new Properties();
            props.load(is);
            return props.getProperty(KEY, "").trim();
        } catch (IOException e) {
            System.err.println("[SymfonyUploads] Impossible de lire la config : " + e.getMessage());
            return null;
        }
    }
}
