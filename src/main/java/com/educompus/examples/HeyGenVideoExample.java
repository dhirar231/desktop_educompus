package com.educompus.examples;

import com.educompus.service.HeyGenVideoService;
import com.educompus.service.GoogleTTSService;
import com.educompus.service.AIVideoGenerationService;

import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'utilisation complète du pipeline de génération vidéo IA:
 * Gemini (script) → Google TTS (audio) → HeyGen (vidéo)
 */
public class HeyGenVideoExample {

    public static void main(String[] args) {
        System.out.println("🎬 DÉMONSTRATION PIPELINE VIDÉO IA COMPLET");
        System.out.println("==========================================");
        System.out.println("Pipeline: Gemini → Google TTS → HeyGen");
        System.out.println();

        // Exemple 1: Test de configuration HeyGen
        testerConfigurationHeyGen();

        // Exemple 2: Génération vidéo simple
        genererVideoSimple();

        // Exemple 3: Pipeline complet avec TTS
        pipelineComplet();

        // Exemple 4: Génération éducative avancée
        generationEducativeAvancee();
    }

    /**
     * Teste la configuration HeyGen.
     */
    private static void testerConfigurationHeyGen() {
        System.out.println("🧪 TEST DE CONFIGURATION HEYGEN");
        System.out.println("--------------------------------");

        try {
            HeyGenVideoService.ResultatHeyGen resultat = HeyGenVideoService.testerConfiguration();
            
            if (resultat.isSucces()) {
                System.out.println("✅ Configuration HeyGen fonctionnelle !");
                System.out.println("📹 URL vidéo: " + resultat.getUrlVideo());
                System.out.println("⏱️ Durée: " + resultat.getDureeFormatee());
            } else {
                System.out.println("❌ Test échoué: " + resultat.getMessageErreur());
                System.out.println("💡 Vérifiez votre clé API HeyGen dans les variables d'environnement");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du test: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Génère une vidéo simple avec HeyGen.
     */
    private static void genererVideoSimple() {
        System.out.println("🎥 GÉNÉRATION VIDÉO SIMPLE");
        System.out.println("---------------------------");

        String script = """
            Bonjour ! Bienvenue dans cette démonstration de génération vidéo avec HeyGen.
            
            Cette technologie permet de créer facilement des vidéos éducatives avec des avatars réalistes.
            
            C'est parfait pour créer du contenu pédagogique engageant et accessible.
            
            Merci de votre attention !
            """;

        try {
            // Configurer les paramètres
            HeyGenVideoService.ParametresHeyGen parametres = new HeyGenVideoService.ParametresHeyGen();
            parametres.setAvatar(HeyGenVideoService.AvatarEducatif.MADISON_1);
            parametres.setVoix("fr-FR-DeniseNeural");
            parametres.setQualite("medium"); // Qualité réduite pour l'exemple
            parametres.setRatio("16:9");
            parametres.setSousTitres(true);
            parametres.setVitesseParole(0.9);

            System.out.println("📋 Paramètres: " + parametres);
            System.out.println("⏳ Génération en cours...");

            // Générer la vidéo
            HeyGenVideoService.ResultatHeyGen resultat = HeyGenVideoService.genererVideo(script, parametres);
            
            if (resultat.isSucces()) {
                System.out.println("✅ Vidéo générée avec succès !");
                System.out.println("📹 URL: " + resultat.getUrlVideo());
                System.out.println("⏱️ Durée: " + resultat.getDureeFormatee());
                if (resultat.getUrlThumbnail() != null) {
                    System.out.println("🖼️ Miniature: " + resultat.getUrlThumbnail());
                }
            } else {
                System.out.println("❌ Erreur: " + resultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Démontre le pipeline complet avec TTS.
     */
    private static void pipelineComplet() {
        System.out.println("🔄 PIPELINE COMPLET (TTS + VIDÉO)");
        System.out.println("----------------------------------");

        String script = """
            Découvrons ensemble les algorithmes de tri !
            
            Un algorithme de tri organise les éléments d'une liste selon un ordre défini.
            
            Par exemple, le tri à bulles compare les éléments adjacents et les échange si nécessaire.
            
            C'est un concept fondamental en informatique que tout développeur doit maîtriser.
            """;

        try {
            System.out.println("🔊 Étape 1: Génération de l'audio TTS...");
            
            // Générer l'audio avec Google TTS
            GoogleTTSService.ParametresTTS parametresTTS = new GoogleTTSService.ParametresTTS();
            parametresTTS.setVoix(GoogleTTSService.VoixFrancaise.FEMALE_A);
            
            CompletableFuture<GoogleTTSService.ResultatTTS> audioFuture = 
                GoogleTTSService.convertirTexteAsync(script, parametresTTS);
            
            GoogleTTSService.ResultatTTS audioResultat = audioFuture.get();
            
            if (audioResultat.isSucces()) {
                System.out.println("✅ Audio généré: " + audioResultat.getCheminFichier());
                System.out.println("📁 Chemin: " + audioResultat.getCheminFichier());
            } else {
                System.out.println("❌ Erreur audio: " + audioResultat.getMessageErreur());
                return;
            }

            System.out.println();
            System.out.println("🎬 Étape 2: Génération de la vidéo HeyGen...");
            
            // Générer la vidéo avec HeyGen
            CompletableFuture<HeyGenVideoService.ResultatHeyGen> videoFuture = 
                HeyGenVideoService.genererVideoEducativeAsync(script, HeyGenVideoService.AvatarEducatif.MADISON_2);
            
            HeyGenVideoService.ResultatHeyGen videoResultat = videoFuture.get();
            
            if (videoResultat.isSucces()) {
                System.out.println("✅ Vidéo générée avec succès !");
                System.out.println("📹 URL vidéo: " + videoResultat.getUrlVideo());
                System.out.println("🔊 Fichier audio: " + audioResultat.getCheminFichier());
                System.out.println("⏱️ Durée vidéo: " + videoResultat.getDureeFormatee());
                
                System.out.println();
                System.out.println("🎉 PIPELINE COMPLET TERMINÉ !");
                System.out.println("Vous avez maintenant une vidéo avec avatar et un fichier audio séparé.");
                
            } else {
                System.out.println("❌ Erreur vidéo: " + videoResultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur dans le pipeline: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }

    /**
     * Génération éducative avancée avec tous les avatars.
     */
    private static void generationEducativeAvancee() {
        System.out.println("🎓 GÉNÉRATION ÉDUCATIVE AVANCÉE");
        System.out.println("--------------------------------");

        // Tester différents avatars pour différents sujets
        String[] sujets = {
            "Les bases de la programmation orientée objet",
            "Introduction aux bases de données relationnelles", 
            "Les principes de l'algorithmique"
        };

        HeyGenVideoService.AvatarEducatif[] avatars = {
            HeyGenVideoService.AvatarEducatif.MADISON_1,
            HeyGenVideoService.AvatarEducatif.MADISON_2,
            HeyGenVideoService.AvatarEducatif.MADISON_3
        };

        for (int i = 0; i < Math.min(sujets.length, avatars.length); i++) {
            String sujet = sujets[i];
            HeyGenVideoService.AvatarEducatif avatar = avatars[i];
            
            System.out.println("📚 Sujet " + (i + 1) + ": " + sujet);
            System.out.println("👤 Avatar: " + avatar.getDescription());
            
            String script = String.format("""
                Bonjour ! Aujourd'hui, nous allons explorer %s.
                
                Ce sujet est essentiel pour votre formation en informatique.
                
                Nous verrons les concepts clés et des exemples pratiques.
                
                Prêts à apprendre ? C'est parti !
                """, sujet.toLowerCase());

            try {
                // Génération asynchrone pour optimiser les performances
                CompletableFuture<HeyGenVideoService.ResultatHeyGen> future = 
                    HeyGenVideoService.genererVideoEducativeAsync(script, avatar);
                
                System.out.println("⏳ Génération en cours...");
                
                // Note: Dans un vrai cas d'usage, vous pourriez traiter les résultats de manière asynchrone
                // Ici on attend pour l'exemple
                HeyGenVideoService.ResultatHeyGen resultat = future.get();
                
                if (resultat.isSucces()) {
                    System.out.println("✅ Succès ! URL: " + resultat.getUrlVideo());
                } else {
                    System.out.println("❌ Échec: " + resultat.getMessageErreur());
                }
                
            } catch (Exception e) {
                System.err.println("❌ Erreur: " + e.getMessage());
            }
            
            System.out.println();
        }

        System.out.println("🏁 Génération éducative terminée !");
        System.out.println();
    }

    /**
     * Démontre l'intégration avec AIVideoGenerationService (Gemini + HeyGen).
     */
    public static void demonstrationGeminiHeyGen() {
        System.out.println("🤖 INTÉGRATION GEMINI + HEYGEN");
        System.out.println("-------------------------------");

        try {
            // Utiliser AIVideoGenerationService pour générer le script avec Gemini
            CompletableFuture<AIVideoGenerationService.VideoGenerationResult> future = 
                AIVideoGenerationService.generateVideo(
                    "Les structures de données: listes, piles et files",
                    "Algorithmique et Structures de Données",
                    "2ème année",
                    "Informatique"
                );

            System.out.println("⏳ Génération du script avec Gemini...");
            AIVideoGenerationService.VideoGenerationResult resultat = future.get();

            if (resultat.isSuccess()) {
                System.out.println("✅ Script généré par Gemini !");
                System.out.println("📝 Script: " + resultat.getScript().substring(0, Math.min(200, resultat.getScript().length())) + "...");
                
                // Maintenant utiliser HeyGen pour créer la vidéo
                System.out.println();
                System.out.println("🎬 Génération de la vidéo avec HeyGen...");
                
                CompletableFuture<HeyGenVideoService.ResultatHeyGen> heygenFuture = 
                    HeyGenVideoService.genererVideoEducativeAsync(
                        resultat.getScript(), 
                        HeyGenVideoService.AvatarEducatif.MADISON_4
                    );
                
                HeyGenVideoService.ResultatHeyGen heygenResultat = heygenFuture.get();
                
                if (heygenResultat.isSucces()) {
                    System.out.println("✅ Vidéo HeyGen créée !");
                    System.out.println("📹 URL: " + heygenResultat.getUrlVideo());
                    System.out.println("⏱️ Durée: " + heygenResultat.getDureeFormatee());
                    
                    System.out.println();
                    System.out.println("🎉 INTÉGRATION COMPLÈTE RÉUSSIE !");
                    System.out.println("Gemini a créé le script, HeyGen a créé la vidéo !");
                    
                } else {
                    System.out.println("❌ Erreur HeyGen: " + heygenResultat.getMessageErreur());
                }
                
            } else {
                System.out.println("❌ Erreur Gemini: " + resultat.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur dans l'intégration: " + e.getMessage());
            e.printStackTrace();
        }
    }
}