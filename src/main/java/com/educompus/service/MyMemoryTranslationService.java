package com.educompus.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client léger pour l'API gratuite MyMemory (https://mymemory.translated.net).
 * Aucune clé API requise pour un usage limité (1000 mots/jour).
 * Format : GET https://api.mymemory.translated.net/get?q=TEXT&langpair=fr|en
 */
public final class MyMemoryTranslationService {

    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private static final int TIMEOUT_MS = 8000;

    private MyMemoryTranslationService() {}

    /**
     * Traduit un texte.
     *
     * @param text     texte source (max ~500 chars recommandé par appel)
     * @param fromLang code langue source ex: "fr"
     * @param toLang   code langue cible  ex: "en"
     * @return texte traduit, ou le texte original en cas d'erreur
     */
    public static String translate(String text, String fromLang, String toLang) {
        if (text == null || text.isBlank()) return text;
        if (fromLang.equalsIgnoreCase(toLang)) return text;

        try {
            String encoded = URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
            String langPair = URLEncoder.encode(fromLang + "|" + toLang, StandardCharsets.UTF_8);
            String urlStr = API_URL + "?q=" + encoded + "&langpair=" + langPair;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) return text;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            return parseTranslatedText(sb.toString(), text);

        } catch (Exception e) {
            // En cas d'erreur réseau, retourner le texte original
            return text;
        }
    }

    /**
     * Extrait la valeur de "translatedText" du JSON MyMemory sans dépendance externe.
     * Réponse type: {"responseData":{"translatedText":"Bonjour"},...}
     */
    private static String parseTranslatedText(String json, String fallback) {
        if (json == null || json.isBlank()) return fallback;
        // Chercher "translatedText":"..."
        String key = "\"translatedText\":\"";
        int start = json.indexOf(key);
        if (start < 0) return fallback;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return fallback;
        String result = json.substring(start, end);
        // Décoder les séquences d'échappement JSON basiques
        result = result.replace("\\n", "\n")
                       .replace("\\t", "\t")
                       .replace("\\\"", "\"")
                       .replace("\\\\", "\\")
                       .replace("\\/", "/");
        return result.isBlank() ? fallback : result;
    }

    /**
     * Langues supportées avec leur libellé d'affichage.
     */
    public enum Language {
        FR("fr", "🇫🇷 Français"),
        EN("en", "🇬🇧 English"),
        AR("ar", "🇸🇦 العربية"),
        ES("es", "🇪🇸 Español"),
        DE("de", "🇩🇪 Deutsch"),
        IT("it", "🇮🇹 Italiano"),
        PT("pt", "🇵🇹 Português"),
        RU("ru", "🇷🇺 Русский"),
        ZH("zh-CN", "🇨🇳 中文"),
        JA("ja", "🇯🇵 日本語"),
        KO("ko", "🇰🇷 한국어"),
        TR("tr", "🇹🇷 Türkçe"),
        NL("nl", "🇳🇱 Nederlands"),
        PL("pl", "🇵🇱 Polski"),
        HI("hi", "🇮🇳 हिन्दी");

        public final String code;
        public final String label;

        Language(String code, String label) {
            this.code = code;
            this.label = label;
        }

        @Override
        public String toString() { return label; }
    }
}
