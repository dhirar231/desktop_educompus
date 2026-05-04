package com.educompus.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Service de génération de vidéos AI avec avatar parlant.
 * Utilise Google Gemini pour le script et D-ID pour la génération vidéo.
 */
public final class AIVideoGenerationService {

    private static final Properties config = loadConfig();

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    // HeyGen remplace D-ID
    // private static final String DID_URL = "https://api.d-id.com/talks";

    private AIVideoGenerationService() {}

    /**
     * Charge la configuration depuis le fichier ai-config.properties.
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        
        // Initialiser la configuration automatiquement
        ConfigurationService.verifierEtCorrigerConfiguration();
        
        try (var input = AIVideoGenerationService.class.getResourceAsStream("/config/ai-config.properties")) {
            if (input != null) {
                props.load(input);
            } else {
                // Essayer le fichier de backup
                try (var backupInput = AIVideoGenerationService.class.getResourceAsStream("/ai-config.properties")) {
                    if (backupInput != null) {
                        props.load(backupInput);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger ai-config.properties : " + e.getMessage());
            
            // Configuration par défaut en cas d'erreur
            props.setProperty("gemini.api.key", "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo");
            props.setProperty("heygen.api.key", "demo_key_for_testing");
        }
        
        return props;
    }

    /**
     * Génère une vidéo AI complète à partir d'une description.
     *
     * @param description Description du contenu à expliquer
     * @param coursTitle  Titre du cours (pour le contexte)
     * @param niveau      Niveau du cours (1er, 2eme, etc.)
     * @param domaine     Domaine du cours (Informatique, etc.)
     * @return CompletableFuture avec le résultat de la génération
     */
    public static CompletableFuture<VideoGenerationResult> generateVideo(
            String description, String coursTitle, String niveau, String domaine) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Vérifier la configuration
                String geminiKey = config.getProperty("gemini.api.key", "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo");
                String heygenKey = config.getProperty("heygen.api.key", "demo_key_for_testing");

                if (geminiKey.isBlank() || geminiKey.equals("VOTRE_CLE_GEMINI_ICI")) {
                    return VideoGenerationResult.error(
                            "Clé API Gemini non configurée. Veuillez configurer ai-config.properties");
                }

                // Note: HeyGen fonctionne avec une clé de démo, pas d'erreur si pas configurée
                System.out.println("🔑 Utilisation de HeyGen avec clé: " + (heygenKey.equals("demo_key_for_testing") ? "DEMO" : "CONFIGURÉE"));

                // Étape 1 : Générer le script pédagogique avec Gemini
                String[] scriptResult = generateScriptWithGemini(description, coursTitle, niveau, domaine, geminiKey);
                String script = scriptResult[0];
                String scriptError = scriptResult[1];

                if (script == null || script.isBlank()) {
                    return VideoGenerationResult.error(
                            "Erreur génération du script : " + (scriptError != null ? scriptError : "réponse vide"));
                }

                // Étape 2 : Créer la vidéo avec HeyGen (remplace D-ID)
                String[] heygenResult = createVideoWithHeyGen(script, heygenKey, description, coursTitle, niveau, domaine);
                String videoUrl = heygenResult[0];
                String heygenError = heygenResult[1];
                
                if (videoUrl == null || videoUrl.isBlank()) {
                    return VideoGenerationResult.error("Erreur génération de la vidéo HeyGen : "
                            + (heygenError != null ? heygenError : "réponse vide HeyGen"));
                }

                // Si c'est un fichier MP4 local, essayer de l'ouvrir automatiquement
                if (videoUrl.endsWith(".mp4") && !videoUrl.startsWith("http")) {
                    System.out.println("🎬 Tentative d'ouverture automatique de la vidéo MP4...");
                    boolean ouvert = LocalVideoGeneratorService.ouvrirVideoMP4(videoUrl);
                    if (ouvert) {
                        System.out.println("✅ Vidéo ouverte dans le lecteur par défaut");
                    } else {
                        System.out.println("⚠️ Impossible d'ouvrir automatiquement la vidéo");
                    }
                } else if (videoUrl.startsWith("file:///")) {
                    // Fallback pour les anciens aperçus HTML
                    System.out.println("🌐 Tentative d'ouverture de l'aperçu HTML...");
                    boolean ouvert = VideoPreviewService.ouvrirApercuDansNavigateur(videoUrl);
                    if (ouvert) {
                        System.out.println("✅ Aperçu ouvert dans le navigateur");
                    } else {
                        System.out.println("⚠️ Impossible d'ouvrir automatiquement l'aperçu");
                    }
                }

