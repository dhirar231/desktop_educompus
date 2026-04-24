package com.educompus.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour extraire des mots-clés pertinents à partir du contenu d'un chapitre.
 * Utilise des techniques NLP basiques pour identifier les termes les plus importants.
 */
public final class KeywordExtractionService {

    private KeywordExtractionService() {}

    // Mots vides français à ignorer
    private static final Set<String> STOP_WORDS = Set.of(
        "le", "la", "les", "un", "une", "des", "du", "de", "et", "ou", "mais", "donc", "car", "ni", "or",
        "ce", "cette", "ces", "cet", "son", "sa", "ses", "mon", "ma", "mes", "ton", "ta", "tes", "notre", "nos", "votre", "vos", "leur", "leurs",
        "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "on", "qui", "que", "quoi", "dont", "où",
        "dans", "sur", "sous", "avec", "sans", "pour", "par", "vers", "chez", "depuis", "pendant", "avant", "après",
        "très", "plus", "moins", "aussi", "encore", "déjà", "toujours", "jamais", "souvent", "parfois",
        "est", "sont", "était", "étaient", "sera", "seront", "avoir", "être", "faire", "aller", "venir",
        "peut", "peuvent", "doit", "doivent", "veut", "veulent", "sait", "savent",
        "bien", "mal", "mieux", "pire", "beaucoup", "peu", "assez", "trop", "tout", "tous", "toute", "toutes",
        "si", "comme", "quand", "lorsque", "puisque", "parce", "afin", "pour", "que", "alors", "ainsi", "donc"
    );

