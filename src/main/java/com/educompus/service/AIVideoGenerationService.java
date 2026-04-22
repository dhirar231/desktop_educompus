package com.educompus.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * Service de génération de vidéos AI avec avatar parlant.
 * Utilise OpenAI GPT-3.5-Turbo pour le script et D-ID pour la génération vidéo.
 */
public final class AIVideoGenerationService {

    private static final Properties config = loadConfig();

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DID_URL = "https://api.d-id.com/talks";

    private AIVideoGenerationService() {}

    /**
     * Charge la configuration depuis le fichier ai-config.properties.
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (var input = AIVideoGenerationService.class.getResourceAsStream("/ai-config.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de charger ai-config.properties : " + e.getMessage());
        }
        return props;
    }

    /**
     * Génère une vidéo AI complète à partir d'une description.
     *
     * @param description Description du contenu à expliquer
     * @param coursTitle  Titre du cours (pour le contexte)
     * @param niveau      Niveau du cours (1er, 2eme, etc.)
     * @param domaine     Domaine du cours (Informatique, etc.)
     * @return CompletableFuture avec le résultat de la génération
     */
    public static CompletableFuture<VideoGenerationResult> generateVideo(
            String description, String coursTitle, String niveau, String domaine) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Vérifier la configuration
                String openaiKey = config.getProperty("openai.api.key", "");
                String didKey = config.getProperty("did.api.key", "");

                if (openaiKey.isBlank() || openaiKey.equals("VOTRE_CLE_OPENAI_ICI")) {
                    return VideoGenerationResult.error(
                            "Clé API OpenAI non configurée. Veuillez configurer ai-config.properties");
                }

                if (didKey.isBlank() || didKey.equals("VOTRE_CLE_DID_ICI")) {
                    return VideoGenerationResult.error(
                            "Clé API D-ID non configurée. Veuillez configurer ai-config.properties");
                }

                // Étape 1 : Générer le script pédagogique avec OpenAI
                String[] scriptResult = generateScriptWithOpenAI(description, coursTitle, niveau, domaine, openaiKey);
                String script = scriptResult[0];
                String scriptError = scriptResult[1];

                if (script == null || script.isBlank()) {
                    return VideoGenerationResult.error(
                            "Erreur génération du script : " + (scriptError != null ? scriptError : "réponse vide"));
                }

                // Étape 2 : Créer la vidéo avec D-ID
                String[] didResult = createVideoWithDID(script, didKey);
                String videoUrl = didResult[0];
                String didError  = didResult[1];
                if (videoUrl == null || videoUrl.isBlank()) {
                    return VideoGenerationResult.error("Erreur génération de la vidéo : "
                            + (didError != null ? didError : "réponse vide D-ID"));
                }

