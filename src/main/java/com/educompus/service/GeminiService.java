package com.educompus.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service simplifié et robuste pour l'API Google Gemini.
 * Gère les erreurs HTTP et fournit des fallbacks.
 */
public final class GeminiService {

    private static final String API_KEY = "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    
    private GeminiService() {}

    /**
     * Génère un script éducatif avec Gemini.
     */
    public static String genererScript(String description, String coursTitle, String niveau, String domaine) {
        try {
            // Essayer d'abord avec Gemini
            String scriptGemini = appellerGeminiAPI(description, coursTitle, niveau, domaine);
            if (scriptGemini != null && !scriptGemini.isBlank()) {
                return scriptGemini;
            }
            
            System.out.println("⚠️ Gemini indisponible, utilisation du générateur de fallback");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur Gemini: " + e.getMessage());
        }
        
        // Fallback : générateur de script local
        return genererScriptFallback(description, coursTitle, niveau, domaine);
    }

    /**
     * Appelle l'API Gemini avec gestion d'erreurs robuste.
     */
    private static String appellerGeminiAPI(String description, String coursTitle, String niveau, String domaine) throws Exception {
        String prompt = construirePrompt(description, coursTitle, niveau, domaine);
        
        // Construire la requête JSON
        String jsonRequest = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.7,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 1000
                }
            }
            """, echapperJSON(prompt));

        // Construire l'URL avec la clé API
        String urlComplete = API_URL + "?key=" + API_KEY;
        
        URL url = new URL(urlComplete);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // Configuration de la connexion
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);

        // Envoyer la requête
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        
        // Lire la réponse
        StringBuilder response = new StringBuilder();
        InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();
            
        if (inputStream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        if (responseCode != 200) {
            System.err.println("Erreur Gemini HTTP " + responseCode + ": " + response.toString());
            throw new RuntimeException("Erreur HTTP " + responseCode + ": " + response.toString());
        }

        // Parser la réponse
        return parserReponseGemini(response.toString());
    }

    /**
     * Parse la réponse JSON de Gemini.
     */
    private static String parserReponseGemini(String jsonResponse) {
        try {
            // Chercher le texte dans la réponse Gemini
            String searchKey = "\"text\":\"";
            int start = jsonResponse.indexOf(searchKey);
            if (start == -1) {
                System.err.println("Format de réponse Gemini inattendu: " + jsonResponse);
                return null;
            }

            start += searchKey.length();
            
            // Extraire le texte en gérant les caractères échappés
            StringBuilder text = new StringBuilder();
            boolean escaped = false;
            
            for (int i = start; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                
                if (escaped) {
                    switch (c) {
                        case 'n' -> text.append('\n');
                        case 'r' -> text.append('\r');
                        case 't' -> text.append('\t');
                        case '"' -> text.append('"');
                        case '\\' -> text.append('\\');
                        default -> {
                            text.append('\\');
                            text.append(c);
                        }
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break; // Fin du texte
                } else {
                    text.append(c);
                }
            }
            
            String result = text.toString().trim();
            return result.isEmpty() ? null : result;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de la réponse Gemini: " + e.getMessage());
            return null;
        }
    }

    /**
     * Générateur de script de fallback (sans API).
     */
    private static String genererScriptFallback(String description, String coursTitle, String niveau, String domaine) {
        return String.format("""
            Bonjour et bienvenue dans cette vidéo éducative !
            
            Aujourd'hui, nous allons explorer le sujet suivant : %s
            
            Ce contenu fait partie du cours "%s" et s'adresse aux étudiants de niveau %s dans le domaine %s.
            
            Commençons par comprendre les concepts de base :
            
            %s
            
            Ce sujet est important car il vous permettra de mieux comprendre les concepts fondamentaux de votre formation.
            
            Prenons maintenant un exemple concret pour illustrer ces notions.
            
            En résumé, nous avons vu aujourd'hui les points essentiels concernant ce sujet.
            
            Ces connaissances vous seront utiles pour la suite de votre apprentissage.
            
            Merci de votre attention et à bientôt pour de nouvelles découvertes !
            
            [Note: Script généré en mode fallback - Gemini temporairement indisponible]
            """,
            description,
            coursTitle != null ? coursTitle : "Formation",
            niveau != null ? niveau : "général",
            domaine != null ? domaine : "éducation",
            description.length() > 100 ? 
                "Les éléments clés à retenir sont multiples et méritent une attention particulière." :
                "Nous allons détailler chaque aspect de manière progressive et claire."
        );
    }

    /**
     * Construit le prompt pour Gemini.
     */
    private static String construirePrompt(String description, String coursTitle, String niveau, String domaine) {
        return String.format("""
            Crée un script de vidéo éducative de 2-3 minutes pour expliquer le concept suivant :
            
            Sujet : %s
            Cours : %s
            Niveau : %s
            Domaine : %s
            
            Le script doit :
            - Commencer par une salutation chaleureuse
            - Expliquer le concept de manière simple et claire
            - Utiliser des exemples concrets
            - Maintenir un ton conversationnel et bienveillant
            - Se terminer par un résumé et des encouragements
            - Faire environ 300-400 mots
            - Être adapté à des étudiants de niveau %s
            
            Écris uniquement le script, sans formatage markdown.
            """, 
            description, 
            coursTitle != null ? coursTitle : "Formation générale",
            niveau != null ? niveau : "débutant", 
            domaine != null ? domaine : "éducation",
            niveau != null ? niveau : "débutant"
        );
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
     * Teste la connexion à l'API Gemini.
     */
    public static boolean testerConnexion() {
        try {
            String script = genererScript("Test de connexion", "Test", "Test", "Test");
            return script != null && !script.contains("[Note: Script généré en mode fallback");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtient le statut de l'API Gemini.
     */
    public static String obtenirStatut() {
        try {
            if (testerConnexion()) {
                return "✅ Gemini API opérationnelle";
            } else {
                return "⚠️ Gemini API indisponible - Mode fallback actif";
            }
        } catch (Exception e) {
            return "❌ Erreur lors du test Gemini: " + e.getMessage();
        }
    }
}