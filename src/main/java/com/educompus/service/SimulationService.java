package com.educompus.service;

/**
 * Service de simulation pour les APIs externes.
 * Permet de tester l'application sans dépendre des services externes.
 */
public final class SimulationService {

    private static boolean modeSimulationActive = true; // Activé par défaut
    
    private SimulationService() {}

    /**
     * Active ou désactive le mode simulation global.
     */
    public static void setModeSimulation(boolean actif) {
        modeSimulationActive = actif;
        System.out.println(actif ? "🎭 Mode simulation ACTIVÉ" : "🎬 Mode réel ACTIVÉ");
    }

    /**
     * Vérifie si le mode simulation est actif.
     */
    public static boolean isModeSimulationActif() {
        return modeSimulationActive;
    }

    /**
     * Détermine si on doit utiliser la simulation pour HeyGen.
     */
    public static boolean utiliserSimulationHeyGen(String apiKey) {
        if (modeSimulationActive) {
            return true;
        }
        
        // Utiliser simulation si pas de vraie clé API
        return apiKey == null || 
               apiKey.isBlank() || 
               apiKey.equals("demo_key_for_testing") ||
               apiKey.equals("VOTRE_CLE_HEYGEN_ICI");
    }

    /**
     * Détermine si on doit utiliser la simulation pour Gemini.
     */
    public static boolean utiliserSimulationGemini(String apiKey) {
        if (modeSimulationActive) {
            return false; // Gemini fonctionne avec la vraie clé
        }
        
        return apiKey == null || 
               apiKey.isBlank() || 
               apiKey.equals("VOTRE_CLE_GEMINI_ICI");
    }

