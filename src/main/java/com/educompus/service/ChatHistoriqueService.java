package com.educompus.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance de l'historique du chatbot produit.
 * Sauvegarde dans ~/.educompus/chat/{userId}_{produitId}.json
 */
public class ChatHistoriqueService {

    private static final Path BASE_DIR = Path.of(
            System.getProperty("user.home"), ".educompus", "chat");

    public static class Message {
        public final String role;   // "client" ou "assistant"
        public final String texte;
        public final String heure;  // HH:mm

        public Message(String role, String texte, String heure) {
            this.role  = role;
            this.texte = texte;
            this.heure = heure;
        }
    }

    // ── Charger ───────────────────────────────────────────────────────────────

    public List<Message> charger(int userId, int produitId) {
        List<Message> liste = new ArrayList<>();
        Path fichier = fichier(userId, produitId);
        if (!Files.exists(fichier)) return liste;

        try {
            String json = Files.readString(fichier, StandardCharsets.UTF_8);
            // Parser manuellement le JSON simple (tableau de {role, texte, heure})
            String[] blocs = json.split("\\},\\s*\\{");
            for (String bloc : blocs) {
                String role  = extraire(bloc, "role");
                String texte = extraire(bloc, "texte");
                String heure = extraire(bloc, "heure");
                if (role != null && texte != null)
                    liste.add(new Message(role, texte, heure != null ? heure : ""));
            }
        } catch (Exception e) {
            System.err.println("[Chat] Erreur chargement historique : " + e.getMessage());
        }
        return liste;
    }

    // ── Sauvegarder ───────────────────────────────────────────────────────────

    public void sauvegarder(int userId, int produitId, List<Message> messages) {
        try {
            Files.createDirectories(BASE_DIR);
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < messages.size(); i++) {
                Message m = messages.get(i);
                sb.append("  {")
                  .append("\"role\":").append(jsonStr(m.role)).append(",")
                  .append("\"texte\":").append(jsonStr(m.texte)).append(",")
                  .append("\"heure\":").append(jsonStr(m.heure))
                  .append("}");
                if (i < messages.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(fichier(userId, produitId), sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[Chat] Erreur sauvegarde historique : " + e.getMessage());
        }
    }

    // ── Effacer ───────────────────────────────────────────────────────────────

    public void effacer(int userId, int produitId) {
        try { Files.deleteIfExists(fichier(userId, produitId)); }
        catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path fichier(int userId, int produitId) {
        return BASE_DIR.resolve(userId + "_" + produitId + ".json");
    }

    private String extraire(String bloc, String cle) {
        String pattern = "\"" + cle + "\":\"";
        int idx = bloc.indexOf(pattern);
        if (idx == -1) return null;
        int debut = idx + pattern.length();
        int fin   = bloc.indexOf("\"", debut);
        if (fin == -1) return null;
        return bloc.substring(debut, fin)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String jsonStr(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "") + "\"";
    }
}
