package com.educompus.examples;

import com.educompus.service.GeminiConfigService;

/**
 * Test simple de la configuration Gemini.
 */
public class TestGeminiSimple {
    
    public static void main(String[] args) {
        System.out.println("=== Test Simple Gemini ===");
        
        // Configuration automatique
        GeminiConfigService.configurerCleAPI();
        
        // Affichage du statut
        GeminiConfigService.afficherStatut();
        
        // Test de base
        if (GeminiConfigService.estConfiguree()) {
            System.out.println("\n✓ Configuration Gemini OK !");
            System.out.println("✓ Prêt pour la génération de vidéos IA");
            
            // Afficher la clé utilisée (masquée)
            String cle = GeminiConfigService.obtenirCleAPI();
            System.out.println("✓ Clé API: " + masquerCle(cle));
            
        } else {
            System.out.println("\n✗ Problème de configuration Gemini");
        }
        
        System.out.println("\n=== Fin du test ===");
    }
    
    private static String masquerCle(String cle) {
        if (cle == null || cle.length() < 8) {
            return "***";
        }
        return cle.substring(0, 8) + "..." + cle.substring(cle.length() - 4);
    }
}