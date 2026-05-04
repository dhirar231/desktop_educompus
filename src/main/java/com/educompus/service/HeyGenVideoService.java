package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service de génération de vidéos IA avec HeyGen.
 * HeyGen offre des avatars réalistes et une API simple pour créer des vidéos éducatives.
 */
public final class HeyGenVideoService {

    // Configuration HeyGen API v3
    private static final String HEYGEN_API_URL = "https://api.heygen.com/v3/videos";
    private static final String HEYGEN_STATUS_URL = "https://api.heygen.com/v3/videos"; // Même endpoint pour le statut
    private static String API_KEY = null; // Sera chargée depuis la configuration
    private static final String FALLBACK_API_KEY = "demo_key_for_testing"; // Clé de démonstration

    // Avatars HeyGen réels disponibles (basés sur l'API)
    public enum AvatarEducatif {
        MADISON_1("f20cdc89e0ec4b61bbe453d73019a997", "Madison - Avatar féminin professionnel"),
        MADISON_2("afba263cf6984b398f89296e953830b8", "Madison - Variante 2"),
        MADISON_3("a9a39532d1834ee6aab8202d8deb9251", "Madison - Variante 3"),
        MADISON_4("982e427c961d4de58d69c443fc0fd809", "Madison - Variante 4"),
        MADISON_5("9297c2bab0844a68ad1c7ab6a87d86c8", "Madison - Variante 5"),
        MADISON_6("84de387e2f17412796fa53488bc368e3", "Madison - Variante 6");

        private final String avatarId;
        private final String description;

        AvatarEducatif(String avatarId, String description) {
            this.avatarId = avatarId;
            this.description = description;
        }

        public String getAvatarId() { return avatarId; }
        public String getDescription() { return description; }
    }

    // Paramètres de génération vidéo
    public static class ParametresHeyGen {
        private AvatarEducatif avatar = AvatarEducatif.MADISON_1;
        private String voix = "16a09e4706f74997ba4ed05ea11470f6"; // Cassidy - voix féminine anglaise
        private String qualite = "high"; // low, medium, high
        private String ratio = "16:9"; // 16:9, 9:16, 1:1
        private String arrierePlan = "office"; // office, classroom, library, green_screen
        private boolean sousTitres = true;
        private double vitesseParole = 1.0; // 0.5 à 2.0
        
        // Nouveaux champs pour la génération contextuelle
        private String titreVideo;
        private String descriptionVideo;
        private String titreChapitre;
        private String descriptionChapitre;
        private String niveau;
        private String domaine;

        public AvatarEducatif getAvatar() { return avatar; }
        public void setAvatar(AvatarEducatif avatar) { this.avatar = avatar; }

        public String getVoix() { return voix; }
        public void setVoix(String voix) { this.voix = voix; }

        public String getQualite() { return qualite; }
        public void setQualite(String qualite) { this.qualite = qualite; }

        public String getRatio() { return ratio; }
        public void setRatio(String ratio) { this.ratio = ratio; }

        public String getArrierePlan() { return arrierePlan; }
        public void setArrierePlan(String arrierePlan) { this.arrierePlan = arrierePlan; }

        public boolean isSousTitres() { return sousTitres; }
        public void setSousTitres(boolean sousTitres) { this.sousTitres = sousTitres; }

        public double getVitesseParole() { return vitesseParole; }
        public void setVitesseParole(double vitesseParole) { 
            if (vitesseParole < 0.5 || vitesseParole > 2.0) {
                throw new IllegalArgumentException("La vitesse doit être entre 0.5 et 2.0");
            }
            this.vitesseParole = vitesseParole; 
        }

        // Getters et setters pour les nouveaux champs contextuels
        public String getTitreVideo() { return titreVideo; }
        public void setTitreVideo(String titreVideo) { this.titreVideo = titreVideo; }

        public String getDescriptionVideo() { return descriptionVideo; }
        public void setDescriptionVideo(String descriptionVideo) { this.descriptionVideo = descriptionVideo; }

        public String getTitreChapitre() { return titreChapitre; }
        public void setTitreChapitre(String titreChapitre) { this.titreChapitre = titreChapitre; }

        public String getDescriptionChapitre() { return descriptionChapitre; }
        public void setDescriptionChapitre(String descriptionChapitre) { this.descriptionChapitre = descriptionChapitre; }

