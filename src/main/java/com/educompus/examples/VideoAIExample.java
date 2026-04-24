package com.educompus.examples;

import com.educompus.model.VideoExplicative;
import com.educompus.model.ParametresGeneration;
import com.educompus.service.VideoExplicatifService;
import com.educompus.service.ValidationResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'utilisation du service de génération de vidéos IA.
 * 
 * Pour utiliser cet exemple :
 * 1. Configurez les variables d'environnement OPENAI_API_KEY et DID_API_KEY
 * 2. Assurez-vous d'avoir un chapitre existant dans votre base de données
 * 3. Exécutez cette classe
 */
public class VideoAIExample {

    public static void main(String[] args) {
        System.out.println("=== Exemple de génération de vidéos IA ===\n");

        VideoExplicatifService service = new VideoExplicatifService();

        try {
            // 1. Lister les vidéos existantes
            System.out.println("1. Vidéos existantes :");
            List<VideoExplicative> videos = service.listerToutes("");
            if (videos.isEmpty()) {
                System.out.println("   Aucune vidéo trouvée.");
            } else {
                videos.forEach(v -> System.out.println("   - " + v.getTitre() + " (IA: " + v.isAIGenerated() + ")"));
            }
            System.out.println();

            // 2. Créer une vidéo manuelle pour test
            System.out.println("2. Création d'une vidéo manuelle de test :");
            VideoExplicative videoManuelle = new VideoExplicative();
            videoManuelle.setTitre("Exemple vidéo manuelle");
            videoManuelle.setDescription("Cette vidéo a été créée manuellement pour démonstration.");
            videoManuelle.setUrlVideo("https://www.youtube.com/watch?v=exemple");
            videoManuelle.setCoursId(1); // Remplacez par un ID de cours existant
            videoManuelle.setChapitreId(1); // Remplacez par un ID de chapitre existant
            videoManuelle.setNiveau("1er");
            videoManuelle.setDomaine("Informatique");
            videoManuelle.setAIGenerated(false);

            try {
                service.creer(videoManuelle);
                System.out.println("   ✓ Vidéo manuelle créée avec succès");
            } catch (Exception e) {
                System.out.println("   ✗ Erreur lors de la création : " + e.getMessage());
                System.out.println("   (Vérifiez que les IDs de cours et chapitre existent)");
            }
            System.out.println();

            // 3. Configurer les paramètres de génération IA
            System.out.println("3. Configuration des paramètres IA :");
            ParametresGeneration parametres = new ParametresGeneration();
            parametres.setDureeMinutes(3);
            parametres.setLangue("fr");
            parametres.setQualite("HD");
            parametres.setVoixType("neutre");
            parametres.setStyleNarration("pédagogique");
            
            System.out.println("   " + parametres);
            System.out.println();

            // 4. Générer une vidéo IA (asynchrone)
            System.out.println("4. Génération d'une vidéo IA :");
            
            // Vérifier les clés API
            String openaiKey = System.getenv("OPENAI_API_KEY");
            String didKey = System.getenv("DID_API_KEY");
            
            if (openaiKey == null || openaiKey.isEmpty()) {
                System.out.println("   ⚠ OPENAI_API_KEY non configurée - mode simulation");
            } else {
                System.out.println("   ✓ OPENAI_API_KEY configurée");
            }
            
            if (didKey == null || didKey.isEmpty()) {
                System.out.println("   ⚠ DID_API_KEY non configurée - mode simulation");
            } else {
                System.out.println("   ✓ DID_API_KEY configurée");
            }
            System.out.println();

            // Générer la vidéo (remplacez 1 par un ID de chapitre existant)
            int chapitreId = 1;
            System.out.println("   Démarrage de la génération pour le chapitre " + chapitreId + "...");
            
            CompletableFuture<VideoExplicative> future = service.genererVideoAsync(chapitreId, parametres);
            
            // Attendre le résultat (avec timeout)
            try {
                VideoExplicative videoIA = future.get(java.util.concurrent.TimeUnit.MINUTES.toMillis(2), 
                                                     java.util.concurrent.TimeUnit.MILLISECONDS);
                
                System.out.println("   ✓ Vidéo IA générée avec succès !");
                System.out.println("     - ID: " + videoIA.getId());
                System.out.println("     - Titre: " + videoIA.getTitre());
                System.out.println("     - Statut: " + videoIA.getGenerationStatus());
                System.out.println("     - URL: " + videoIA.getUrlVideo());
                
                if (videoIA.getAiScript() != null) {
                    String script = videoIA.getAiScript();
                    String preview = script.length() > 200 ? script.substring(0, 200) + "..." : script;
                    System.out.println("     - Script (aperçu): " + preview);
                }
                
            } catch (java.util.concurrent.TimeoutException e) {
                System.out.println("   ⏱ Timeout - la génération prend plus de temps que prévu");
                System.out.println("   Vous pouvez vérifier le statut plus tard avec verifierStatutGeneration()");
            } catch (Exception e) {
                System.out.println("   ✗ Erreur lors de la génération : " + e.getMessage());
                
                if (e.getMessage().contains("Chapitre introuvable")) {
                    System.out.println("   💡 Conseil: Créez d'abord un cours et un chapitre, ou utilisez des IDs existants");
                } else if (e.getMessage().contains("API")) {
                    System.out.println("   💡 Conseil: Vérifiez vos clés API ou testez en mode simulation");
                }
            }
            System.out.println();

            // 5. Lister les vidéos après génération
            System.out.println("5. Vidéos après génération :");
            List<VideoExplicative> videosApres = service.listerToutes("");
            videosApres.forEach(v -> {
                System.out.println("   - " + v.getTitre());
                System.out.println("     Type: " + (v.isAIGenerated() ? "IA" : "Manuelle"));
                System.out.println("     Statut: " + (v.getGenerationStatus() != null ? v.getGenerationStatus() : "N/A"));
            });

        } catch (Exception e) {
            System.err.println("Erreur générale : " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Fermer le service
            service.fermer();
            System.out.println("\n=== Fin de l'exemple ===");
        }
    }

    /**
     * Exemple de validation des paramètres
     */
    public static void exempleValidation() {
        System.out.println("=== Exemple de validation ===");
        
        VideoExplicatifService service = new VideoExplicatifService();
        
        // Test avec des paramètres invalides
        try {
            ParametresGeneration parametres = new ParametresGeneration();
            parametres.setDureeMinutes(0); // Invalide
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur attendue : " + e.getMessage());
        }
        
        try {
            ParametresGeneration parametres = new ParametresGeneration();
            parametres.setLangue(""); // Invalide
        } catch (IllegalArgumentException e) {
            System.out.println("Erreur attendue : " + e.getMessage());
        }
        
        // Test avec une vidéo invalide
        VideoExplicative video = new VideoExplicative();
        video.setTitre("Ti"); // Trop court
        
        ValidationResult result = service.validerSansException(video);
        if (!result.isValid()) {
            System.out.println("Erreurs de validation :");
            result.getErrors().forEach(e -> System.out.println("  - " + e));
        }
        
        service.fermer();
    }
}