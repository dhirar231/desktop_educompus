package com.educompus.examples;

import com.educompus.model.VideoExplicative;
import com.educompus.model.ParametresGeneration;
import com.educompus.service.GeminiConfigService;
import com.educompus.service.VideoExplicatifService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'utilisation du service de génération de vidéos IA avec Google Gemini.
 */
public class GeminiVideoExample {

    public static void main(String[] args) {
        System.out.println("=== Test de génération de vidéos IA avec Google Gemini ===\n");

        // 1. Configuration automatique de Gemini
        GeminiConfigService.configurerCleAPI();
        GeminiConfigService.afficherStatut();
        System.out.println();

        VideoExplicatifService service = new VideoExplicatifService();

        try {
            // 2. Test de génération de script simple
            System.out.println("2. Test de génération de script avec Gemini :");
            testGenerationScript(service);
            System.out.println();

            // 3. Test de génération complète de vidéo
            System.out.println("3. Test de génération complète de vidéo :");
            testGenerationComplete(service);
            System.out.println();

            // 4. Lister les vidéos existantes
            System.out.println("4. Vidéos existantes dans la base :");
            listerVideos(service);

        } catch (Exception e) {
            System.err.println("Erreur générale : " + e.getMessage());
            e.printStackTrace();
        } finally {
            service.fermer();
            System.out.println("\n=== Fin du test Gemini ===");
        }
    }

    private static void testGenerationScript(VideoExplicatifService service) {
        try {
            // Créer un chapitre de test
            com.educompus.model.Chapitre chapitre = new com.educompus.model.Chapitre();
            chapitre.setId(1);
            chapitre.setTitre("Introduction aux Algorithmes");
            chapitre.setDescription("Ce chapitre présente les concepts fondamentaux des algorithmes informatiques, incluant la complexité temporelle, les structures de données de base, et les algorithmes de tri et de recherche.");
            chapitre.setNiveau("1er");
            chapitre.setDomaine("Informatique");
            chapitre.setCoursId(1);
            chapitre.setCoursTitre("Algorithmique et Programmation");

            // Paramètres de génération
            ParametresGeneration parametres = new ParametresGeneration();
            parametres.setDureeMinutes(5);
            parametres.setLangue("fr");
            parametres.setStyleNarration("pédagogique");

            System.out.println("   Génération du script avec Gemini...");
            
            // Utiliser la méthode privée via réflexion pour tester
            java.lang.reflect.Method method = VideoExplicatifService.class.getDeclaredMethod(
                "genererScript", com.educompus.model.Chapitre.class, ParametresGeneration.class);
            method.setAccessible(true);
            
            String script = (String) method.invoke(service, chapitre, parametres);
            
            System.out.println("   ✓ Script généré avec succès !");
            System.out.println("   Longueur: " + script.length() + " caractères");
            System.out.println("   Aperçu: " + script.substring(0, Math.min(200, script.length())) + "...");
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur lors de la génération du script : " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Cause: " + e.getCause().getMessage());
            }
        }
    }

    private static void testGenerationComplete(VideoExplicatifService service) {
        try {
            // Paramètres de génération
            ParametresGeneration parametres = new ParametresGeneration();
            parametres.setDureeMinutes(3);
            parametres.setLangue("fr");
            parametres.setQualite("HD");
            parametres.setVoixType("neutre");
            parametres.setStyleNarration("pédagogique");

            System.out.println("   Configuration: " + parametres);
            System.out.println("   Démarrage de la génération complète...");

            // Note: Utilisez un ID de chapitre existant dans votre base
            int chapitreId = 1;
            
            CompletableFuture<VideoExplicative> future = service.genererVideoAsync(chapitreId, parametres);
            
            // Attendre le résultat avec timeout
            VideoExplicative video = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            System.out.println("   ✓ Vidéo générée avec succès !");
            System.out.println("     - ID: " + video.getId());
            System.out.println("     - Titre: " + video.getTitre());
            System.out.println("     - Statut: " + video.getGenerationStatus());
            System.out.println("     - IA: " + video.isAIGenerated());
            System.out.println("     - URL: " + video.getUrlVideo());
            
        } catch (java.util.concurrent.TimeoutException e) {
            System.out.println("   ⏱ Timeout - la génération prend plus de temps que prévu");
        } catch (Exception e) {
            System.out.println("   ✗ Erreur lors de la génération complète : " + e.getMessage());
            
            if (e.getMessage().contains("Chapitre introuvable")) {
                System.out.println("   💡 Conseil: Créez d'abord un cours et un chapitre dans votre base de données");
                System.out.println("   💡 Ou modifiez l'ID du chapitre dans le code pour utiliser un chapitre existant");
            }
        }
    }

    private static void listerVideos(VideoExplicatifService service) {
        try {
            List<VideoExplicative> videos = service.listerToutes("");
            
            if (videos.isEmpty()) {
                System.out.println("   Aucune vidéo trouvée dans la base de données.");
                System.out.println("   💡 Créez d'abord des cours et chapitres pour tester la génération.");
            } else {
                System.out.println("   Nombre de vidéos: " + videos.size());
                videos.forEach(v -> {
                    System.out.println("   - " + v.getTitre());
                    System.out.println("     Type: " + (v.isAIGenerated() ? "IA (Gemini)" : "Manuelle"));
                    System.out.println("     Statut: " + (v.getGenerationStatus() != null ? v.getGenerationStatus() : "N/A"));
                    System.out.println("     Date: " + v.getDateCreation());
                });
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur lors du listage : " + e.getMessage());
        }
    }

    /**
     * Test rapide de la configuration Gemini
     */
    public static void testConfiguration() {
        System.out.println("=== Test de configuration Gemini ===");
        
        GeminiConfigService.configurerCleAPI();
        GeminiConfigService.afficherStatut();
        
        if (GeminiConfigService.estConfiguree()) {
            System.out.println("✓ Configuration OK - Prêt pour la génération IA");
        } else {
            System.out.println("✗ Configuration manquante");
        }
    }
}