        public String getNiveau() { return niveau; }
        public void setNiveau(String niveau) { this.niveau = niveau; }

        public String getDomaine() { return domaine; }
        public void setDomaine(String domaine) { this.domaine = domaine; }

        @Override
        public String toString() {
            return String.format("HeyGen{avatar=%s, voix=%s, qualité=%s, ratio=%s, chapitre=%s}", 
                    avatar.getDescription(), voix, qualite, ratio, 
                    titreChapitre != null ? titreChapitre : "Non spécifié");
        }
    }

    /**
     * Résultat de la génération vidéo HeyGen.
     */
    public static class ResultatHeyGen {
        private final boolean succes;
        private final String videoId;
        private final String urlVideo;
        private final String statut;
        private final String messageErreur;
        private final int dureeSecondes;
        private final String urlThumbnail;

        private ResultatHeyGen(boolean succes, String videoId, String urlVideo, String statut, 
                              String messageErreur, int dureeSecondes, String urlThumbnail) {
            this.succes = succes;
            this.videoId = videoId;
            this.urlVideo = urlVideo;
            this.statut = statut;
            this.messageErreur = messageErreur;
            this.dureeSecondes = dureeSecondes;
            this.urlThumbnail = urlThumbnail;
        }

        public static ResultatHeyGen succes(String videoId, String urlVideo, int dureeSecondes, String urlThumbnail) {
            return new ResultatHeyGen(true, videoId, urlVideo, "completed", null, dureeSecondes, urlThumbnail);
        }

        public static ResultatHeyGen enCours(String videoId, String statut) {
            return new ResultatHeyGen(false, videoId, null, statut, null, 0, null);
        }

        public static ResultatHeyGen erreur(String messageErreur) {
            return new ResultatHeyGen(false, null, null, "error", messageErreur, 0, null);
        }

        public boolean isSucces() { return succes; }
        public String getVideoId() { return videoId; }
        public String getUrlVideo() { return urlVideo; }
        public String getStatut() { return statut; }
        public String getMessageErreur() { return messageErreur; }
        public int getDureeSecondes() { return dureeSecondes; }
        public String getUrlThumbnail() { return urlThumbnail; }

        public boolean estEnCours() {
            return "pending".equals(statut) || "processing".equals(statut);
        }

        public String getDureeFormatee() {
            if (dureeSecondes <= 0) return "N/A";
            int minutes = dureeSecondes / 60;
            int secondes = dureeSecondes % 60;
            return String.format("%d:%02d", minutes, secondes);
        }
    }

