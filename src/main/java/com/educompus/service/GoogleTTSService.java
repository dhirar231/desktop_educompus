package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Service de conversion texte-vers-parole (TTS) utilisant Google Cloud Text-to-Speech API.
 * Génère des fichiers audio MP3 à partir de texte.
 */
public final class GoogleTTSService {

    // Configuration Google Cloud TTS
    private static final String TTS_API_URL = "https://texttospeech.googleapis.com/v1/text:synthesize";
    private static final String API_KEY = "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo"; // Votre clé Gemini fonctionne aussi pour TTS
    private static final String AUDIO_DIRECTORY = "audio/generated/";

    // Voix disponibles
    public enum VoixFrancaise {
        FEMALE_A("fr-FR-Standard-A", "Féminine standard"),
        FEMALE_C("fr-FR-Standard-C", "Féminine claire"),
        FEMALE_E("fr-FR-Standard-E", "Féminine douce"),
        MALE_B("fr-FR-Standard-B", "Masculine standard"),
        MALE_D("fr-FR-Standard-D", "Masculine grave"),
        NEURAL_FEMALE("fr-FR-Neural2-A", "Féminine neurale (premium)"),
        NEURAL_MALE("fr-FR-Neural2-B", "Masculine neurale (premium)");

        private final String code;
        private final String description;

        VoixFrancaise(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // Paramètres de synthèse
    public static class ParametresTTS {
        private VoixFrancaise voix = VoixFrancaise.FEMALE_A;
        private double vitesse = 1.0; // 0.25 à 4.0
        private double pitch = 0.0;   // -20.0 à 20.0
        private double volumeGain = 0.0; // -96.0 à 16.0 dB
        private String format = "MP3"; // MP3, WAV, OGG

        public VoixFrancaise getVoix() { return voix; }
        public void setVoix(VoixFrancaise voix) { this.voix = voix; }

        public double getVitesse() { return vitesse; }
        public void setVitesse(double vitesse) { 
            if (vitesse < 0.25 || vitesse > 4.0) {
                throw new IllegalArgumentException("La vitesse doit être entre 0.25 et 4.0");
            }
            this.vitesse = vitesse; 
        }

        public double getPitch() { return pitch; }
        public void setPitch(double pitch) { 
            if (pitch < -20.0 || pitch > 20.0) {
                throw new IllegalArgumentException("Le pitch doit être entre -20.0 et 20.0");
            }
            this.pitch = pitch; 
        }

        public double getVolumeGain() { return volumeGain; }
        public void setVolumeGain(double volumeGain) { 
            if (volumeGain < -96.0 || volumeGain > 16.0) {
                throw new IllegalArgumentException("Le volume doit être entre -96.0 et 16.0 dB");
            }
            this.volumeGain = volumeGain; 
        }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        @Override
        public String toString() {
            return String.format("TTS{voix=%s, vitesse=%.1f, pitch=%.1f, volume=%.1fdB}", 
                    voix.getDescription(), vitesse, pitch, volumeGain);
        }
    }

    /**
     * Résultat de la synthèse TTS.
     */
    public static class ResultatTTS {
        private final boolean succes;
        private final String cheminFichier;
        private final String messageErreur;
        private final long tailleFichier;
        private final double dureeEstimee;

        private ResultatTTS(boolean succes, String cheminFichier, String messageErreur, long tailleFichier, double dureeEstimee) {
            this.succes = succes;
            this.cheminFichier = cheminFichier;
            this.messageErreur = messageErreur;
            this.tailleFichier = tailleFichier;
            this.dureeEstimee = dureeEstimee;
        }

        public static ResultatTTS succes(String cheminFichier, long tailleFichier, double dureeEstimee) {
            return new ResultatTTS(true, cheminFichier, null, tailleFichier, dureeEstimee);
        }

        public static ResultatTTS erreur(String messageErreur) {
            return new ResultatTTS(false, null, messageErreur, 0, 0);
        }

        public boolean isSucces() { return succes; }
        public String getCheminFichier() { return cheminFichier; }
        public String getMessageErreur() { return messageErreur; }
        public long getTailleFichier() { return tailleFichier; }
        public double getDureeEstimee() { return dureeEstimee; }

        public String getTailleFormatee() {
            if (tailleFichier < 1024) return tailleFichier + " B";
            if (tailleFichier < 1024 * 1024) return String.format("%.1f KB", tailleFichier / 1024.0);
            return String.format("%.1f MB", tailleFichier / (1024.0 * 1024.0));
        }

        public String getDureeFormatee() {
            int minutes = (int) (dureeEstimee / 60);
            int secondes = (int) (dureeEstimee % 60);
            return String.format("%d:%02d", minutes, secondes);
        }
    }

    static {
        // Créer le répertoire audio s'il n'existe pas
        try {
            Files.createDirectories(Paths.get(AUDIO_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du répertoire audio: " + e.getMessage());
        }
    }

    /**
     * Convertit un texte en audio de manière asynchrone.
     */
    public static CompletableFuture<ResultatTTS> convertirTexteAsync(String texte, ParametresTTS parametres) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return convertirTexte(texte, parametres);
            } catch (Exception e) {
                return ResultatTTS.erreur("Erreur lors de la conversion TTS: " + e.getMessage());
            }
        });
    }

