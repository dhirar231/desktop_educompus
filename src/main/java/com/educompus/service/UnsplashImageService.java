package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour télécharger des images contextuelles depuis Unsplash API.
 * Fournit des images réelles basées sur les mots-clés extraits du contenu éducatif.
 */
public final class UnsplashImageService {

    private static final String IMAGES_DIRECTORY = "videos/images/";
    private static final String TEMP_DIRECTORY = "videos/temp/";
    
    // Clé API Unsplash (remplacer par une vraie clé en production)
    private static final String UNSPLASH_ACCESS_KEY = "DEMO_KEY";
    private static final String UNSPLASH_API_URL = "https://api.unsplash.com/search/photos";
    
    // Images de fallback intégrées (base64 ou URLs publiques)
    private static final Map<String, String> FALLBACK_IMAGES = Map.of(
        "education", "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800",
        "learning", "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800",
        "technology", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800",
        "science", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800",
        "mathematics", "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800"
    );
    
    private UnsplashImageService() {}

    /**
     * Télécharge des images contextuelles basées sur les mots-clés.
     * 
     * @param motsCles Liste des mots-clés pour la recherche d'images
     * @param videoId Identifiant unique pour nommer les fichiers
     * @return Liste des chemins vers les images téléchargées
     */
    public static List<String> telechargerImagesContextuelles(List<String> motsCles, String videoId) {
        try {
            System.out.println("🖼️ Démarrage téléchargement images contextuelles...");
            
            // Créer les répertoires si nécessaires
            Files.createDirectories(Paths.get(IMAGES_DIRECTORY));
            Files.createDirectories(Paths.get(TEMP_DIRECTORY));
            
            List<String> cheminsImages = new ArrayList<>();
            
            // Limiter à 3-5 images pour éviter les vidéos trop lourdes
            int maxImages = Math.min(5, Math.max(3, motsCles.size()));
            
            for (int i = 0; i < maxImages && i < motsCles.size(); i++) {
                String motCle = motsCles.get(i);
                String cheminImage = telechargerImagePourMotCle(motCle, videoId, i);
                
                if (cheminImage != null) {
                    cheminsImages.add(cheminImage);
                    System.out.println("✅ Image téléchargée pour '" + motCle + "': " + cheminImage);
                } else {
                    System.out.println("⚠️ Échec téléchargement pour '" + motCle + "', utilisation fallback");
                    String fallbackImage = obtenirImageFallback(motCle, videoId, i);
                    if (fallbackImage != null) {
                        cheminsImages.add(fallbackImage);
                    }
                }
            }
            
            // S'assurer d'avoir au moins une image
            if (cheminsImages.isEmpty()) {
                System.out.println("🔄 Aucune image téléchargée, création d'images par défaut...");
                cheminsImages.addAll(creerImagesParDefaut(videoId));
            }
            
            System.out.println("✅ " + cheminsImages.size() + " images prêtes pour la vidéo");
            return cheminsImages;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur téléchargement images: " + e.getMessage());
            // Fallback vers images par défaut
            return creerImagesParDefaut(videoId);
        }
    }

