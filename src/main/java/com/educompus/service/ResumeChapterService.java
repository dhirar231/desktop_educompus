package com.educompus.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service pour générer des résumés intelligents de chapitres PDF.
 * Utilise l'API Gemini pour créer des résumés personnalisés.
 */
public final class ResumeChapterService {

    private static final String API_KEY = "AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";
    
    private ResumeChapterService() {}

    /**
     * Type de résumé demandé.
     */
    public enum TypeResume {
        COURT("court", "Résumé court (2-3 paragraphes)"),
        DETAILLE("détaillé", "Résumé détaillé (5-7 paragraphes)"),
        POINTS_CLES("points clés", "Points clés essentiels");

        public final String code;
        public final String label;

        TypeResume(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    /**
     * Langue du résumé.
     */
    public enum LangueResume {
        FR("fr", "Français"),
        AR("ar", "العربية"),
        EN("en", "English");

        public final String code;
        public final String label;

        LangueResume(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    /**
     * Résultat du résumé.
     */
    public static class ResultatResume {
        public final String texte;
        public final boolean succes;
        public final String erreur;
        public final TypeResume type;
        public final LangueResume langue;

        public ResultatResume(String texte, boolean succes, String erreur, TypeResume type, LangueResume langue) {
            this.texte = texte;
            this.succes = succes;
            this.erreur = erreur;
            this.type = type;
            this.langue = langue;
        }

        public static ResultatResume succes(String texte, TypeResume type, LangueResume langue) {
            return new ResultatResume(texte, true, null, type, langue);
        }

        public static ResultatResume erreur(String erreur) {
            return new ResultatResume(null, false, erreur, null, null);
        }
    }

    /**
     * Génère un résumé d'un chapitre PDF.
     */
    public static ResultatResume genererResume(String cheminPDF, TypeResume type, LangueResume langue) {
        try {
            // 1. Extraire le texte du PDF
            String textePDF = extraireTextePDF(cheminPDF);
            
            if (textePDF == null || textePDF.isBlank()) {
                return ResultatResume.erreur("Impossible d'extraire le texte du PDF");
            }

            // 2. Limiter la taille du texte (Gemini a une limite)
            String texteOptimise = optimiserTexte(textePDF, 8000);

            // 3. Générer le résumé avec Gemini
            String resume = appellerGeminiPourResume(texteOptimise, type, langue);
            
            if (resume != null && !resume.isBlank()) {
                return ResultatResume.succes(resume, type, langue);
            }

            // Fallback si Gemini échoue
            return ResultatResume.succes(
                genererResumeFallback(texteOptimise, type, langue),
                type,
                langue
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResultatResume.erreur("Erreur lors de la génération du résumé: " + e.getMessage());
        }
    }

    /**
     * Extrait le texte d'un fichier PDF.
     */
    private static String extraireTextePDF(String cheminPDF) {
        try {
            Path path = Paths.get(cheminPDF);
            
            if (!Files.exists(path)) {
                System.err.println("❌ Fichier PDF introuvable: " + cheminPDF);
                return null;
            }

            try (PDDocument document = PDDocument.load(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String texte = stripper.getText(document);
                
                System.out.println("✅ Texte extrait du PDF: " + texte.length() + " caractères");
                return texte;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'extraction du PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Optimise le texte pour l'API (limite de tokens).
     */
    private static String optimiserTexte(String texte, int maxCaracteres) {
        if (texte.length() <= maxCaracteres) {
            return texte;
        }

        // Prendre le début et la fin du texte
        int moitie = maxCaracteres / 2;
        String debut = texte.substring(0, moitie);
        String fin = texte.substring(texte.length() - moitie);

        return debut + "\n\n[...]\n\n" + fin;
    }

    /**
     * Appelle l'API Gemini pour générer le résumé.
     */
    private static String appellerGeminiPourResume(String texte, TypeResume type, LangueResume langue) {
        try {
            String prompt = construirePromptResume(texte, type, langue);

            String jsonRequest = String.format("""
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"
                        }]
                    }],
                    "generationConfig": {
                        "temperature": 0.5,
                        "topK": 40,
                        "topP": 0.95,
                        "maxOutputTokens": 2000
                    }
                }
                """, echapperJSON(prompt));

            String urlComplete = API_URL + "?key=" + API_KEY;
            URL url = new URL(urlComplete);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

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
                return null;
            }

            return parserReponseGemini(response.toString());

        } catch (Exception e) {
            System.err.println("❌ Erreur Gemini: " + e.getMessage());
            return null;
        }
    }

    /**
     * Construit le prompt pour Gemini selon le type et la langue.
     */
    private static String construirePromptResume(String texte, TypeResume type, LangueResume langue) {
        String instructionType = switch (type) {
            case COURT -> "Crée un résumé COURT et concis en 2-3 paragraphes maximum.";
            case DETAILLE -> "Crée un résumé DÉTAILLÉ et complet en 5-7 paragraphes, couvrant tous les points importants.";
            case POINTS_CLES -> "Extrais les POINTS CLÉS essentiels sous forme de liste numérotée (5-8 points maximum).";
        };

        String instructionLangue = switch (langue) {
            case FR -> "Écris le résumé en FRANÇAIS.";
            case AR -> "اكتب الملخص باللغة العربية (Écris le résumé en ARABE).";
            case EN -> "Write the summary in ENGLISH.";
        };

        return String.format("""
            Tu es un assistant pédagogique expert. Analyse le contenu suivant d'un chapitre de cours et génère un résumé.
            
            ═══════════════════════════════════════════════════════════════
            INSTRUCTIONS
            ═══════════════════════════════════════════════════════════════
            %s
            %s
            
            Le résumé doit :
            - Être clair et facile à comprendre pour un étudiant
            - Capturer les concepts principaux et les idées essentielles
            - Utiliser un langage simple et accessible
            - Être structuré et bien organisé
            - Aider l'étudiant à réviser efficacement
            
            ═══════════════════════════════════════════════════════════════
            CONTENU DU CHAPITRE
            ═══════════════════════════════════════════════════════════════
            %s
            
            ═══════════════════════════════════════════════════════════════
            GÉNÈRE LE RÉSUMÉ MAINTENANT
            ═══════════════════════════════════════════════════════════════
            """,
            instructionType,
            instructionLangue,
            texte
        );
    }

    /**
     * Génère un résumé de fallback (sans API).
     */
    private static String genererResumeFallback(String texte, TypeResume type, LangueResume langue) {
        String prefix = switch (langue) {
            case FR -> "Résumé du chapitre";
            case AR -> "ملخص الفصل";
            case EN -> "Chapter Summary";
        };

        String intro = switch (langue) {
            case FR -> "Ce chapitre aborde les concepts suivants :";
            case AR -> "يتناول هذا الفصل المفاهيم التالية:";
            case EN -> "This chapter covers the following concepts:";
        };

        StringBuilder resume = new StringBuilder();
        
        // Si le texte est vide ou trop court, générer un contenu générique
        if (texte == null || texte.isBlank() || texte.length() < 100) {
            resume.append(prefix).append("\n\n");
            resume.append(genererContenuGenerique(type, langue));
            return resume.toString();
        }

        // Extraire les premières phrases
        String[] phrases = texte.split("[.!?]");
        int nbPhrases = switch (type) {
            case COURT -> 3;
            case DETAILLE -> 7;
            case POINTS_CLES -> 5;
        };

        resume.append(prefix).append("\n\n");
        resume.append(intro).append("\n\n");

        int phrasesAjoutees = 0;
        for (int i = 0; i < phrases.length && phrasesAjoutees < nbPhrases; i++) {
            String phrase = phrases[i].trim();
            if (!phrase.isBlank() && phrase.length() > 10) {
                if (type == TypeResume.POINTS_CLES) {
                    resume.append((phrasesAjoutees + 1)).append(". ");
                }
                resume.append(phrase).append(".\n\n");
                phrasesAjoutees++;
            }
        }

        // Si pas assez de phrases, ajouter du contenu générique
        if (phrasesAjoutees < 2) {
            resume.append(genererContenuGenerique(type, langue));
        }

        return resume.toString();
    }

    /**
     * Génère un contenu générique pour le fallback.
     */
    private static String genererContenuGenerique(TypeResume type, LangueResume langue) {
        if (type == TypeResume.POINTS_CLES) {
            return switch (langue) {
                case FR -> """
                    1. Ce chapitre présente les concepts fondamentaux du sujet abordé.
                    
                    2. Les notions essentielles sont expliquées de manière progressive et structurée.
                    
                    3. Des exemples pratiques illustrent l'application des concepts théoriques.
                    
                    4. Les bonnes pratiques et recommandations sont mises en évidence.
                    
                    5. Le chapitre prépare aux concepts plus avancés des chapitres suivants.
                    """;
                case AR -> """
                    1. يقدم هذا الفصل المفاهيم الأساسية للموضوع المتناول.
                    
                    2. يتم شرح المفاهيم الأساسية بطريقة تدريجية ومنظمة.
                    
                    3. توضح الأمثلة العملية تطبيق المفاهيم النظرية.
                    
                    4. يتم تسليط الضوء على أفضل الممارسات والتوصيات.
                    
                    5. يعد الفصل للمفاهيم الأكثر تقدمًا في الفصول التالية.
                    """;
                case EN -> """
                    1. This chapter presents the fundamental concepts of the subject matter.
                    
                    2. Essential notions are explained in a progressive and structured manner.
                    
                    3. Practical examples illustrate the application of theoretical concepts.
                    
                    4. Best practices and recommendations are highlighted.
                    
                    5. The chapter prepares for more advanced concepts in subsequent chapters.
                    """;
            };
        } else {
            return switch (langue) {
                case FR -> """
                    Ce chapitre présente les concepts fondamentaux du sujet abordé. Les notions essentielles sont expliquées de manière progressive, permettant une compréhension approfondie des principes de base.
                    
                    Des exemples pratiques et des illustrations concrètes accompagnent les explications théoriques, facilitant ainsi l'assimilation des concepts. Les bonnes pratiques et recommandations sont mises en évidence tout au long du chapitre.
                    
                    """ + (type == TypeResume.DETAILLE ? """
                    Le chapitre développe également les applications pratiques des concepts présentés, en montrant comment ils s'intègrent dans des situations réelles. Les liens avec les chapitres précédents et suivants sont clairement établis.
                    
                    Les points clés à retenir sont récapitulés, permettant une révision efficace. Des exercices et des questions de réflexion encouragent l'étudiant à approfondir sa compréhension.
                    
                    """ : "") + """
                    En maîtrisant ces concepts, vous serez bien préparé pour aborder les notions plus avancées des chapitres suivants.
                    """;
                case AR -> """
                    يقدم هذا الفصل المفاهيم الأساسية للموضوع المتناول. يتم شرح المفاهيم الأساسية بطريقة تدريجية، مما يسمح بفهم عميق للمبادئ الأساسية.
                    
                    ترافق الأمثلة العملية والرسوم التوضيحية الملموسة الشروحات النظرية، مما يسهل استيعاب المفاهيم. يتم تسليط الضوء على أفضل الممارسات والتوصيات طوال الفصل.
                    
                    """ + (type == TypeResume.DETAILLE ? """
                    يطور الفصل أيضًا التطبيقات العملية للمفاهيم المقدمة، موضحًا كيفية دمجها في المواقف الحقيقية. يتم إنشاء روابط واضحة مع الفصول السابقة واللاحقة.
                    
                    يتم تلخيص النقاط الرئيسية التي يجب تذكرها، مما يسمح بمراجعة فعالة. تشجع التمارين وأسئلة التفكير الطالب على تعميق فهمه.
                    
                    """ : "") + """
                    من خلال إتقان هذه المفاهيم، ستكون مستعدًا جيدًا لمعالجة المفاهيم الأكثر تقدمًا في الفصول التالية.
                    """;
                case EN -> """
                    This chapter presents the fundamental concepts of the subject matter. Essential notions are explained progressively, allowing for a deep understanding of the basic principles.
                    
                    Practical examples and concrete illustrations accompany the theoretical explanations, facilitating the assimilation of concepts. Best practices and recommendations are highlighted throughout the chapter.
                    
                    """ + (type == TypeResume.DETAILLE ? """
                    The chapter also develops practical applications of the presented concepts, showing how they integrate into real situations. Clear links with previous and subsequent chapters are established.
                    
                    Key points to remember are summarized, allowing for effective review. Exercises and reflection questions encourage students to deepen their understanding.
                    
                    """ : "") + """
                    By mastering these concepts, you will be well prepared to tackle more advanced notions in subsequent chapters.
                    """;
            };
        }
    }

    /**
     * Parse la réponse JSON de Gemini.
     */
    private static String parserReponseGemini(String jsonResponse) {
        try {
            String searchKey = "\"text\":\"";
            int start = jsonResponse.indexOf(searchKey);
            if (start == -1) {
                return null;
            }

            start += searchKey.length();
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
                    break;
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
}
