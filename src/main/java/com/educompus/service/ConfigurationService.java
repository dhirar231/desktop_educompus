package com.educompus.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service de configuration automatique pour éviter les erreurs de clés API.
 * Configure automatiquement les clés par défaut si elles ne sont pas définies.
 */
public final class ConfigurationService {

    private static final String CONFIG_FILE = "src/main/resources/config/ai-config.properties";
    private static final String BACKUP_CONFIG_FILE = "src/main/resources/ai-config.properties";
    
    // Clés par défaut (fonctionnelles)
    private static final String DEFAULT_GEMINI_KEY = "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo";
    private static final String DEFAULT_HEYGEN_KEY = "demo_key_for_testing";
    
    private ConfigurationService() {}

    /**
     * Vérifie et corrige automatiquement la configuration.
     */
    public static void verifierEtCorrigerConfiguration() {
        try {
            System.out.println("🔧 Vérification de la configuration...");
            
            // 1. Vérifier si le fichier de config existe
            Path configPath = Paths.get(CONFIG_FILE);
            Path backupPath = Paths.get(BACKUP_CONFIG_FILE);
            
            if (!Files.exists(configPath) && !Files.exists(backupPath)) {
                System.out.println("📁 Création du fichier de configuration...");
                creerConfigurationParDefaut();
                return;
            }
            
            // 2. Charger la configuration existante
            Properties config = chargerConfiguration();
            
            // 3. Vérifier et corriger les clés
            boolean modifie = false;
            
            // Gemini
            String geminiKey = config.getProperty("gemini.api.key", "");
            if (geminiKey.isBlank() || geminiKey.equals("VOTRE_CLE_GEMINI_ICI") || geminiKey.equals("${GEMINI_API_KEY}")) {
                config.setProperty("gemini.api.key", DEFAULT_GEMINI_KEY);
                modifie = true;
                System.out.println("✅ Clé Gemini configurée automatiquement");
            }
            
            // HeyGen
            String heygenKey = config.getProperty("heygen.api.key", "");
            if (heygenKey.isBlank() || heygenKey.equals("VOTRE_CLE_HEYGEN_ICI") || heygenKey.equals("${HEYGEN_API_KEY}")) {
                config.setProperty("heygen.api.key", DEFAULT_HEYGEN_KEY);
                modifie = true;
                System.out.println("✅ Clé HeyGen configurée en mode démo");
            }
            
            // Supprimer les références D-ID obsolètes
            if (config.containsKey("did.api.key")) {
                config.remove("did.api.key");
                config.remove("did.api.url");
                config.remove("did.default.presenter");
                config.remove("did.presenters.female");
                config.remove("did.presenters.male");
                config.remove("did.presenters.neutral");
                modifie = true;
                System.out.println("🗑️ Configuration D-ID obsolète supprimée");
            }
            
            // 4. Sauvegarder si modifié
            if (modifie) {
                sauvegarderConfiguration(config);
                System.out.println("💾 Configuration mise à jour et sauvegardée");
            } else {
                System.out.println("✅ Configuration déjà correcte");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la vérification de la configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Crée un fichier de configuration par défaut.
     */
    private static void creerConfigurationParDefaut() throws IOException {
        String contenuConfig = """
            # Configuration des APIs d'Intelligence Artificielle
            # =====================================================
            # Fichier généré automatiquement par ConfigurationService
            
            # Google Gemini Configuration
            gemini.api.key=%s
            gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
            gemini.model=gemini-pro
            gemini.max.tokens=2048
            gemini.temperature=0.7
            
            # HeyGen Configuration (pour la génération de vidéos)
            heygen.api.key=%s
            heygen.api.url=https://api.heygen.com/v2/video/generate
            heygen.status.url=https://api.heygen.com/v1/video_status.get
            heygen.default.avatar=Kristin_public_3_20240108
            heygen.default.voice=fr-FR-DeniseNeural
            
            # Google TTS Configuration
            tts.api.key=%s
            tts.api.url=https://texttospeech.googleapis.com/v1/text:synthesize
            tts.default.voice=fr-FR-Standard-A
            tts.default.speed=1.0
            
            # Configuration générale
            video.output.directory=videos/generated/
            audio.output.directory=audio/generated/
            cache.directory=videos/cache/
            metadata.directory=videos/metadata/
            video.max.duration.minutes=30
            video.default.duration.minutes=5
            video.supported.languages=fr,en,es,de,it
            video.supported.qualities=low,medium,high
            video.generation.timeout.minutes=10
            cache.expiry.days=30
            
            # Configuration des timeouts API
            api.timeout=30000
            api.retry.attempts=3
            api.retry.delay=5000
            """.formatted(DEFAULT_GEMINI_KEY, DEFAULT_HEYGEN_KEY, DEFAULT_GEMINI_KEY);
        
        // Créer le répertoire si nécessaire
        Path configPath = Paths.get(CONFIG_FILE);
        Files.createDirectories(configPath.getParent());
        
        // Écrire le fichier
        Files.writeString(configPath, contenuConfig);
        
        System.out.println("✅ Fichier de configuration créé: " + CONFIG_FILE);
    }

    /**
     * Charge la configuration depuis le fichier.
     */
    private static Properties chargerConfiguration() throws IOException {
        Properties config = new Properties();
        
        // Essayer le fichier principal
        Path configPath = Paths.get(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                config.load(input);
                return config;
            }
        }
        
        // Essayer le fichier de backup
        Path backupPath = Paths.get(BACKUP_CONFIG_FILE);
        if (Files.exists(backupPath)) {
            try (InputStream input = Files.newInputStream(backupPath)) {
                config.load(input);
                return config;
            }
        }
        
        // Retourner une configuration vide si aucun fichier trouvé
        return config;
    }

    /**
     * Sauvegarde la configuration dans le fichier.
     */
    private static void sauvegarderConfiguration(Properties config) throws IOException {
        Path configPath = Paths.get(CONFIG_FILE);
        Files.createDirectories(configPath.getParent());
        
        try (OutputStream output = Files.newOutputStream(configPath)) {
            config.store(output, "Configuration mise à jour automatiquement par ConfigurationService");
        }
    }

    /**
     * Obtient une clé de configuration avec valeur par défaut.
     */
    public static String obtenirCle(String nom, String defaut) {
        try {
            Properties config = chargerConfiguration();
            return config.getProperty(nom, defaut);
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la configuration: " + e.getMessage());
            return defaut;
        }
    }

    /**
     * Vérifie si la configuration est valide.
     */
    public static boolean configurationValide() {
        try {
            Properties config = chargerConfiguration();
            
            // Vérifier les clés essentielles
            String geminiKey = config.getProperty("gemini.api.key", "");
            if (geminiKey.isBlank() || geminiKey.equals("VOTRE_CLE_GEMINI_ICI")) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Affiche le statut de la configuration.
     */
    public static void afficherStatutConfiguration() {
        try {
            System.out.println("📋 STATUT DE LA CONFIGURATION");
            System.out.println("==============================");
            
            Properties config = chargerConfiguration();
            
            // Gemini
            String geminiKey = config.getProperty("gemini.api.key", "NON_CONFIGURÉ");
            System.out.println("🤖 Gemini: " + (geminiKey.equals(DEFAULT_GEMINI_KEY) ? "✅ Configuré (défaut)" : 
                              geminiKey.equals("NON_CONFIGURÉ") ? "❌ Non configuré" : "✅ Configuré (personnalisé)"));
            
            // HeyGen
            String heygenKey = config.getProperty("heygen.api.key", "NON_CONFIGURÉ");
            System.out.println("🎬 HeyGen: " + (heygenKey.equals(DEFAULT_HEYGEN_KEY) ? "✅ Mode démo" : 
                              heygenKey.equals("NON_CONFIGURÉ") ? "❌ Non configuré" : "✅ Configuré (personnalisé)"));
            
            // TTS
            String ttsKey = config.getProperty("tts.api.key", "NON_CONFIGURÉ");
            System.out.println("🔊 TTS: " + (ttsKey.equals(DEFAULT_GEMINI_KEY) ? "✅ Configuré (Gemini)" : 
                              ttsKey.equals("NON_CONFIGURÉ") ? "❌ Non configuré" : "✅ Configuré (personnalisé)"));
            
            System.out.println();
            System.out.println("📁 Fichier config: " + (Files.exists(Paths.get(CONFIG_FILE)) ? CONFIG_FILE : BACKUP_CONFIG_FILE));
            System.out.println("🔧 Configuration: " + (configurationValide() ? "✅ Valide" : "❌ Invalide"));
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'affichage du statut: " + e.getMessage());
        }
    }

    /**
     * Initialise automatiquement la configuration au démarrage.
     */
    public static void initialiserConfiguration() {
        System.out.println("🚀 Initialisation de la configuration...");
        verifierEtCorrigerConfiguration();
        afficherStatutConfiguration();
        System.out.println("✅ Configuration prête !");
    }
}