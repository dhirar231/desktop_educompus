package com.educompus.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Service pour générer de l'audio avec synthèse vocale (Text-to-Speech).
 * Supporte Windows SAPI, espeak (Linux), et say (macOS).
 */
public final class TextToSpeechService {

    private static final String AUDIO_DIRECTORY = "videos/audio/";
    private static final String TEMP_DIRECTORY = "videos/temp/";
    
    private TextToSpeechService() {}

    /**
     * Génère un fichier audio à partir d'un texte avec synthèse vocale.
     * 
     * @param texte Le texte à convertir en audio
     * @param audioId Identifiant unique pour nommer le fichier
     * @param langue Code de langue (fr, en, etc.)
     * @param voix Type de voix (optionnel)
     * @return Chemin vers le fichier audio généré, ou null en cas d'échec
     */
    public static String genererAudio(String texte, String audioId, String langue, String voix) {
        try {
            System.out.println("🔊 Démarrage génération audio TTS...");
            
            // Créer les répertoires si nécessaires
            Files.createDirectories(Paths.get(AUDIO_DIRECTORY));
            Files.createDirectories(Paths.get(TEMP_DIRECTORY));
            
            // Nettoyer le texte pour la synthèse vocale
            String texteNettoye = nettoyerTextePourTTS(texte);
            
            // Nom du fichier audio
            String nomFichier = String.format("audio_%s.wav", audioId);
            Path cheminAudio = Paths.get(AUDIO_DIRECTORY + nomFichier);
            
            // Essayer différentes méthodes selon le système d'exploitation
            boolean audioGenere = false;
            
            // Méthode 1 : Windows SAPI (PowerShell)
            if (!audioGenere && isWindows()) {
                audioGenere = genererAudioAvecSAPI(texteNettoye, cheminAudio, langue, voix);
            }
            
            // Méthode 2 : espeak (Linux/Windows avec espeak installé)
            if (!audioGenere) {
                audioGenere = genererAudioAvecEspeak(texteNettoye, cheminAudio, langue);
            }
            
            // Méthode 3 : say (macOS)
            if (!audioGenere && isMacOS()) {
                audioGenere = genererAudioAvecSay(texteNettoye, cheminAudio, langue);
            }
            
            // Méthode 4 : Fallback - audio de silence
            if (!audioGenere) {
                audioGenere = genererAudioSilence(cheminAudio, calculerDureeAudio(texteNettoye));
            }
            
            if (audioGenere && Files.exists(cheminAudio) && Files.size(cheminAudio) > 1000) {
                System.out.println("✅ Audio généré: " + nomFichier + " (" + formatTaille(Files.size(cheminAudio)) + ")");
                return cheminAudio.toAbsolutePath().toString();
            } else {
                System.err.println("❌ Impossible de générer l'audio TTS");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur génération audio TTS: " + e.getMessage());
            return null;
        }
    }

    /**
     * Génère un fichier audio avec Windows SAPI via PowerShell.
     */
    private static boolean genererAudioAvecSAPI(String texte, Path cheminAudio, String langue, String voix) {
        try {
            System.out.println("🔊 Génération audio avec Windows SAPI...");
            
            // Script PowerShell pour utiliser SAPI
            String scriptPS = String.format("""
                Add-Type -AssemblyName System.Speech
                $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
                
                # Sélectionner une voix française si disponible
                $voixFrancaise = $synth.GetInstalledVoices() | Where-Object {$_.VoiceInfo.Culture.Name -like "fr*"} | Select-Object -First 1
                if ($voixFrancaise) {
                    $synth.SelectVoice($voixFrancaise.VoiceInfo.Name)
                    Write-Host "Voix sélectionnée: " $voixFrancaise.VoiceInfo.Name
                } else {
                    Write-Host "Aucune voix française trouvée, utilisation de la voix par défaut"
                }
                
                # Configuration de la synthèse
                $synth.Rate = 0      # Vitesse normale
                $synth.Volume = 100  # Volume maximum
                
                # Génération du fichier audio
                $synth.SetOutputToWaveFile('%s')
                $synth.Speak('%s')
                $synth.Dispose()
                
                Write-Host "Audio généré avec succès"
                """, 
                cheminAudio.toString().replace("\\", "\\\\"), 
                texte.replace("'", "''").replace("\"", "\"\""));
            
            // Écrire le script dans un fichier temporaire
            Path scriptPath = Paths.get(TEMP_DIRECTORY + "tts_script_" + System.currentTimeMillis() + ".ps1");
            Files.write(scriptPath, scriptPS.getBytes("UTF-8"));
            
            // Exécuter le script PowerShell
            ProcessBuilder psCmd = new ProcessBuilder(
                "powershell", "-ExecutionPolicy", "Bypass", "-File", scriptPath.toString()
            );
            psCmd.redirectErrorStream(true);
            
            Process psProcess = psCmd.start();
            
            // Lire la sortie pour le débogage
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(psProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("SAPI: " + line);
                }
            }
            
            boolean finished = psProcess.waitFor(30, TimeUnit.SECONDS);
            int exitCode = finished ? psProcess.exitValue() : -1;
            
            // Nettoyer le script temporaire
            try {
                Files.deleteIfExists(scriptPath);
            } catch (Exception ignored) {}
            
            if (finished && exitCode == 0 && Files.exists(cheminAudio) && Files.size(cheminAudio) > 1000) {
                System.out.println("✅ Audio généré avec Windows SAPI");
                return true;
            } else {
                System.out.println("⚠️ Échec SAPI (code: " + exitCode + ", finished: " + finished + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ SAPI non disponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère un fichier audio avec espeak.
     */
    private static boolean genererAudioAvecEspeak(String texte, Path cheminAudio, String langue) {
        try {
            System.out.println("🔊 Génération audio avec espeak...");
            
            // Calculer la vitesse de parole appropriée
            int vitesse = calculerVitesseParole(texte);
            
            // Mapper les codes de langue
            String voixEspeak = mapperLangueEspeak(langue);
            
            ProcessBuilder espeakCmd = new ProcessBuilder(
                "espeak", 
                "-v", voixEspeak,           // Voix
                "-s", String.valueOf(vitesse), // Vitesse de parole
                "-a", "100",                // Amplitude (volume)
                "-g", "10",                 // Gap entre mots
                "-w", cheminAudio.toString(), // Fichier de sortie WAV
                texte
            );
            
            espeakCmd.redirectErrorStream(true);
            Process espeakProcess = espeakCmd.start();
            
            // Lire la sortie pour le débogage
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(espeakProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("espeak: " + line);
                }
            }
            
            boolean finished = espeakProcess.waitFor(20, TimeUnit.SECONDS);
            int exitCode = finished ? espeakProcess.exitValue() : -1;
            
            if (finished && exitCode == 0 && Files.exists(cheminAudio) && Files.size(cheminAudio) > 1000) {
                System.out.println("✅ Audio généré avec espeak");
                return true;
            } else {
                System.out.println("⚠️ Échec espeak (code: " + exitCode + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ espeak non disponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère un fichier audio avec say (macOS).
     */
    private static boolean genererAudioAvecSay(String texte, Path cheminAudio, String langue) {
        try {
            System.out.println("🔊 Génération audio avec say (macOS)...");
            
            // Sélectionner une voix appropriée
            String voixSay = mapperLangueSay(langue);
            
            ProcessBuilder sayCmd = new ProcessBuilder(
                "say", 
                "-v", voixSay,              // Voix
                "-r", "180",                // Vitesse de parole (mots par minute)
                "-o", cheminAudio.toString(), // Fichier de sortie
                "--data-format=LEI16@22050", // Format WAV
                texte
            );
            
            sayCmd.redirectErrorStream(true);
            Process sayProcess = sayCmd.start();
            
            boolean finished = sayProcess.waitFor(20, TimeUnit.SECONDS);
            int exitCode = finished ? sayProcess.exitValue() : -1;
            
            if (finished && exitCode == 0 && Files.exists(cheminAudio) && Files.size(cheminAudio) > 1000) {
                System.out.println("✅ Audio généré avec say (macOS)");
                return true;
            } else {
                System.out.println("⚠️ Échec say (code: " + exitCode + ")");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ say non disponible: " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère un fichier audio de silence comme fallback.
     */
    private static boolean genererAudioSilence(Path cheminAudio, int dureeSecondes) {
        try {
            System.out.println("🔇 Génération audio de silence (fallback)...");
            
            ProcessBuilder silenceCmd = new ProcessBuilder(
                "ffmpeg", "-y",
                "-f", "lavfi",
                "-i", String.format("anullsrc=channel_layout=stereo:sample_rate=44100:duration=%d", dureeSecondes),
                "-c:a", "pcm_s16le",
                cheminAudio.toString()
            );
            
            silenceCmd.redirectErrorStream(true);
            Process silenceProcess = silenceCmd.start();
            
            boolean finished = silenceProcess.waitFor(10, TimeUnit.SECONDS);
            int exitCode = finished ? silenceProcess.exitValue() : -1;
            
            if (finished && exitCode == 0 && Files.exists(cheminAudio)) {
                System.out.println("✅ Audio de silence généré");
                return true;
            } else {
                System.out.println("⚠️ Impossible de générer l'audio de silence");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("⚠️ Erreur génération silence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Nettoie le texte pour la synthèse vocale.
     */
    private static String nettoyerTextePourTTS(String texte) {
        if (texte == null || texte.isBlank()) {
            return "Bienvenue dans cette vidéo éducative EduCompus générée par intelligence artificielle.";
        }
        
        // Supprimer les émojis et caractères non-ASCII problématiques
        String nettoye = texte.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", " ");
        
        // Remplacer les caractères problématiques pour TTS
        nettoye = nettoye.replace("\\n", ". ")
                        .replace("\\", " ")
                        .replace("\"", " ")
                        .replace("'", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
        
        // Améliorer la prononciation
        nettoye = nettoye.replace("IA", "intelligence artificielle")
                        .replace("AI", "intelligence artificielle")
                        .replace("TTS", "synthèse vocale")
                        .replace("API", "interface de programmation")
                        .replace("URL", "adresse web")
                        .replace("HTML", "langage de balisage")
                        .replace("CSS", "feuilles de style")
                        .replace("JS", "JavaScript");
        
        // Limiter la longueur pour éviter les textes trop longs
        if (nettoye.length() > 800) {
            nettoye = nettoye.substring(0, 800);
            // Couper à la dernière phrase complète
            int dernierPoint = nettoye.lastIndexOf('.');
            if (dernierPoint > 400) {
                nettoye = nettoye.substring(0, dernierPoint + 1);
            }
        }
        
        // S'assurer qu'il y a du contenu
        if (nettoye.isBlank()) {
            nettoye = "Bienvenue dans cette vidéo éducative EduCompus générée par intelligence artificielle.";
        }
        
        // Ajouter des pauses pour une meilleure élocution
        nettoye = nettoye.replace(". ", ". ... ")
                        .replace("! ", "! ... ")
                        .replace("? ", "? ... ");
        
        return nettoye;
    }

    /**
     * Calcule la vitesse de parole appropriée selon la longueur du texte.
     */
    private static int calculerVitesseParole(String texte) {
        int longueur = texte.length();
        
        if (longueur < 100) return 160;      // Lent pour textes courts
        if (longueur < 300) return 180;      // Normal
        if (longueur < 500) return 200;      // Un peu plus rapide
        return 220;                          // Rapide pour longs textes
    }

    /**
     * Calcule la durée approximative de l'audio basée sur le texte.
     */
    private static int calculerDureeAudio(String texte) {
        // Estimation : environ 150 mots par minute, 5 caractères par mot
        int nbCaracteres = texte.length();
        int nbMots = nbCaracteres / 5;
        int dureeMinutes = Math.max(1, nbMots / 150);
        return dureeMinutes * 60; // Convertir en secondes
    }

    /**
     * Mappe un code de langue vers une voix espeak.
     */
    private static String mapperLangueEspeak(String langue) {
        if (langue == null) return "fr";
        
        return switch (langue.toLowerCase()) {
            case "fr", "french", "français" -> "fr";
            case "en", "english", "anglais" -> "en";
            case "es", "spanish", "espagnol" -> "es";
            case "de", "german", "allemand" -> "de";
            case "it", "italian", "italien" -> "it";
            default -> "fr"; // Français par défaut
        };
    }

    /**
     * Mappe un code de langue vers une voix say (macOS).
     */
    private static String mapperLangueSay(String langue) {
        if (langue == null) return "Thomas";
        
        return switch (langue.toLowerCase()) {
            case "fr", "french", "français" -> "Thomas";    // Voix française
            case "en", "english", "anglais" -> "Alex";      // Voix anglaise
            case "es", "spanish", "espagnol" -> "Diego";    // Voix espagnole
            case "de", "german", "allemand" -> "Anna";      // Voix allemande
            case "it", "italian", "italien" -> "Luca";      // Voix italienne
            default -> "Thomas"; // Français par défaut
        };
    }

    /**
     * Vérifie si le système est Windows.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Vérifie si le système est macOS.
     */
    private static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    /**
     * Teste la disponibilité des outils TTS sur le système.
     */
    public static String testerDisponibiliteTTS() {
        StringBuilder rapport = new StringBuilder();
        rapport.append("🔊 TEST DISPONIBILITÉ TEXT-TO-SPEECH\n\n");
        
        String os = System.getProperty("os.name");
        rapport.append("Système d'exploitation: ").append(os).append("\n\n");
        
        // Test Windows SAPI
        if (isWindows()) {
            rapport.append("🪟 Windows SAPI: ");
            try {
                ProcessBuilder test = new ProcessBuilder("powershell", "-Command", "Add-Type -AssemblyName System.Speech; Write-Host 'OK'");
                Process process = test.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    rapport.append("✅ Disponible\n");
                } else {
                    rapport.append("❌ Non disponible\n");
                }
            } catch (Exception e) {
                rapport.append("❌ Erreur: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Test espeak
        rapport.append("🔊 espeak: ");
        try {
            ProcessBuilder test = new ProcessBuilder("espeak", "--version");
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
        
        // Test say (macOS)
        if (isMacOS()) {
            rapport.append("🍎 say (macOS): ");
            try {
                ProcessBuilder test = new ProcessBuilder("say", "--version");
                Process process = test.start();
                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (finished && process.exitValue() == 0) {
                    rapport.append("✅ Disponible\n");
                } else {
                    rapport.append("❌ Non disponible\n");
                }
            } catch (Exception e) {
                rapport.append("❌ Erreur: ").append(e.getMessage()).append("\n");
            }
        }
        
        // Test FFmpeg (pour fallback silence)
        rapport.append("🎬 FFmpeg: ");
        try {
            ProcessBuilder test = new ProcessBuilder("ffmpeg", "-version");
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
        
        return rapport.toString();
    }

    /**
     * Nettoie le cache audio (supprime les anciens fichiers).
     */
    public static void nettoyerCache() {
        try {
            Path dossierAudio = Paths.get(AUDIO_DIRECTORY);
            if (Files.exists(dossierAudio)) {
                Files.walk(dossierAudio)
                     .filter(Files::isRegularFile)
                     .filter(path -> {
                         try {
                             // Supprimer les fichiers de plus de 3 jours
                             return Files.getLastModifiedTime(path).toMillis() < 
                                    System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L);
                         } catch (Exception e) {
                             return false;
                         }
                     })
                     .forEach(path -> {
                         try {
                             Files.deleteIfExists(path);
                             System.out.println("🗑️ Audio supprimé: " + path.getFileName());
                         } catch (Exception e) {
                             System.err.println("⚠️ Erreur suppression: " + e.getMessage());
                         }
                     });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur nettoyage cache audio: " + e.getMessage());
        }
    }

    /**
     * Obtient des statistiques sur le cache audio.
     */
    public static String obtenirStatistiques() {
        try {
            Path dossierAudio = Paths.get(AUDIO_DIRECTORY);
            if (!Files.exists(dossierAudio)) {
                return "📁 Aucun cache audio";
            }
            
            long nbAudios = Files.walk(dossierAudio)
                               .filter(Files::isRegularFile)
                               .count();
            
            long tailleTotal = Files.walk(dossierAudio)
                                  .filter(Files::isRegularFile)
                                  .mapToLong(path -> {
                                      try {
                                          return Files.size(path);
                                      } catch (Exception e) {
                                          return 0;
                                      }
                                  })
                                  .sum();
            
            return String.format("🔊 %d audio(s) | Taille totale: %s | Dossier: %s", 
                               nbAudios, formatTaille(tailleTotal), AUDIO_DIRECTORY);
            
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