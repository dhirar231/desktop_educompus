package com.educompus.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OpenAiClient {
    private static final Gson gson = new Gson();
    private static final HttpClient http = HttpClient.newHttpClient();

    public static String generateMessageFromSubject(String subject) throws IOException, InterruptedException {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv("OPENAI_MODEL");
        if (apiKey == null || apiKey.isBlank()) {
            var dotenv = loadDotenvConfig();
            apiKey = dotenv.get("OPENAI_API_KEY");
        }
        if (model == null || model.isBlank()) {
            var dotenv = loadDotenvConfig();
            model = firstNonBlank(System.getenv("OPENAI_MODEL"), dotenv.get("OPENAI_MODEL"));
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not set in environment or .env");
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        String prompt = "Rédige un message d'email professionnel et concis destiné à des étudiants, en se basant sur le sujet suivant: \"" + subject + "\". Utilise un ton clair et pédagogique.";
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        payload.add("messages", messages);
        payload.addProperty("max_tokens", 600);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        JsonArray choices = json.has("choices") ? json.getAsJsonArray("choices") : null;
        if (choices != null && choices.size() > 0) {
            JsonObject c0 = choices.get(0).getAsJsonObject();
            JsonObject message = c0.has("message") ? c0.getAsJsonObject("message") : null;
            if (message != null && message.has("content")) {
                return message.get("content").getAsString().trim();
            }
            // fallback for other shape
            JsonElement textEl = c0.get("text");
            if (textEl != null && !textEl.isJsonNull()) {
                return textEl.getAsString().trim();
            }
        }
        return "";
    }

    public static String chatCompletion(String prompt, int maxTokens) throws IOException, InterruptedException {
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv("OPENAI_MODEL");
        if (apiKey == null || apiKey.isBlank()) {
            var dotenv = loadDotenvConfig();
            apiKey = dotenv.get("OPENAI_API_KEY");
        }
        if (model == null || model.isBlank()) {
            var dotenv = loadDotenvConfig();
            model = firstNonBlank(System.getenv("OPENAI_MODEL"), dotenv.get("OPENAI_MODEL"));
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY not set in environment or .env");
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        payload.add("messages", messages);
        payload.addProperty("max_tokens", Math.max(128, Math.min(2000, maxTokens)));
        // encourage varied outputs
        payload.addProperty("temperature", 0.78);
        payload.addProperty("top_p", 0.95);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        JsonArray choices = json.has("choices") ? json.getAsJsonArray("choices") : null;
        if (choices != null && choices.size() > 0) {
            JsonObject c0 = choices.get(0).getAsJsonObject();
            JsonObject message = c0.has("message") ? c0.getAsJsonObject("message") : null;
            if (message != null && message.has("content")) {
                return message.get("content").getAsString().trim();
            }
            JsonElement textEl = c0.get("text");
            if (textEl != null && !textEl.isJsonNull()) {
                return textEl.getAsString().trim();
            }
        }
        return "";
    }

    private static java.util.Map<String, String> loadDotenvConfig() {
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>();
        java.io.File desktopDir = new java.io.File(System.getProperty("user.dir")).getAbsoluteFile();
        loadEnvFile(new java.io.File(desktopDir, ".env"), values);
        loadEnvFile(new java.io.File(desktopDir, ".env.local"), values);
        String appEnv = firstNonBlank(System.getenv("APP_ENV"), values.get("APP_ENV"), "dev");
        loadEnvFile(new java.io.File(desktopDir, ".env." + appEnv), values);
        loadEnvFile(new java.io.File(desktopDir, ".env." + appEnv + ".local"), values);
        return values;
    }

    private static void loadEnvFile(java.io.File file, java.util.Map<String, String> values) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(file.toPath())), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = safe(line);
                if (trimmed.isBlank() || trimmed.startsWith("#")) {
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = safe(trimmed.substring(0, idx));
                String value = trimQuotes(trimmed.substring(idx + 1).trim());
                if (!key.isBlank()) {
                    values.put(key, value);
                }
            }
        } catch (Exception ignored) {
            // ignore malformed or missing local env files
        }
    }

    private static String firstNonBlank(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimQuotes(String value) {
        String v = safe(value);
        if (v.length() >= 2) {
            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                return v.substring(1, v.length() - 1);
            }
        }
        return v;
    }
}