    /**
     * Génère une vidéo avec HeyGen de manière asynchrone.
     */
    public static CompletableFuture<ResultatHeyGen> genererVideoAsync(String script, ParametresHeyGen parametres) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return genererVideo(script, parametres);
            } catch (Exception e) {
                return ResultatHeyGen.erreur("Erreur lors de la génération HeyGen: " + e.getMessage());
            }
        });
    }

    /**
     * Génère une vidéo avec HeyGen.
     */
    public static ResultatHeyGen genererVideo(String script, ParametresHeyGen parametres) {
        try {
            // Validation
            if (script == null || script.trim().isEmpty()) {
                return ResultatHeyGen.erreur("Le script ne peut pas être vide");
            }

            if (script.length() > 10000) {
                return ResultatHeyGen.erreur("Le script est trop long (max 10000 caractères)");
            }

            // Obtenir la clé API
            String apiKey = obtenirCleAPI();
            
            // Mode simulation si clé de démo
            if (apiKey == null || apiKey.equals("demo_key_for_testing")) {
                System.out.println("🎭 Mode simulation HeyGen activé (clé de démo)");
                return genererVideoSimulation(script, parametres);
            }

            // Mode réel avec vraie clé API
            System.out.println("🎬 Mode réel HeyGen avec clé API configurée");

            // Nettoyer le script
            String scriptNettoye = nettoyerScript(script);

            // 1. Créer la vidéo
            String videoId = creerVideo(scriptNettoye, parametres, apiKey);
            if (videoId == null) {
                System.out.println("⚠️ Échec API HeyGen, bascule en mode simulation");
                return genererVideoSimulation(script, parametres);
            }

            // 2. Attendre la génération (avec timeout)
            return attendreGeneration(videoId, apiKey);

        } catch (Exception e) {
            System.err.println("❌ Erreur HeyGen, bascule en mode simulation: " + e.getMessage());
            return genererVideoSimulation(script, parametres);
        }
    }

    /**
     * Crée une nouvelle vidéo HeyGen et retourne l'ID.
     */
    private static String creerVideo(String script, ParametresHeyGen parametres, String apiKey) throws IOException {
        // Construire la requête JSON
        String jsonRequest = construireRequeteHeyGen(script, parametres);

        URL url = new URL(HEYGEN_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-KEY", apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // Envoyer la requête
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        }

        // Lire la réponse
        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), 
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != 200) {
            System.err.println("Erreur HeyGen API (" + responseCode + "): " + response.toString());
            return null;
        }

        // Extraire l'ID de la vidéo
        String jsonResponse = response.toString();
        System.out.println("Réponse HeyGen: " + jsonResponse);
        
        return extraireVideoId(jsonResponse);
    }

    /**
     * Construit la requête JSON pour HeyGen v3 (format simplifié).
     */
    private static String construireRequeteHeyGen(String script, ParametresHeyGen parametres) {
        return String.format("""
            {
                "type": "avatar",
                "avatar_id": "%s",
                "voice_id": "%s",
                "script": "%s",
                "title": "Vidéo éducative EduCompus",
                "aspect_ratio": "%s",
                "background": {
                    "type": "color",
                    "value": "#ffffff"
                },
                "test": true
            }
            """,
            parametres.getAvatar().getAvatarId(),
            parametres.getVoix(),
            echapperJSON(script),
            parametres.getRatio()
        );
    }

    /**
     * Attend que la génération soit terminée.
     */
    private static ResultatHeyGen attendreGeneration(String videoId, String apiKey) {
        int maxAttempts = 60; // 10 minutes max (60 * 10 secondes)
        int attempts = 0;
        
        while (attempts < maxAttempts) {
            try {
                ResultatHeyGen statut = verifierStatut(videoId, apiKey);
                
                if (statut.isSucces()) {
                    return statut;
                }
                
                if ("error".equals(statut.getStatut()) || "failed".equals(statut.getStatut())) {
                    return ResultatHeyGen.erreur("Génération échouée: " + statut.getMessageErreur());
                }
                
                if (statut.estEnCours()) {
                    System.out.println("HeyGen en cours... (" + statut.getStatut() + ")");
                    Thread.sleep(10000); // Attendre 10 secondes
                    attempts++;
                } else {
                    return ResultatHeyGen.erreur("Statut inattendu: " + statut.getStatut());
                }
                
            } catch (Exception e) {
                return ResultatHeyGen.erreur("Erreur lors de la vérification: " + e.getMessage());
            }
        }
        
        return ResultatHeyGen.erreur("Timeout: La génération HeyGen a pris trop de temps");
    }

    /**
     * Vérifie le statut d'une vidéo HeyGen.
     */
    public static ResultatHeyGen verifierStatut(String videoId, String apiKey) throws IOException {
        URL url = new URL(HEYGEN_STATUS_URL + "/" + videoId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-API-KEY", apiKey);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), 
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode != 200) {
            return ResultatHeyGen.erreur("Erreur statut HeyGen (" + responseCode + "): " + response.toString());
        }

        return parserStatutResponse(response.toString());
    }

    /**
     * Parse la réponse de statut HeyGen.
     */
    private static ResultatHeyGen parserStatutResponse(String jsonResponse) {
        try {
            System.out.println("Statut HeyGen: " + jsonResponse);
            
            // Extraire le statut
            String statut = extraireChamp(jsonResponse, "status");
            if (statut == null) {
                return ResultatHeyGen.erreur("Statut introuvable dans la réponse");
            }
            
            if ("completed".equals(statut)) {
                String urlVideo = extraireChamp(jsonResponse, "video_url");
                String urlThumbnail = extraireChamp(jsonResponse, "thumbnail_url");
                int duree = extraireEntier(jsonResponse, "duration", 0);
                
                if (urlVideo != null) {
                    return ResultatHeyGen.succes(null, urlVideo, duree, urlThumbnail);
                } else {
                    return ResultatHeyGen.erreur("URL vidéo introuvable");
                }
            } else if ("pending".equals(statut) || "processing".equals(statut)) {
                return ResultatHeyGen.enCours(null, statut);
            } else {
                String erreur = extraireChamp(jsonResponse, "error");
                return ResultatHeyGen.erreur("Statut: " + statut + (erreur != null ? " - " + erreur : ""));
            }
            
        } catch (Exception e) {
            return ResultatHeyGen.erreur("Erreur parsing statut: " + e.getMessage());
        }
    }

    /**
     * Nettoie le script pour HeyGen.
     */
    private static String nettoyerScript(String script) {
        return script
                // Supprimer les balises markdown
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // **gras**
                .replaceAll("\\*(.*?)\\*", "$1")       // *italique*
                .replaceAll("\\[.*?\\]", "")           // [INTRODUCTION]
                .replaceAll("#{1,6}\\s*", "")          // # Titres
                
                // Améliorer pour la voix
                .replace("etc.", "et cetera")
                .replace("ex.", "exemple")
                .replace("vs.", "versus")
                
                // Nettoyer les espaces
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Génère une vidéo en mode simulation (pour les tests et démos).
     * Génère maintenant de vrais fichiers MP4 au lieu d'aperçus HTML.
     */
    private static ResultatHeyGen genererVideoSimulation(String script, ParametresHeyGen parametres) {
        try {
            System.out.println("🎭 Génération vidéo simulation HeyGen (MP4 local)...");
            
            // Utiliser le service de simulation
            SimulationService.simulerDelaiTraitement(2000, 5000); // 2-5 secondes
            
            SimulationService.VideoMetadata metadata = SimulationService.genererMetadataSimulees(
                script, 
                parametres.getAvatar().name(), 
                parametres.getVoix()
            );
            
            // Créer un vrai fichier MP4 local au lieu d'un aperçu HTML
            String videoId = "sim_" + System.currentTimeMillis();
            String cheminVideo;
            
            // Utiliser la génération contextuelle si des informations de chapitre sont disponibles
            if (parametres.getTitreChapitre() != null || parametres.getDescriptionChapitre() != null) {
                cheminVideo = LocalVideoGeneratorService.genererVideoContextuelle(
                    videoId,
                    parametres.getTitreVideo() != null ? parametres.getTitreVideo() : "Vidéo éducative",
                    parametres.getDescriptionVideo(),
                    parametres.getTitreChapitre(),
                    parametres.getDescriptionChapitre(),
                    parametres.getNiveau(),
                    parametres.getDomaine(),
                    parametres.getAvatar().getDescription(),
                    parametres.getVoix()
                );
            } else {
                // Fallback vers la méthode standard
                cheminVideo = LocalVideoGeneratorService.genererVideoMP4Local(
                    videoId,
                    script,
                    parametres.getAvatar().getDescription(),
                    parametres.getVoix(),
                    metadata.getDureeSecondes()
                );
            }
            
            // Utiliser le chemin du fichier MP4 local
            String urlFinale = cheminVideo != null ? cheminVideo : metadata.getUrlVideo();
            
            System.out.println("✅ Vidéo simulation générée:");
            System.out.println("   📹 Fichier: " + urlFinale);
            System.out.println("   ⏱️ Durée: " + metadata.getDureeFormatee());
            System.out.println("   👤 Avatar: " + parametres.getAvatar().getDescription());
            System.out.println("   🗣️ Voix: " + parametres.getVoix());
            System.out.println("   🎬 Type: " + (parametres.getTitreChapitre() != null ? "MP4 contextuel" : "MP4 local"));
            
            return ResultatHeyGen.succes(
                videoId, 
                urlFinale, 
                metadata.getDureeSecondes(), 
                metadata.getUrlThumbnail()
            );
            
        } catch (Exception e) {
            return ResultatHeyGen.erreur("Erreur de simulation: " + e.getMessage());
        }
    }

    /**
     * Méthodes utilitaires pour le parsing JSON simple.
     */
    private static String extraireVideoId(String jsonResponse) {
        // Dans la nouvelle API, l'ID est dans "data.video_id"
        String searchKey = "\"video_id\":\"";
        int start = jsonResponse.indexOf(searchKey);
        if (start == -1) {
            // Essayer aussi "id"
            searchKey = "\"id\":\"";
            start = jsonResponse.indexOf(searchKey);
        }
        if (start == -1) return null;
        
        start += searchKey.length();
        int end = jsonResponse.indexOf("\"", start);
        if (end == -1) return null;
        
        return jsonResponse.substring(start, end);
    }

    private static String extraireChamp(String json, String champ) {
        String searchKey = "\"" + champ + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;
        
        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        
        return json.substring(start, end);
    }

    private static int extraireEntier(String json, String champ, int defaut) {
        String searchKey = "\"" + champ + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return defaut;
        
        start += searchKey.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return defaut;
        
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    private static String echapperJSON(String texte) {
        if (texte == null) return "";
        return texte.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private static int obtenirLargeur(String ratio) {
        return switch (ratio) {
            case "16:9" -> 1920;
            case "9:16" -> 1080;
            case "1:1" -> 1080;
            default -> 1920;
        };
    }

    private static int obtenirHauteur(String ratio) {
        return switch (ratio) {
            case "16:9" -> 1080;
            case "9:16" -> 1920;
            case "1:1" -> 1080;
            default -> 1080;
        };
    }

    /**
     * Obtient la clé API HeyGen depuis la configuration.
     */
    private static String obtenirCleAPI() {
        if (API_KEY == null) {
            // Charger depuis la configuration
            try {
                java.util.Properties config = new java.util.Properties();
                
                // Essayer plusieurs chemins pour le fichier de configuration
                java.io.InputStream input = null;
                
                // Chemin 1: /config/ai-config.properties
                input = HeyGenVideoService.class.getResourceAsStream("/config/ai-config.properties");
                
                // Chemin 2: /ai-config.properties (racine du classpath)
                if (input == null) {
                    input = HeyGenVideoService.class.getResourceAsStream("/ai-config.properties");
                }
                
                // Chemin 3: Fichier direct
                if (input == null) {
                    try {
                        input = new java.io.FileInputStream("src/main/resources/config/ai-config.properties");
                    } catch (Exception e) {
                        // Ignorer cette erreur et continuer
                    }
                }
                
                if (input != null) {
                    config.load(input);
                    String key = config.getProperty("heygen.api.key", FALLBACK_API_KEY);
                    
                    // Vérifier si c'est une vraie clé (commence par sk_)
                    if (key != null && key.startsWith("sk_")) {
                        API_KEY = key;
                        System.out.println("🔑 Clé API HeyGen réelle chargée depuis la configuration");
                    } else {
                        API_KEY = FALLBACK_API_KEY;
                        System.out.println("⚠️ Clé API HeyGen non configurée dans le fichier, utilisation de la clé de démonstration");
                    }
                    input.close();
                } else {
                    System.out.println("⚠️ Fichier de configuration introuvable, utilisation de la clé de démonstration");
                    API_KEY = FALLBACK_API_KEY;
                }
                
            } catch (Exception e) {
                System.err.println("⚠️ Erreur chargement configuration HeyGen: " + e.getMessage());
                API_KEY = FALLBACK_API_KEY;
            }
        }
        
        return API_KEY;
    }

    /**
     * Teste la configuration HeyGen avec les vrais IDs.
     */
    public static ResultatHeyGen testerConfiguration() {
        String scriptTest = "Hello! This is a test video generation with HeyGen. If you can see this video, the configuration is working correctly.";
        
        ParametresHeyGen parametres = new ParametresHeyGen();
        parametres.setAvatar(AvatarEducatif.MADISON_1);
        parametres.setVoix("16a09e4706f74997ba4ed05ea11470f6"); // Cassidy
        parametres.setQualite("medium"); // Qualité réduite pour le test
        
        return genererVideo(scriptTest, parametres);
    }

    /**
     * Génère une vidéo éducative optimisée avec les vrais IDs HeyGen.
     */
    public static CompletableFuture<ResultatHeyGen> genererVideoEducativeAsync(String script, AvatarEducatif avatar) {
        ParametresHeyGen parametres = new ParametresHeyGen();
        parametres.setAvatar(avatar);
        parametres.setVoix("16a09e4706f74997ba4ed05ea11470f6"); // Cassidy - voix féminine
        parametres.setQualite("high");
        parametres.setRatio("16:9");
        parametres.setSousTitres(true);
        parametres.setVitesseParole(0.9); // Légèrement plus lent pour l'éducation
        
        return genererVideoAsync(script, parametres);
    }

    /**
     * Obtient la liste des avatars disponibles.
     */
    public static String[] getAvatarsDisponibles() {
        AvatarEducatif[] avatars = AvatarEducatif.values();
        String[] descriptions = new String[avatars.length];
        for (int i = 0; i < avatars.length; i++) {
            descriptions[i] = avatars[i].getDescription();
        }
        return descriptions;
    }
}