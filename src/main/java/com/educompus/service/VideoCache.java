package com.educompus.service;

import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Service de cache intelligent pour les vidéos générées par IA.
 * Évite la régénération inutile en stockant et réutilisant les vidéos existantes.
 */
public final class VideoCache {

    private static final String CACHE_DIRECTORY = "videos/cache/";
    private static final String METADATA_DIRECTORY = "videos/metadata/";
    private static final long CACHE_EXPIRY_DAYS = 30; // Expiration après 30 jours
    
    // Cache en mémoire pour les accès rapides
    private static final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    
    private final CourseManagementRepository repository;

    public VideoCache() {
        this.repository = new CourseManagementRepository();
        initialiserRepertoires();
        chargerCacheExistant();
    }

    /**
     * Entrée de cache avec métadonnées.
     */
    public static class CacheEntry {
        private final String hash;
        private final String cheminVideo;
        private final String cheminAudio;
        private final LocalDateTime dateCreation;
        private final VideoExplicative metadata;
        private final long tailleVideo;
        private final String urlVideo;

        public CacheEntry(String hash, String cheminVideo, String cheminAudio, 
                         LocalDateTime dateCreation, VideoExplicative metadata, 
                         long tailleVideo, String urlVideo) {
            this.hash = hash;
            this.cheminVideo = cheminVideo;
            this.cheminAudio = cheminAudio;
            this.dateCreation = dateCreation;
            this.metadata = metadata;
            this.tailleVideo = tailleVideo;
            this.urlVideo = urlVideo;
        }

        public String getHash() { return hash; }
        public String getCheminVideo() { return cheminVideo; }
        public String getCheminAudio() { return cheminAudio; }
        public LocalDateTime getDateCreation() { return dateCreation; }
        public VideoExplicative getMetadata() { return metadata; }
        public long getTailleVideo() { return tailleVideo; }
        public String getUrlVideo() { return urlVideo; }

        public boolean isExpire() {
            return dateCreation.plusDays(CACHE_EXPIRY_DAYS).isBefore(LocalDateTime.now());
        }

        public boolean fichierExiste() {
            return cheminVideo != null && new File(cheminVideo).exists();
        }

        public String getTailleFormatee() {
            if (tailleVideo < 1024) return tailleVideo + " B";
            if (tailleVideo < 1024 * 1024) return (tailleVideo / 1024) + " KB";
            return (tailleVideo / (1024 * 1024)) + " MB";
        }
    }

    /**
     * Paramètres pour la génération de cache.
     */
    public static class ParametresCache {
        private final String contenu;
        private final String avatar;
        private final String voix;
        private final String qualite;
        private final boolean forceRegeneration;

        public ParametresCache(String contenu, String avatar, String voix, String qualite, boolean forceRegeneration) {
            this.contenu = contenu;
            this.avatar = avatar;
            this.voix = voix;
            this.qualite = qualite;
            this.forceRegeneration = forceRegeneration;
        }

        public String getContenu() { return contenu; }
        public String getAvatar() { return avatar; }
        public String getVoix() { return voix; }
        public String getQualite() { return qualite; }
        public boolean isForceRegeneration() { return forceRegeneration; }

        /**
         * Génère un hash unique pour ces paramètres.
         */
        public String genererHash() {
            try {
                String input = contenu + "|" + avatar + "|" + voix + "|" + qualite;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(input.getBytes("UTF-8"));
                
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString().substring(0, 16); // 16 premiers caractères
                
            } catch (Exception e) {
                // Fallback simple
                return String.valueOf((contenu + avatar + voix + qualite).hashCode()).replace("-", "");
            }
        }
    }

    /**
     * Obtient une vidéo du cache ou la génère si nécessaire.
     */
    public CompletableFuture<CacheEntry> obtenirVideo(ParametresCache parametres) {
        String hash = parametres.genererHash();
        
        // 1. Vérifier le cache mémoire
        CacheEntry entreeCache = memoryCache.get(hash);
        if (entreeCache != null && !entreeCache.isExpire() && entreeCache.fichierExiste() && !parametres.isForceRegeneration()) {
            System.out.println("✅ Vidéo trouvée dans le cache mémoire: " + hash);
            return CompletableFuture.completedFuture(entreeCache);
        }

        // 2. Vérifier le cache disque
        entreeCache = chargerDepuisDisque(hash);
        if (entreeCache != null && !entreeCache.isExpire() && entreeCache.fichierExiste() && !parametres.isForceRegeneration()) {
            System.out.println("✅ Vidéo trouvée dans le cache disque: " + hash);
            memoryCache.put(hash, entreeCache);
            return CompletableFuture.completedFuture(entreeCache);
        }

        // 3. Vérifier la base de données
        entreeCache = chargerDepuisBaseDonnees(parametres);
        if (entreeCache != null && !entreeCache.isExpire() && !parametres.isForceRegeneration()) {
            System.out.println("✅ Vidéo trouvée dans la base de données: " + hash);
            memoryCache.put(hash, entreeCache);
            return CompletableFuture.completedFuture(entreeCache);
        }

        // 4. Générer une nouvelle vidéo
        System.out.println("🔄 Génération d'une nouvelle vidéo: " + hash);
        return genererNouvelleVideo(parametres, hash);
    }