    /**
     * Convertit un texte en audio.
     */
    public static ResultatTTS convertirTexte(String texte, ParametresTTS parametres) {
        try {
            // Validation
            if (texte == null || texte.trim().isEmpty()) {
                return ResultatTTS.erreur("Le texte ne peut pas être vide");
            }

            if (texte.length() > 5000) {
                return ResultatTTS.erreur("Le texte est trop long (max 5000 caractères)");
            }

            // Nettoyer le texte
            String texteNettoye = nettoyerTexte(texte);

            // Construire la requête JSON
            String jsonRequest = construireRequeteJSON(texteNettoye, parametres);

            // Appeler l'API Google TTS
            byte[] audioData = appellerAPITTS(jsonRequest);

            // Sauvegarder le fichier audio
            String cheminFichier = sauvegarderAudio(audioData, parametres.getFormat());

            // Calculer les métadonnées
            long tailleFichier = audioData.length;
            double dureeEstimee = estimerDureeAudio(texteNettoye, parametres.getVitesse());

            return ResultatTTS.succes(cheminFichier, tailleFichier, dureeEstimee);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultatTTS.erreur("Erreur TTS: " + e.getMessage());
        }
    }

    /**
     * Nettoie le texte pour la synthèse vocale.
     */
    private static String nettoyerTexte(String texte) {
        return texte
                // Supprimer les balises markdown
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1") // **gras**
                .replaceAll("\\*(.*?)\\*", "$1")       // *italique*
                .replaceAll("\\[.*?\\]", "")           // [INTRODUCTION]
                .replaceAll("#{1,6}\\s*", "")          // # Titres
                
                // Remplacer les abréviations
                .replace("etc.", "et cetera")
                .replace("ex.", "exemple")
                .replace("cf.", "voir")
                .replace("vs.", "versus")
                
                // Améliorer la ponctuation pour la voix
                .replace(":", " : ")
                .replace(";", " ; ")
                .replace("!", " ! ")
                .replace("?", " ? ")
                
                // Nettoyer les espaces
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Construit la requête JSON pour l'API Google TTS.
     */
    private static String construireRequeteJSON(String texte, ParametresTTS parametres) {
        return String.format("""
            {
                "input": {
                    "text": "%s"
                },
                "voice": {
                    "languageCode": "fr-FR",
                    "name": "%s",
                    "ssmlGender": "%s"
                },
                "audioConfig": {
                    "audioEncoding": "%s",
                    "speakingRate": %.2f,
                    "pitch": %.2f,
                    "volumeGainDb": %.2f
                }
            }
            """,
            echapperJSON(texte),
            parametres.getVoix().getCode(),
            parametres.getVoix().getCode().contains("A") || parametres.getVoix().getCode().contains("C") || parametres.getVoix().getCode().contains("E") ? "FEMALE" : "MALE",
            parametres.getFormat(),
            parametres.getVitesse(),
            parametres.getPitch(),
            parametres.getVolumeGain()
        );
    }

    /**
     * Appelle l'API Google Text-to-Speech.
     */
    private static byte[] appellerAPITTS(String jsonRequest) throws IOException {
        URL url = new URL(TTS_API_URL + "?key=" + API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // Envoyer la requête
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        }

        // Lire la réponse
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorMsg = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorMsg.append(line);
                }
            }
            throw new IOException("Erreur API TTS (" + responseCode + "): " + errorMsg.toString());
        }

        // Lire la réponse JSON
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        // Extraire l'audio encodé en base64
        String jsonResponse = response.toString();
        String audioContentKey = "\"audioContent\":\"";
        int start = jsonResponse.indexOf(audioContentKey);
        if (start == -1) {
            throw new IOException("Réponse TTS invalide: pas d'audioContent");
        }

        start += audioContentKey.length();
        int end = jsonResponse.indexOf("\"", start);
        if (end == -1) {
            throw new IOException("Réponse TTS invalide: fin d'audioContent introuvable");
        }

        String base64Audio = jsonResponse.substring(start, end);
        return Base64.getDecoder().decode(base64Audio);
    }

    /**
     * Sauvegarde les données audio dans un fichier.
     */
    private static String sauvegarderAudio(byte[] audioData, String format) throws IOException {
        String extension = format.toLowerCase();
        String nomFichier = "tts_" + System.currentTimeMillis() + "." + extension;
        Path cheminFichier = Paths.get(AUDIO_DIRECTORY, nomFichier);

        Files.write(cheminFichier, audioData);
        
        System.out.println("Audio sauvegardé: " + cheminFichier.toAbsolutePath());
        return cheminFichier.toString();
    }

    /**
     * Estime la durée audio en secondes.
     */
    private static double estimerDureeAudio(String texte, double vitesse) {
        // Estimation: ~150 mots par minute en français à vitesse normale
        int nombreMots = texte.split("\\s+").length;
        double dureeMinutes = nombreMots / 150.0;
        return (dureeMinutes * 60.0) / vitesse; // Ajuster selon la vitesse
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private static String echapperJSON(String texte) {
        if (texte == null) return "";
        return texte.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Convertit un script de chapitre en audio avec paramètres optimisés.
     */
    public static CompletableFuture<ResultatTTS> convertirScriptChapitreAsync(String script) {
        ParametresTTS parametres = new ParametresTTS();
        parametres.setVoix(VoixFrancaise.FEMALE_A);
        parametres.setVitesse(0.9); // Légèrement plus lent pour l'éducation
        parametres.setPitch(0.0);
        parametres.setVolumeGain(2.0); // Légèrement plus fort
        
        return convertirTexteAsync(script, parametres);
    }

    /**
     * Teste la configuration TTS.
     */
    public static ResultatTTS testerConfiguration() {
        String texteTest = "Bonjour ! Ceci est un test de la synthèse vocale Google. Si vous entendez ce message, la configuration fonctionne correctement.";
        
        ParametresTTS parametres = new ParametresTTS();
        parametres.setVoix(VoixFrancaise.FEMALE_A);
        
        return convertirTexte(texteTest, parametres);
    }

    /**
     * Obtient la liste des voix disponibles avec leurs descriptions.
     */
    public static String[] getVoixDisponibles() {
        VoixFrancaise[] voix = VoixFrancaise.values();
        String[] descriptions = new String[voix.length];
        for (int i = 0; i < voix.length; i++) {
            descriptions[i] = voix[i].getDescription();
        }
        return descriptions;
    }
}