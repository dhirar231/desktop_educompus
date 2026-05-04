package com.educompus.service;

/**
 * Service utilitaire pour configurer et tester l'API Gemini.
 */
public class GeminiConfigService {
    
    // Votre clé API Gemini
    public static final String GEMINI_API_KEY = "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo";
    
    /**
     * Configure automatiquement la clé API Gemini comme variable d'environnement.
     * À utiliser uniquement en développement !
     */
    public static void configurerCleAPI() {
        System.setProperty("GEMINI_API_KEY", GEMINI_API_KEY);
        System.out.println("✓ Clé API Gemini configurée automatiquement");
    }
    
    /**
     * Vérifie si la clé API Gemini est configurée.
     */
    public static boolean estConfiguree() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getProperty("GEMINI_API_KEY");
        }
        return key != null && !key.isEmpty();
    }
    
    /**
     * Retourne la clé API configurée.
     */
    public static String obtenirCleAPI() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getProperty("GEMINI_API_KEY");
        }
        if (key == null || key.isEmpty()) {
            return GEMINI_API_KEY; // Fallback sur la clé codée en dur
        }
        return key;
    }
    
    /**
     * Affiche le statut de la configuration.
     */
    public static void afficherStatut() {
        System.out.println("=== Configuration Gemini ===");
        
        String envKey = System.getenv("GEMINI_API_KEY");
        String propKey = System.getProperty("GEMINI_API_KEY");
        
        if (envKey != null && !envKey.isEmpty()) {
            System.out.println("✓ Variable d'environnement GEMINI_API_KEY: " + masquerCle(envKey));
        } else {
            System.out.println("✗ Variable d'environnement GEMINI_API_KEY: non définie");
        }
        
        if (propKey != null && !propKey.isEmpty()) {
            System.out.println("✓ Propriété système GEMINI_API_KEY: " + masquerCle(propKey));
        } else {
            System.out.println("✗ Propriété système GEMINI_API_KEY: non définie");
        }
        
        System.out.println("✓ Clé de fallback disponible: " + masquerCle(GEMINI_API_KEY));
        System.out.println("Clé utilisée: " + masquerCle(obtenirCleAPI()));
    }
    
    /**
     * Masque une clé API pour l'affichage sécurisé.
     */
    private static String masquerCle(String cle) {
        if (cle == null || cle.length() < 8) {
            return "***";
        }
        return cle.substring(0, 8) + "..." + cle.substring(cle.length() - 4);
    }
}