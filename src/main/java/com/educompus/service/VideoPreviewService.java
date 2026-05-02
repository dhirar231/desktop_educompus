package com.educompus.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service pour créer des aperçus locaux des vidéos simulées.
 * Génère des fichiers HTML de démonstration pour les vidéos simulées.
 */
public final class VideoPreviewService {

    private static final String PREVIEW_DIRECTORY = "videos/previews/";
    private static final String ASSETS_DIRECTORY = "videos/assets/";
    
    private VideoPreviewService() {}

    /**
     * Crée un aperçu HTML local pour une vidéo simulée.
     */
    public static String creerApercuLocal(String videoId, String script, String avatar, String voix, int dureeSecondes) {
        try {
            // Créer les répertoires si nécessaires
            Files.createDirectories(Paths.get(PREVIEW_DIRECTORY));
            Files.createDirectories(Paths.get(ASSETS_DIRECTORY));
            
            // Générer le contenu HTML
            String htmlContent = genererContenuHTML(videoId, script, avatar, voix, dureeSecondes);
            
            // Nom du fichier
            String nomFichier = "video_preview_" + videoId + ".html";
            Path cheminFichier = Paths.get(PREVIEW_DIRECTORY + nomFichier);
            
            // Écrire le fichier
            Files.writeString(cheminFichier, htmlContent);
            
            // Créer les assets (CSS, images)
            creerAssets();
            
            // Retourner le chemin absolu
            String cheminAbsolu = cheminFichier.toAbsolutePath().toString();
            System.out.println("✅ Aperçu créé: " + cheminAbsolu);
            
            return "file:///" + cheminAbsolu.replace("\\", "/");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur création aperçu: " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère le contenu HTML de l'aperçu.
     */
    private static String genererContenuHTML(String videoId, String script, String avatar, String voix, int dureeSecondes) {
        String dateGeneration = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String dureeFormatee = String.format("%d:%02d", dureeSecondes / 60, dureeSecondes % 60);
        
        return String.format("""
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Aperçu Vidéo IA - %s</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    
                    .container {
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        max-width: 800px;
                        width: 100%%;
                        overflow: hidden;
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    
                    .header h1 {
                        font-size: 2.5em;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.3);
                    }
                    
                    .header p {
                        font-size: 1.2em;
                        opacity: 0.9;
                    }
                    
                    .video-placeholder {
                        background: linear-gradient(45deg, #f0f0f0 25%%, transparent 25%%), 
                                    linear-gradient(-45deg, #f0f0f0 25%%, transparent 25%%), 
                                    linear-gradient(45deg, transparent 75%%, #f0f0f0 75%%), 
                                    linear-gradient(-45deg, transparent 75%%, #f0f0f0 75%%);
                        background-size: 20px 20px;
                        background-position: 0 0, 0 10px, 10px -10px, -10px 0px;
                        height: 300px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        position: relative;
                        border-top: 3px solid #4CAF50;
                        border-bottom: 3px solid #4CAF50;
                    }
                    
                    .play-button {
                        width: 80px;
                        height: 80px;
                        background: #4CAF50;
                        border-radius: 50%%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                        transition: all 0.3s ease;
                        box-shadow: 0 8px 16px rgba(76, 175, 80, 0.3);
                    }
                    
                    .play-button:hover {
                        transform: scale(1.1);
                        box-shadow: 0 12px 24px rgba(76, 175, 80, 0.4);
                    }
                    
                    .play-button::after {
                        content: '';
                        width: 0;
                        height: 0;
                        border-left: 25px solid white;
                        border-top: 15px solid transparent;
                        border-bottom: 15px solid transparent;
                        margin-left: 5px;
                    }
                    
                    .info {
                        padding: 30px;
                    }
                    
                    .info-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                        margin-bottom: 30px;
                    }
                    
                    .info-card {
                        background: #f8f9fa;
                        padding: 20px;
                        border-radius: 10px;
                        border-left: 4px solid #4CAF50;
                    }
                    
                    .info-card h3 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 1.1em;
                    }
                    
                    .info-card p {
                        color: #666;
                        line-height: 1.5;
                    }
                    
                    .script-section {
                        background: #f8f9fa;
                        padding: 25px;
                        border-radius: 10px;
                        margin-top: 20px;
                    }
                    
                    .script-section h3 {
                        color: #333;
                        margin-bottom: 15px;
                        display: flex;
                        align-items: center;
                        gap: 10px;
                    }
                    
                    .script-content {
                        background: white;
                        padding: 20px;
                        border-radius: 8px;
                        border: 1px solid #e0e0e0;
                        line-height: 1.6;
                        color: #444;
                        max-height: 200px;
                        overflow-y: auto;
                    }
                    
                    .footer {
                        background: #f8f9fa;
                        padding: 20px 30px;
                        text-align: center;
                        color: #666;
                        border-top: 1px solid #e0e0e0;
                    }
                    
                    .badge {
                        display: inline-block;
                        background: #4CAF50;
                        color: white;
                        padding: 5px 12px;
                        border-radius: 20px;
                        font-size: 0.9em;
                        margin: 5px;
                    }
                    
                    .simulation-notice {
                        background: linear-gradient(135deg, #FF9800 0%%, #F57C00 100%%);
                        color: white;
                        padding: 15px;
                        text-align: center;
                        font-weight: bold;
                    }
                    
                    @keyframes pulse {
                        0%% { transform: scale(1); }
                        50%% { transform: scale(1.05); }
                        100%% { transform: scale(1); }
                    }
                    
                    .play-button {
                        animation: pulse 2s infinite;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="simulation-notice">
                        🎭 Mode Simulation - Aperçu de la vidéo générée par IA
                    </div>
                    
                    <div class="header">
                        <h1>🎬 Vidéo IA Générée</h1>
                        <p>Aperçu de votre contenu éducatif</p>
                    </div>
                    
                    <div class="video-placeholder" onclick="jouerSimulation()">
                        <div class="play-button" id="playButton"></div>
                    </div>
                    
                    <div class="info">
                        <div class="info-grid">
                            <div class="info-card">
                                <h3>👤 Avatar</h3>
                                <p>%s</p>
                            </div>
                            <div class="info-card">
                                <h3>🗣️ Voix</h3>
                                <p>%s</p>
                            </div>
                            <div class="info-card">
                                <h3>⏱️ Durée</h3>
                                <p>%s</p>
                            </div>
                            <div class="info-card">
                                <h3>📅 Généré le</h3>
                                <p>%s</p>
                            </div>
                        </div>
                        
                        <div class="script-section">
                            <h3>📝 Script généré par IA</h3>
                            <div class="script-content">
                                %s
                            </div>
                        </div>
                    </div>
                    
                    <div class="footer">
                        <div>
                            <span class="badge">🤖 IA Généré</span>
                            <span class="badge">🎭 Simulation</span>
                            <span class="badge">📱 Responsive</span>
                        </div>
                        <p style="margin-top: 15px;">
                            ID Vidéo: %s | EduCompus - Plateforme d'apprentissage IA
                        </p>
                    </div>
                </div>
                
                <script>
                    function jouerSimulation() {
                        const button = document.getElementById('playButton');
                        const placeholder = document.querySelector('.video-placeholder');
                        
                        // Animation de lecture
                        button.style.background = '#45a049';
                        button.innerHTML = '⏸️';
                        
                        // Simuler la lecture
                        placeholder.style.background = 'linear-gradient(135deg, #4CAF50 0%%, #45a049 100%%)';
                        placeholder.innerHTML = '<div style="color: white; font-size: 1.5em; text-align: center;"><div style="font-size: 3em; margin-bottom: 10px;">🎬</div>Simulation de lecture vidéo<br><small>Dans une vraie implémentation, la vidéo HeyGen s\\'afficherait ici</small></div>';
                        
                        // Retour à l'état initial après 3 secondes
                        setTimeout(() => {
                            location.reload();
                        }, 5000);
                    }
                    
                    // Animation d'entrée
                    document.addEventListener('DOMContentLoaded', function() {
                        const container = document.querySelector('.container');
                        container.style.opacity = '0';
                        container.style.transform = 'translateY(50px)';
                        
                        setTimeout(() => {
                            container.style.transition = 'all 0.8s ease';
                            container.style.opacity = '1';
                            container.style.transform = 'translateY(0)';
                        }, 100);
                    });
                </script>
            </body>
            </html>
            """, 
            videoId,
            avatar,
            voix, 
            dureeFormatee,
            dateGeneration,
            script.replace("\n", "<br>"),
            videoId
        );
    }

    /**
     * Crée les fichiers assets nécessaires.
     */
    private static void creerAssets() {
        try {
            // Créer un fichier CSS supplémentaire si nécessaire
            Path cssPath = Paths.get(ASSETS_DIRECTORY + "video-preview.css");
            if (!Files.exists(cssPath)) {
                String css = """
                    /* Styles supplémentaires pour les aperçus vidéo */
                    .video-controls {
                        display: flex;
                        justify-content: center;
                        gap: 10px;
                        margin-top: 20px;
                    }
                    
                    .control-button {
                        padding: 10px 20px;
                        border: none;
                        border-radius: 5px;
                        background: #4CAF50;
                        color: white;
                        cursor: pointer;
                        transition: background 0.3s;
                    }
                    
                    .control-button:hover {
                        background: #45a049;
                    }
                    """;
                Files.writeString(cssPath, css);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur création assets: " + e.getMessage());
        }
    }

    /**
     * Ouvre l'aperçu dans le navigateur par défaut.
     */
    public static boolean ouvrirApercuDansNavigateur(String urlApercu) {
        try {
            if (urlApercu == null || !urlApercu.startsWith("file:///")) {
                System.err.println("❌ URL d'aperçu invalide: " + urlApercu);
                return false;
            }
            
            // Commande pour ouvrir dans le navigateur selon l'OS
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                // Windows - utiliser cmd /c start
                pb = new ProcessBuilder("cmd", "/c", "start", "\"\"", urlApercu);
            } else if (os.contains("mac")) {
                // macOS
                pb = new ProcessBuilder("open", urlApercu);
            } else {
                // Linux
                pb = new ProcessBuilder("xdg-open", urlApercu);
            }
            
            Process process = pb.start();
            
            // Attendre un peu pour voir si ça marche
            Thread.sleep(1000);
            
            System.out.println("🌐 Aperçu ouvert dans le navigateur: " + urlApercu);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture navigateur: " + e.getMessage());
            System.err.println("💡 Vous pouvez copier cette URL dans votre navigateur: " + urlApercu);
            return false;
        }
    }

    /**
     * Méthode utilitaire pour ouvrir manuellement un aperçu.
     */
    public static void ouvrirApercuManuel(String videoId) {
        try {
            String nomFichier = "video_preview_" + videoId + ".html";
            String cheminFichier = PREVIEW_DIRECTORY + nomFichier;
            
            if (java.nio.file.Files.exists(java.nio.file.Paths.get(cheminFichier))) {
                String urlApercu = "file:///" + java.nio.file.Paths.get(cheminFichier).toAbsolutePath().toString().replace("\\", "/");
                ouvrirApercuDansNavigateur(urlApercu);
            } else {
                System.err.println("❌ Aperçu introuvable: " + cheminFichier);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur ouverture manuelle: " + e.getMessage());
        }
    }

    /**
     * Nettoie les anciens aperçus (plus de 24h).
     */
    public static void nettoyerAnciensApercus() {
        try {
            Path previewDir = Paths.get(PREVIEW_DIRECTORY);
            if (!Files.exists(previewDir)) {
                return;
            }
            
            Files.list(previewDir)
                .filter(path -> path.toString().endsWith(".html"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < 
                               System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24h
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("🗑️ Aperçu supprimé: " + path.getFileName());
                    } catch (Exception e) {
                        System.err.println("Erreur suppression: " + e.getMessage());
                    }
                });
                
        } catch (Exception e) {
            System.err.println("Erreur nettoyage: " + e.getMessage());
        }
    }

    /**
     * Obtient les statistiques des aperçus.
     */
    public static String obtenirStatistiques() {
        try {
            Path previewDir = Paths.get(PREVIEW_DIRECTORY);
            if (!Files.exists(previewDir)) {
                return "Aucun aperçu créé";
            }
            
            long nbApercus = Files.list(previewDir)
                .filter(path -> path.toString().endsWith(".html"))
                .count();
                
            return String.format("📊 %d aperçu(s) disponible(s) dans %s", nbApercus, PREVIEW_DIRECTORY);
            
        } catch (Exception e) {
            return "Erreur lecture statistiques: " + e.getMessage();
        }
    }
}