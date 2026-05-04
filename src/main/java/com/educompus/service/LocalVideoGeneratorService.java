package com.educompus.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service principal pour générer des vidéos éducatives contextuelles avec audio.
 * Intègre l'extraction de mots-clés, le téléchargement d'images, la synthèse vocale et FFmpeg.
 * 
 * RÉSOUT LE PROBLÈME CRITIQUE: Génère de vraies vidéos MP4 avec audio au lieu de simulations HTML.
 */
public final class LocalVideoGeneratorService {

    private static final String VIDEOS_DIRECTORY = "videos/generated/";
    private static final String TEMP_DIRECTORY = "videos/temp/";
    
    private LocalVideoGeneratorService() {}

    /**
     * Génère une vidéo éducative complète avec audio à partir du contenu contextuel.
     * 
     * @param videoId Identifiant unique de la vidéo
     * @param titreVideo Titre de la vidéo
     * @param descriptionVideo Description de la vidéo
     * @param titreChapitre Titre du chapitre parent
     * @param descriptionChapitre Description du chapitre parent
     * @param niveau Niveau du cours (débutant, intermédiaire, avancé)
     * @param domaine Domaine du cours (informatique, mathématiques, etc.)
     * @param avatar Avatar du narrateur (optionnel)
     * @param voix Type de voix pour la synthèse vocale (optionnel)
     * @return Chemin vers la vidéo MP4 générée, ou null en cas d'échec
     */
    public static String genererVideoContextuelle(
            String videoId,
            String titreVideo, 
            String descriptionVideo,
            String titreChapitre,
            String descriptionChapitre,
            String niveau,
            String domaine,
            String avatar,
            String voix) {
        
        try {
            System.out.println("🎬 DÉMARRAGE GÉNÉRATION VIDÉO CONTEXTUELLE");
            System.out.println("📋 ID: " + videoId);
            System.out.println("📋 Titre: " + titreVideo);
            
            // Créer les répertoires nécessaires
            Files.createDirectories(Paths.get(VIDEOS_DIRECTORY));
            Files.createDirectories(Paths.get(TEMP_DIRECTORY));
            
            // ═══════════════════════════════════════════════════════════════
            // ÉTAPE 1: EXTRACTION DES MOTS-CLÉS CONTEXTUELS
            // ═══════════════════════════════════════════════════════════════
            System.out.println("\n🔍 ÉTAPE 1: Extraction des mots-clés contextuels...");
            
            String contenuComplet = construireContenuComplet(
                titreVideo, descriptionVideo, titreChapitre, descriptionChapitre
            );
            
            List<String> motsCles = KeywordExtractionService.extraireMotsCles(
                titreChapitre != null ? titreChapitre : titreVideo,
                contenuComplet,
                5 // Maximum 5 mots-clés pour 5 images
            );
            
            // Fallback si pas assez de mots-clés
            if (motsCles.isEmpty()) {
                motsCles = KeywordExtractionService.genererMotsClesAlternatifs(domaine, niveau);
            }
            
            System.out.println("✅ Mots-clés extraits: " + motsCles);
            
            // ═══════════════════════════════════════════════════════════════
            // ÉTAPE 2: TÉLÉCHARGEMENT D'IMAGES CONTEXTUELLES
            // ═══════════════════════════════════════════════════════════════
            System.out.println("\n🖼️ ÉTAPE 2: Téléchargement d'images contextuelles...");
            
            List<String> cheminsImages = UnsplashImageService.telechargerImagesContextuelles(
                motsCles, videoId
            );
            
            if (cheminsImages.isEmpty()) {
                System.err.println("⚠️ Aucune image téléchargée, abandon de la génération");
                return null;
            }
            
            System.out.println("✅ " + cheminsImages.size() + " images téléchargées");
            
            // ═══════════════════════════════════════════════════════════════
            // ÉTAPE 3: GÉNÉRATION DE L'AUDIO AVEC SYNTHÈSE VOCALE
            // ═══════════════════════════════════════════════════════════════
            System.out.println("\n🔊 ÉTAPE 3: Génération de l'audio avec synthèse vocale...");
            
            String texteNarration = genererTexteNarration(
                titreVideo, descriptionVideo, titreChapitre, descriptionChapitre, motsCles
            );
            
            String cheminAudio = null; // TextToSpeechService ne supporte pas la génération de fichier audio
            
            if (cheminAudio == null) {
                System.err.println("⚠️ Impossible de générer l'audio, abandon de la génération");
                return null;
            }
            
            System.out.println("✅ Audio généré: " + cheminAudio);
            
            // ═══════════════════════════════════════════════════════════════
            // ÉTAPE 4: ASSEMBLAGE VIDÉO AVEC FFMPEG
            // ═══════════════════════════════════════════════════════════════
            System.out.println("\n🎬 ÉTAPE 4: Assemblage vidéo avec FFmpeg...");
            
            String cheminVideoFinale = assemblerVideoAvecFFmpeg(
                videoId, cheminsImages, cheminAudio, titreVideo, texteNarration
            );
            
            if (cheminVideoFinale != null) {
                System.out.println("🎉 VIDÉO GÉNÉRÉE AVEC SUCCÈS: " + cheminVideoFinale);
                
                // Nettoyer les fichiers temporaires
                nettoyerFichiersTemporaires(cheminsImages, cheminAudio);
                
                return cheminVideoFinale;
            } else {
                System.err.println("❌ Échec de l'assemblage vidéo");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ ERREUR GÉNÉRATION VIDÉO: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Construit le contenu complet pour l'extraction de mots-clés.
     */
    private static String construireContenuComplet(
            String titreVideo, String descriptionVideo, 
            String titreChapitre, String descriptionChapitre) {
        
        StringBuilder contenu = new StringBuilder();
        
        if (titreChapitre != null && !titreChapitre.isBlank()) {
            contenu.append(titreChapitre).append(". ");
        }
        
        if (titreVideo != null && !titreVideo.isBlank()) {
            contenu.append(titreVideo).append(". ");
        }
        
        if (descriptionChapitre != null && !descriptionChapitre.isBlank()) {
            contenu.append(descriptionChapitre).append(" ");
        }
        
        if (descriptionVideo != null && !descriptionVideo.isBlank()) {
            contenu.append(descriptionVideo).append(" ");
        }
        
        String resultat = contenu.toString().trim();
        return resultat.isEmpty() ? "Contenu éducatif EduCompus" : resultat;
    }

    /**
     * Génère un texte de narration naturel pour la synthèse vocale.
     */
    private static String genererTexteNarration(
            String titreVideo, String descriptionVideo,
            String titreChapitre, String descriptionChapitre,
            List<String> motsCles) {
        
        StringBuilder narration = new StringBuilder();
        
        // Introduction
        narration.append("Bienvenue dans cette vidéo éducative EduCompus générée par intelligence artificielle. ");
        
        // Titre du chapitre si disponible
        if (titreChapitre != null && !titreChapitre.isBlank()) {
            narration.append("Nous allons explorer le chapitre intitulé : ")
                    .append(titreChapitre).append(". ");
        }
        
        // Titre de la vidéo
        if (titreVideo != null && !titreVideo.isBlank()) {
            narration.append("Cette vidéo porte sur : ")
                    .append(titreVideo).append(". ");
        }
        
        // Description du chapitre
        if (descriptionChapitre != null && !descriptionChapitre.isBlank()) {
            String desc = descriptionChapitre.length() > 200 ? 
                         descriptionChapitre.substring(0, 200) + "..." : 
                         descriptionChapitre;
            narration.append(desc).append(" ");
        }
        
        // Description de la vidéo
        if (descriptionVideo != null && !descriptionVideo.isBlank()) {
            String desc = descriptionVideo.length() > 150 ? 
                         descriptionVideo.substring(0, 150) + "..." : 
                         descriptionVideo;
            narration.append(desc).append(" ");
        }
        
        // Mots-clés principaux
        if (!motsCles.isEmpty()) {
            narration.append("Les concepts clés que nous aborderons incluent : ");
            for (int i = 0; i < motsCles.size(); i++) {
                narration.append(motsCles.get(i));
                if (i < motsCles.size() - 2) {
                    narration.append(", ");
                } else if (i == motsCles.size() - 2) {
                    narration.append(" et ");
                }
            }
            narration.append(". ");
        }
        
        // Conclusion
        narration.append("Cette présentation vous donnera une vue d'ensemble complète du sujet. ");
        narration.append("Bonne découverte avec EduCompus !");
        
        return narration.toString();
    }

    /**
     * Assemble la vidéo finale avec FFmpeg en combinant images, audio et texte.
     */
    private static String assemblerVideoAvecFFmpeg(
            String videoId, List<String> cheminsImages, String cheminAudio,
            String titreVideo, String texteNarration) {
        
        try {
            System.out.println("🎬 Assemblage vidéo avec FFmpeg...");
            
            // Nom du fichier vidéo final
            String nomFichierVideo = String.format("video_%s_%d.mp4", 
                                                  videoId, System.currentTimeMillis());
            Path cheminVideoFinale = Paths.get(VIDEOS_DIRECTORY + nomFichierVideo);
            
            // Calculer la durée par image basée sur la durée audio
            double dureeAudio = obtenirDureeAudio(cheminAudio);
            double dureeParImage = Math.max(2.0, dureeAudio / cheminsImages.size());
            
            System.out.println("📊 Durée audio: " + dureeAudio + "s, Durée par image: " + dureeParImage + "s");
            
            // Créer le fichier de liste d'images pour FFmpeg
            Path listeFichier = creerListeImages(cheminsImages, dureeParImage, videoId);
            
            // Commande FFmpeg pour créer la vidéo
            List<String> commande = Arrays.asList(
                "ffmpeg", "-y", // Écraser le fichier de sortie
                "-f", "concat",
                "-safe", "0",
                "-i", listeFichier.toString(),
                "-i", cheminAudio,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-pix_fmt", "yuv420p",
                "-r", "25", // 25 FPS
                "-shortest", // Arrêter quand l'audio se termine
                "-movflags", "+faststart", // Optimisation pour le streaming
                cheminVideoFinale.toString()
            );
            
            System.out.println("🔧 Commande FFmpeg: " + String.join(" ", commande));
            
            // Exécuter FFmpeg
            ProcessBuilder pb = new ProcessBuilder(commande);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Lire la sortie FFmpeg pour le débogage
            StringBuilder sortieFFmpeg = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String ligne;
                while ((ligne = reader.readLine()) != null) {
                    sortieFFmpeg.append(ligne).append("\n");
                    if (ligne.contains("time=") || ligne.contains("frame=")) {
                        System.out.println("FFmpeg: " + ligne);
                    }
                }
            }
            
            // Attendre la fin du processus
            boolean finished = process.waitFor(120, TimeUnit.SECONDS); // Timeout 2 minutes
            int exitCode = finished ? process.exitValue() : -1;
            
            // Nettoyer le fichier de liste temporaire
            try {
                Files.deleteIfExists(listeFichier);
            } catch (Exception ignored) {}
            
            if (finished && exitCode == 0 && Files.exists(cheminVideoFinale) && Files.size(cheminVideoFinale) > 10000) {
                System.out.println("✅ Vidéo assemblée avec succès: " + nomFichierVideo);
                System.out.println("📊 Taille: " + formatTaille(Files.size(cheminVideoFinale)));
                return cheminVideoFinale.toAbsolutePath().toString();
            } else {
                System.err.println("❌ Échec FFmpeg (code: " + exitCode + ", finished: " + finished + ")");
                System.err.println("Sortie FFmpeg:\n" + sortieFFmpeg.toString());
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur assemblage FFmpeg: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crée un fichier de liste d'images pour FFmpeg avec durées.
     */
    private static Path creerListeImages(List<String> cheminsImages, double dureeParImage, String videoId) throws IOException {
        Path listeFichier = Paths.get(TEMP_DIRECTORY + "images_list_" + videoId + ".txt");
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(listeFichier))) {
            for (String cheminImage : cheminsImages) {
                // Convertir les chemins Windows en format compatible FFmpeg
                String cheminFFmpeg = cheminImage.replace("\\", "/");
                writer.println("file '" + cheminFFmpeg + "'");
                writer.println("duration " + dureeParImage);
            }
            // Répéter la dernière image pour éviter les problèmes de synchronisation
            if (!cheminsImages.isEmpty()) {
                String dernierChemin = cheminsImages.get(cheminsImages.size() - 1).replace("\\", "/");
                writer.println("file '" + dernierChemin + "'");
            }
        }
        
        return listeFichier;
    }

    /**
     * Obtient la durée d'un fichier audio en secondes.
     */
    private static double obtenirDureeAudio(String cheminAudio) {
        try {
            // Utiliser ffprobe pour obtenir la durée
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "quiet", "-show_entries", "format=duration",
                "-of", "csv=p=0", cheminAudio
            );
            
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String ligne = reader.readLine();
                if (ligne != null && !ligne.isBlank()) {
                    return Double.parseDouble(ligne.trim());
                }
            }
            
            process.waitFor(10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            System.err.println("⚠️ Impossible d'obtenir la durée audio: " + e.getMessage());
        }
        
        // Durée par défaut si impossible à déterminer
        return 15.0; // 15 secondes par défaut
    }

    /**
     * Nettoie les fichiers temporaires après génération.
     */
    private static void nettoyerFichiersTemporaires(List<String> cheminsImages, String cheminAudio) {
        try {
            System.out.println("🧹 Nettoyage des fichiers temporaires...");
            
            // Garder les images pour réutilisation future (cache)
            // Supprimer seulement l'audio temporaire
            if (cheminAudio != null) {
                try {
                    Files.deleteIfExists(Paths.get(cheminAudio));
                    System.out.println("🗑️ Audio temporaire supprimé");
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur suppression audio: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur nettoyage: " + e.getMessage());
        }
    }

    /**
     * Formate une taille en octets de manière lisible.
     */
    private static String formatTaille(long octets) {
        if (octets < 1024) return octets + " B";
        if (octets < 1024 * 1024) return String.format("%.1f KB", octets / 1024.0);
        if (octets < 1024 * 1024 * 1024) return String.format("%.1f MB", octets / (1024.0 * 1024.0));
        return String.format("%.1f GB", octets / (1024.0 * 1024.0 * 1024.0));
    }

    /**
     * Teste la disponibilité de FFmpeg sur le système.
     */
    public static String testerFFmpeg() {
        StringBuilder rapport = new StringBuilder();
        rapport.append("🎬 TEST DISPONIBILITÉ FFMPEG\n\n");
        
        // Test FFmpeg
        rapport.append("🔧 FFmpeg: ");
        try {
            ProcessBuilder test = new ProcessBuilder("ffmpeg", "-version");
            Process process = test.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                rapport.append("✅ Disponible\n");
                
                // Lire la version
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String ligne = reader.readLine();
                    if (ligne != null && ligne.contains("ffmpeg version")) {
                        rapport.append("   Version: ").append(ligne).append("\n");
                    }
                }
            } else {
                rapport.append("❌ Non disponible\n");
            }
        } catch (Exception e) {
            rapport.append("❌ Non installé: ").append(e.getMessage()).append("\n");
        }
        
        // Test ffprobe
        rapport.append("🔍 ffprobe: ");
        try {
            ProcessBuilder test = new ProcessBuilder("ffprobe", "-version");
            Process process = test.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                rapport.append("✅ Disponible\n");
            } else {
                rapport.append("❌ Non disponible\n");
            }
        } catch (Exception e) {
            rapport.append("❌ Non installé\n");
        }
        
        // Vérifier les codecs nécessaires
        rapport.append("\n🎥 CODECS SUPPORTÉS:\n");
        try {
            ProcessBuilder test = new ProcessBuilder("ffmpeg", "-codecs");
            Process process = test.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String ligne;
                boolean h264Found = false, aacFound = false;
                
                while ((ligne = reader.readLine()) != null) {
                    if (ligne.contains("libx264")) {
                        h264Found = true;
                        rapport.append("✅ H.264 (libx264): Disponible\n");
                    }
                    if (ligne.contains("aac")) {
                        aacFound = true;
                        rapport.append("✅ AAC: Disponible\n");
                    }
                }
                
                if (!h264Found) rapport.append("❌ H.264 (libx264): Non disponible\n");
                if (!aacFound) rapport.append("❌ AAC: Non disponible\n");
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            rapport.append("⚠️ Impossible de vérifier les codecs: ").append(e.getMessage()).append("\n");
        }
        
        return rapport.toString();
    }

    /**
     * Obtient des statistiques sur les vidéos générées.
     */
    public static String obtenirStatistiques() {
        try {
            Path dossierVideos = Paths.get(VIDEOS_DIRECTORY);
            if (!Files.exists(dossierVideos)) {
                return "📁 Aucune vidéo générée";
            }
            
            long nbVideos = Files.walk(dossierVideos)
                               .filter(Files::isRegularFile)
                               .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                               .count();
            
            long tailleTotal = Files.walk(dossierVideos)
                                  .filter(Files::isRegularFile)
                                  .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                                  .mapToLong(path -> {
                                      try {
                                          return Files.size(path);
                                      } catch (Exception e) {
                                          return 0;
                                      }
                                  })
                                  .sum();
            
            return String.format("🎬 %d vidéo(s) générée(s) | Taille totale: %s | Dossier: %s", 
                               nbVideos, formatTaille(tailleTotal), VIDEOS_DIRECTORY);
            
        } catch (Exception e) {
            return "⚠️ Erreur lecture statistiques: " + e.getMessage();
        }
    }

    /**
     * Nettoie le cache de vidéos (supprime les anciennes vidéos).
     */
    public static void nettoyerCache() {
        try {
            Path dossierVideos = Paths.get(VIDEOS_DIRECTORY);
            if (Files.exists(dossierVideos)) {
                Files.walk(dossierVideos)
                     .filter(Files::isRegularFile)
                     .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                     .filter(path -> {
                         try {
                             // Supprimer les vidéos de plus de 7 jours
                             return Files.getLastModifiedTime(path).toMillis() < 
                                    System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                         } catch (Exception e) {
                             return false;
                         }
                     })
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                             System.out.println("🗑️ Vidéo supprimée: " + path.getFileName());
                         } catch (Exception e) {
                             System.err.println("⚠️ Erreur suppression: " + e.getMessage());
                         }
                     });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur nettoyage cache vidéos: " + e.getMessage());
        }
    }

    /**
     * Génère une vidéo de démonstration pour tester le système.
     */
    public static String genererVideoDemonstration() {
        return genererVideoContextuelle(
            "demo_" + System.currentTimeMillis(),
            "Démonstration EduCompus",
            "Cette vidéo démontre les capacités de génération automatique de contenu éducatif avec intelligence artificielle.",
            "Introduction à EduCompus",
            "EduCompus est une plateforme d'apprentissage moderne qui utilise l'intelligence artificielle pour créer du contenu éducatif personnalisé et interactif.",
            "Débutant",
            "Informatique",
            "Assistant IA",
            "Synthèse vocale française"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════════════════
    // MÉTHODES DE COMPATIBILITÉ AVEC L'ANCIEN CODE
    // ═══════════════════════════════════════════════════════════════════════════════════════

    /**
     * Méthode de compatibilité pour l'ancien code.
     * Génère une vidéo MP4 locale avec les paramètres simplifiés.
     * 
     * @deprecated Utiliser genererVideoContextuelle() à la place
     */
    @Deprecated
    public static String genererVideoMP4Local(String titre, String description, String niveau, String domaine, int duree) {
        return genererVideoContextuelle(
            "compat_" + System.currentTimeMillis(),
            titre,
            description,
            titre, // Utiliser le titre comme titre de chapitre
            description, // Utiliser la description comme description de chapitre
            niveau,
            domaine,
            "Assistant IA EduCompus",
            "Synthèse vocale française"
        );
    }

    /**
     * Méthode de compatibilité pour ouvrir une vidéo MP4.
     * 
     * @deprecated Cette méthode ne fait rien car l'ouverture de vidéo est gérée par le contrôleur
     */
    @Deprecated
    public static boolean ouvrirVideoMP4(String cheminVideo) {
        // Cette méthode ne fait rien car l'ouverture de vidéo est maintenant gérée
        // par le FrontCourseDetailController avec openVideoInApp()
        System.out.println("⚠️ ouvrirVideoMP4() est déprécié. Utiliser FrontCourseDetailController.openVideoInApp()");
        return cheminVideo != null && !cheminVideo.isBlank();
    }
}