    /**
     * Extrait les mots-clés les plus pertinents d'un texte.
     * 
     * @param titre Le titre du chapitre
     * @param description La description du chapitre
     * @param maxKeywords Nombre maximum de mots-clés à retourner (3-5 recommandé)
     * @return Liste des mots-clés triés par pertinence
     */
    public static List<String> extraireMotsCles(String titre, String description, int maxKeywords) {
        try {
            // Combiner titre et description
            String texteComplet = (titre != null ? titre : "") + " " + (description != null ? description : "");
            
            if (texteComplet.trim().isEmpty()) {
                return Arrays.asList("éducation", "apprentissage", "cours");
            }

            // Nettoyer et tokeniser le texte
            List<String> mots = tokeniser(texteComplet);
            
            // Calculer la fréquence des mots
            Map<String, Integer> frequences = calculerFrequences(mots);
            
            // Calculer les scores de pertinence
            Map<String, Double> scores = calculerScores(frequences, mots, titre);
            
            // Retourner les meilleurs mots-clés
            return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxKeywords)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            System.err.println("Erreur extraction mots-clés: " + e.getMessage());
            return Arrays.asList("éducation", "apprentissage", "cours", "formation");
        }
    }

    /**
     * Tokenise le texte en mots individuels.
     */
    private static List<String> tokeniser(String texte) {
        return Arrays.stream(texte.toLowerCase()
                .replaceAll("[^a-zA-ZÀ-ÿ\\s]", " ") // Garder seulement lettres et espaces
                .split("\\s+"))
                .filter(mot -> mot.length() >= 3) // Mots d'au moins 3 caractères
                .filter(mot -> !STOP_WORDS.contains(mot)) // Exclure les mots vides
                .filter(mot -> !mot.matches("\\d+")) // Exclure les nombres purs
                .collect(Collectors.toList());
    }

    /**
     * Calcule la fréquence d'apparition de chaque mot.
     */
    private static Map<String, Integer> calculerFrequences(List<String> mots) {
        Map<String, Integer> frequences = new HashMap<>();
        for (String mot : mots) {
            frequences.put(mot, frequences.getOrDefault(mot, 0) + 1);
        }
        return frequences;
    }

    /**
     * Calcule un score de pertinence pour chaque mot.
     * Prend en compte la fréquence, la position dans le titre, et la longueur du mot.
     */
    private static Map<String, Double> calculerScores(Map<String, Integer> frequences, List<String> mots, String titre) {
        Map<String, Double> scores = new HashMap<>();
        
        // Mots du titre (bonus de pertinence)
        Set<String> motsTitre = titre != null ? 
            Set.of(titre.toLowerCase().replaceAll("[^a-zA-ZÀ-ÿ\\s]", " ").split("\\s+")) : 
            Collections.emptySet();
        
        for (Map.Entry<String, Integer> entry : frequences.entrySet()) {
            String mot = entry.getKey();
            int frequence = entry.getValue();
            
            double score = frequence;
            
            // Bonus si le mot apparaît dans le titre
            if (motsTitre.contains(mot)) {
                score *= 2.0;
            }
            
            // Bonus pour les mots plus longs (souvent plus spécifiques)
            if (mot.length() >= 6) {
                score *= 1.5;
            }
            
            // Bonus pour les mots techniques/éducatifs
            if (estMotTechnique(mot)) {
                score *= 1.3;
            }
            
            scores.put(mot, score);
        }
        
        return scores;
    }

    /**
     * Détermine si un mot est technique/éducatif.
     */
    private static boolean estMotTechnique(String mot) {
        Set<String> motsTechniques = Set.of(
            "algorithme", "programmation", "développement", "conception", "architecture",
            "base", "données", "système", "réseau", "sécurité", "cryptographie",
            "mathématiques", "statistiques", "analyse", "modélisation", "optimisation",
            "intelligence", "artificielle", "apprentissage", "machine", "learning",
            "interface", "utilisateur", "expérience", "design", "ergonomie",
            "gestion", "projet", "méthode", "processus", "qualité", "test",
            "science", "recherche", "théorie", "pratique", "application",
            "technologie", "innovation", "numérique", "digital", "informatique"
        );
        
        return motsTechniques.contains(mot) || 
               motsTechniques.stream().anyMatch(tech -> mot.contains(tech) || tech.contains(mot));
    }

    /**
     * Extrait des mots-clés avec des paramètres par défaut optimisés.
     */
    public static List<String> extraireMotsCles(String titre, String description) {
        return extraireMotsCles(titre, description, 4);
    }

    /**
     * Génère des mots-clés alternatifs basés sur le domaine du cours.
     */
    public static List<String> genererMotsClesAlternatifs(String domaine, String niveau) {
        Map<String, List<String>> motsClesDomaine = Map.of(
            "informatique", Arrays.asList("programmation", "algorithme", "code", "développement", "logiciel"),
            "mathématiques", Arrays.asList("calcul", "équation", "géométrie", "statistiques", "analyse"),
            "sciences", Arrays.asList("expérience", "recherche", "théorie", "laboratoire", "découverte"),
            "gestion", Arrays.asList("management", "organisation", "stratégie", "leadership", "équipe"),
            "design", Arrays.asList("créativité", "esthétique", "interface", "utilisateur", "ergonomie"),
            "marketing", Arrays.asList("communication", "publicité", "marque", "client", "stratégie")
        );

        String domaineKey = domaine != null ? domaine.toLowerCase() : "général";
        List<String> motsBase = motsClesDomaine.getOrDefault(domaineKey, 
            Arrays.asList("éducation", "apprentissage", "formation", "connaissance"));

        // Ajouter des mots selon le niveau
        List<String> motsFinaux = new ArrayList<>(motsBase);
        if (niveau != null) {
            switch (niveau.toLowerCase()) {
                case "débutant":
                    motsFinaux.addAll(Arrays.asList("introduction", "bases", "fondamentaux"));
                    break;
                case "intermédiaire":
                    motsFinaux.addAll(Arrays.asList("pratique", "application", "méthode"));
                    break;
                case "avancé":
                    motsFinaux.addAll(Arrays.asList("expert", "maîtrise", "perfectionnement"));
                    break;
            }
        }

        return motsFinaux.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Valide et nettoie une liste de mots-clés.
     */
    public static List<String> validerMotsCles(List<String> motsCles) {
        return motsCles.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(mot -> !mot.isEmpty())
            .filter(mot -> mot.length() >= 3)
            .filter(mot -> mot.length() <= 20)
            .map(mot -> mot.toLowerCase())
            .distinct()
            .limit(5)
            .collect(Collectors.toList());
    }
}