    /**
     * Génère une URL de vidéo simulée (maintenant avec fichiers MP4 locaux).
     */
    public static String genererUrlVideoSimulee(String type, String parametres) {
        // Essayer de créer un fichier MP4 local d'abord
        try {
            String videoId = type + "_" + String.valueOf(parametres.hashCode()).replace("-", "") + "_" + System.currentTimeMillis();
            String script = "Ceci est une vidéo de démonstration générée en mode simulation. " +
                          "Le contenu réel serait basé sur vos paramètres: " + parametres;
            
            String cheminVideo = LocalVideoGeneratorService.genererVideoMP4Local(
                videoId,
                script,
                "Avatar de démonstration",
                "Voix de démonstration",
                calculerDureeEstimee(script)
            );
            
            if (cheminVideo != null) {
                System.out.println("🎬 Fichier MP4 local créé pour simulation: " + cheminVideo);
                return cheminVideo;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Échec création fichier MP4 local, utilisation URL fictive: " + e.getMessage());
        }
        
        // Fallback vers URL fictive si échec
        long timestamp = System.currentTimeMillis();
        String hash = String.valueOf(parametres.hashCode()).replace("-", "");
        
        return String.format("https://demo.educompus.com/videos/%s_%s_%d.mp4", 
                           type, hash, timestamp);
    }

    /**
     * Génère une URL de miniature simulée.
     */
    public static String genererUrlThumbnailSimulee(String videoUrl) {
        return videoUrl.replace(".mp4", "_thumb.jpg");
    }

    /**
     * Simule un délai de traitement réaliste.
     */
    public static void simulerDelaiTraitement(int minMs, int maxMs) {
        try {
            int delai = minMs + (int)(Math.random() * (maxMs - minMs));
            Thread.sleep(delai);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Calcule une durée de vidéo estimée basée sur le script.
     */
    public static int calculerDureeEstimee(String script) {
        if (script == null || script.isBlank()) {
            return 30; // 30 secondes par défaut
        }
        
        // Estimation: ~150 mots par minute, ~5 caractères par mot
        int nbCaracteres = script.length();
        int nbMots = nbCaracteres / 5;
        int dureeSecondes = (nbMots * 60) / 150;
        
        // Limiter entre 30 secondes et 10 minutes
        return Math.max(30, Math.min(600, dureeSecondes));
    }

    /**
     * Génère des métadonnées de vidéo simulées (avec fichiers MP4 locaux).
     */
    public static VideoMetadata genererMetadataSimulees(String script, String avatar, String voix) {
        // Essayer de créer un fichier MP4 local
        String videoId = "meta_" + System.currentTimeMillis();
        String urlVideo = null;
        
        try {
            urlVideo = LocalVideoGeneratorService.genererVideoMP4Local(
                videoId,
                script,
                avatar,
                voix,
                calculerDureeEstimee(script)
            );
        } catch (Exception e) {
            System.err.println("⚠️ Échec création fichier MP4 dans métadonnées: " + e.getMessage());
        }
        
        // Fallback vers URL fictive si échec
        if (urlVideo == null) {
            urlVideo = genererUrlVideoSimulee("heygen", avatar + voix);
        }
        
        return new VideoMetadata(
            urlVideo,
            genererUrlThumbnailSimulee(urlVideo),
            calculerDureeEstimee(script),
            "Vidéo générée en mode simulation avec fichier MP4 local",
            avatar,
            voix,
            "simulation"
        );
    }

    /**
     * Affiche le statut du mode simulation.
     */
    public static String obtenirStatutSimulation() {
        if (modeSimulationActive) {
            return "🎭 Mode simulation ACTIF - Toutes les APIs sont simulées";
        } else {
            return "🎬 Mode réel ACTIF - Utilisation des vraies APIs quand disponibles";
        }
    }

    /**
     * Classe pour les métadonnées de vidéo simulées.
     */
    public static class VideoMetadata {
        private final String urlVideo;
        private final String urlThumbnail;
        private final int dureeSecondes;
        private final String description;
        private final String avatar;
        private final String voix;
        private final String mode;

        public VideoMetadata(String urlVideo, String urlThumbnail, int dureeSecondes, 
                           String description, String avatar, String voix, String mode) {
            this.urlVideo = urlVideo;
            this.urlThumbnail = urlThumbnail;
            this.dureeSecondes = dureeSecondes;
            this.description = description;
            this.avatar = avatar;
            this.voix = voix;
            this.mode = mode;
        }

        public String getUrlVideo() { return urlVideo; }
        public String getUrlThumbnail() { return urlThumbnail; }
        public int getDureeSecondes() { return dureeSecondes; }
        public String getDescription() { return description; }
        public String getAvatar() { return avatar; }
        public String getVoix() { return voix; }
        public String getMode() { return mode; }

        public String getDureeFormatee() {
            int minutes = dureeSecondes / 60;
            int secondes = dureeSecondes % 60;
            return String.format("%d:%02d", minutes, secondes);
        }

        @Override
        public String toString() {
            return String.format("VideoMetadata{url='%s', durée=%s, avatar='%s', mode='%s'}", 
                               urlVideo, getDureeFormatee(), avatar, mode);
        }
    }

    /**
     * Configuration du mode simulation selon l'environnement.
     */
    public static void configurerSelonEnvironnement() {
        // Détecter si on est en développement ou production
        String env = System.getProperty("app.environment", "development");
        
        switch (env.toLowerCase()) {
            case "production" -> {
                setModeSimulation(false);
                System.out.println("🏭 Environnement PRODUCTION - Mode réel activé");
            }
            case "test" -> {
                setModeSimulation(true);
                System.out.println("🧪 Environnement TEST - Mode simulation activé");
            }
            default -> {
                setModeSimulation(true);
                System.out.println("🛠️ Environnement DÉVELOPPEMENT - Mode simulation activé");
            }
        }
    }

    /**
     * Initialise le service de simulation.
     */
    public static void initialiser() {
        System.out.println("🎭 Initialisation du service de simulation...");
        configurerSelonEnvironnement();
        System.out.println("✅ Service de simulation prêt: " + obtenirStatutSimulation());
    }
}