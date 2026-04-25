package com.educompus.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenAiMinimalRunner {
    public static void main(String[] args) {
        String subject = args != null && args.length > 0 ? String.join(" ", args) : "Test sujet génération IA";
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv("OPENAI_MODEL");
        if (apiKey == null || apiKey.isBlank()) {
            java.nio.file.Path envFile = java.nio.file.Path.of(System.getProperty("user.dir"), ".env");
            if (java.nio.file.Files.exists(envFile)) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(envFile, java.nio.charset.StandardCharsets.UTF_8)) {
                        line = line.trim();
                        if (line.startsWith("#") || line.isBlank()) continue;
                        int idx = line.indexOf('=');
                        if (idx <= 0) continue;
                        String k = line.substring(0, idx).trim();
                        String v = line.substring(idx + 1).trim();
                        if (k.equals("OPENAI_API_KEY") && (apiKey == null || apiKey.isBlank())) {
                            apiKey = trimQuotes(v);
                        }
                        if (k.equals("OPENAI_MODEL") && (model == null || model.isBlank())) {
                            model = trimQuotes(v);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("OPENAI_API_KEY not set");
            System.exit(2);
        }
        if (model == null || model.isBlank()) model = "gpt-4o-mini";

        String prompt = "Rédige un message d'email professionnel et concis destiné à des étudiants, en se basant sur le sujet suivant: \"" + subject + "\". Utilise un ton clair et pédagogique.";
        String payload = "{\"model\":\"" + escapeJson(model) + "\",\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}],\"max_tokens\":400}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.err.println("OpenAI API error: " + resp.statusCode());
                System.err.println(resp.body());
                System.exit(3);
            }
            String body = resp.body();
            String content = extractContent(body);
            if (content == null) content = body;
            Path out = Path.of("target", "openai_result.txt");
            Files.createDirectories(out.getParent());
            Files.writeString(out, content, StandardCharsets.UTF_8);
            System.out.println("OPENAI_RESULT_WRITTEN");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(4);
        }
    }

    private static String trimQuotes(String value) {
        if (value == null) return null;
        value = value.trim();
        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractContent(String json) {
        // Very small heuristic: find first "content": "..."
        String key = "\"content\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int start = json.indexOf('"', colon);
        if (start < 0) return null;
        start++;
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                if (n == 'n') { sb.append('\n'); i++; continue; }
                if (n == '"') { sb.append('"'); i++; continue; }
                if (n == '\\') { sb.append('\\'); i++; continue; }
            }
            sb.append(c);
        }
        return sb.toString().trim();
    }
}
