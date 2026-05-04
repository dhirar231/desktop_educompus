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
        // Créer un script plus naturel et pédagogique en fallback
        String intro = "Bonjour et bienvenue ! Aujourd'hui, nous allons explorer ensemble un sujet vraiment intéressant.";
        
        String explication = description.length() > 50 
            ? description 
            : "Nous allons découvrir les concepts fondamentaux de ce domaine et comprendre comment ils s'appliquent dans la pratique.";
        
        String exemples = "Pour mieux comprendre, prenons un exemple concret. Imaginez que... Cet exemple nous montre bien comment ces concepts s'appliquent dans la réalité.";
        
        String resume = "Récapitulons les points essentiels que nous avons vus aujourd'hui. D'abord, nous avons compris que... Ensuite, nous avons vu que... Et enfin, l'important à retenir c'est que...";
        
        String conclusion = "Voilà ! Vous avez maintenant une bonne compréhension de ce sujet. Je vous encourage à continuer votre apprentissage et à explorer davantage. Merci de votre attention, et à bientôt pour de nouvelles découvertes !";
        
        return String.format("""
            %s
            
            Aujourd'hui, nous parlons de : %s
            
            Ce sujet fait partie du cours "%s" et s'adresse aux étudiants de niveau %s dans le domaine %s.
            
            %s
            
            %s
            
            %s
            
            %s
            
            [Note: Script généré en mode fallback - Gemini temporairement indisponible]
            """,
            intro,
            description,
            coursTitle != null ? coursTitle : "Formation",
            niveau != null ? niveau : "général",
            domaine != null ? domaine : "éducation",
            explication,
            exemples,
            resume,
            conclusion
        );
    }

    /**
     * Construit le prompt pour Gemini.
     */
    private static String construirePrompt(String description, String coursTitle, String niveau, String domaine) {
        return String.format("""
            Tu es un expert en création de scripts pédagogiques pour vidéos éducatives. 
            Crée un script de vidéo explicative de 2-3 minutes (environ 350-450 mots) pour expliquer le concept suivant :
            
            ═══════════════════════════════════════════════════════════════
            CONTEXTE PÉDAGOGIQUE
            ═══════════════════════════════════════════════════════════════
            Sujet à expliquer : %s
            Cours : %s
            Niveau des étudiants : %s
            Domaine : %s
            
            ═══════════════════════════════════════════════════════════════
            INSTRUCTIONS POUR LE SCRIPT
            ═══════════════════════════════════════════════════════════════
            Le script DOIT :
            
            1. OUVERTURE (30 secondes)
               - Commencer par une salutation chaleureuse et engageante
               - Présenter le sujet de manière captivante
               - Expliquer pourquoi c'est important à apprendre
            
            2. EXPLICATION PRINCIPALE (1 minute 30 secondes)
               - Expliquer les concepts clés de manière progressive
               - Utiliser un langage simple et accessible
               - Inclure 2-3 exemples concrets et pertinents
               - Utiliser des analogies si nécessaire
               - Maintenir un ton conversationnel, comme si tu parlais à un ami
               - Éviter le jargon technique sauf si nécessaire (et l'expliquer)
            
            3. POINTS CLÉS À RETENIR (30 secondes)
               - Résumer les 3-4 points essentiels
               - Utiliser des formulations claires et mémorables
            
            4. CONCLUSION (30 secondes)
               - Encourager l'étudiant à continuer son apprentissage
               - Proposer une action suivante ou une question de réflexion
               - Terminer de manière positive et motivante
            
            ═══════════════════════════════════════════════════════════════
            CRITÈRES DE QUALITÉ
            ═══════════════════════════════════════════════════════════════
            - Le script doit être naturel et fluide, comme s'il était parlé
            - Utiliser des transitions douces entre les sections
            - Adapter le niveau de complexité au niveau %s
            - Inclure des pauses naturelles (indiquées par "...")
            - Être engageant et motivant
            - Éviter les listes à puces ou le formatage markdown
            - Écrire en français naturel et conversationnel
            
            ═══════════════════════════════════════════════════════════════
            FORMAT DE SORTIE
            ═══════════════════════════════════════════════════════════════
            Écris UNIQUEMENT le script, sans aucun formatage markdown, sans numérotation, 
            sans titre, sans explications supplémentaires. 
            Le script doit être prêt à être lu directement par un narrateur.
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