                return VideoGenerationResult.success(videoUrl, script);

            } catch (Exception e) {
                e.printStackTrace();
                return VideoGenerationResult.error("Erreur : " + e.getMessage());
            }
        });
    }

    /**
     * Génère un script pédagogique avec Google Gemini (version robuste).
     * Retourne un tableau [script, erreur].
     */
    private static String[] generateScriptWithGemini(
            String description, String coursTitle, String niveau, String domaine, String apiKey) {
        try {
            // Utiliser le service Gemini robuste
            String script = GeminiService.genererScript(description, coursTitle, niveau, domaine);
            
            if (script != null && !script.isBlank()) {
                return new String[]{script, null};
            } else {
                return new String[]{null, "Réponse vide de Gemini"};
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{null, e.getMessage()};
        }
    }

    /**
     * Parse la réponse de Gemini (méthode legacy - maintenant gérée par GeminiService).
     */
    private static String parseGeminiResponse(String jsonResponse) {
        // Cette méthode est maintenant gérée par GeminiService
        return "Script généré par GeminiService";
    }

    /**
     * Génère une vraie vidéo éducative locale (images + TTS + FFmpeg).
     * Retourne un tableau [videoUrl, erreur].
     */
    private static String[] createVideoWithHeyGen(String script, String apiKey, String description, String coursTitle, String niveau, String domaine) {
        try {
            System.out.println("[RealVideo] Génération vidéo réelle (images + TTS + FFmpeg)...");
            String id = "ai_video_" + System.currentTimeMillis();
            String path = RealVideoGeneratorService.genererVideoReelle(
                id, coursTitle, description, coursTitle, description, niveau, domaine, script);
            if (path != null) {
                System.out.println("[RealVideo] ✅ Vidéo créée: " + path);
                return new String[]{path, null};
            }
            return new String[]{null, "Echec génération vidéo (FFmpeg/TTS indisponible)"};
        } catch (Exception e) {
            System.err.println("[RealVideo] Exception: " + e.getMessage());
            return new String[]{null, "Exception: " + e.getMessage()};
        }
    }

    /**
     * Construit le prompt pédagogique (méthode legacy - utilise maintenant GeminiService).
     */
    private static String buildEducationalPrompt(String description, String coursTitle, String niveau, String domaine) {
        // Cette méthode est maintenant gérée par GeminiService
        return description;
    }

    /**
     * Parse la réponse de HeyGen pour extraire l'URL/ID de la vidéo.
     */
    private static String parseHeyGenResponse(String jsonResponse) {
        try {
            // HeyGen retourne directement l'URL dans la réponse
            String searchKey = "\"video_url\":\"";
            int start = jsonResponse.indexOf(searchKey);
            if (start < 0) {
                // Essayer avec video_id si pas d'URL directe
                searchKey = "\"video_id\":\"";
                start = jsonResponse.indexOf(searchKey);
                if (start < 0) return null;
                
                start += searchKey.length();
                int end = jsonResponse.indexOf("\"", start);
                if (end < 0) return null;
                
                String videoId = jsonResponse.substring(start, end);
                return "https://api.heygen.com/v1/video/" + videoId;
            }

            start += searchKey.length();
            int end = jsonResponse.indexOf("\"", start);
            if (end < 0) return null;

            return jsonResponse.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Vérifie le statut d'une vidéo HeyGen et récupère l'URL finale.
     */
    public static CompletableFuture<String> checkVideoStatus(String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String heygenKey = config.getProperty("heygen.api.key", "demo_key_for_testing");
                
                // Utiliser le service HeyGen pour vérifier le statut
                HeyGenVideoService.ResultatHeyGen resultat = HeyGenVideoService.verifierStatut(videoId, heygenKey);
                
                if (resultat.isSucces()) {
                    return resultat.getUrlVideo();
                } else {
                    System.err.println("Erreur statut HeyGen: " + resultat.getMessageErreur());
                    return null;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Résultat de la génération de vidéo.
     */
    public static class VideoGenerationResult {
        private final boolean success;
        private final String videoUrl;
        private final String script;
        private final String errorMessage;

        private VideoGenerationResult(boolean success, String videoUrl, String script, String errorMessage) {
            this.success = success;
            this.videoUrl = videoUrl;
            this.script = script;
            this.errorMessage = errorMessage;
        }

        public static VideoGenerationResult success(String videoUrl, String script) {
            return new VideoGenerationResult(true, videoUrl, script, null);
        }

        public static VideoGenerationResult error(String errorMessage) {
            return new VideoGenerationResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getVideoUrl() { return videoUrl; }
        public String getScript() { return script; }
        public String getErrorMessage() { return errorMessage; }
    }
}