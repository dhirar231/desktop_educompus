package com.educompus.util;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Filtre de mots interdits multilingue (FR / EN / AR).
 * Détecte les variantes avec espaces, chiffres substitués (l33t speak),
 * répétitions de lettres et séparateurs.~committed
 */
public final class BadWordsFilter {

    private BadWordsFilter() {}

    // ── Liste des mots interdits ──────────────────────────────────────────────
    // FR = français, EN = anglais, AR = translittération arabe courante

    private static final List<String> BAD_WORDS = List.of(
        // ── Français ──
        "merde", "putain", "connard", "connasse", "salope", "enculé", "enculer",
        "fdp", "fils de pute", "va te faire foutre", "nique", "niquer", "baise",
        "baiser", "con", "conne", "couille", "couilles", "bite", "chatte",
        "pute", "putes", "bordel", "batard", "bâtard", "cul", "fesse",
        "pénis", "vagin", "branleur", "branler", "foutre", "merdique",
        "salopard", "ordure", "crétin", "idiot", "imbécile", "abruti",
        "débile", "taré", "tarée", "racaille", "bouffon",

        // ── Anglais ──
        "fuck", "fucking", "fucker", "shit", "bitch", "asshole", "bastard",
        "cunt", "dick", "cock", "pussy", "whore", "slut", "nigger", "nigga",
        "faggot", "retard", "moron", "idiot", "stupid", "dumbass", "ass",
        "piss", "crap", "damn", "hell", "wtf", "stfu", "kys",

        // ── Arabe translittéré (darija / arabe standard) ──
        "kess", "kes", "kos", "zebi", "zeb", "9ahba", "kahba", "sharmouta",
        "sharmuta", "ibn el sharmouta", "kol khara", "khara", "ayir",
        "air", "3ayir", "3air", "weld el kahba", "barra", "hmar", "hmir",
        "7mar", "7mir", "kelb", "kalb", "3ahre", "ahre", "nayek", "naika",
        "nayak", "manyak", "manyouk", "lbes", "3ars", "3arsa", "sharr",
        "yel3an", "yil3an", "la3na", "la3an", "ibn el 9ahba"
    );

    // Pré-compilation des patterns pour la performance
    private static final List<Pattern> PATTERNS = BAD_WORDS.stream()
            .map(BadWordsFilter::buildPattern)
            .collect(Collectors.toList());

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Retourne true si le texte contient un mot interdit.
     */
    public static boolean contientMotInterdit(String texte) {
        if (texte == null || texte.isBlank()) return false;
        String normalise = normaliser(texte);
        return PATTERNS.stream().anyMatch(p -> p.matcher(normalise).find());
    }

    /**
     * Retourne le premier mot interdit trouvé, ou null si aucun.
     */
    public static String premierMotInterdit(String texte) {
        if (texte == null || texte.isBlank()) return null;
        String normalise = normaliser(texte);
        for (int i = 0; i < PATTERNS.size(); i++) {
            if (PATTERNS.get(i).matcher(normalise).find()) return BAD_WORDS.get(i);
        }
        return null;
    }

    /**
     * Remplace les mots interdits par des astérisques (ex: f***).
     */
    public static String censurer(String texte) {
        if (texte == null) return null;
        String result = texte;
        for (int i = 0; i < PATTERNS.size(); i++) {
            String mot = BAD_WORDS.get(i);
            String censure = mot.charAt(0) + "*".repeat(Math.max(mot.length() - 1, 2));
            result = PATTERNS.get(i).matcher(normaliser(result)).replaceAll(censure);
        }
        return result;
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    /**
     * Normalise le texte : minuscules, suppression des accents,
     * remplacement des substitutions l33t (@ → a, 3 → e, 0 → o, etc.)
     */
    private static String normaliser(String texte) {
        String s = texte.toLowerCase();
        // Accents
        s = s.replace("é", "e").replace("è", "e").replace("ê", "e").replace("ë", "e")
             .replace("à", "a").replace("â", "a").replace("ä", "a")
             .replace("ù", "u").replace("û", "u").replace("ü", "u")
             .replace("î", "i").replace("ï", "i")
             .replace("ô", "o").replace("ö", "o")
             .replace("ç", "c").replace("ñ", "n");
        // L33t speak
        s = s.replace("@", "a").replace("4", "a")
             .replace("3", "e")
             .replace("1", "i").replace("!", "i")
             .replace("0", "o")
             .replace("5", "s").replace("$", "s")
             .replace("7", "t")
             .replace("+", "t")
             .replace("9", "q");
        return s;
    }

    /**
     * Construit un Pattern qui tolère les séparateurs entre les lettres
     * et les répétitions (ex: "f u c k", "fuuuck").
     */
    private static Pattern buildPattern(String mot) {
        StringBuilder sb = new StringBuilder();
        // Séparateur optionnel entre chaque lettre : espace, tiret, point, underscore
        String sep = "[\\s\\-_\\.]*";
        for (int i = 0; i < mot.length(); i++) {
            char c = mot.charAt(i);
            // Répétition possible de chaque lettre (fuuuck)
            sb.append(Pattern.quote(String.valueOf(c))).append("+");
            if (i < mot.length() - 1) sb.append(sep);
        }
        // Mot entier avec frontières souples (pas \b car ça ne marche pas avec les accents)
        return Pattern.compile("(?<![a-z])" + sb + "(?![a-z])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
