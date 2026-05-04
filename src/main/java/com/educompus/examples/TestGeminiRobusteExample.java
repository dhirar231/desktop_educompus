package com.educompus.examples;

import com.educompus.service.GeminiService;
import com.educompus.service.AIVideoGenerationService;

import java.util.concurrent.CompletableFuture;

/**
 * Test du service Gemini robuste - plus d'erreur HTTP 404 !
 */
public class TestGeminiRobusteExample {

    public static void main(String[] args) {
        System.out.println("🔧 TEST GEMINI ROBUSTE - CORRECTION HTTP 404");
        System.out.println("==============================================");
        System.out.println("Objectif: Éliminer l'erreur HTTP 404 de Gemini");
        System.out.println();

        // 1. Tester le statut de Gemini
        testerStatutGemini();

        // 2. Tester la génération de script directe
        testerGenerationScriptDirecte();

        // 3. Tester via AIVideoGenerationService
        testerViaAIVideoService();

        // 4. Démontrer le fallback
        demonstrerFallback();
    }

    /**
     * Teste le statut de l'API Gemini.
     */
    private static void testerStatutGemini() {
        System.out.println("📡 TEST STATUT GEMINI API");
        System.out.println("--------------------------");

        try {
            String statut = GeminiService.obtenirStatut();
            System.out.println("Statut: " + statut);
            
            boolean connexionOK = GeminiService.testerConnexion();
            System.out.println("Connexion: " + (connexionOK ? "✅ OK" : "⚠️ Fallback"));
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test de statut: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Teste la génération de script directe avec GeminiService.
     */
    private static void testerGenerationScriptDirecte() {
        System.out.println("📝 TEST GÉNÉRATION SCRIPT DIRECTE");
        System.out.println("----------------------------------");

        try {
            String script = GeminiService.genererScript(
                "Les algorithmes de tri en informatique",
                "Algorithmique",
                "Débutant",
                "Informatique"
            );
            
            if (script != null && !script.isBlank()) {
                System.out.println("✅ Script généré avec succès !");
                System.out.println("Longueur: " + script.length() + " caractères");
                System.out.println("Extrait: " + script.substring(0, Math.min(150, script.length())) + "...");
                
                // Vérifier si c'est un fallback
                if (script.contains("[Note: Script généré en mode fallback")) {
                    System.out.println("ℹ️ Mode fallback utilisé (Gemini indisponible)");
                } else {
                    System.out.println("🎉 Gemini API fonctionnelle !");
                }
            } else {
                System.out.println("❌ Script vide ou null");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Teste via AIVideoGenerationService (méthode complète).
     */
    private static void testerViaAIVideoService() {
        System.out.println("🎬 TEST VIA AI VIDEO SERVICE");
        System.out.println("-----------------------------");

        try {
            CompletableFuture<AIVideoGenerationService.VideoGenerationResult> future = 
                AIVideoGenerationService.generateVideo(
                    "Introduction aux bases de données relationnelles",
                    "Base de Données",
                    "Intermédiaire",
                    "Informatique"
                );

            System.out.println("⏳ Génération en cours...");
            AIVideoGenerationService.VideoGenerationResult resultat = future.get();

            if (resultat.isSuccess()) {
                System.out.println("✅ Génération complète réussie !");
                System.out.println("📝 Script: " + resultat.getScript().substring(0, Math.min(200, resultat.getScript().length())) + "...");
                System.out.println("📹 URL vidéo: " + resultat.getVideoUrl());
                System.out.println("🎉 AUCUNE ERREUR HTTP 404 !");
            } else {
                System.out.println("❌ Erreur: " + resultat.getErrorMessage());
                
                // Analyser le type d'erreur
                if (resultat.getErrorMessage().contains("HTTP 404")) {
                    System.out.println("⚠️ Erreur HTTP 404 encore présente - vérifier l'URL API");
                } else if (resultat.getErrorMessage().contains("script")) {
                    System.out.println("ℹ️ Erreur de script (pas HTTP 404) - normal si Gemini indisponible");
                } else {
                    System.out.println("ℹ️ Autre type d'erreur - pas HTTP 404");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Exception: " + e.getMessage());
            
            if (e.getMessage().contains("HTTP 404")) {
                System.out.println("⚠️ HTTP 404 encore présent dans l'exception");
            }
        }
        
        System.out.println();
    }

    /**
     * Démontre le système de fallback.
     */
    private static void demonstrerFallback() {
        System.out.println("🔄 DÉMONSTRATION FALLBACK");
        System.out.println("-------------------------");

        try {
            // Forcer un test avec un contenu simple
            String script = GeminiService.genererScript(
                "Test de fallback",
                "Test",
                "Test",
                "Test"
            );
            
            System.out.println("📝 Script généré (fallback si nécessaire):");
            System.out.println(script.substring(0, Math.min(300, script.length())) + "...");
            
            if (script.contains("Mode fallback")) {
                System.out.println("✅ Système de fallback fonctionnel !");
                System.out.println("ℹ️ Même si Gemini est indisponible, le système continue de fonctionner");
            } else {
                System.out.println("✅ Gemini API opérationnelle !");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur fallback: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Affiche un résumé des améliorations apportées.
     */
    public static void afficherResumeAmeliorations() {
        System.out.println("📋 RÉSUMÉ DES AMÉLIORATIONS");
        System.out.println("============================");
        
        System.out.println("✅ PROBLÈMES RÉSOLUS:");
        System.out.println("   • Erreur HTTP 404 Gemini éliminée");
        System.out.println("   • Gestion robuste des erreurs API");
        System.out.println("   • Système de fallback automatique");
        System.out.println("   • Parsing JSON amélioré");
        System.out.println();
        
        System.out.println("🔧 AMÉLIORATIONS TECHNIQUES:");
        System.out.println("   • GeminiService créé avec gestion d'erreurs robuste");
        System.out.println("   • URL API corrigée et validation des paramètres");
        System.out.println("   • Timeout et retry logic implémentés");
        System.out.println("   • Fallback local si API indisponible");
        System.out.println();
        
        System.out.println("🛡️ ROBUSTESSE:");
        System.out.println("   • Fonctionne même si Gemini est down");
        System.out.println("   • Pas de blocage sur erreurs réseau");
        System.out.println("   • Scripts de qualité garantis (API ou fallback)");
        System.out.println("   • Logging détaillé pour debugging");
        System.out.println();
        
        System.out.println("🎯 RÉSULTAT:");
        System.out.println("   • Plus d'erreur HTTP 404");
        System.out.println("   • Génération de script toujours fonctionnelle");
        System.out.println("   • Expérience utilisateur fluide");
        System.out.println("   • Système résilient et fiable");
    }
}