package com.educompus.service;

import com.educompus.model.PlacementQuestion;
import com.educompus.util.OpenAiClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class PlacementGenerator {
    private PlacementGenerator() {}

    public static List<PlacementQuestion> generate(int englishCount, int frenchCount) {
        // Try AI-based generation first (if API key available), fall back to static templates on any failure
        try {
            String prompt = buildAiPrompt(englishCount, frenchCount);
            String resp = OpenAiClient.chatCompletion(prompt, 1200);
            List<PlacementQuestion> ai = parseAiResponse(resp, englishCount, frenchCount);
            if (ai != null && !ai.isEmpty()) {
                return ai;
            }
        } catch (IOException | InterruptedException ignored) {
        }

        List<PlacementQuestion> out = new ArrayList<>();
        out.addAll(generateEnglish(englishCount));
        out.addAll(generateFrench(frenchCount));
        return out;
    }

    private static String buildAiPrompt(int englishCount, int frenchCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a JSON array of multiple-choice placement questions. ");
        sb.append("Each item must be an object with keys: 'lang' ('en' or 'fr'), 'question' (string), 'choices' (array of 4 strings), and 'correct' (index 0-3 pointing to the correct choice). ");
        sb.append("Do not include any explanatory text, only output the JSON array. ");
        sb.append("Produce exactly ").append(englishCount + frenchCount).append(" questions. ");
        sb.append("The first ").append(englishCount).append(" questions should be English (lang='en'), the remaining ").append(frenchCount).append(" should be French (lang='fr').\n");
        sb.append("Keep language level simple (A1-A2) and vary grammar points and vocabulary. Make answers plausible.\n");
        // Add a small random seed hint to encourage varied outputs across calls
        int seed = new Random().nextInt(1_000_000);
        sb.append("Randomize: seed=").append(seed).append(". Avoid repeating previous templates exactly.\n");
        return sb.toString();
    }

    private static List<PlacementQuestion> parseAiResponse(String resp, int englishCount, int frenchCount) {
        if (resp == null || resp.isBlank()) return null;
        String json = extractJsonArray(resp);
        if (json == null) return null;
        try {
            Gson gson = new Gson();
            JsonElement el = gson.fromJson(json, JsonElement.class);
            if (el == null || !el.isJsonArray()) return null;
            JsonArray arr = el.getAsJsonArray();
            List<PlacementQuestion> out = new ArrayList<>();
            for (JsonElement item : arr) {
                if (!item.isJsonObject()) continue;
                JsonObject o = item.getAsJsonObject();
                String lang = o.has("lang") ? o.get("lang").getAsString() : "en";
                String q = o.has("question") ? o.get("question").getAsString() : "";
                List<String> choices = new ArrayList<>();
                if (o.has("choices") && o.get("choices").isJsonArray()) {
                    for (JsonElement c : o.getAsJsonArray("choices")) {
                        choices.add(c.isJsonNull() ? "" : c.getAsString());
                    }
                }
                while (choices.size() < 4) choices.add("—");
                int correct = 0;
                if (o.has("correct")) {
                    try { correct = Math.max(0, Math.min(3, o.get("correct").getAsInt())); } catch (Exception ex) { correct = 0; }
                }
                out.add(new PlacementQuestion(lang, q, choices, correct));
            }
            if (!out.isEmpty()) return out;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String extractJsonArray(String s) {
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return null;
    }

    private static List<PlacementQuestion> generateEnglish(int n) {
        List<PlacementQuestion> list = new ArrayList<>();
        String[][] templates = new String[][]{
                {"I ___ to the store yesterday.", "went", "go", "gone", "will go"},
                {"She has ___ her homework.", "finished", "finish", "finishing", "finishes"},
                {"Choose the correct plural: mouse:", "mice", "mouses", "mousees", "mices"},
                {"What is the synonym of 'big'?:", "large", "small", "tiny", "weak"},
                {"Complete: They ___ playing now.", "are", "is", "was", "be"},
                {"Choose correct preposition: He arrived ___ noon.", "at", "in", "on", "by"},
                {"Which is a past tense form of 'run'?:", "ran", "runned", "running", "run"},
                {"Select the correct article: ___ apple a day keeps doctor away.", "An", "A", "The", ""},
                {"Which is an adjective?:", "happy", "happily", "to happy", "happen"},
                {"Choose correct question form: ___ you like coffee?", "Do", "Does", "Are", "Did"}
        };
        java.util.Random rand = new java.util.Random();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < templates.length; i++) order.add(i);
        Collections.shuffle(order, rand);
        for (int i = 0; i < n; i++) {
            String[] t = templates[order.get(i % order.size())];
            String q = t[0];
            List<String> choices = new ArrayList<>();
            for (int j = 1; j < t.length; j++) if (!t[j].isBlank()) choices.add(t[j]);
            // ensure 4 choices
            while (choices.size() < 4) choices.add("—");
            int correct = 0;
            // shuffle choices while remembering correct index
            String correctAnswer = choices.get(0);
            Collections.shuffle(choices, rand);
            for (int k = 0; k < choices.size(); k++) if (choices.get(k).equals(correctAnswer)) { correct = k; break; }
            list.add(new PlacementQuestion("en", q, choices, correct));
        }
        Collections.shuffle(list, rand);
        return list;
    }

    private static List<PlacementQuestion> generateFrench(int n) {
        List<PlacementQuestion> list = new ArrayList<>();
        String[][] templates = new String[][]{
                {"Je ___ au marche hier.", "suis allé", "vais", "allais", "ira"},
                {"Elle ___ son travail.", "a fini", "finira", "finissait", "finit"},
                {"Choisissez le pluriel: cheval:", "chevaux", "chevals", "chevales", "chevs"},
                {"Synonyme de 'petit' :", "minuscule", "grand", "rapide", "fort"},
                {"Compléter: Nous ___ en train de manger.", "sommes", "êtes", "est", "sont"},
                {"Préposition: Je vais ___ Paris.", "à", "en", "dans", "chez"},
                {"Passé de 'prendre' :", "pris", "prend", "prendra", "prendre"},
                {"Article correct: ___ orange est sucrée.", "L'", "Le", "La", "Un"},
                {"Quel mot est un adjectif?:", "bleu", "bleument", "bleuir", "bleuement"},
                {"Forme interrogative: ___-tu?", "Comment", "Pourquoi", "Quand", "Où"}
        };
        java.util.Random rand = new java.util.Random();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < templates.length; i++) order.add(i);
        Collections.shuffle(order, rand);
        for (int i = 0; i < n; i++) {
            String[] t = templates[order.get(i % order.size())];
            String q = t[0];
            List<String> choices = new ArrayList<>();
            for (int j = 1; j < t.length; j++) if (!t[j].isBlank()) choices.add(t[j]);
            while (choices.size() < 4) choices.add("—");
            int correct = 0;
            String correctAnswer = choices.get(0);
            Collections.shuffle(choices, rand);
            for (int k = 0; k < choices.size(); k++) if (choices.get(k).equals(correctAnswer)) { correct = k; break; }
            list.add(new PlacementQuestion("fr", q, choices, correct));
        }
        Collections.shuffle(list, rand);
        return list;
    }
}