    /**
     * Télécharge une image pour un mot-clé spécifique.
     */
    private static String telechargerImagePourMotCle(String motCle, String videoId, int index) {
        try {
            // Nettoyer le mot-clé pour la recherche
            String motCleNettoye = nettoyerMotClePourRecherche(motCle);
            
            // Essayer d'abord avec l'API Unsplash (si clé disponible)
            if (!UNSPLASH_ACCESS_KEY.equals("DEMO_KEY")) {
                String urlImage = rechercherImageUnsplash(motCleNettoye);
                if (urlImage != null) {
                    return telechargerImageDepuisUrl(urlImage, videoId, index, motCleNettoye);
                }
            }
            
            // Fallback vers images publiques Unsplash (sans API)
            String urlFallback = obtenirUrlImagePublique(motCleNettoye);
            if (urlFallback != null) {
                return telechargerImageDepuisUrl(urlFallback, videoId, index, motCleNettoye);
            }
            
            return null;
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur téléchargement image pour '" + motCle + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Recherche une image via l'API Unsplash.
     */
    private static String rechercherImageUnsplash(String motCle) {
        try {
            String urlApi = UNSPLASH_API_URL + "?query=" + 
                           java.net.URLEncoder.encode(motCle, "UTF-8") + 
                           "&per_page=1&orientation=landscape";
            
            URL url = new URL(urlApi);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Client-ID " + UNSPLASH_ACCESS_KEY);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            
            if (conn.getResponseCode() == 200) {
                // Parser la réponse JSON (implémentation simplifiée)
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String response = reader.lines().collect(Collectors.joining());
                    // Extraction basique de l'URL de l'image (à améliorer avec une vraie lib JSON)
                    if (response.contains("\"urls\"") && response.contains("\"regular\"")) {
                        int start = response.indexOf("\"regular\":\"") + 11;
                        int end = response.indexOf("\"", start);
                        if (start > 10 && end > start) {
                            return response.substring(start, end).replace("\\", "");
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur API Unsplash: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Obtient une URL d'image publique Unsplash basée sur le mot-clé.
     */
    private static String obtenirUrlImagePublique(String motCle) {
        // URLs d'images Unsplash publiques par catégorie
        Map<String, String> imagesPubliques = Map.of(
            "education", "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&q=80",
            "learning", "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800&q=80",
            "technology", "https://images.unsplash.com/photo-1518709268805-4e9042af2176?w=800&q=80",
            "computer", "https://images.unsplash.com/photo-1484417894907-623942c8ee29?w=800&q=80",
            "science", "https://images.unsplash.com/photo-1532094349884-543bc11b234d?w=800&q=80",
            "mathematics", "https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80",
            "programming", "https://images.unsplash.com/photo-1461749280684-dccba630e2f6?w=800&q=80",
            "development", "https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80",
            "algorithm", "https://images.unsplash.com/photo-1509228468518-180dd4864904?w=800&q=80",
            "data", "https://images.unsplash.com/photo-1551288049-bebda4e38f71?w=800&q=80"
        );
        
        // Recherche exacte
        if (imagesPubliques.containsKey(motCle.toLowerCase())) {
            return imagesPubliques.get(motCle.toLowerCase());
        }
        
        // Recherche par similarité
        for (Map.Entry<String, String> entry : imagesPubliques.entrySet()) {
            if (motCle.toLowerCase().contains(entry.getKey()) || 
                entry.getKey().contains(motCle.toLowerCase())) {
                return entry.getValue();
            }
        }
        
        // Image par défaut éducative
        return "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&q=80";
    }

    /**
     * Télécharge une image depuis une URL.
     */
    private static String telechargerImageDepuisUrl(String urlImage, String videoId, int index, String motCle) {
        try {
            URL url = new URL(urlImage);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "EduCompus-VideoGenerator/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            
            if (conn.getResponseCode() == 200) {
                String nomFichier = String.format("image_%s_%d_%s.jpg", 
                                                videoId, index, 
                                                motCle.replaceAll("[^a-zA-Z0-9]", "_"));
                Path cheminImage = Paths.get(IMAGES_DIRECTORY + nomFichier);
                
                try (InputStream in = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(cheminImage)) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                
                // Vérifier que l'image a été téléchargée correctement
                if (Files.exists(cheminImage) && Files.size(cheminImage) > 1000) {
                    return cheminImage.toAbsolutePath().toString();
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur téléchargement depuis URL: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Obtient une image de fallback locale.
     */
    private static String obtenirImageFallback(String motCle, String videoId, int index) {
        try {
            // Utiliser une image de fallback depuis les ressources ou créer une image simple
            return creerImageSimple(motCle, videoId, index);
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur création image fallback: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crée une image simple avec du texte (fallback ultime).
     */
    private static String creerImageSimple(String motCle, String videoId, int index) {
        try {
            // Créer une image SVG simple avec le mot-clé
            String nomFichier = String.format("fallback_%s_%d_%s.svg", 
                                            videoId, index, 
                                            motCle.replaceAll("[^a-zA-Z0-9]", "_"));
            Path cheminImage = Paths.get(IMAGES_DIRECTORY + nomFichier);
            
            String contenuSVG = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <svg width="800" height="600" xmlns="http://www.w3.org/2000/svg">
                    <defs>
                        <linearGradient id="grad1" x1="0%%" y1="0%%" x2="100%%" y2="100%%">
                            <stop offset="0%%" style="stop-color:#1e40af;stop-opacity:1" />
                            <stop offset="100%%" style="stop-color:#3b82f6;stop-opacity:1" />
                        </linearGradient>
                    </defs>
                    <rect width="800" height="600" fill="url(#grad1)" />
                    <text x="400" y="280" font-family="Arial, sans-serif" font-size="48" 
                          font-weight="bold" text-anchor="middle" fill="white">
                        🎓 EduCompus
                    </text>
                    <text x="400" y="340" font-family="Arial, sans-serif" font-size="32" 
                          text-anchor="middle" fill="white" opacity="0.9">
                        %s
                    </text>
                    <text x="400" y="380" font-family="Arial, sans-serif" font-size="18" 
                          text-anchor="middle" fill="white" opacity="0.7">
                        Contenu éducatif généré par IA
                    </text>
                </svg>
                """, capitaliserMotCle(motCle));
            
            Files.write(cheminImage, contenuSVG.getBytes());
            
            if (Files.exists(cheminImage)) {
                return cheminImage.toAbsolutePath().toString();
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur création image SVG: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Crée des images par défaut quand aucune image n'a pu être téléchargée.
     */
    private static List<String> creerImagesParDefaut(String videoId) {
        List<String> images = new ArrayList<>();
        
        try {
            // Créer 3 images par défaut avec des thèmes éducatifs
            String[] themes = {"Apprentissage", "Éducation", "Formation"};
            String[] couleurs = {"#1e40af", "#059669", "#7c3aed"};
            
            for (int i = 0; i < themes.length; i++) {
                String nomFichier = String.format("default_%s_%d.svg", videoId, i);
                Path cheminImage = Paths.get(IMAGES_DIRECTORY + nomFichier);
                
                String contenuSVG = String.format("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <svg width="800" height="600" xmlns="http://www.w3.org/2000/svg">
                        <rect width="800" height="600" fill="%s" />
                        <circle cx="400" cy="200" r="80" fill="white" opacity="0.2" />
                        <text x="400" y="280" font-family="Arial, sans-serif" font-size="42" 
                              font-weight="bold" text-anchor="middle" fill="white">
                            🎓 %s
                        </text>
                        <text x="400" y="320" font-family="Arial, sans-serif" font-size="24" 
                              text-anchor="middle" fill="white" opacity="0.8">
                            EduCompus IA
                        </text>
                        <text x="400" y="380" font-family="Arial, sans-serif" font-size="16" 
                              text-anchor="middle" fill="white" opacity="0.6">
                            Plateforme d'apprentissage intelligente
                        </text>
                    </svg>
                    """, couleurs[i], themes[i]);
                
                Files.write(cheminImage, contenuSVG.getBytes());
                
                if (Files.exists(cheminImage)) {
                    images.add(cheminImage.toAbsolutePath().toString());
                }
            }
            
        } catch (Exception e) {
            System.err.println("⚠️ Erreur création images par défaut: " + e.getMessage());
        }
        
        return images;
    }

    /**
     * Nettoie un mot-clé pour la recherche d'images.
     */
    private static String nettoyerMotClePourRecherche(String motCle) {
        if (motCle == null || motCle.isBlank()) {
            return "education";
        }
        
        // Supprimer les accents et caractères spéciaux
        String nettoye = motCle.toLowerCase()
                               .replace("é", "e")
                               .replace("è", "e")
                               .replace("à", "a")
                               .replace("ç", "c")
                               .replaceAll("[^a-z0-9]", "");
        
        // Traductions français -> anglais pour améliorer les résultats Unsplash
        Map<String, String> traductions = Map.of(
            "apprentissage", "learning",
            "enseignement", "teaching",
            "formation", "education",
            "cours", "course",
            "etude", "study",
            "recherche", "research",
            "developpement", "development",
            "programmation", "programming",
            "informatique", "computer",
            "mathematiques", "mathematics"
        );
        
        return traductions.getOrDefault(nettoye, nettoye);
    }

    /**
     * Capitalise la première lettre d'un mot-clé.
     */
    private static String capitaliserMotCle(String motCle) {
        if (motCle == null || motCle.isEmpty()) {
            return "Éducation";
        }
        return motCle.substring(0, 1).toUpperCase() + motCle.substring(1).toLowerCase();
    }

    /**
     * Nettoie le cache d'images (supprime les anciennes images).
     */
    public static void nettoyerCache() {
        try {
            Path dossierImages = Paths.get(IMAGES_DIRECTORY);
            if (Files.exists(dossierImages)) {
                Files.walk(dossierImages)
                     .filter(Files::isRegularFile)
                     .filter(path -> {
                         try {
                             // Supprimer les fichiers de plus de 7 jours
                             return Files.getLastModifiedTime(path).toMillis() < 
                                    System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
                         } catch (Exception e) {
                             return false;
                         }
                     })
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                             System.out.println("🗑️ Image supprimée: " + path.getFileName());
                         } catch (Exception e) {
                             System.err.println("⚠️ Erreur suppression: " + e.getMessage());
                         }
                     });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur nettoyage cache: " + e.getMessage());
        }
    }

    /**
     * Obtient des statistiques sur le cache d'images.
     */
    public static String obtenirStatistiques() {
        try {
            Path dossierImages = Paths.get(IMAGES_DIRECTORY);
            if (!Files.exists(dossierImages)) {
                return "📁 Aucun cache d'images";
            }
            
            long nbImages = Files.walk(dossierImages)
                                .filter(Files::isRegularFile)
                                .count();
            
            long tailleTotal = Files.walk(dossierImages)
                                  .filter(Files::isRegularFile)
                                  .mapToLong(path -> {
                                      try {
                                          return Files.size(path);
                                      } catch (Exception e) {
                                          return 0;
                                      }
                                  })
                                  .sum();
            
            return String.format("🖼️ %d image(s) | Taille totale: %s | Dossier: %s", 
                               nbImages, formatTaille(tailleTotal), IMAGES_DIRECTORY);
            
        } catch (Exception e) {
            return "⚠️ Erreur lecture statistiques: " + e.getMessage();
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
}