                return VideoGenerationResult.success(videoUrl, script);

            } catch (Exception e) {
                e.printStackTrace();
                return VideoGenerationResult.error("Erreur : " + e.getMessage());
            }
        });
    }

    /**
     * Génère un script pédagogique avec OpenAI GPT-3.5-Turbo.
     * Retourne un tableau [script, erreur].
     */
    private static String[] generateScriptWithOpenAI(
            String description, String coursTitle, String niveau, String domaine, String apiKey) {
        try {
            String prompt = buildEducationalPrompt(description, coursTitle, niveau, domaine);
            int timeout = Integer.parseInt(config.getProperty("api.timeout", "30000"));

            // Format de requête OpenAI
            String jsonPayload = """
                    {
                        "model": "gpt-3.5-turbo",
                        "messages": [
                            {
                                "role": "system",
                                "content": "Tu es un expert pédagogue qui crées des scripts pour des vidéos éducatives. Ton style est clair, engageant et adapté au niveau des étudiants."
                            },
                            {
                                "role": "user",
                                "content": "%s"
                            }
                        ],
                        "max_tokens": %d,
                        "temperature": 0.7
                    }
                    """.formatted(
                    escapeJson(prompt),
                    Integer.parseInt(config.getProperty("script.max.length", "1000")));

            URL url = new URL(OPENAI_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

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
                String errorDetail = errorMsg.toString();
                System.err.println("Erreur OpenAI API (" + responseCode + "): " + errorDetail);
                return new String[]{null, "HTTP " + responseCode + " - " + errorDetail};
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String script = parseOpenAIResponse(response.toString());
            return new String[]{script, null};

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{null, e.getMessage()};
        }
    }

    /**
     * Parse la réponse de l'API OpenAI pour extraire le texte du script.
     */
    private static String parseOpenAIResponse(String jsonResponse) {
        try {
            System.out.println("Réponse OpenAI brute : " + jsonResponse);

            // Chercher le content après le rôle "assistant"
            String roleKey = "\"assistant\"";
            int roleIdx = jsonResponse.indexOf(roleKey);
            if (roleIdx < 0) roleIdx = 0;

            String searchKey1 = "\"content\":\"";
            String searchKey2 = "\"content\": \"";

            int start = -1;
            int idx1 = jsonResponse.indexOf(searchKey1, roleIdx);
            int idx2 = jsonResponse.indexOf(searchKey2, roleIdx);

            if (idx1 >= 0 && (idx2 < 0 || idx1 <= idx2)) {
                start = idx1 + searchKey1.length();
            } else if (idx2 >= 0) {
                start = idx2 + searchKey2.length();
            }

            if (start < 0) {
                System.err.println("Impossible de trouver 'content' dans la réponse OpenAI");
                return null;
            }

            StringBuilder text = new StringBuilder();
            int i = start;
            while (i < jsonResponse.length()) {
                char c = jsonResponse.charAt(i);
                if (c == '\\' && i + 1 < jsonResponse.length()) {
                    char next = jsonResponse.charAt(i + 1);
                    switch (next) {
                        case 'n' -> { text.append('\n'); i += 2; }
                        case 'r' -> { text.append('\r'); i += 2; }
                        case 't' -> { text.append('\t'); i += 2; }
                        case '"' -> { text.append('"');  i += 2; }
                        case '\\' -> { text.append('\\'); i += 2; }
                        default   -> { text.append(c);   i++; }
                    }
                } else if (c == '"') {
                    break;
                } else {
                    text.append(c);
                    i++;
                }
            }

            String result = text.toString().trim();
            System.out.println("Script extrait (" + result.length() + " chars)");
            return result.isEmpty() ? null : result;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Crée une vidéo avec avatar parlant via D-ID API.
     * Retourne un tableau [videoUrl, erreur].
     */
    private static String[] createVideoWithDID(String script, String apiKey) {
        try {
            int timeout = Integer.parseInt(config.getProperty("api.timeout", "30000"));
            String avatarUrl = config.getProperty("did.avatar.url",
                    "https://create-images-results.d-id.com/DefaultPresenters/Noelle_f/image.jpeg");
            String voiceId = config.getProperty("did.voice.id", "en-US-JennyNeural");

            // Limiter le script à 900 caractères (limite D-ID)
            String scriptTruncated = script.length() > 900 ? script.substring(0, 900) : script;

            // Payload D-ID - format correct selon la documentation
            String jsonPayload = "{"
                    + "\"source_url\":\"" + escapeJson(avatarUrl) + "\","
                    + "\"script\":{"
                    + "\"type\":\"text\","
                    + "\"input\":\"" + escapeJson(scriptTruncated) + "\","
                    + "\"provider\":{"
                    + "\"type\":\"microsoft\","
                    + "\"voice_id\":\"" + escapeJson(voiceId) + "\""
                    + "}"
                    + "}"
                    + "}";
            System.out.println("[D-ID] Payload envoyé : " + jsonPayload.substring(0, Math.min(300, jsonPayload.length())));

            URL url = new URL(DID_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + apiKey);
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 201) {
                StringBuilder errorMsg = new StringBuilder();
                var errStream = conn.getErrorStream();
                if (errStream != null) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) errorMsg.append(line);
                    }
                } else {
                    // Parfois D-ID met l'erreur dans le flux normal
                    var inStream = conn.getInputStream();
                    if (inStream != null) {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) errorMsg.append(line);
                        } catch (Exception ignored) {}
                    }
                }
                String detail = errorMsg.toString();
                System.err.println("[D-ID] Erreur " + responseCode + " : " + detail);
                return new String[]{null, "HTTP " + responseCode + " : " + detail};
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String videoUrl = parseDIDResponse(response.toString());
            return new String[]{videoUrl, null};

        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{null, e.getMessage()};
        }
    }

    /**
     * Construit le prompt pédagogique.
     */
    private static String buildEducationalPrompt(String description, String coursTitle, String niveau, String domaine) {
        return String.format("""
                Crée un script de vidéo éducative de 2-3 minutes pour expliquer le concept suivant :
                
                **Description à expliquer :** %s
                
                **Contexte :**
                - Cours : %s
                - Niveau : %s
                - Domaine : %s
                
                **Instructions :**
                1. Commence par une salutation chaleureuse ("Bonjour ! Aujourd'hui, nous allons découvrir...")
                2. Explique le concept de manière simple et claire
                3. Utilise des exemples concrets et des analogies
                4. Pose des questions rhétoriques pour maintenir l'engagement
                5. Termine par un résumé des points clés et un encouragement
                6. Adopte un ton conversationnel et bienveillant
                7. Limite le script à 300-400 mots maximum
                8. Écris à la première personne comme un professeur qui s'adresse à ses étudiants
                9. Utilise des transitions fluides entre les idées
                10. Évite le jargon technique complexe, privilégie la clarté
                
                **Format de réponse :** Retourne uniquement le script, sans formatage markdown, prêt à être lu par un avatar.
                """, description, coursTitle, niveau, domaine);
    }

    /**
     * Parse la réponse de l'API Gemini pour extraire le texte généré.
     * Format : candidates[0].content.parts[0].text
     */
    private static String parseGeminiResponse(String jsonResponse) {
        try {
            // Chercher "text":"..." dans la réponse Gemini
            String searchKey = "\"text\":\"";
            int start = jsonResponse.indexOf(searchKey);
            if (start < 0) {
                System.err.println("Réponse Gemini inattendue : " + jsonResponse);
                return null;
            }

            start += searchKey.length();
            // Trouver la fin en gérant les caractères échappés
            StringBuilder text = new StringBuilder();
            int i = start;
            while (i < jsonResponse.length()) {
                char c = jsonResponse.charAt(i);
                if (c == '\\' && i + 1 < jsonResponse.length()) {
                    char next = jsonResponse.charAt(i + 1);
                    switch (next) {
                        case 'n' -> { text.append('\n'); i += 2; }
                        case 'r' -> { text.append('\r'); i += 2; }
                        case 't' -> { text.append('\t'); i += 2; }
                        case '"' -> { text.append('"'); i += 2; }
                        case '\\' -> { text.append('\\'); i += 2; }
                        default -> { text.append(c); i++; }
                    }
                } else if (c == '"') {
                    break; // Fin du texte
                } else {
                    text.append(c);
                    i++;
                }
            }
            return text.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse la réponse de D-ID pour extraire l'URL/ID de la vidéo.
     */
    private static String parseDIDResponse(String jsonResponse) {
        try {
            String searchKey = "\"id\":\"";
            int start = jsonResponse.indexOf(searchKey);
            if (start < 0) return null;

            start += searchKey.length();
            int end = jsonResponse.indexOf("\"", start);
            if (end < 0) return null;

            String videoId = jsonResponse.substring(start, end);
            return "https://api.d-id.com/talks/" + videoId;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Échappe les caractères spéciaux pour JSON.
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Vérifie le statut d'une vidéo D-ID et récupère l'URL finale.
     */
    public static CompletableFuture<String> checkVideoStatus(String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String didKey = config.getProperty("did.api.key", "");
                if (didKey.isBlank()) return null;

                int timeout = Integer.parseInt(config.getProperty("api.timeout", "30000"));

                URL url = new URL("https://api.d-id.com/talks/" + videoId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Basic " + didKey);
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) return null;

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                String jsonResponse = response.toString();
                String searchKey = "\"result_url\":\"";
                int start = jsonResponse.indexOf(searchKey);
                if (start < 0) return null;

                start += searchKey.length();
                int end = jsonResponse.indexOf("\"", start);
                if (end < 0) return null;

                return jsonResponse.substring(start, end);

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Résultat de la génération de vidéo.
     */
    public static class VideoGenerationResult {
        private final boolean success;
        private final String videoUrl;
        private final String script;
        private final String errorMessage;

        private VideoGenerationResult(boolean success, String videoUrl, String script, String errorMessage) {
            this.success = success;
            this.videoUrl = videoUrl;
            this.script = script;
            this.errorMessage = errorMessage;
        }

        public static VideoGenerationResult success(String videoUrl, String script) {
            return new VideoGenerationResult(true, videoUrl, script, null);
        }

        public static VideoGenerationResult error(String errorMessage) {
            return new VideoGenerationResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getVideoUrl() { return videoUrl; }
        public String getScript() { return script; }
        public String getErrorMessage() { return errorMessage; }
    }
}