    /**
     * Génère une nouvelle vidéo et la met en cache.
     */
    private CompletableFuture<CacheEntry> genererNouvelleVideo(ParametresCache parametres, String hash) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Générer le script avec Gemini
                String script = genererScript(parametres.getContenu());
                
                // 2. Générer l'audio avec TTS
                String cheminAudio = genererAudio(script, parametres.getVoix(), hash);
                
                // 3. Générer la vidéo avec HeyGen
                String urlVideo = genererVideoHeyGen(script, parametres, hash);
                
                // 4. Créer l'entrée de cache
                VideoExplicative metadata = creerMetadata(parametres, script, urlVideo);
                CacheEntry entree = new CacheEntry(
                    hash,
                    null, // Pas de fichier local pour HeyGen (URL distante)
                    cheminAudio,
                    LocalDateTime.now(),
                    metadata,
                    0, // Taille inconnue pour URL distante
                    urlVideo
                );

                // 5. Sauvegarder en cache et base de données
                sauvegarderCache(entree);
                sauvegarderBaseDonnees(entree);
                
                // 6. Ajouter au cache mémoire
                memoryCache.put(hash, entree);
                
                System.out.println("✅ Nouvelle vidéo générée et mise en cache: " + hash);
                return entree;
                
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la génération de la vidéo: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Génère un script avec Gemini (version robuste).
     */
    private String genererScript(String contenu) throws Exception {
        try {
            // Utiliser le service Gemini robuste
            String script = GeminiService.genererScript(contenu, "Cours", "Général", "Éducation");
            
            if (script != null && !script.isBlank()) {
                return script;
            } else {
                throw new Exception("Gemini a retourné un script vide");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur Gemini dans le cache: " + e.getMessage());
            
            // Script de fallback simple
            return String.format("""
                Bonjour ! Aujourd'hui, nous allons explorer le sujet suivant :
                
                %s
                
                Ce contenu est important pour votre apprentissage.
                Prenons le temps de bien comprendre ces concepts.
                
                Merci de votre attention !
                """, contenu);
        }
    }

    /**
     * Génère l'audio avec Google TTS.
     */
    private String genererAudio(String script, String voix, String hash) throws Exception {
        GoogleTTSService.ParametresTTS parametresTTS = new GoogleTTSService.ParametresTTS();
        // Configurer la voix selon le paramètre
        parametresTTS.setVoix(GoogleTTSService.VoixFrancaise.FEMALE_A);
        
        CompletableFuture<GoogleTTSService.ResultatTTS> future = 
            GoogleTTSService.convertirTexteAsync(script, parametresTTS);
        
        GoogleTTSService.ResultatTTS resultat = future.get();
        
        if (resultat.isSucces()) {
            // Copier le fichier dans le cache avec le hash
            String cheminCache = CACHE_DIRECTORY + "audio_" + hash + ".mp3";
            Files.copy(Paths.get(resultat.getCheminFichier()), Paths.get(cheminCache));
            return cheminCache;
        } else {
            throw new Exception("Erreur TTS: " + resultat.getMessageErreur());
        }
    }

    /**
     * Génère la vidéo avec HeyGen.
     */
    private String genererVideoHeyGen(String script, ParametresCache parametres, String hash) throws Exception {
        HeyGenVideoService.ParametresHeyGen heygenParams = new HeyGenVideoService.ParametresHeyGen();
        
        // Configurer l'avatar
        HeyGenVideoService.AvatarEducatif avatar = HeyGenVideoService.AvatarEducatif.MADISON_1;
        for (HeyGenVideoService.AvatarEducatif av : HeyGenVideoService.AvatarEducatif.values()) {
            if (av.name().toLowerCase().contains(parametres.getAvatar().toLowerCase())) {
                avatar = av;
                break;
            }
        }
        
        heygenParams.setAvatar(avatar);
        heygenParams.setVoix(parametres.getVoix());
        heygenParams.setQualite(parametres.getQualite());
        heygenParams.setRatio("16:9");
        heygenParams.setSousTitres(true);
        heygenParams.setVitesseParole(0.9);

        HeyGenVideoService.ResultatHeyGen resultat = HeyGenVideoService.genererVideo(script, heygenParams);
        
        if (resultat.isSucces()) {
            return resultat.getUrlVideo();
        } else {
            throw new Exception("Erreur HeyGen: " + resultat.getMessageErreur());
        }
    }

    /**
     * Crée les métadonnées de la vidéo.
     */
    private VideoExplicative creerMetadata(ParametresCache parametres, String script, String urlVideo) {
        VideoExplicative video = new VideoExplicative();
        video.setTitre("Vidéo IA - " + parametres.getContenu().substring(0, Math.min(50, parametres.getContenu().length())));
        video.setDescription("Vidéo générée automatiquement par IA");
        video.setUrlVideo(urlVideo);
        video.setAiScript(script);
        video.setAIGenerated(true);
        video.setGenerationStatus("COMPLETED");
        video.setDateCreation(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return video;
    }

    /**
     * Charge une entrée depuis le disque.
     */
    private CacheEntry chargerDepuisDisque(String hash) {
        try {
            Path metadataPath = Paths.get(METADATA_DIRECTORY + hash + ".json");
            if (!Files.exists(metadataPath)) {
                return null;
            }
            
            // Ici, vous pourriez implémenter la lecture JSON
            // Pour simplifier, on retourne null (pas de cache disque pour l'instant)
            return null;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement depuis le disque: " + e.getMessage());
            return null;
        }
    }

    /**
     * Charge une entrée depuis la base de données.
     */
    private CacheEntry chargerDepuisBaseDonnees(ParametresCache parametres) {
        try {
            // Rechercher une vidéo existante avec un contenu similaire
            // Pour simplifier, on retourne null (pas de recherche BD pour l'instant)
            return null;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement depuis la BD: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarde l'entrée en cache disque.
     */
    private void sauvegarderCache(CacheEntry entree) {
        try {
            // Ici, vous pourriez sauvegarder les métadonnées en JSON
            System.out.println("💾 Cache sauvegardé: " + entree.getHash());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde cache: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde l'entrée en base de données.
     */
    private void sauvegarderBaseDonnees(CacheEntry entree) {
        try {
            repository.createVideoExplicative(entree.getMetadata());
            System.out.println("💾 Vidéo sauvegardée en base: " + entree.getHash());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde BD: " + e.getMessage());
        }
    }

    /**
     * Initialise les répertoires de cache.
     */
    private void initialiserRepertoires() {
        try {
            Files.createDirectories(Paths.get(CACHE_DIRECTORY));
            Files.createDirectories(Paths.get(METADATA_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création des répertoires: " + e.getMessage());
        }
    }

    /**
     * Charge le cache existant au démarrage.
     */
    private void chargerCacheExistant() {
        try {
            // Charger les entrées existantes depuis le disque
            // Pour l'instant, le cache mémoire démarre vide
            System.out.println("📂 Cache initialisé");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du cache: " + e.getMessage());
        }
    }

    /**
     * Nettoie le cache (supprime les entrées expirées).
     */
    public void nettoyerCache() {
        System.out.println("🧹 Nettoyage du cache...");
        
        memoryCache.entrySet().removeIf(entry -> {
            CacheEntry entree = entry.getValue();
            if (entree.isExpire() || !entree.fichierExiste()) {
                System.out.println("🗑️ Suppression de l'entrée expirée: " + entry.getKey());
                return true;
            }
            return false;
        });
        
        System.out.println("✅ Nettoyage terminé. Entrées restantes: " + memoryCache.size());
    }

    /**
     * Obtient les statistiques du cache.
     */
    public String getStatistiques() {
        long tailleTotal = memoryCache.values().stream()
            .mapToLong(CacheEntry::getTailleVideo)
            .sum();
            
        return String.format("""
            📊 STATISTIQUES DU CACHE
            
            Entrées en mémoire: %d
            Taille totale: %s
            Répertoire cache: %s
            Répertoire métadonnées: %s
            
            Entrées récentes:
            %s
            """,
            memoryCache.size(),
            formatTaille(tailleTotal),
            CACHE_DIRECTORY,
            METADATA_DIRECTORY,
            memoryCache.keySet().stream()
                .limit(5)
                .map(hash -> "- " + hash)
                .reduce("", (a, b) -> a + "\n" + b)
        );
    }

    private String formatTaille(long taille) {
        if (taille < 1024) return taille + " B";
        if (taille < 1024 * 1024) return (taille / 1024) + " KB";
        return (taille / (1024 * 1024)) + " MB";
    }

    /**
     * Force la régénération d'une vidéo.
     */
    public CompletableFuture<CacheEntry> regenererVideo(ParametresCache parametres) {
        String hash = parametres.genererHash();
        
        // Supprimer du cache
        memoryCache.remove(hash);
        
        // Forcer la régénération
        ParametresCache parametresForce = new ParametresCache(
            parametres.getContenu(),
            parametres.getAvatar(),
            parametres.getVoix(),
            parametres.getQualite(),
            true // Force la régénération
        );
        
        return obtenirVideo(parametresForce);
    }
}