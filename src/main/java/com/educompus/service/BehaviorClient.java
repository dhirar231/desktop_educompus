package com.educompus.service;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BehaviorClient {
    private BehaviorClient() {}

    public static void sendReport(String endpoint, String apiKey, int examId, int userId, List<String> events) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("examId", examId);
        payload.put("userId", userId);
        payload.put("events", events);
        String json = new Gson().toJson(payload);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest.Builder reqb = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
        if (apiKey != null && !apiKey.isBlank()) reqb.header("Authorization", "Bearer " + apiKey);

        HttpResponse<String> resp = client.send(reqb.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new IOException("Behavior API returned " + resp.statusCode() + ": " + resp.body());
        }
    }
}
