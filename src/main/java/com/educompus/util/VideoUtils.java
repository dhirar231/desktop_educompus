package com.educompus.util;

import com.educompus.service.VideoPreviewService;
import com.educompus.service.LocalVideoGeneratorService;

/**
 * Utilitaires pour la gestion des vidéos IA.
 */
public final class VideoUtils {

    private VideoUtils() {}

    /**
     * Ouvre une vidéo générée par l'IA.
     * Gère automatiquement les fichiers MP4 locaux, aperçus HTML et URLs externes.
     */
    public static boolean ouvrirVideo(String urlVideo) {
        if (urlVideo == null || urlVideo.isBlank()) {
            System.err.println("❌ URL de vidéo vide");
            return false;
        }

        // Si c'est un fichier MP4 local
        if (urlVideo.endsWith(".mp4") && !urlVideo.startsWith("http")) {
            System.out.println("🎬 Ouverture du fichier MP4 local...");
            return LocalVideoGeneratorService.ouvrirVideoMP4(urlVideo);
        }

        // Si c'est un aperçu local HTML (file:///)
        if (urlVideo.startsWith("file:///")) {
            System.out.println("🌐 Ouverture de l'aperçu HTML local...");
            return VideoPreviewService.ouvrirApercuDansNavigateur(urlVideo);
        }

        // Si c'est une URL externe réelle
        if (urlVideo.startsWith("http://") || urlVideo.startsWith("https://")) {
            // Vérifier si c'est une URL de simulation (demo.educompus.com, example.com)
            if (urlVideo.contains("demo.educompus.com") || urlVideo.contains("example.com")) {
                System.err.println("❌ Cette vidéo est une simulation et n'existe pas réellement");
                System.err.println("💡 Les vidéos en mode simulation sont maintenant des fichiers MP4 locaux");
                return false;
            }

            // URL réelle - essayer d'ouvrir dans le navigateur
            System.out.println("🌐 Ouverture de la vidéo en ligne...");
            return ouvrirUrlDansNavigateur(urlVideo);
        }

        System.err.println("❌ Format d'URL non reconnu: " + urlVideo);
        return false;
    }

    /**
     * Ouvre une URL dans le navigateur par défaut.
     */
    private static boolean ouvrirUrlDansNavigateur(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", url);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", url);
            } else {
                pb = new ProcessBuilder("xdg-open", url);
            }

            pb.start();
            System.out.println("✅ URL ouverte: " + url);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture URL: " + e.getMessage());
            System.err.println("💡 Vous pouvez copier cette URL dans votre navigateur: " + url);
            return false;
        }
    }

    /**
     * Vérifie si une URL de vidéo est valide et accessible.
     */
    public static boolean verifierUrlVideo(String urlVideo) {
        if (urlVideo == null || urlVideo.isBlank()) {
            return false;
        }

        // Fichier MP4 local - vérifier si le fichier existe
        if (urlVideo.endsWith(".mp4") && !urlVideo.startsWith("http")) {
            try {
                return java.nio.file.Files.exists(java.nio.file.Paths.get(urlVideo));
            } catch (Exception e) {
                return false;
            }
        }

        // Aperçu local HTML - vérifier si le fichier existe
        if (urlVideo.startsWith("file:///")) {
            try {
                String cheminFichier = urlVideo.replace("file:///", "").replace("/", "\\");
                return java.nio.file.Files.exists(java.nio.file.Paths.get(cheminFichier));
            } catch (Exception e) {
                return false;
            }
        }

        // URL externe - vérifier le format
        if (urlVideo.startsWith("http://") || urlVideo.startsWith("https://")) {
            // URLs de simulation ne sont pas valides
            if (urlVideo.contains("demo.educompus.com") || urlVideo.contains("example.com")) {
                return false;
            }
            return true; // Supposer que les autres URLs externes sont valides
        }

        return false;
    }

    /**
     * Obtient le type de vidéo (fichier MP4 local, aperçu HTML, vidéo en ligne, simulation).
     */
    public static String obtenirTypeVideo(String urlVideo) {
        if (urlVideo == null || urlVideo.isBlank()) {
            return "invalide";
        }

        if (urlVideo.endsWith(".mp4") && !urlVideo.startsWith("http")) {
            return "fichier MP4 local";
        }

        if (urlVideo.startsWith("file:///")) {
            return "aperçu HTML local";
        }

        if (urlVideo.startsWith("http://") || urlVideo.startsWith("https://")) {
            if (urlVideo.contains("demo.educompus.com") || urlVideo.contains("example.com")) {
                return "simulation (non accessible)";
            }
            return "vidéo en ligne";
        }

        return "format inconnu";
    }

    /**
     * Affiche des informations sur une vidéo.
     */
    public static void afficherInfosVideo(String urlVideo) {
        System.out.println("📹 Informations vidéo:");
        System.out.println("   URL: " + urlVideo);
        System.out.println("   Type: " + obtenirTypeVideo(urlVideo));
        System.out.println("   Accessible: " + (verifierUrlVideo(urlVideo) ? "✅ Oui" : "❌ Non"));
    }

    /**
     * Méthode utilitaire pour tester l'ouverture de vidéos.
     */
    public static void testerOuvertureVideo(String urlVideo) {
        System.out.println("\n🧪 Test d'ouverture de vidéo:");
        afficherInfosVideo(urlVideo);
        
        if (verifierUrlVideo(urlVideo)) {
            System.out.println("🚀 Tentative d'ouverture...");
            boolean succes = ouvrirVideo(urlVideo);
            System.out.println(succes ? "✅ Ouverture réussie" : "❌ Échec ouverture");
        } else {
            System.out.println("⚠️ Vidéo non accessible, ouverture annulée");
        }
    }
}