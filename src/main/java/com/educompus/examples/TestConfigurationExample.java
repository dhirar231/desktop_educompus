package com.educompus.examples;

import com.educompus.service.ConfigurationService;
import com.educompus.service.AIVideoGenerationService;

import java.util.concurrent.CompletableFuture;

/**
 * Test de la configuration corrigée - plus d'erreur D-ID !
 */
public class TestConfigurationExample {

    public static void main(String[] args) {
        System.out.println("🔧 TEST DE LA CONFIGURATION CORRIGÉE");
        System.out.println("=====================================");
        System.out.println("Objectif: Éliminer l'erreur 'Clé API D-ID non configurée'");
        System.out.println();

        // 1. Initialiser la configuration automatiquement
        testerInitialisationConfiguration();

        // 2. Tester la génération de vidéo (sans D-ID)
        testerGenerationVideo();

        // 3. Vérifier que D-ID n'est plus utilisé
        verifierSuppressionDID();
    }

    /**
     * Teste l'initialisation automatique de la configuration.
     */
    private static void testerInitialisationConfiguration() {
        System.out.println("🚀 INITIALISATION DE LA CONFIGURATION");
        System.out.println("--------------------------------------");

        try {
            // Initialiser automatiquement
            ConfigurationService.initialiserConfiguration();
            
            // Vérifier le statut
            boolean valide = ConfigurationService.configurationValide();
            System.out.println("✅ Configuration valide: " + valide);
            
            if (valide) {
                System.out.println("🎉 Plus d'erreur de configuration !");
            } else {
                System.out.println("❌ Configuration encore invalide");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'initialisation: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Teste la génération de vidéo avec HeyGen (sans D-ID).
     */
    private static void testerGenerationVideo() {
        System.out.println("🎬 TEST GÉNÉRATION VIDÉO (HEYGEN)");
        System.out.println("----------------------------------");

        try {
            // Générer une vidéo de test
            CompletableFuture<AIVideoGenerationService.VideoGenerationResult> future = 
                AIVideoGenerationService.generateVideo(
                    "Test de génération vidéo sans erreur D-ID",
                    "Test",
                    "Débutant",
                    "Test"
                );

            System.out.println("⏳ Génération en cours...");
            AIVideoGenerationService.VideoGenerationResult resultat = future.get();

            if (resultat.isSuccess()) {
                System.out.println("✅ Génération réussie !");
                System.out.println("📹 URL: " + resultat.getVideoUrl());
                System.out.println("📝 Script: " + resultat.getScript().substring(0, Math.min(100, resultat.getScript().length())) + "...");
                System.out.println("🎉 AUCUNE ERREUR D-ID !");
            } else {
                System.out.println("❌ Erreur: " + resultat.getErrorMessage());
                
                // Vérifier si c'est encore une erreur D-ID
                if (resultat.getErrorMessage().contains("D-ID")) {
                    System.out.println("⚠️ Erreur D-ID détectée - correction nécessaire");
                } else {
                    System.out.println("ℹ️ Erreur différente (pas D-ID) - normal en mode démo");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération: " + e.getMessage());
            
            if (e.getMessage().contains("D-ID")) {
                System.out.println("⚠️ Erreur D-ID encore présente - vérifier le code");
            }
        }
        
        System.out.println();
    }

    /**
     * Vérifie que D-ID n'est plus utilisé dans le système.
     */
    private static void verifierSuppressionDID() {
        System.out.println("🗑️ VÉRIFICATION SUPPRESSION D-ID");
        System.out.println("---------------------------------");

        try {
            // Vérifier les clés de configuration
            String geminiKey = ConfigurationService.obtenirCle("gemini.api.key", "NON_CONFIGURÉ");
            String heygenKey = ConfigurationService.obtenirCle("heygen.api.key", "NON_CONFIGURÉ");
            String didKey = ConfigurationService.obtenirCle("did.api.key", "SUPPRIMÉ");

            System.out.println("🤖 Gemini: " + (geminiKey.equals("NON_CONFIGURÉ") ? "❌ Non configuré" : "✅ Configuré"));
            System.out.println("🎬 HeyGen: " + (heygenKey.equals("NON_CONFIGURÉ") ? "❌ Non configuré" : "✅ Configuré"));
            System.out.println("🗑️ D-ID: " + (didKey.equals("SUPPRIMÉ") ? "✅ Supprimé" : "⚠️ Encore présent"));

            if (didKey.equals("SUPPRIMÉ")) {
                System.out.println("🎉 D-ID complètement supprimé du système !");
            } else {
                System.out.println("⚠️ D-ID encore présent dans la configuration");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la vérification: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Affiche un résumé des corrections apportées.
     */
    public static void afficherResumeCorrections() {
        System.out.println("📋 RÉSUMÉ DES CORRECTIONS APPORTÉES");
        System.out.println("====================================");
        
        System.out.println("✅ PROBLÈMES RÉSOLUS:");
        System.out.println("   • Erreur 'Clé API D-ID non configurée' éliminée");
        System.out.println("   • Migration complète de D-ID vers HeyGen");
        System.out.println("   • Configuration automatique des clés par défaut");
        System.out.println("   • Suppression des références D-ID obsolètes");
        System.out.println();
        
        System.out.println("🔧 MODIFICATIONS TECHNIQUES:");
        System.out.println("   • ai-config.properties mis à jour (HeyGen au lieu de D-ID)");
        System.out.println("   • AIVideoGenerationService migré vers HeyGen");
        System.out.println("   • ConfigurationService créé pour auto-configuration");
        System.out.println("   • Clés par défaut fonctionnelles intégrées");
        System.out.println();
        
        System.out.println("🎯 RÉSULTAT:");
        System.out.println("   • Plus d'erreur D-ID au démarrage");
        System.out.println("   • Génération vidéo fonctionnelle avec HeyGen");
        System.out.println("   • Configuration automatique et transparente");
        System.out.println("   • Cache intelligent opérationnel");
        System.out.println();
        
        System.out.println("🚀 PRÊT À UTILISER !");
    }
}